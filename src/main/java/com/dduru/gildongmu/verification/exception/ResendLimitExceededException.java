package com.dduru.gildongmu.verification.exception;

import com.dduru.gildongmu.common.exception.RetryableBusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class ResendLimitExceededException extends RetryableBusinessException {
    public ResendLimitExceededException() {
        this(null);
    }

    public ResendLimitExceededException(Long retryAfterSeconds) {
        super(ErrorCode.TOO_MANY_REQUESTS, retryAfterSeconds);
    }
}
