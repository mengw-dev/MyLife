package cn.mengw21.mylife.module.media.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 媒体文件存储服务（《04-项目开发规范》第 9 节 文件上传规范）
 * <ul>
 *     <li>文件名由服务端生成 UUID，不使用客户端文件名作为存储路径；</li>
 *     <li>文件按用户隔离保存在私有目录（非公开目录）；</li>
 *     <li>storageKey 为相对私有目录的路径，数据库只存元数据；</li>
 *     <li>数据库事务与文件系统不是同一个事务，删除失败只记日志不影响业务。</li>
 * </ul>
 */
@Service
public class MediaStorageService {

    /** 私有存储根目录 */
    private final Path rootDir;

    public MediaStorageService(@Value("${mylife.upload-dir:./uploads}") String uploadDir) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 保存上传文件（字节数组，便于先做签名校验 / SHA-256 计算）
     *
     * @param bytes  文件字节
     * @param userId 所属用户
     * @param ext    扩展名（已通过白名单校验，不含点）
     * @return storageKey（{userId}/{uuid}.{ext}）
     */
    public String store(byte[] bytes, Long userId, String ext) {
        String dir = userId.toString();
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dest = rootDir.resolve(dir).resolve(fileName).normalize();
        try {
            Files.createDirectories(dest.getParent());
            Files.write(dest, bytes);
        } catch (IOException e) {
            throw new IllegalStateException("文件保存失败", e);
        }
        return dir + "/" + fileName;
    }

    /**
     * 解析 storageKey 对应的绝对路径
     */
    public Path resolve(String storageKey) {
        return rootDir.resolve(storageKey).normalize();
    }

    /**
     * 静默删除文件（文件系统失败只记日志，不影响数据库事务）
     *
     * @return true 删除成功或文件本就不存在
     */
    public boolean deleteQuietly(String storageKey) {
        try {
            return Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            // 文件系统与数据库不是同一个事务：失败只记日志
            return false;
        }
    }
}