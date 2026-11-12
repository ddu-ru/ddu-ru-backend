package com.dduru.gildongmu.support.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class InvalidInquiryException extends BusinessException {

    public InvalidInquiryException() {
        super(ErrorCode.INVALID_INQUIRY);
    }
}
