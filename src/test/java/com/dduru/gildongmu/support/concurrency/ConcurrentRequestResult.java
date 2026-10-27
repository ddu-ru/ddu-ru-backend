package com.dduru.gildongmu.support.concurrency;

import java.time.Duration;

public record ConcurrentRequestResult(
        ConcurrentRequestStatus status,
        Duration elapsed,
        Throwable failure
) {
}
