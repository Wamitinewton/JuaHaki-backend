package com.juahaki.juahaki.infrastructure.messaging.storage.dto.firebase;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FirebaseFileDto {
    private String downloadUrl;
    private String storagePath;
}
