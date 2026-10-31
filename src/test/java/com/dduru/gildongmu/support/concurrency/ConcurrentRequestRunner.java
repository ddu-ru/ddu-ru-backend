package com.dduru.gildongmu.support.concurrency;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public final class ConcurrentRequestRunner {

    private static final int DEADLOCK_ERROR_CODE = 1213;
    private static final int LOCK_TIMEOUT_ERROR_CODE = 1205;
    private static final Duration START_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration COMPLETION_TIMEOUT = Duration.ofSeconds(30);

    private ConcurrentRequestRunner() {
    }

    public static List<ConcurrentRequestResult> run(
            List<ThrowingRunnable> requests,
            Predicate<Throwable> expectedBusinessConflict
    ) {
        int requestCount = requests.size();
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(requestCount);

        try {
            List<Callable<ConcurrentRequestResult>> workers = requests.stream()
                    .<Callable<ConcurrentRequestResult>>map(request -> () -> execute(
                            request, ready, start, expectedBusinessConflict
                    ))
                    .toList();
            var futures = workers.stream().map(executor::submit).toList();

            if (!ready.await(START_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new AssertionError("동시 요청 작업 스레드 준비 시간 초과");
            }
            start.countDown();

            return futures.stream().map(ConcurrentRequestRunner::awaitResult).toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("동시 요청 시작 대기 중 인터럽트", e);
        } finally {
            start.countDown();
            executor.shutdownNow();
            awaitTermination(executor);
        }
    }

    private static ConcurrentRequestResult execute(
            ThrowingRunnable request,
            CountDownLatch ready,
            CountDownLatch start,
            Predicate<Throwable> expectedBusinessConflict
    ) {
        ready.countDown();
        try {
            if (!start.await(START_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                return new ConcurrentRequestResult(
                        ConcurrentRequestStatus.UNEXPECTED_FAILURE,
                        START_TIMEOUT,
                        new IllegalStateException("동시 요청 시작 신호 대기 시간 초과")
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ConcurrentRequestResult(ConcurrentRequestStatus.UNEXPECTED_FAILURE, Duration.ZERO, e);
        }

        long startedAt = System.nanoTime();
        try {
            request.run();
            return result(ConcurrentRequestStatus.SUCCESS, startedAt, null);
        } catch (Exception failure) {
            if (hasSqlErrorCode(failure, DEADLOCK_ERROR_CODE)) {
                return result(ConcurrentRequestStatus.DEADLOCK, startedAt, failure);
            }
            if (hasSqlErrorCode(failure, LOCK_TIMEOUT_ERROR_CODE)) {
                return result(ConcurrentRequestStatus.LOCK_TIMEOUT, startedAt, failure);
            }
            if (expectedBusinessConflict.test(failure)) {
                return result(ConcurrentRequestStatus.EXPECTED_BUSINESS_CONFLICT, startedAt, failure);
            }
            return result(ConcurrentRequestStatus.UNEXPECTED_FAILURE, startedAt, failure);
        }
    }

    public static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        Throwable current = failure;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static ConcurrentRequestResult awaitResult(Future<ConcurrentRequestResult> future) {
        try {
            return future.get(COMPLETION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AssertionError("동시 요청 완료 시간 초과", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("동시 요청 완료 대기 중 인터럽트", e);
        } catch (Exception e) {
            throw new AssertionError("동시 요청 실행기 실패", e);
        }
    }

    private static void awaitTermination(ExecutorService executor) {
        try {
            if (!executor.awaitTermination(START_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new AssertionError("동시 요청 실행기 종료 시간 초과");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("동시 요청 실행기 종료 대기 중 인터럽트", e);
        }
    }

    private static ConcurrentRequestResult result(
            ConcurrentRequestStatus status,
            long startedAt,
            Throwable failure
    ) {
        return new ConcurrentRequestResult(status, Duration.ofNanos(System.nanoTime() - startedAt), failure);
    }

    private static boolean hasSqlErrorCode(Throwable failure, int errorCode) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sqlException && sqlException.getErrorCode() == errorCode) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
