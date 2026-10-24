package com.dduru.gildongmu.superhost.dto.response;

import java.util.List;

public record MySuperHostStatusResponse(
        int unusedTicketCount,
        List<SuperHostManagePostResponse> appliedPosts,
        List<SuperHostManagePostResponse> availablePosts
) {
    public static MySuperHostStatusResponse of(
            int unusedTicketCount,
            List<SuperHostManagePostResponse> appliedPosts,
            List<SuperHostManagePostResponse> availablePosts
    ) {
        return new MySuperHostStatusResponse(
                unusedTicketCount,
                List.copyOf(appliedPosts),
                List.copyOf(availablePosts)
        );
    }
}
