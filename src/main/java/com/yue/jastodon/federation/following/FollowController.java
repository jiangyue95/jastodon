package com.yue.jastodon.federation.following;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
class FollowController {

    private final FollowService followService;

    FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PutMapping("/following/{accountId}")
    ResponseEntity<FollowResponse> follow(@PathVariable long accountId) {
        Optional<FollowResult> result = followService.follow(accountId);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Follow follow = result.get().follow();
        FollowResponse body = new FollowResponse(follow.remoteAccountId(), follow.state());
        if (result.get().created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
        return ResponseEntity.ok(body);
    }

    record FollowResponse(long accountId, FollowState state) {
    }
}
