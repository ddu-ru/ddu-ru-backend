package com.dduru.gildongmu.report.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class DuplicateReportException extends BusinessException {

    public DuplicateReportException() {
        super(ErrorCode.DUPLICATE_REPORT);
    }
}
