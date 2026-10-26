package com.dduru.gildongmu.chat.exception;

import com.dduru.gildongmu.common.exception.BusinessException;
import com.dduru.gildongmu.common.exception.ErrorCode;

public class ChatPollMessageSendAccessDeniedException extends BusinessException {
    public ChatPollMessageSendAccessDeniedException() {
        super(ErrorCode.CHAT_POLL_MESSAGE_SEND_ACCESS_DENIED);
    }
}
