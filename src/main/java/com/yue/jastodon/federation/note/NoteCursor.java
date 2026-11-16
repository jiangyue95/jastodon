package com.yue.jastodon.federation.note;

import java.time.Instant;
import java.util.UUID;

record NoteCursor(Instant published, UUID id) {
}
