package com.juahaki.juahaki.service.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.juahaki.juahaki.dto.firebase.FirebaseFileDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FirebaseStorageService {

    @Value("${firebase.bucket-name}")
    private String bucketName;

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    private Storage storage;

    private void initializeFirebase() throws Exception {
        try {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new ClassPathResource(credentialsPath).getInputStream());

            storage = StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();

            log.info("Firebase storage initialized for bucket: {}", bucketName);
        } catch (Exception e) {
            log.error("Error initializing firebase storage", e);
            throw e;
        }
    }

    public FirebaseFileDto uploadFile(MultipartFile file, String fileName) throws Exception {
        String originalFilename = fileName != null ? fileName : file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;
        String storagePath = "juahaki/" + uniqueFileName;

        BlobId blobId = BlobId.of(bucketName, storagePath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        String downloadUrl = storage.signUrl(blobInfo, 10 * 365, TimeUnit.DAYS).toString();

        return new FirebaseFileDto(downloadUrl, storagePath);
    }

    public boolean deleteFile(String storagePath) {
        try {
            BlobId blobId = BlobId.of(bucketName, storagePath);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                log.info("Successfully deleted file from Firebase Storage: {}", storagePath);
            } else {
                log.warn("File not found in Firebase Storage: {}", storagePath);
            }
            return deleted;
        }catch (Exception e) {
            log.error("Error deleting file from Firebase Storage: {}", storagePath, e);
            return false;
        }
    }

    public List<FirebaseFileDto> getAllDocuments() {
        List<FirebaseFileDto> fileList = new ArrayList<>();

        try {
            Iterable<Blob> blobs = storage.list(
                    bucketName,
                    Storage.BlobListOption.prefix("juahaki/")
            )
                    .iterateAll();

            for (Blob blob : blobs) {
                if (!blob.isDirectory() && blob.getName().endsWith(".pdf")) {
                    String downloadUrl = storage.signUrl(
                            BlobInfo.newBuilder(bucketName, blob.getName()).build(),
                            10 * 365,
                            TimeUnit.DAYS
                    ).toString();
                    fileList.add(new FirebaseFileDto(downloadUrl, blob.getName()));
                }
            }
            log.info("Fetched {} PDF files from juahaki folder", fileList.size());
        } catch (Exception e) {
            log.error("Failed to fetch");
        }
        return fileList;
    }
}
