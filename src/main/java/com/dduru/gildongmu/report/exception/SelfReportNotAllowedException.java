package com.dduru.gildongmu.report.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class SelfReportNotAllowedException extends BusinessException {

    public SelfReportNotAllowedException() {
        super(ErrorCode.SELF_REPORT_NOT_ALLOWED);
    }
}
