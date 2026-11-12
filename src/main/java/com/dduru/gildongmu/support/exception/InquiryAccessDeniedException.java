package com.dduru.gildongmu.support.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class InquiryAccessDeniedException extends BusinessException {

    public InquiryAccessDeniedException() {
        super(ErrorCode.INQUIRY_ACCESS_DENIED);
    }
}
