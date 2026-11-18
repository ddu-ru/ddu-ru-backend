package com.dduru.gildongmu.post.share.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.post.share.dto.PostShareView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostShareService {

    private final PostRepository postRepository;
    private final TimeProvider timeProvider;

    public PostShareView getShareView(Long postId) {
        Post post = postRepository.getActiveByIdOrThrow(postId);
        return PostShareView.from(post, timeProvider.today());
    }
}
