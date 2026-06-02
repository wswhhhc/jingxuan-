package com.jingxuan.util;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.util.StrUtil;
import com.jingxuan.constant.CommonConstants;
import com.jingxuan.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件工具类 — 校验扩展名、生成文件名
 */
@Slf4j
public class FileUtil {

    private static final Set<String> ALLOWED_EXT_SET = Set.of(CommonConstants.ALLOWED_EXTENSIONS);
    private static final Set<String> FORBIDDEN_EXT_SET = Set.of(CommonConstants.FORBIDDEN_EXTENSIONS);

    private FileUtil() {}

    /**
     * 校验文件扩展名是否在白名单内
     */
    public static void validateExtension(String fileName) {
        String ext = getExtension(fileName);
        if (StrUtil.isBlank(ext)) {
            throw new BusinessException("文件无扩展名，不允许上传");
        }
        if (FORBIDDEN_EXT_SET.contains(ext.toLowerCase())) {
            throw new BusinessException("不允许上传 " + ext + " 格式的文件");
        }
        if (!ALLOWED_EXT_SET.contains(ext.toLowerCase())) {
            throw new BusinessException("不支持的文件格式: " + ext
                    + "，允许格式: " + String.join(", ", CommonConstants.ALLOWED_EXTENSIONS));
        }
    }

    /**
     * 生成唯一文件名，防止路径穿越
     */
    public static String generateFileName(String originalFileName) {
        String ext = getExtension(originalFileName);
        return UUID.randomUUID().toString().replace("-", "") + "." + ext;
    }

    /**
     * 获取文件扩展名（小写，不带点）
     */
    public static String getExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) return "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * 粗略校验 MIME 类型
     */
    public static boolean isAllowedMimeType(String mimeType) {
        if (StrUtil.isBlank(mimeType)) return false;
        return mimeType.startsWith("image/")
                || mimeType.startsWith("video/")
                || "application/zip".equals(mimeType)
                || "application/octet-stream".equals(mimeType)
                || "application/x-zip-compressed".equals(mimeType)
                || "application/x-rar-compressed".equals(mimeType)
                || "application/vnd.rar".equals(mimeType)
                || "application/x-7z-compressed".equals(mimeType)
                || "application/pdf".equals(mimeType);
    }
}
