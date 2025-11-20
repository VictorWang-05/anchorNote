package com.example.anchornotes_team3.util;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Utility Classes (8 tests) - Jeffrey
 *
 * 16-18. MarkdownConverter white-box tests
 */
@RunWith(RobolectricTestRunner.class)
public class MarkdownConverterTest {

    /**
     * Test 16: MarkdownConverter - Test converting Spanned with styles to Markdown
     * White-box: verifies markers for bold (** **) and size (<big>) are inserted correctly
     */
    @Test
    public void testToMarkdown_convertsStyledSpans() {
        SpannableStringBuilder builder = new SpannableStringBuilder("Hello World");
        // Bold "Hello"
        builder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Large "World"
        builder.setSpan(new RelativeSizeSpan(1.3f), 6, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        String md = MarkdownConverter.toMarkdown(builder);

        assertEquals("**Hello** <big>World</big>", md);
    }

    /**
     * Test 17: MarkdownConverter - Test handling empty markdown string
     * White-box: fromMarkdown returns empty Spanned when input is empty/null
     */
    @Test
    public void testFromMarkdown_emptyString() {
        Spanned s1 = MarkdownConverter.fromMarkdown(RuntimeEnvironment.getApplication(), "");
        Spanned s2 = MarkdownConverter.fromMarkdown(RuntimeEnvironment.getApplication(), null);
        assertNotNull(s1);
        assertNotNull(s2);
        assertEquals(0, s1.length());
        assertEquals(0, s2.length());
    }

    /**
     * Test 18: MarkdownConverter - Test markdown with special characters and nested markers
     * White-box: verifies parsing of **, *, ***, <small>, <big> into correct spans
     */
    @Test
    public void testFromMarkdown_parsesMarkersAndSpecials() {
        String markdown = "***BoldItalic*** and *italic* and **bold** <small>x</small> <big>y</big>";
        Spanned spanned = MarkdownConverter.fromMarkdown(RuntimeEnvironment.getApplication(), markdown);
        String text = spanned.toString();

        // Plain text should have markers removed
        assertEquals("BoldItalic and italic and bold x y", text);

        // Assert spans exist at expected regions
        int idxBoldItalic = text.indexOf("BoldItalic");
        int idxItalic = text.indexOf("italic");
        int idxBold = text.indexOf("bold");
        int idxSmall = text.indexOf("x");
        int idxBig = text.indexOf("y");

        // BoldItalic -> StyleSpan(BOLD_ITALIC)
        StyleSpan[] spansBI = spanned.getSpans(idxBoldItalic, idxBoldItalic + "BoldItalic".length(), StyleSpan.class);
        assertTrue(containsStyle(spanned, spansBI, android.graphics.Typeface.BOLD_ITALIC, idxBoldItalic, idxBoldItalic + "BoldItalic".length()));

        // Italic -> StyleSpan(ITALIC)
        StyleSpan[] spansI = spanned.getSpans(idxItalic, idxItalic + "italic".length(), StyleSpan.class);
        assertTrue(containsStyle(spanned, spansI, android.graphics.Typeface.ITALIC, idxItalic, idxItalic + "italic".length()));

        // Bold -> StyleSpan(BOLD)
        StyleSpan[] spansB = spanned.getSpans(idxBold, idxBold + "bold".length(), StyleSpan.class);
        assertTrue(containsStyle(spanned, spansB, android.graphics.Typeface.BOLD, idxBold, idxBold + "bold".length()));

        // <small> -> RelativeSizeSpan(0.8f ± tolerance)
        RelativeSizeSpan[] spansSmall = spanned.getSpans(idxSmall, idxSmall + 1, RelativeSizeSpan.class);
        assertTrue(containsSize(spanned, spansSmall, 0.8f, 0.11f, idxSmall, idxSmall + 1));

        // <big> -> RelativeSizeSpan(1.3f ± tolerance)
        RelativeSizeSpan[] spansBig = spanned.getSpans(idxBig, idxBig + 1, RelativeSizeSpan.class);
        assertTrue(containsSize(spanned, spansBig, 1.3f, 0.11f, idxBig, idxBig + 1));
    }

    private static boolean containsStyle(Spanned spanned, StyleSpan[] spans, int style, int start, int end) {
        for (StyleSpan s : spans) {
            if (s.getStyle() == style) {
                int ss = spanned.getSpanStart(s);
                int se = spanned.getSpanEnd(s);
                if (ss <= start && se >= end) return true;
            }
        }
        return false;
    }

    private static boolean containsSize(Spanned spanned, RelativeSizeSpan[] spans, float expected, float tol, int start, int end) {
        for (RelativeSizeSpan s : spans) {
            float scale = s.getSizeChange();
            if (Math.abs(scale - expected) <= tol) {
                int ss = spanned.getSpanStart(s);
                int se = spanned.getSpanEnd(s);
                if (ss <= start && se >= end) return true;
            }
        }
        return false;
    }
}


