package com.vorzak.service;

import com.vorzak.security.HardenedDownloader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class EvidenceStorageService {

    private final Path storageDirectory;

    public EvidenceStorageService(@Value("${vorzak.storage.evidence-path:./data/evidence}") String storagePath) {
        this.storageDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize evidence storage directory: " + this.storageDirectory, e);
        }
    }

    public Path storeEvidence(byte[] data, String sha256) throws IOException {
        Path targetFile = storageDirectory.resolve(sha256);
        if (!Files.exists(targetFile)) {
            Files.write(targetFile, data, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
        return targetFile;
    }

    public byte[] loadEvidence(String sha256) throws IOException {
        Path targetFile = storageDirectory.resolve(sha256);
        if (!Files.exists(targetFile)) {
            throw new IOException("Evidence file not found on disk for hash: " + sha256);
        }
        return Files.readAllBytes(targetFile);
    }

    public boolean evidenceExists(String sha256) {
        return Files.exists(storageDirectory.resolve(sha256));
    }

    public Path getStorageDirectory() {
        return storageDirectory;
    }
}
