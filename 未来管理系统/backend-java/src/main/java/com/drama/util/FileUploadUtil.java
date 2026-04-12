package com.drama.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class FileUploadUtil {

    // ========== 文件类型白名单 ==========

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo"
    );

    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final List<String> DANGEROUS_EXTENSIONS = Arrays.asList(
            ".jsp", ".jspx", ".php", ".asp", ".aspx", ".exe", ".sh", ".bat",
            ".cmd", ".com", ".pif", ".scr", ".vbs", ".js", ".jar", ".war", ".htaccess"
    );

    // ========== 文件大小限制 ==========

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;       // 5MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024;    // 100MB
    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024;  // 10MB

    // ========== 文件头魔数 ==========

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] GIF_MAGIC = {0x47, 0x49, 0x46, 0x38};
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

    // ========== 验证方法 ==========

    public void validateImage(MultipartFile file) {
        validateNotEmpty(file);
        validateSize(file, MAX_IMAGE_SIZE, "图片");
        validateMimeType(file, ALLOWED_IMAGE_TYPES, "图片");
        validateExtension(file, ".jpg,.jpeg,.png,.gif,.webp", "图片");
        validateNoDangerousExtension(file);
        validateMagicBytes(file, "image", new byte[][]{JPEG_MAGIC, PNG_MAGIC, GIF_MAGIC});
        log.info("✅ 图片验证通过: filename={}, size={}, type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());
    }

    public void validateVideo(MultipartFile file) {
        validateNotEmpty(file);
        validateSize(file, MAX_VIDEO_SIZE, "视频");
        validateMimeType(file, ALLOWED_VIDEO_TYPES, "视频");
        validateExtension(file, ".mp4,.mpeg,.mov,.avi", "视频");
        validateNoDangerousExtension(file);
        log.info("✅ 视频验证通过: filename={}, size={}, type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());
    }

    public void validateDocument(MultipartFile file) {
        validateNotEmpty(file);
        validateSize(file, MAX_DOCUMENT_SIZE, "文档");
        validateMimeType(file, ALLOWED_DOCUMENT_TYPES, "文档");
        validateNoDangerousExtension(file);
        validateMagicBytes(file, "document", new byte[][]{PDF_MAGIC});
        log.info("✅ 文档验证通过: filename={}, size={}, type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());
    }

    // ========== 文件操作 ==========

    public String saveFile(MultipartFile file, String uploadDir) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建上传目录: " + uploadDir);
        }
        String extension = getFileExtension(file.getOriginalFilename());
        String newFilename = UUID.randomUUID().toString() + extension;
        Path filePath = Paths.get(uploadDir, newFilename);
        file.transferTo(filePath.toFile());
        log.info("✅ 文件保存成功: path={}", filePath);
        return newFilename;
    }

    public void deleteFile(String uploadDir, String filename) {
        try {
            Path filePath = Paths.get(uploadDir, filename);
            Files.deleteIfExists(filePath);
            log.info("✅ 文件删除成功: path={}", filePath);
        } catch (IOException e) {
            log.error("❌ 文件删除失败", e);
        }
    }

    // ========== 内部验证工具 ==========

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SecurityException("文件不能为空");
        }
    }

    private void validateSize(MultipartFile file, long maxSize, String typeName) {
        if (file.getSize() > maxSize) {
            throw new SecurityException(typeName + "大小不能超过 " + (maxSize / 1024 / 1024) + "MB");
        }
    }

    private void validateMimeType(MultipartFile file, List<String> allowed, String typeName) {
        String contentType = file.getContentType();
        if (contentType == null || !allowed.contains(contentType)) {
            throw new SecurityException("只允许上传 " + typeName + "类型: " + allowed);
        }
    }

    private void validateExtension(MultipartFile file, String allowed, String typeName) {
        String ext = getFileExtension(file.getOriginalFilename()).toLowerCase();
        String[] allowedList = allowed.split(",");
        for (String a : allowedList) {
            if (ext.equals(a.trim())) return;
        }
        throw new SecurityException("文件扩展名不合法，仅允许: " + allowed);
    }

    private void validateNoDangerousExtension(MultipartFile file) {
        String ext = getFileExtension(file.getOriginalFilename()).toLowerCase();
        for (String d : DANGEROUS_EXTENSIONS) {
            if (ext.equals(d)) {
                throw new SecurityException("禁止上传危险扩展名: " + ext);
            }
        }
    }

    private void validateMagicBytes(MultipartFile file, String typeName, byte[][] magicNumbers) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                throw new SecurityException("文件内容过短，无法验证类型");
            }
            boolean matched = false;
            for (byte[] magic : magicNumbers) {
                if (startsWith(header, magic)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new SecurityException("文件内容与扩展名不匹配，可能是伪造文件");
            }
        } catch (IOException e) {
            log.error("文件类型检测失败", e);
            throw new SecurityException("文件验证失败");
        }
    }

    private boolean startsWith(byte[] array, byte[] prefix) {
        if (array.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) return false;
        }
        return true;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}