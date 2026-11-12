package com.dduru.gildongmu.support.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class InvalidInquiryAnswerException extends BusinessException {

    public InvalidInquiryAnswerException() {
        super(ErrorCode.INVALID_INQUIRY_ANSWER);
    }
}
