package com.foodfast.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class ComplaintEvidenceService {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path storageDirectory;

    public ComplaintEvidenceService(
            @Value("${foodfast.upload.complaint-dir:./runtime-data/complaint-evidence}") String directory
    ) {
        storageDirectory = Paths.get(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh bằng chứng");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Mỗi ảnh bằng chứng không được vượt quá 5 MB");
        }
        String contentType = detectContentType(file);
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP");
        }

        String fileName = UUID.randomUUID() + extension;
        Path destination = storageDirectory.resolve(fileName).normalize();
        if (!destination.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Tên tệp bằng chứng không hợp lệ");
        }
        try {
            Files.createDirectories(storageDirectory);
            file.transferTo(destination);
        } catch (IOException error) {
            throw new IllegalStateException("Không thể lưu ảnh bằng chứng. Vui lòng thử lại");
        }
        return "/api/complaints/evidence/" + fileName;
    }

    private String detectContentType(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (header.length >= 8
                    && (header[0] & 0xff) == 0x89
                    && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47
                    && header[4] == 0x0d && header[5] == 0x0a
                    && header[6] == 0x1a && header[7] == 0x0a) {
                return "image/png";
            }
            if (header.length >= 3
                    && (header[0] & 0xff) == 0xff
                    && (header[1] & 0xff) == 0xd8
                    && (header[2] & 0xff) == 0xff) {
                return "image/jpeg";
            }
            if (header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return "image/webp";
            }
        } catch (IOException error) {
            throw new IllegalArgumentException("Không thể đọc tệp ảnh bằng chứng");
        }
        return "";
    }

    public StoredEvidence load(String fileName) {
        if (!StringUtils.hasText(fileName)
                || !fileName.matches("[0-9a-fA-F-]+\\.(jpg|png|webp)")) {
            throw new IllegalArgumentException("Tệp bằng chứng không hợp lệ");
        }
        Path file = storageDirectory.resolve(fileName).normalize();
        if (!file.startsWith(storageDirectory) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Không tìm thấy ảnh bằng chứng");
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            String contentType = Files.probeContentType(file);
            return new StoredEvidence(resource, contentType == null ? "application/octet-stream" : contentType);
        } catch (IOException error) {
            throw new IllegalStateException("Không thể đọc ảnh bằng chứng");
        }
    }

    public record StoredEvidence(Resource resource, String contentType) {
    }
}
