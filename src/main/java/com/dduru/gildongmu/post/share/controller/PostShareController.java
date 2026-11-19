package com.dduru.gildongmu.post.share.controller;

import com.dduru.gildongmu.post.exception.PostNotFoundException;
import com.dduru.gildongmu.post.share.config.PostShareProperties;
import com.dduru.gildongmu.post.share.dto.PostShareView;
import com.dduru.gildongmu.post.share.service.PostShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class PostShareController {

    private final PostShareService postShareService;
    private final PostShareProperties postShareProperties;

    @GetMapping("/share/posts/{postId}")
    public String retrievePostSharePage(@PathVariable Long postId, Model model) {
        PostShareView post = postShareService.getShareView(postId);
        model.addAttribute("post", post);
        model.addAttribute("canonicalUrl", postShareProperties.postShareUrl(postId));
        model.addAttribute("playStoreUrl", postShareProperties.getPlayStoreUrl());
        model.addAttribute("hasPlayStoreUrl", postShareProperties.hasPlayStoreUrl());
        return "share/post-detail";
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ModelAndView handlePostNotFound() {
        ModelAndView modelAndView = new ModelAndView("share/post-not-found");
        modelAndView.setStatus(HttpStatus.NOT_FOUND);
        return modelAndView;
    }
}
