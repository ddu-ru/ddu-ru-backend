package com.dduru.gildongmu.block.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class BlockedUserException extends BusinessException {
    public BlockedUserException() {
        super(ErrorCode.USER_BLOCKED);
    }
}
