package com.wizlit.file.exception;

import lombok.Getter;

@Getter
public class QuotaExceededException extends RuntimeException {

    private final Long usedBytes;
    private final Long maxBytes;
    private final Long requestedBytes;

    public QuotaExceededException(Long usedBytes, Long maxBytes, Long requestedBytes) {
        super(String.format("Monthly storage quota exceeded. Used: %d, Max: %d, Requested: %d", usedBytes, maxBytes, requestedBytes));
        this.usedBytes = usedBytes;
        this.maxBytes = maxBytes;
        this.requestedBytes = requestedBytes;
    }
}
