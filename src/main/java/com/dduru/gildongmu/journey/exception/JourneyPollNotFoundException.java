package com.dduru.gildongmu.journey.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class JourneyPollNotFoundException extends BusinessException {
    public JourneyPollNotFoundException() {
        super(ErrorCode.JOURNEY_POLL_NOT_FOUND);
    }
}
