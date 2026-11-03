package com.dduru.gildongmu.support.concurrency;

public enum ConcurrentRequestStatus {
    SUCCESS,
    EXPECTED_BUSINESS_CONFLICT,
    DEADLOCK,
    LOCK_TIMEOUT,
    UNEXPECTED_FAILURE
}
