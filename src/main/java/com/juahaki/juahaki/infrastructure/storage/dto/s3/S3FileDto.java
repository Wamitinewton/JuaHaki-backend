package com.juahaki.juahaki.infrastructure.storage.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3FileDto {
    private String fileName;
    private String s3Key;
    private String contentType;
    private Long fileSize;
    private String publicUrl;
    private String presignedUrl;
    private String bucketName;
    private Instant lastModified;
}
