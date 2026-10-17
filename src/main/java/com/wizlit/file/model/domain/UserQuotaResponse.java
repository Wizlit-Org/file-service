package com.wizlit.file.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserQuotaResponse {
    private Long userId;
    private String yearMonth;
    private Long usedBytes;
    private Long maxBytes;
    private Long remainingBytes;
}
