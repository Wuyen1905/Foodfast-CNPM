package com.foodfast.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComplaintEvidenceServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesAndLoadsSupportedImage() {
        ComplaintEvidenceService service = new ComplaintEvidenceService(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "evidence.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3}
        );

        String url = service.store(file);
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        assertThat(url).startsWith("/api/complaints/evidence/").endsWith(".png");
        assertThat(service.load(fileName).resource().exists()).isTrue();
    }

    @Test
    void rejectsNonImageUpload() {
        ComplaintEvidenceService service = new ComplaintEvidenceService(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "not an image".getBytes()
        );

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPG, PNG hoặc WEBP");
    }
}
