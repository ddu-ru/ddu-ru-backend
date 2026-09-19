package com.dduru.gildongmu.profile.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class BirthdayNotFoundException extends BusinessException {
    public BirthdayNotFoundException() {
        super(ErrorCode.BIRTHDAY_NOT_FOUND);
    }
}
