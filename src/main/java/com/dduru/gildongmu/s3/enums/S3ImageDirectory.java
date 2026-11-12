package com.dduru.gildongmu.s3.enums;

public enum S3ImageDirectory {
    INQUIRIES("inquiries"),
    POSTS("posts"),
    PROFILES("profiles"),
    JOURNEYS("journeys"),
    JOURNEY_POSTS("journeys/posts"),
    JOURNEY_SCHEDULES("journeys/schedules"),
    CHATS("chats");

    private final String directory;

    S3ImageDirectory(String directory) {
        this.directory = directory;
    }

    public String keyPrefix() {
        return directory + "/";
    }

    public String uriPrefix() {
        return "/" + directory + "/";
    }
}
