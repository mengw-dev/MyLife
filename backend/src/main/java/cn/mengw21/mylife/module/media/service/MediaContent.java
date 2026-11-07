package cn.mengw21.mylife.module.media.service;

import org.springframework.core.io.Resource;

/**
 * 照片原图内容（鉴权校验通过后返回）
 *
 * @param resource     文件资源
 * @param mimeType     文件 MIME 类型
 * @param originalName 原始文件名
 */
public record MediaContent(Resource resource, String mimeType, String originalName) {
}