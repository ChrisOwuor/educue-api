package com.owuor.educue.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.storage.local-root:./storage}")
    private String storageRoot;

    @Override
    public String store(MultipartFile file, String folder) {
        try {
            String originalName = StringUtils.cleanPath(
                    file.getOriginalFilename() != null
                            ? file.getOriginalFilename()
                            : "file"
            );

            String extension = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : "";

            String key = folder + "/" + UUID.randomUUID() + extension;

            Path targetPath = Paths.get(storageRoot, key);

            Files.createDirectories(targetPath.getParent());

            Files.copy(file.getInputStream(), targetPath);

            return key;

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }
    }

    @Override
    public String getAccessUrl(String key) {
        return "/files/" + key;
    }

    @Override
    public Resource load(String key) {
        try {
            Path path = Paths.get(storageRoot, key);

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found: " + key);
            }

            return resource;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load file: " + key,
                    e
            );
        }
    }

    @Override
    public String getContentType(String key) {
        try {
            Path path = Paths.get(storageRoot, key);

            String contentType =
                    Files.probeContentType(path);

            return contentType != null
                    ? contentType
                    : "application/octet-stream";

        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
}
