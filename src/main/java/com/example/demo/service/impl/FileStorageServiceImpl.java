package com.example.demo.service.impl;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.StoredFile;
import com.example.demo.repository.StoredFileRepository;
import com.example.demo.service.FileStorageService;

import lombok.RequiredArgsConstructor;

/**
 * Stores files as database blobs so they survive restarts on hosts with
 * ephemeral filesystems (Render, Fly, etc.).
 */
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final StoredFileRepository storedFileRepository;

    @Override
    public StoredFile storePdf(MultipartFile file) {
        String name = requireFileName(file);
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())
                || !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }
        return save(file, name, "application/pdf");
    }

    @Override
    public StoredFile storeImage(MultipartFile file) {
        String name = requireFileName(file);
        String contentType = file.getContentType();
        if (contentType == null || !IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPEG, PNG or WebP images are allowed.");
        }
        return save(file, name, contentType);
    }

    private String requireFileName(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a file to upload.");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new IllegalArgumentException("Uploaded file has no name.");
        }
        // strips any path segments a hostile client might send
        return StringUtils.getFilename(StringUtils.cleanPath(original));
    }

    private StoredFile save(MultipartFile file, String name, String contentType) {
        try {
            StoredFile stored = StoredFile.builder()
                    .fileName(name)
                    .contentType(contentType)
                    .data(file.getBytes())
                    .build();
            return storedFileRepository.save(stored);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
    }
}
