package com.dduru.gildongmu.support.domain;

import com.dduru.gildongmu.support.domain.enums.*;
import com.dduru.gildongmu.support.exception.*;
import com.dduru.gildongmu.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class InquiryTest {
    private final User owner = User.builder().name("작성자").build();
    private Inquiry inquiry(String content) {
        ReflectionTestUtils.setField(owner, "id", 1L);
        return Inquiry.create(owner, InquiryCategory.BUG, content, List.of());
    }

    @Test
    void acceptsContentBoundariesAndFourImages() {
        assertThat(inquiry("가").getStatus()).isEqualTo(InquiryStatus.WAITING);
        Inquiry item = inquiry("가".repeat(500));
        item.update(InquiryCategory.ACCOUNT, "내용", List.of("a", "b", "c", "d"));
        assertThat(item.getImages()).extracting(InquiryImage::getSortOrder).containsExactly(0, 1, 2, 3);
        item.update(InquiryCategory.SERVICE, "수정", List.of());
        assertThat(item.getImages()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "　"})
    void rejectsBlankContent(String content) {
        assertThatThrownBy(() -> inquiry(content)).isInstanceOf(InvalidInquiryException.class);
    }

    @Test
    void rejectsInvalidCategoryLengthAndImageCount() {
        assertThatThrownBy(() -> inquiry("가".repeat(501))).isInstanceOf(InvalidInquiryException.class);
        assertThatThrownBy(() -> Inquiry.create(owner, null, "내용", List.of())).isInstanceOf(InvalidInquiryException.class);
        assertThatThrownBy(() -> Inquiry.create(owner, InquiryCategory.BUG, "내용", List.of("a","b","c","d","e")))
                .isInstanceOf(InvalidInquiryException.class);
    }

    @Test
    void ownerOnlyAndAnswerLocksEditingButAllowsAnswerRevision() {
        Inquiry item = inquiry("내용");
        item.requireOwner(1L);
        assertThatThrownBy(() -> item.requireOwner(2L)).isInstanceOf(InquiryAccessDeniedException.class);
        LocalDateTime first = LocalDateTime.of(2026, 11, 12, 12, 0);
        item.answer("답변", owner, first);
        assertThat(item.isEditable()).isFalse();
        assertThatThrownBy(() -> item.update(InquiryCategory.BUG, "수정", List.of()))
                .isInstanceOf(InquiryAlreadyAnsweredException.class);
        item.answer("수정된 답변", owner, first.plusDays(1));
        assertThat(item.getAnsweredAt()).isEqualTo(first);
        assertThat(item.getAnswerModifiedAt()).isEqualTo(first.plusDays(1));
        assertThat(item.getAnswerContent()).isEqualTo("수정된 답변");
    }

    @Test
    void answerValidationDoesNotChangeState() {
        Inquiry item = inquiry("내용");
        for (String content : List.of("", "　", "가".repeat(5001))) {
            assertThatThrownBy(() -> item.answer(content, owner, LocalDateTime.of(2026, 11, 12, 12, 0)))
                    .isInstanceOf(InvalidInquiryAnswerException.class);
        }
        assertThat(item.isEditable()).isTrue();
        item.answer("가".repeat(5000), owner, LocalDateTime.of(2026, 11, 12, 12, 0));
        assertThat(item.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
    }
}
