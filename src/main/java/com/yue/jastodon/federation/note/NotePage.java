package com.yue.jastodon.federation.note;

import java.util.List;
import java.util.Optional;

record NotePage(List<Note> notes, Optional<NoteCursor> next) {
}
