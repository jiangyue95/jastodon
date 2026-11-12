package com.yue.jastodon.federation.profile;

@FunctionalInterface
interface AvatarStorage {

    String store(byte[] png);
}
