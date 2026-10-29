package com.yue.jastodon.federation.note;

import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

final class NoteContentRenderer {

    private static final Pattern BLANK_LINES = Pattern.compile("\n{2,}");

    private NoteContentRenderer() {}

    static String render(String content) {
        String normalised = content.replace("\r\n", "\n").replace("\r", "\n");
        String trimmed = normalised.strip();
        String escaped = HtmlUtils.htmlEscape(trimmed, StandardCharsets.UTF_8.name());
        StringBuilder html = new StringBuilder();
        for (String paragraph : BLANK_LINES.split(escaped)) {
            html.append("<p>").append(paragraph.replace("\n", "<br />")).append("</p>");
        }
        return html.toString();
    }
}
