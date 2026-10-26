package com.dduru.gildongmu.journey.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class InvalidJourneyPollException extends BusinessException {
    private InvalidJourneyPollException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static InvalidJourneyPollException invalidText() {
        return new InvalidJourneyPollException(ErrorCode.JOURNEY_POLL_INVALID_TEXT);
    }

    public static InvalidJourneyPollException invalidOptions() {
        return new InvalidJourneyPollException(ErrorCode.JOURNEY_POLL_INVALID_OPTIONS);
    }

    public static InvalidJourneyPollException invalidClosingTime() {
        return new InvalidJourneyPollException(ErrorCode.JOURNEY_POLL_INVALID_CLOSING_TIME);
    }

    public static InvalidJourneyPollException invalidSelection() {
        return new InvalidJourneyPollException(ErrorCode.JOURNEY_POLL_INVALID_SELECTION);
    }
}
