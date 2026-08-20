package com.settled.service;

import com.settled.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private Path root;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @PostConstruct
    void init() {
        root = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory", e);
        }
    }

    public StoredFile store(MultipartFile file, UUID claimId) {
        Path claimDir = root.resolve(claimId.toString());
        try {
            Files.createDirectories(claimDir);
            String storedName = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
            Path target = claimDir.resolve(storedName).normalize();
            if (!target.startsWith(root)) {
                throw new FileStorageException("Invalid file path");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(target.toString(), file.getOriginalFilename(), file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file " + file.getOriginalFilename(), e);
        }
    }

    public Path resolve(String storagePath) {
        Path path = Path.of(storagePath).normalize();
        if (!path.startsWith(root)) {
            throw new FileStorageException("Invalid storage path");
        }
        return path;
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "document";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record StoredFile(String storagePath, String originalName, String contentType, long size) {
    }
}