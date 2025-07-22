package com.juahaki.juahaki.infrastructure.storage.mapper;

import com.juahaki.juahaki.infrastructure.storage.dto.s3.S3FileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.model.S3Object;

@Configuration
@RequiredArgsConstructor
public class S3Mapper {

    public S3FileDto mapToS3FileDto(S3Object s3Object, String publicUrl, String presignedUrl, String bucketName) {

        return S3FileDto.builder()
                .fileName(s3Object.key().substring(s3Object.key().lastIndexOf('/') + 1))
                .s3Key(s3Object.key())
                .fileSize(s3Object.size())
                .publicUrl(publicUrl)
                .presignedUrl(presignedUrl)
                .bucketName(bucketName)
                .lastModified(s3Object.lastModified())
                .build();
    }
}
