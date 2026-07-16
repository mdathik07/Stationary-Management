package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.StoredFile;

public interface FileStorageService {

    /** Validates and stores a PDF document; returns the persisted file. */
    StoredFile storePdf(MultipartFile file);

    /** Validates and stores a JPEG/PNG/WebP image; returns the persisted file. */
    StoredFile storeImage(MultipartFile file);
}
