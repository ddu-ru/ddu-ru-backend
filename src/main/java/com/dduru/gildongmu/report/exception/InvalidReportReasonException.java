package com.dduru.gildongmu.report.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class InvalidReportReasonException extends BusinessException {

    public InvalidReportReasonException() {
        super(ErrorCode.INVALID_REPORT_REASON);
    }
}
