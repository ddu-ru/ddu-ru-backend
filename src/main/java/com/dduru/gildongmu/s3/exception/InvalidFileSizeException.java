package com.dduru.gildongmu.s3.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class InvalidFileSizeException extends BusinessException {
    public InvalidFileSizeException() {
        super(ErrorCode.INVALID_FILE_SIZE);
    }
}
