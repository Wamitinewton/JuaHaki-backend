package com.juahaki.juahaki.infrastructure.storage.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3UploadRequest {

    private String folder;
    private String customFileName;

    public static S3UploadRequest of(String folder) {
        return S3UploadRequest.builder()
                .folder(folder)
                .build();
    }

    public static S3UploadRequest of(String folder, String customFileName) {
        return S3UploadRequest.builder()
                .folder(folder)
                .customFileName(customFileName)
                .build();
    }
}
