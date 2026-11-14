package com.yue.jastodon.federation.actor;

import com.yue.jastodon.federation.profile.AvatarStorageException;
import com.yue.jastodon.federation.profile.InvalidImageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;

@RestController
class AvatarController {

    private static final Logger log = LoggerFactory.getLogger(AvatarController.class);

    private final AvatarService avatarService;

    AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @PostMapping("/profile/avatar")
    AvatarResponse change(@RequestParam("avatar")MultipartFile avatar) throws IOException {
        URI url = avatarService.change(avatar.getBytes());
        return new AvatarResponse(url);
    }

    @ExceptionHandler(InvalidImageException.class)
    ResponseEntity<Void> invalidImage(InvalidImageException e) {
        log.warn("Avatar upload answered 400: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(AvatarStorageException.class)
    ResponseEntity<Void> storageFailed(AvatarStorageException e) {
        log.error("Avatar upload answered 503: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    record AvatarResponse(URI url) {
    }
}
