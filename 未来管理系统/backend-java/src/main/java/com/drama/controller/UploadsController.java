package com.drama.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/uploads")
public class UploadsController {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> get(@PathVariable String filename) {
        if (!filename.matches("^[a-zA-Z0-9._-]+$")) {
            return ResponseEntity.notFound().build();
        }
        Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
        Path f = dir.resolve(filename).normalize();
        if (!f.startsWith(dir) || !Files.isRegularFile(f)) {
            return ResponseEntity.notFound().build();
        }
        try {
            String probe = Files.probeContentType(f);
            MediaType mediaType =
                    probe != null ? MediaType.parseMediaType(probe) : MediaType.APPLICATION_OCTET_STREAM;
            FileSystemResource res = new FileSystemResource(f.toFile());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(mediaType)
                    .body(res);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
