package com.dduru.gildongmu.journey.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class JourneyPollOptionAdditionDeniedException extends BusinessException {
    public JourneyPollOptionAdditionDeniedException() {
        super(ErrorCode.JOURNEY_POLL_OPTION_ADDITION_DENIED);
    }
}
