package com.yue.jastodon.federation.actor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;

@RestController
class AvatarController {

    private final AvatarService avatarService;

    AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @PostMapping("/profile/avatar")
    AvatarResponse change(@RequestParam("avatar")MultipartFile avatar) throws IOException {
        URI url = avatarService.change(avatar.getBytes());
        return new AvatarResponse(url);
    }

    record AvatarResponse(URI url) {
    }
}
