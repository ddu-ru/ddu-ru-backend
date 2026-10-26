package com.dduru.gildongmu.journey.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class JourneyPollClosedException extends BusinessException {
    public JourneyPollClosedException() {
        super(ErrorCode.JOURNEY_POLL_CLOSED);
    }
}
