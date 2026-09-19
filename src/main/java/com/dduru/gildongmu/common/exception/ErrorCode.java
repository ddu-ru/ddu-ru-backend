package com.dduru.gildongmu.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 공통 (COMMON)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력 값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    JSON_CONVERT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 변환 중 오류가 발생했습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type입니다."),

    // 인증 (AUTH)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 인증 토큰입니다."),
    UNSUPPORTED_SOCIAL_LOGIN(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 다른 소셜 계정으로 가입된 이메일입니다."),
    USER_CREATION_INTEGRITY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 생성 중 예상치 못한 데이터 무결성 오류가 발생했습니다."),

    // 휴대폰 인증 (VERIFICATION)
    SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SMS 발송에 실패했습니다."),
    INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "재발송 제한 시간이 지나지 않았습니다."),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 정보를 찾을 수 없습니다."),
    VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.BAD_REQUEST, "검증 시도 횟수를 초과했습니다."),
    ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 완료된 인증입니다."),
    DAILY_SMS_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "일일 SMS 발송 한도를 초과했습니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "이미 가입된 전화번호입니다."),
    SMS_PROVIDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SMS 서비스에 일시적인 오류가 발생했습니다."),

    // 닉네임 (NICKNAME)
    NICKNAME_NOT_BLANK(HttpStatus.BAD_REQUEST, "닉네임은 공백일 수 없습니다."),
    NICKNAME_INVALID_LENGTH(HttpStatus.BAD_REQUEST, "닉네임은 2자 이상 14자 이하로 입력해주세요."),
    NICKNAME_INVALID_CHARACTERS(HttpStatus.BAD_REQUEST, "닉네임은 한글, 영어, 숫자만 사용 가능합니다."),
    NICKNAME_CONSECUTIVE_SPACES(HttpStatus.BAD_REQUEST, "공백은 단어 사이에 한 번만 사용할 수 있습니다."),
    NICKNAME_CONTAINS_BAD_WORD(HttpStatus.BAD_REQUEST, "닉네임에 부적절한 단어가 포함되어 있습니다."),
    NICKNAME_ALREADY_TAKEN(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    NICKNAME_CONTAINS_EMOJI_OR_SYMBOL(HttpStatus.BAD_REQUEST, "닉네임에 이모지 또는 특수 기호는 사용할 수 없습니다."),

    // 설문 (SURVEY)
    SURVEY_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "설문 결과를 찾을 수 없습니다."),
    AVATAR_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "아바타 프로필을 찾을 수 없습니다."),
    SURVEY_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "설문조사가 이미 완료된 상태에서는 스킵할 수 없습니다."),
    SURVEY_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출된 설문이 존재합니다."),
    SURVEY_RETAKE_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "아직 다시 테스트할 수 없습니다."),
    SURVEY_REQUIRED(HttpStatus.FORBIDDEN, "설문 완료가 필요한 기능입니다."),
    INVALID_TRAVEL_TENDENCY_SCORE(HttpStatus.BAD_REQUEST, "여행 성향 점수는 0.0 이상 10.0 이하로 입력해야 합니다."),

    // 프로필 (PROFILE)
    BIRTHDAY_NOT_FOUND(HttpStatus.NOT_FOUND, "생년월일을 찾을 수 없습니다."),
    BG_COLOR_NOT_FOUND(HttpStatus.NOT_FOUND, "배경색을 찾을 수 없습니다."),
    INVALID_PROFILE_IMAGE_URL(HttpStatus.BAD_REQUEST, "프로필 이미지 URL이 유효하지 않습니다."),
    USER_ONBOARDING_NOT_FOUND(HttpStatus.NOT_FOUND, "유저 온보딩 정보를 찾을 수 없습니다."),

    // 사용자 (USER)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 유저의 프로필을 찾을 수 없습니다."),

    // 게시글 (POST)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시글에 대한 접근 권한이 없습니다."),
    INVALID_POST_DATE(HttpStatus.BAD_REQUEST, "여행 종료일은 시작일과 같거나 이후여야 합니다"),
    INVALID_PREFERRED_AGE(HttpStatus.BAD_REQUEST, "선호 연령 설정이 올바르지 않습니다."),
    INVALID_RECRUIT_CAPACITY(HttpStatus.BAD_REQUEST, "잘못된 모집 인원 설정입니다."),
    TRAVEL_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "이미 시작된 여행입니다."),
    TRAVEL_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "이미 종료된 여행입니다."),
    RECRUIT_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "모집 마감된 게시글은 수정할 수 없습니다."),
    RECRUIT_COUNT_EXCEED_CAPACITY(HttpStatus.BAD_REQUEST, "모집 정원을 초과할 수 없습니다."),
    RECRUIT_COUNT_BELOW_ZERO(HttpStatus.BAD_REQUEST, "모집된 인원이 1명 미만이 될 수 없습니다."),
    INVALID_POST_STATUS(HttpStatus.BAD_REQUEST, "모집이 완료된 게시글은 모집 상태를 변경할 수 없습니다."),
    INVALID_POST_TAGS(HttpStatus.BAD_REQUEST, "태그 형식이 올바르지 않습니다."),
    INVALID_POST_TITLE(HttpStatus.BAD_REQUEST, "제목은 5자 이상 40자 이하여야 합니다."),
    INVALID_POST_CONTENT(HttpStatus.BAD_REQUEST, "내용은 20자 이상 1000자 이하여야 합니다."),

    // 여행지 (DESTINATION)
    DESTINATION_NOT_FOUND(HttpStatus.NOT_FOUND, "여행지를 찾을 수 없습니다."),

    // 참여신청 (PARTICIPATION)
    PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND, "참여신청을 찾을 수 없습니다."),
    DUPLICATE_PARTICIPATION(HttpStatus.CONFLICT, "이미 참여 신청한 게시글입니다."),
    RECRUITMENT_CLOSED(HttpStatus.BAD_REQUEST, "모집이 마감된 게시글입니다."),
    RECRUITMENT_FULL(HttpStatus.BAD_REQUEST, "모집 정원이 가득 찬 게시글입니다."),
    SELF_PARTICIPATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자신의 게시글에는 참여신청할 수 없습니다."),
    PARTICIPATION_POST_MISMATCH(HttpStatus.BAD_REQUEST, "해당 참여신청은 해당 게시글에 속해있지 않습니다."),
    PARTICIPATION_CONTACT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "대기 중인 신청만 연락할 수 있습니다."),
    PARTICIPATION_APPROVAL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "승인할 수 없습니다. 이미 승인되었거나 거절된 신청입니다."),
    PARTICIPATION_REJECTION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "거절할 수 없습니다. 이미 승인되었거나 거절된 신청입니다."),
    PARTICIPATION_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "대기 중인 신청만 취소할 수 있습니다."),

    // 나의 여정 (JOURNEY)
    JOURNEY_NOT_FOUND(HttpStatus.NOT_FOUND, "나의 여정을 찾을 수 없습니다."),
    CURRENT_OR_UPCOMING_JOURNEY_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중이거나 예정된 나의 여정이 없습니다."),
    JOURNEY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "나의 여정에 접근할 권한이 없습니다."),
    JOURNEY_HOST_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "나의 여정 호스트 정보를 찾을 수 없습니다."),
    JOURNEY_EMPTY_PATCH(HttpStatus.BAD_REQUEST, "수정할 항목을 하나 이상 입력해야 합니다."),
    JOURNEY_INVALID_TITLE_LENGTH(HttpStatus.BAD_REQUEST, "제목은 5자 이상 40자 이하여야 합니다."),
    JOURNEY_INVALID_PHOTO_URL(HttpStatus.BAD_REQUEST, "대표 사진 URL 형식이 올바르지 않습니다."),
    JOURNEY_INCOMPLETE_TRAVEL_DATE(HttpStatus.BAD_REQUEST, "여행 시작일과 종료일은 함께 입력해야 합니다."),
    JOURNEY_INVALID_TRAVEL_DATE(HttpStatus.BAD_REQUEST, "종료일은 시작일과 같거나 이후여야 합니다."),
    JOURNEY_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "나의 여정 게시글을 찾을 수 없습니다."),
    JOURNEY_POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 게시글에 대한 권한이 없습니다."),
    JOURNEY_POST_EMPTY_PATCH(HttpStatus.BAD_REQUEST, "수정할 게시글 내용이 없습니다."),
    JOURNEY_POST_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "게시글 내용은 1자 이상 300자 이하여야 합니다."),
    JOURNEY_POST_NOTICE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "공지 게시글은 최대 5개까지 설정할 수 있습니다."),
    JOURNEY_POST_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "나의 여정 게시글 댓글을 찾을 수 없습니다."),
    JOURNEY_POST_COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 댓글에 대한 권한이 없습니다."),
    JOURNEY_POST_COMMENT_EMPTY_PATCH(HttpStatus.BAD_REQUEST, "수정할 댓글 내용이 없습니다."),
    JOURNEY_POST_COMMENT_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "댓글 내용은 1자 이상 300자 이하여야 합니다."),
    JOURNEY_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "나의 여정 일정을 찾을 수 없습니다."),
    JOURNEY_SCHEDULE_EMPTY_PATCH(HttpStatus.BAD_REQUEST, "수정할 일정 내용이 없습니다."),
    JOURNEY_SCHEDULE_INVALID_DATE(HttpStatus.BAD_REQUEST, "일정 날짜는 여행 기간 내에서만 설정할 수 있습니다."),
    JOURNEY_SCHEDULE_INVALID_TITLE(HttpStatus.BAD_REQUEST, "일정 제목은 1자 이상 30자 이하여야 합니다."),
    JOURNEY_SCHEDULE_INVALID_MEMO(HttpStatus.BAD_REQUEST, "일정 메모는 100자 이하여야 합니다."),
    JOURNEY_SCHEDULE_INVALID_PLACE_NAME(HttpStatus.BAD_REQUEST, "장소명은 30자 이하여야 합니다."),
    JOURNEY_SCHEDULE_INVALID_TIME(HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간이 설정된 경우에만 입력할 수 있습니다."),
    JOURNEY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "나의 여정 멤버를 찾을 수 없습니다."),
    JOURNEY_MEMBER_CANNOT_REMOVE_SELF(HttpStatus.BAD_REQUEST, "호스트 본인은 강퇴할 수 없습니다."),
    JOURNEY_MEMBER_INVALID_CUSTOM_ROLE_LABEL(HttpStatus.BAD_REQUEST, "직접 입력 역할명은 1자 이상 10자 이하여야 합니다."),
    JOURNEY_MEMBER_ROLE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "멤버당 역할은 최대 5개까지 지정할 수 있습니다."),
    JOURNEY_MEMBER_DUPLICATE_CUSTOM_ROLE_LABEL(HttpStatus.BAD_REQUEST, "동일한 커스텀 역할 라벨은 중복 지정할 수 없습니다."),

    // 신고 (REPORT)
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다."),
    SELF_POST_REPORT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자신의 게시글은 신고할 수 없습니다."),
    DUPLICATE_POST_REPORT(HttpStatus.CONFLICT, "이미 신고한 게시글입니다."),

    // 파일 (FILE)
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 확장자입니다."),

    // 슈퍼호스트 (SUPER_HOST)
    SUPER_HOST_TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "사용 가능한 슈퍼호스트 티켓이 없습니다."),
    SUPER_HOST_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 활성화된 슈퍼호스트 게시글이 있습니다."),
    SUPER_HOST_TICKET_NOT_USABLE(HttpStatus.BAD_REQUEST, "사용할 수 없는 슈퍼호스트 티켓입니다."),
    SUPER_HOST_POST_NOT_APPLICABLE(HttpStatus.BAD_REQUEST, "슈퍼호스트로 노출할 수 없는 게시글입니다."),

    // 채팅 (CHAT)
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방 멤버를 찾을 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅 메시지를 찾을 수 없습니다."),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "채팅방 접근 권한이 없습니다."),
    CHAT_ROOM_CLOSED(HttpStatus.BAD_REQUEST, "종료된 채팅방입니다."),
    CHAT_ROOM_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "현재 채팅방 정원을 초과했습니다."),
    CHAT_ROOM_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "채팅방 타입은 필수입니다."),
    PRIVATE_CHAT_ROOM_INVALID_CONTEXT(HttpStatus.BAD_REQUEST, "1:1 채팅방은 게시글만 참조해야 합니다."),
    GROUP_CHAT_ROOM_INVALID_CONTEXT(HttpStatus.BAD_REQUEST, "그룹 채팅방은 나의 여정만 참조해야 합니다."),
    CHAT_NOT_GROUP_ROOM(HttpStatus.BAD_REQUEST, "그룹 채팅방이 아닙니다."),
    CHAT_INVITE_HOST_ONLY(HttpStatus.FORBIDDEN, "방장만 멤버를 초대할 수 있습니다."),
    NOT_SELF_CHAT(HttpStatus.FORBIDDEN, "자신과의 채팅은 허용되지 않습니다."),
    CHAT_ROOM_INVITE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시글 작성자만 그룹 채팅 멤버를 추가할 수 있습니다."),
    GROUP_CHAT_ALREADY_MEMBER(HttpStatus.BAD_REQUEST, "이미 그룹 채팅방의 멤버입니다."),

    // 이미지 URL (IMAGE URL)
    IMAGE_URL_REQUIRED(HttpStatus.BAD_REQUEST, "이미지 URL은 필수입니다."),
    IMAGE_URL_BLANK(HttpStatus.BAD_REQUEST, "이미지 URL은 비어 있을 수 없습니다."),
    IMAGE_URL_TOO_LONG(HttpStatus.BAD_REQUEST, "이미지 URL이 너무 깁니다."),
    IMAGE_URL_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "이미지 URL 형식이 올바르지 않습니다."),
    IMAGE_URL_NOT_ABSOLUTE(HttpStatus.BAD_REQUEST, "이미지 URL은 절대 경로(https)여야 합니다."),
    IMAGE_URL_INVALID_SCHEME(HttpStatus.BAD_REQUEST, "이미지 URL은 https 만 허용됩니다."),
    IMAGE_URL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "업로드된 이미지 URL만 허용됩니다."),

    // 채팅 메세지 (CHAT MESSAGE)
    CHAT_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, "메시지 내용은 필수입니다."),
    CHAT_TEXT_BLANK(HttpStatus.BAD_REQUEST, "공백만 있는 메시지는 전송할 수 없습니다."),
    CHAT_TEXT_TOO_LONG(HttpStatus.BAD_REQUEST, "메시지가 너무 깁니다."),
    CHAT_SYSTEM_MESSAGE_SEND_ACCESS_DENIED(HttpStatus.BAD_REQUEST, "SYSTEM 은 사용자 메시지로 처리되지 않습니다."),

    // 알림 (NOTIFICATION)
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 알림에 접근할 수 없습니다."),

    // 추천 (RECOMMENDATION)
    RECOMMENDATION_TENDENCY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "설문 완료 사용자의 여행 성향 점수를 찾을 수 없습니다."),
    MATE_RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 결과를 찾을 수 없습니다."),
    INVALID_MATE_RECOMMENDATION_RANK(HttpStatus.BAD_REQUEST, "추천 순위가 허용 범위를 벗어났습니다."),
    INVALID_MATE_RECOMMENDATION_MATCH_PERCENTAGE(HttpStatus.BAD_REQUEST, "추천 일치율이 허용 범위를 벗어났습니다."),

    // 여행 선호 설정 (TRAVEL_PREFERENCE),
    INVALID_AVAILABLE_DATE(HttpStatus.BAD_REQUEST, "여행 가능 종료일은 시작일과 같거나 이후여야 합니다."),
    DUPLICATE_AVAILABLE_DATE(HttpStatus.BAD_REQUEST, "중복된 여행 가능 날짜가 있습니다."),
    INVALID_DESTINATION_PREFERENCE(HttpStatus.BAD_REQUEST, "잘못된 여행지 선호 입력입니다.");

    private final int status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status.value();
        this.message = message;
    }
}
