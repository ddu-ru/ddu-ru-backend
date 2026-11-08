package com.dduru.gildongmu.report.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class InvalidReportEvidenceException extends BusinessException {

    public InvalidReportEvidenceException() {
        super(ErrorCode.INVALID_REPORT_EVIDENCE);
    }
}
