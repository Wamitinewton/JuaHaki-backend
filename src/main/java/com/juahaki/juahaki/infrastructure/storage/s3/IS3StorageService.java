package com.juahaki.juahaki.infrastructure.storage.s3;

import com.juahaki.juahaki.infrastructure.storage.dto.s3.S3FileDto;
import com.juahaki.juahaki.infrastructure.storage.dto.s3.S3UploadRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

public interface IS3StorageService {

    /**
     * Upload file to S3 bucket
     *
     * @param file          the file to upload
     * @param uploadRequest upload configuration
     * @return S3FileDto containing file information
     */
    S3FileDto uploadFile(MultipartFile file, S3UploadRequest uploadRequest);

    /**
     * Delete file from S3 bucket
     *
     * @param s3Key the S3 key of the file to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteFile(String s3Key);

    /**
     * Generate presigned URL for file access
     *
     * @param s3Key      the S3 key of the file
     * @param expiration URL expiration duration
     * @return presigned URL string
     */
    String generatePresignedUrl(String s3Key, Duration expiration);

    /**
     * Check if file exists in S3 bucket
     *
     * @param s3Key the S3 key to check
     * @return true if file exists, false otherwise
     */
    boolean fileExists(String s3Key);

    /**
     * List all files in a specific folder
     *
     * @param folder the folder path to list files from
     * @return list of S3FileDto objects
     */
    List<S3FileDto> listFiles(String folder);

    /**
     * Get file size from S3
     *
     * @param s3Key the S3 key of the file
     * @return file size in bytes
     */
    long getFileSize(String s3Key);
}
