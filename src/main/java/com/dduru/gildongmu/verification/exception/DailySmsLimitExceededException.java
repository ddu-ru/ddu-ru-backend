package com.dduru.gildongmu.verification.exception;

import com.dduru.gildongmu.common.exception.RetryableBusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class DailySmsLimitExceededException extends RetryableBusinessException {
    public DailySmsLimitExceededException() {
        this(null);
    }

    public DailySmsLimitExceededException(Long retryAfterSeconds) {
        super(ErrorCode.DAILY_SMS_LIMIT_EXCEEDED, retryAfterSeconds);
    }
}
