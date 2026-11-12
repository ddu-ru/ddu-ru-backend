package com.dduru.gildongmu.support.service;

import com.dduru.gildongmu.admin.support.dto.request.InquiryAnswerRequest;
import com.dduru.gildongmu.admin.support.service.AdminInquiryService;
import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.common.config.S3Properties;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.common.validation.*;
import com.dduru.gildongmu.support.domain.enums.*;
import com.dduru.gildongmu.support.dto.request.*;
import com.dduru.gildongmu.support.exception.*;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({QueryDslConfig.class, InquiryService.class, FaqService.class, AdminInquiryService.class,
        S3ImageUrlValidator.class, S3Properties.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SupportIntegrationTest {
    @Autowired InquiryService service;
    @Autowired FaqService faqs;
    @Autowired AdminInquiryService admin;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean TimeProvider time;
    private Long owner;
    private Long other;
    private final LocalDateTime now = LocalDateTime.of(2026, 11, 12, 12, 0);
    private static final String IMAGE = "https://dummy-bucket.s3.ap-northeast-2.amazonaws.com/inquiries/photo.png";

    @BeforeEach
    void setup() {
        owner = newUser(); other = newUser();
        when(time.now()).thenReturn(now);
    }

    private Long newUser() {
        String id = UUID.randomUUID().toString();
        return users.save(User.builder().name("문의자").email(id + "@example.com")
                .oauthId(id).oauthType(OauthType.KAKAO).build()).getId();
    }

    @AfterEach
    void cleanup() {
        jdbc.update("delete from inquiry_images where inquiry_id in (select id from inquiries where user_id in (?,?))", owner, other);
        jdbc.update("delete from inquiries where user_id in (?,?)", owner, other);
        jdbc.update("delete from faqs");
        users.deleteById(owner); users.deleteById(other);
    }

    private Long create(Long user, String content) {
        return service.create(user, new InquiryWriteRequest(InquiryCategory.BUG, content, List.of())).id();
    }

    @Test
    void listsAllAgesOnlyForOwnerWithCursorAndEmptyLastPage() {
        Long old = create(owner, "오래된 문의");
        jdbc.update("update inquiries set created_at = ? where id = ?", now.minusYears(2), old);
        create(other, "다른 사용자 문의");
        Long newest = create(owner, "새 문의");
        var first = service.list(owner, new InquiryListRequest(null, 1));
        assertThat(first.inquiries()).extracting(x -> x.id()).containsExactly(newest);
        assertThat(first.nextCursor()).isEqualTo(newest);
        var second = service.list(owner, new InquiryListRequest(first.nextCursor(), 1));
        assertThat(second.inquiries()).extracting(x -> x.id()).containsExactly(old);
        assertThat(second.hasNext()).isFalse();
        assertThat(second.nextCursor()).isNull();
        assertThat(service.list(owner, new InquiryListRequest(old, 20)).inquiries()).isEmpty();
        assertThat(service.detail(old, owner).createdAt()).isEqualTo(now.minusYears(2));
    }

    @Test
    void persistsOrderedImagesReplacesAndRemovesAttachments() {
        var created = service.create(owner, new InquiryWriteRequest(InquiryCategory.BUG, "　문의 내용　",
                List.of(IMAGE, IMAGE.replace("photo", "second"))));
        assertThat(created.content()).isEqualTo("문의 내용");
        assertThat(created.answer()).isNull();
        assertThat(service.detail(created.id(), owner).imageUrls()).containsExactly(IMAGE, IMAGE.replace("photo", "second"));
        service.update(created.id(), owner, new InquiryWriteRequest(InquiryCategory.ACCOUNT, "수정", List.of(IMAGE)));
        assertThat(service.detail(created.id(), owner).imageUrls()).containsExactly(IMAGE);
        service.update(created.id(), owner, new InquiryWriteRequest(InquiryCategory.SERVICE, "최종", List.of()));
        assertThat(service.detail(created.id(), owner).imageUrls()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from inquiry_images where inquiry_id = ?", Long.class, created.id())).isZero();
    }

    @Test
    void rejectsForeignAccessMissingInquiryAndInvalidImageWithoutSaving() {
        Long id = create(owner, "내용");
        assertThatThrownBy(() -> service.detail(id, other)).isInstanceOf(InquiryAccessDeniedException.class);
        assertThatThrownBy(() -> service.update(id, other, new InquiryWriteRequest(InquiryCategory.BUG, "변경", List.of())))
                .isInstanceOf(InquiryAccessDeniedException.class);
        assertThatThrownBy(() -> service.detail(Long.MAX_VALUE, owner)).isInstanceOf(InquiryNotFoundException.class);
        for (String image : List.of("https://example.com/photo.png", IMAGE.replace("inquiries", "posts"), IMAGE.replace(".png", ".gif"))) {
            assertThatThrownBy(() -> service.create(owner, new InquiryWriteRequest(InquiryCategory.BUG, "내용", List.of(image))))
                    .isInstanceOf(InvalidImageUrlException.class);
        }
        assertThat(service.list(owner, new InquiryListRequest(null, null)).size()).isEqualTo(1);
    }

    @Test
    void adminAnswersAndRevisesWhileUserCannotModify() {
        Long id = create(owner, "내용");
        assertThat(admin.findAll(InquiryStatus.WAITING, PageRequest.of(0, 20)).content()).extracting(x -> x.id()).contains(id);
        admin.updateAnswer(id, other, new InquiryAnswerRequest("　최초 답변　"));
        var detail = service.detail(id, owner);
        assertThat(detail.status()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(detail.editable()).isFalse();
        assertThat(detail.answer().content()).isEqualTo("최초 답변");
        assertThatThrownBy(() -> service.update(id, owner, new InquiryWriteRequest(InquiryCategory.BUG, "수정", List.of())))
                .isInstanceOf(InquiryAlreadyAnsweredException.class);
        when(time.now()).thenReturn(now.plusHours(1));
        admin.updateAnswer(id, other, new InquiryAnswerRequest("정정 답변"));
        assertThat(admin.findById(id).answer().answeredAt()).isEqualTo(now);
        assertThat(admin.findById(id).answer().modifiedAt()).isEqualTo(now.plusHours(1));
        assertThat(admin.findAll(InquiryStatus.WAITING, PageRequest.of(0, 20)).content()).isEmpty();
    }

    @Test
    void faqFiltersVisibilityCategoryQuestionAnswerAndLiteralWildcards() {
        insertFaq("SERVICE", "사진 첨부", "업로드 안내", true, 2);
        insertFaq("ACCOUNT", "계정", "사진 변경", true, 1);
        insertFaq("SERVICE", "숨김 사진", "답변", false, 0);
        insertFaq("SERVICE", "100% 보장", "안내", true, 3);
        assertThat(faqs.list(new FaqListRequest(null, "　사진　"))).extracting(x -> x.question()).containsExactly("계정", "사진 첨부");
        assertThat(faqs.list(new FaqListRequest(FaqCategory.SERVICE, "사진"))).extracting(x -> x.question()).containsExactly("사진 첨부");
        assertThat(faqs.list(new FaqListRequest(null, "%"))).extracting(x -> x.question()).containsExactly("100% 보장");
        assertThat(faqs.list(new FaqListRequest(null, "　"))).hasSize(3);
        assertThat(faqs.list(new FaqListRequest(null, "없는단어"))).isEmpty();
    }

    private void insertFaq(String category, String question, String answer, boolean visible, int order) {
        jdbc.update("insert into faqs(category, question, answer, visible, sort_order) values (?,?,?,?,?)", category, question, answer, visible, order);
    }

    @Test
    void concurrentUpdateWaitsForAnswerCommitAndIsRejected() throws Exception {
        Long id = create(owner, "원래 내용");
        CountDownLatch answered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch editing = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> answer = pool.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                admin.updateAnswer(id, other, new InquiryAnswerRequest("답변"));
                answered.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("release timeout"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            }));
            assertThat(answered.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> edit = pool.submit(() -> {
                editing.countDown();
                service.update(id, owner, new InquiryWriteRequest(InquiryCategory.BUG, "변경 시도", List.of()));
            });
            assertThat(editing.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> edit.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            answer.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> edit.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(InquiryAlreadyAnsweredException.class);
            assertThat(service.detail(id, owner).content()).isEqualTo("원래 내용");
        } finally {
            release.countDown(); pool.shutdownNow(); pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
