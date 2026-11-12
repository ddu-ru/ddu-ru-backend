package com.dduru.gildongmu.support.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class InquiryAlreadyAnsweredException extends BusinessException {

    public InquiryAlreadyAnsweredException() {
        super(ErrorCode.INQUIRY_ALREADY_ANSWERED);
    }
}
