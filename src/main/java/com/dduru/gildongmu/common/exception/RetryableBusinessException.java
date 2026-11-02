package com.dduru.gildongmu.common.exception;

import lombok.Getter;

@Getter
public abstract class RetryableBusinessException extends BusinessException {
    private final Long retryAfterSeconds;

    protected RetryableBusinessException(ErrorCode errorCode, Long retryAfterSeconds) {
        super(errorCode);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
