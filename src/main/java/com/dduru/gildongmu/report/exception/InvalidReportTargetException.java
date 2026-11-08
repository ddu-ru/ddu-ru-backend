package com.dduru.gildongmu.report.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class InvalidReportTargetException extends BusinessException {

    public InvalidReportTargetException() {
        super(ErrorCode.INVALID_REPORT_TARGET);
    }
}
