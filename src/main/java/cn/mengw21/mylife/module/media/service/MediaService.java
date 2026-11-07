package cn.mengw21.mylife.module.media.service;

import cn.mengw21.mylife.common.api.PageResponse;
import cn.mengw21.mylife.module.media.vo.MediaVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 照片服务（对齐《01-产品需求说明书》第 9 节 + 《02-API接口文档》第 9 节）
 * <p>
 * 业务规则：同一张照片可被多个记录引用；文件只保存一份；
 * 被引用的照片不允许直接删除（409）；私人照片必须鉴权访问（不允许公开静态 URL）。
 */
public interface MediaService {

    /**
     * 上传照片：校验真实文件类型（魔数）与大小，计算 SHA-256，保存到私有目录
     *
     * @param file 上传文件（multipart/form-data 字段 file）
     * @return 照片信息
     */
    MediaVO upload(MultipartFile file);

    /**
     * 照片列表（分页，按上传时间倒序）
     */
    PageResponse<MediaVO> page(long pageNo, long pageSize);

    /** 照片详情 */
    MediaVO getDetail(Long id);

    /**
     * 获取照片原图内容（登录 + 归属校验后流式返回）
     *
     * @return 文件资源（含 MIME 类型与原始文件名）
     */
    MediaContent getContent(Long id);

    /**
     * 删除照片：仍被引用的照片返回 409
     */
    void delete(Long id);
}