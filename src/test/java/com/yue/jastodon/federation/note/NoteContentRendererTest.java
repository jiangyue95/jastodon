package com.yue.jastodon.federation.note;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteContentRendererTest {

    @Test
    void wrapsTextInAParagraph() {
        assertThat(NoteContentRenderer.render("hello")).isEqualTo("<p>hello</p>");
    }

    @Test
    void escapesHtmlSpecialCharacters() {
        assertThat(NoteContentRenderer.render("<b>a</b> & \"c\" 'd'"))
                .isEqualTo("<p>&lt;b&gt;a&lt;/b&gt; &amp; &quot;c&quot; &#39;d&#39;</p>");
    }

    @Test
    void keepsNonAsciiCharacters() {
        assertThat(NoteContentRenderer.render("café — 你好")).isEqualTo("<p>café — 你好</p>");
    }

    @Test
    void turnsSingleLineBreaksIntoBr() {
        assertThat(NoteContentRenderer.render("a\nb\nc"))
                .isEqualTo("<p>a<br />b<br />c</p>");
    }

    @Test
    void splitsParagraphsOnBlankLines() {
        assertThat(NoteContentRenderer.render("a\n\nb")).isEqualTo("<p>a</p><p>b</p>");
    }

    @Test
    void treatsSeveralBlankLinesAsOne() {
        assertThat(NoteContentRenderer.render("a\n\n\nb")).isEqualTo("<p>a</p><p>b</p>");
    }

    @Test
    void normalisesWindowsLineBreaks() {
        assertThat(NoteContentRenderer.render("a\r\nb\r\n\r\nc")).isEqualTo("<p>a<br />b</p><p>c</p>");
    }

    @Test
    void normalisesLoneCarriageReturns() {
        assertThat(NoteContentRenderer.render("a\rb")).isEqualTo("<p>a<br />b</p>");
    }

    @Test
    void ignoresLeadingAndTrailingLineBreaks() {
        assertThat(NoteContentRenderer.render("\n\nhello\n")).isEqualTo("<p>hello</p>");
    }
}
