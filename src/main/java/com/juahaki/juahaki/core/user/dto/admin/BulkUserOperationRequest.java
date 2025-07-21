package com.juahaki.juahaki.core.user.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkUserOperationRequest {
    private List<Long> userIds;
}
