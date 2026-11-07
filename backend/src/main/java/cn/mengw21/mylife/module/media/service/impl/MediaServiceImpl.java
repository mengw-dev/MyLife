package cn.mengw21.mylife.module.media.service.impl;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.common.exception.BusinessException;
import cn.mengw21.mylife.module.diary.entity.DiaryMedia;
import cn.mengw21.mylife.module.diary.mapper.DiaryMediaMapper;
import cn.mengw21.mylife.module.event.entity.EventMedia;
import cn.mengw21.mylife.module.event.mapper.EventMediaMapper;
import cn.mengw21.mylife.module.knowledge.note.entity.KnowledgeNoteMedia;
import cn.mengw21.mylife.module.knowledge.note.mapper.KnowledgeNoteMediaMapper;
import cn.mengw21.mylife.module.media.entity.MediaFile;
import cn.mengw21.mylife.module.media.mapper.MediaFileMapper;
import cn.mengw21.mylife.module.media.service.MediaContent;
import cn.mengw21.mylife.module.media.service.MediaService;
import cn.mengw21.mylife.module.media.service.MediaStorageService;
import cn.mengw21.mylife.module.media.vo.MediaVO;
import cn.mengw21.mylife.module.stage.entity.LifeStage;
import cn.mengw21.mylife.module.stage.entity.LifeStageMedia;
import cn.mengw21.mylife.module.stage.mapper.LifeStageMapper;
import cn.mengw21.mylife.module.stage.mapper.LifeStageMediaMapper;
import cn.mengw21.mylife.module.yearprofile.entity.YearProfile;
import cn.mengw21.mylife.module.yearprofile.mapper.YearProfileMapper;
import cn.mengw21.mylife.security.CurrentUser;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 照片服务实现（《04-项目开发规范》第 9 节 文件上传规范）
 * <ul>
 *     <li>白名单：JPEG / PNG / WebP / GIF，单张最大 10MB（multipart 限制兜底）；</li>
 *     <li>校验文件真实签名（魔数），不信任扩展名与客户端声明的 MIME；</li>
 *     <li>文件名由服务端生成 UUID，保存到按用户隔离的私有目录；</li>
 *     <li>删除前检查全部引用关系，被引用返回 409。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    /** 允许的扩展名白名单（PRD 9：JPEG、PNG、WebP、GIF） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    /** 单张最大体积（字节），与 spring.servlet.multipart.max-file-size 保持一致 */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /**
     * 扩展名 → 魔数前缀（文件真实签名校验）
     */
    private static final Map<String, byte[][]> MAGIC_BYTES = Map.of(
        "jpg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}},
        "jpeg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}},
        "png", new byte[][]{{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}},
        "gif", new byte[][]{{0x47, 0x49, 0x46, 0x38}},
        "webp", new byte[][]{{0x52, 0x49, 0x46, 0x46}}
    );

    /** WebP：RIFF 头 + 第 8-11 字节为 WEBP 标识 */
    private static final byte[] WEBP_MARK = {0x57, 0x45, 0x42, 0x50};

    private final MediaFileMapper mediaFileMapper;
    private final MediaStorageService mediaStorageService;
    private final DiaryMediaMapper diaryMediaMapper;
    private final LifeStageMediaMapper lifeStageMediaMapper;
    private final KnowledgeNoteMediaMapper knowledgeNoteMediaMapper;
    private final EventMediaMapper eventMediaMapper;
    private final LifeStageMapper lifeStageMapper;
    private final YearProfileMapper yearProfileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaVO upload(MultipartFile file) {
        Long userId = requireLogin();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ApiResponse.PARAM_ERROR, "请选择要上传的文件");
        }

        // 1. 扩展名白名单
        String ext = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ApiResponse.UNSUPPORTED_MEDIA_TYPE, "不支持的文件类型（仅支持 JPEG / PNG / WebP / GIF）");
        }

        // 2. 读取内容并做真实签名校验
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ApiResponse.PARAM_ERROR, "文件读取失败");
        }
        if (bytes.length == 0) {
            throw new BusinessException(ApiResponse.PARAM_ERROR, "文件内容为空");
        }
        if (bytes.length > MAX_FILE_SIZE) {
            throw new BusinessException(ApiResponse.PAYLOAD_TOO_LARGE, "文件大小超过限制（最大 10MB）");
        }
        if (!matchesMagic(ext, bytes)) {
            // 扩展名与真实内容不符，拒绝伪装文件
            throw new BusinessException(ApiResponse.UNSUPPORTED_MEDIA_TYPE, "文件内容与扩展名不符，已拒绝上传");
        }

        // 3. 归一出 MIME（以真实签名判定，不信任客户端 Content-Type）
        String mimeType = resolveMimeType(ext);

        // 4. 落盘（私有目录，服务端 UUID 文件名）
        String storageKey = mediaStorageService.store(bytes, userId, ext);

        // 5. 写入元数据
        MediaFile media = new MediaFile();
        media.setUserId(userId);
        media.setStorageKey(storageKey);
        media.setOriginalName(file.getOriginalFilename());
        media.setMimeType(mimeType);
        media.setSizeBytes((long) bytes.length);
        media.setSha256(sha256(bytes));
        extractImageSize(bytes, ext, media);
        mediaFileMapper.insert(media);

        return MediaVO.from(media);
    }

    @Override
    public PageResponse<MediaVO> page(long pageNo, long pageSize) {
        Long userId = requireLogin();
        IPage<MediaFile> result = mediaFileMapper.selectPage(
            new Page<>(pageNo, pageSize),
            Wrappers.<MediaFile>lambdaQuery()
                .eq(MediaFile::getUserId, userId)
                .orderByDesc(MediaFile::getCreatedAt)
                .orderByDesc(MediaFile::getId)
        );
        return PageResponse.of(result, MediaVO::from);
    }

    @Override
    public MediaVO getDetail(Long id) {
        MediaFile media = requireOwned(id);
        return MediaVO.from(media);
    }

    @Override
    public MediaContent getContent(Long id) {
        MediaFile media = requireOwned(id);
        Path path = mediaStorageService.resolve(media.getStorageKey());
        if (!path.toFile().exists()) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "照片文件不存在");
        }
        return new MediaContent(new FileSystemResource(path), media.getMimeType(), media.getOriginalName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MediaFile media = requireOwned(id);

        // 引用检查：任一引用存在即拒绝删除（409）
        long refCount = diaryMediaMapper.selectCount(
                Wrappers.<DiaryMedia>lambdaQuery().eq(DiaryMedia::getMediaId, id))
            + lifeStageMediaMapper.selectCount(
                Wrappers.<LifeStageMedia>lambdaQuery().eq(LifeStageMedia::getMediaId, id))
            + knowledgeNoteMediaMapper.selectCount(
                Wrappers.<KnowledgeNoteMedia>lambdaQuery().eq(KnowledgeNoteMedia::getMediaId, id))
            + eventMediaMapper.selectCount(
                Wrappers.<EventMedia>lambdaQuery().eq(EventMedia::getMediaId, id))
            + lifeStageMapper.selectCount(
                Wrappers.<LifeStage>lambdaQuery().eq(LifeStage::getCoverMediaId, id))
            + yearProfileMapper.selectCount(
                Wrappers.<YearProfile>lambdaQuery().eq(YearProfile::getCoverMediaId, id));
        if (refCount > 0) {
            throw new BusinessException(ApiResponse.CONFLICT, "照片仍被引用，无法删除");
        }

        // 先删数据库记录，再尽力删除物理文件（文件系统失败只记日志）
        mediaFileMapper.deleteById(media.getId());
        if (!mediaStorageService.deleteQuietly(media.getStorageKey())) {
            log.warn("照片删除：物理文件删除失败，storageKey={}", media.getStorageKey());
        }
    }

    // ---- 内部辅助 ----

    private Long requireLogin() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ApiResponse.UNAUTHORIZED, "未登录或登录已过期");
        }
        return userId;
    }

    /**
     * 校验照片存在且属于当前用户（跨用户 → 404）
     */
    private MediaFile requireOwned(Long id) {
        Long userId = requireLogin();
        MediaFile media = mediaFileMapper.selectOne(
            Wrappers.<MediaFile>lambdaQuery()
                .eq(MediaFile::getId, id)
                .eq(MediaFile::getUserId, userId)
        );
        if (media == null) {
            throw new BusinessException(ApiResponse.NOT_FOUND, "照片不存在");
        }
        return media;
    }

    /**
     * 从原始文件名解析扩展名（小写），无法解析时返回空串
     */
    private String resolveExtension(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 魔数校验：文件头必须匹配扩展名对应的真实签名
     */
    private boolean matchesMagic(String ext, byte[] bytes) {
        byte[][] signatures = MAGIC_BYTES.get(ext);
        if (signatures == null) {
            return false;
        }
        for (byte[] signature : signatures) {
            if (startsWith(bytes, signature)) {
                // WebP 额外校验第 8-11 字节的 WEBP 标识
                if ("webp".equals(ext)) {
                    return bytes.length > 11 && startsWith(bytes, 8, WEBP_MARK);
                }
                return true;
            }
        }
        return false;
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        return startsWith(bytes, 0, prefix);
    }

    private boolean startsWith(byte[] bytes, int offset, byte[] prefix) {
        if (bytes.length - offset < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 按扩展名归一出服务端认可的 MIME 类型
     */
    private String resolveMimeType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    /**
     * SHA-256 十六进制摘要
     */
    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 解析图片宽高（Java 内置 ImageIO 不支持 WebP，解析失败时置 null 不影响入库）
     */
    private void extractImageSize(byte[] bytes, String ext, MediaFile media) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(in);
            if (image != null) {
                media.setWidth(image.getWidth());
                media.setHeight(image.getHeight());
            }
        } catch (IOException e) {
            // 宽高只是附加信息，解析失败不阻断上传
            log.debug("图片尺寸解析失败，extension={}", ext);
        }
    }
}