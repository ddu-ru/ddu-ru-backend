package com.dduru.gildongmu.block.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class SelfBlockNotAllowedException extends BusinessException {
    public SelfBlockNotAllowedException() {
        super(ErrorCode.SELF_BLOCK_NOT_ALLOWED);
    }
}
