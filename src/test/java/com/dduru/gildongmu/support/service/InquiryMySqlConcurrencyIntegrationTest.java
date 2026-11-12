package com.dduru.gildongmu.support.service;

import com.dduru.gildongmu.admin.support.dto.request.InquiryAnswerRequest;
import com.dduru.gildongmu.admin.support.service.AdminInquiryService;
import com.dduru.gildongmu.support.domain.enums.InquiryCategory;
import com.dduru.gildongmu.support.dto.request.InquiryWriteRequest;
import com.dduru.gildongmu.support.exception.InquiryAlreadyAnsweredException;
import com.dduru.gildongmu.support.mysql.MySqlIntegrationTestSupport;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryMySqlConcurrencyIntegrationTest extends MySqlIntegrationTestSupport {
    @Autowired InquiryService inquiries;
    @Autowired AdminInquiryService admin;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void answerCommitPreventsWaitingUserUpdateUnderRepeatableRead() throws Exception {
        String key = UUID.randomUUID().toString();
        Long userId = users.save(User.builder().name("문의 테스트").email(key + "@example.com")
                .oauthId(key).oauthType(OauthType.KAKAO).build()).getId();
        Long id = inquiries.create(userId, new InquiryWriteRequest(InquiryCategory.BUG, "원래 문의", List.of())).id();
        CountDownLatch answered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch editing = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var answer = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                admin.updateAnswer(id, userId, new InquiryAnswerRequest("답변"));
                answered.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("release timeout");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }));
            assertThat(answered.await(10, TimeUnit.SECONDS)).isTrue();
            var edit = executor.submit(() -> {
                editing.countDown();
                inquiries.update(id, userId, new InquiryWriteRequest(InquiryCategory.BUG, "변경 시도", List.of()));
            });
            assertThat(editing.await(10, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> edit.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            answer.get(10, TimeUnit.SECONDS);
            assertThatThrownBy(() -> edit.get(10, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(InquiryAlreadyAnsweredException.class);
            assertThat(inquiries.detail(id, userId).content()).isEqualTo("원래 문의");
            assertThat(inquiries.detail(id, userId).answer().content()).isEqualTo("답변");
        } finally {
            release.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
