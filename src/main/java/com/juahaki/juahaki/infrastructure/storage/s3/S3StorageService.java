package com.juahaki.juahaki.infrastructure.storage.s3;

import com.juahaki.juahaki.infrastructure.storage.dto.s3.S3FileDto;
import com.juahaki.juahaki.infrastructure.storage.dto.s3.S3UploadRequest;
import com.juahaki.juahaki.infrastructure.storage.mapper.S3Mapper;
import com.juahaki.juahaki.shared.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService implements IS3StorageService {

    private final S3Client s3Client;
    private S3Presigner s3Presigner;
    private S3Mapper s3Mapper;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.accessKeyId}")
    private String awsAccessKeyId;

    @Value("${aws.secretKey}")
    private String awsSecretKey;

    @PostConstruct
    private void initializePresigner() {
        try {
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(awsAccessKeyId, awsSecretKey);

            s3Presigner = S3Presigner.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();

            log.info("S3 presigner initialized successfully for region: {}", awsRegion);
        } catch (Exception e) {
            log.error("Error initializing S3 presigner", e);
            throw new CustomException("Failed to initialize S3 storage service");
        }
    }

    @PreDestroy
    private void closePresigner() {
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }

    @Override
    public S3FileDto uploadFile(MultipartFile file, S3UploadRequest uploadRequest) {
        validateFile(file);
        validateUploadRequest(uploadRequest);

        try {
            String fileName = generateFileName(file.getOriginalFilename(), uploadRequest.getCustomFileName());
            String s3Key = buildS3Key(uploadRequest.getFolder(), fileName);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String publicUrl = generatePublicUrl(s3Key);
            String presignedUrl = generatePresignedUrl(s3Key, Duration.ofHours(24));

            log.info("Successfully uploaded file to S3: {}", s3Key);

            return S3FileDto.builder()
                    .fileName(fileName)
                    .s3Key(s3Key)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .publicUrl(publicUrl)
                    .presignedUrl(presignedUrl)
                    .bucketName(bucketName)
                    .build();

        } catch (IOException e) {
            log.error("Error reading file during upload: {}", e.getMessage(), e);
            throw new CustomException("Failed to read file for upload");
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage(), e);
            throw new CustomException("Failed to upload file to S3");
        }
    }

    @Override
    public boolean deleteFile(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            log.warn("Attempted to delete file with empty S3 key");
            return false;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted file from S3: {}", s3Key);
            return true;

        } catch (Exception e) {
            log.error("Error deleting file from S3: {}", s3Key, e);
            return false;
        }
    }

    @Override
    public String generatePresignedUrl(String s3Key, Duration expiration) {
        if (!StringUtils.hasText(s3Key)) {
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();

        } catch (Exception e) {
            log.error("Error generating presigned URL for S3 key: {}", s3Key, e);
            throw new CustomException("Failed to generate presigned URL");
        }
    }

    @Override
    public boolean fileExists(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            return false;
        }

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking if file exists in S3: {}", s3Key, e);
            return false;
        }
    }

    @Override
    public List<S3FileDto> listFiles(String folder) {
        try {
            String prefix = StringUtils.hasText(folder) ? folder + "/" : "";

            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);

            return listObjectsV2Response.contents().stream()
                    .filter(s3Object -> !s3Object.key().endsWith("/")) // exclude folder markers
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error listing files from S3 folder: {}", folder, e);
            throw new CustomException("Failed to list files from S3");
        }
    }

    @Override
    public long getFileSize(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }

        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
            return headObjectResponse.contentLength();
        } catch (Exception e) {
            log.error("Error getting file size from S3: {}", s3Key, e);
            throw new CustomException("Failed to get file size from S3");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (file.getSize() <= 0) {
            throw new IllegalArgumentException("File size must be greater than zero");
        }

        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new IllegalArgumentException("File must have a valid filename");
        }
    }

    private void validateUploadRequest(S3UploadRequest uploadRequest) {
        if (uploadRequest == null) {
            throw new IllegalArgumentException("Upload request cannot be null");
        }

        if (!StringUtils.hasText(uploadRequest.getFolder())) {
            throw new IllegalArgumentException("Folder path is required");
        }
    }

    private String generateFileName(String originalFileName, String customFileName) {
        if (StringUtils.hasText(customFileName)) {
            String extension = getFileExtension(originalFileName);
            return customFileName + ((StringUtils.hasText(extension) ? "." + extension : ""));
        }

        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String extension = getFileExtension(originalFileName);
        String baseName = getBaseName(originalFileName);

        return baseName + "_" + uuid + (StringUtils.hasText(extension) ? "." + extension : "");
    }

    private String buildS3Key(String folder, String fileName) {
        return folder.endsWith("/") ? folder + fileName : folder + "/" + fileName;
    }

    private String generatePublicUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, awsRegion, s3Key);
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    private String getBaseName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "file";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    private S3FileDto mapToDto(S3Object s3Object) {
        String publicUrl = generatePublicUrl(s3Object.key());
        String presignedUrl = generatePresignedUrl(s3Object.key(), Duration.ofHours(1));

        return s3Mapper.mapToS3FileDto(s3Object, publicUrl, presignedUrl, bucketName);
    }
}