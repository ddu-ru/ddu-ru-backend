package com.dduru.gildongmu.report.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class ReportEvidenceUnavailableException extends BusinessException {

    public ReportEvidenceUnavailableException() {
        super(ErrorCode.REPORT_EVIDENCE_UNAVAILABLE);
    }
}
