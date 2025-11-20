package com.example.anchornotes_team3.util;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.widget.EditText;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Utility Classes (8 tests) - Jeffrey
 *
 * 19-20. TextSpanUtils white-box tests
 */
@RunWith(RobolectricTestRunner.class)
public class TextSpanUtilsTest {

    /**
     * Test 19: TextSpanUtils - Test text formatting with bold
     * White-box: toggles bold on selection, verifies StyleSpan(BOLD) presence then removal
     */
    @Test
    public void testToggleBold_appliesAndRemoves() {
        EditText editText = new EditText(RuntimeEnvironment.getApplication());
        editText.setText(new SpannableStringBuilder("Hello World"));
        editText.setSelection(0, 5); // "Hello"

        boolean applied = TextSpanUtils.toggleBold(editText);
        assertTrue(applied);

        StyleSpan[] boldAfterApply = editText.getText().getSpans(0, 5, StyleSpan.class);
        assertTrue(containsStyle(editText, boldAfterApply, android.graphics.Typeface.BOLD, 0, 5));

        // Toggle again to remove bold
        boolean removed = TextSpanUtils.toggleBold(editText);
        assertTrue(removed);

        StyleSpan[] spansAfterRemove = editText.getText().getSpans(0, 5, StyleSpan.class);
        assertFalse(containsStyle(editText, spansAfterRemove, android.graphics.Typeface.BOLD, 0, 5));
    }

    /**
     * Test 20: TextSpanUtils - Test text formatting with italic
     * White-box: toggles italic on selection, verifies StyleSpan(ITALIC) presence then removal
     */
    @Test
    public void testToggleItalic_appliesAndRemoves() {
        EditText editText = new EditText(RuntimeEnvironment.getApplication());
        editText.setText(new SpannableStringBuilder("Hello World"));
        editText.setSelection(6, 11); // "World"

        boolean applied = TextSpanUtils.toggleItalic(editText);
        assertTrue(applied);

        StyleSpan[] italicAfterApply = editText.getText().getSpans(6, 11, StyleSpan.class);
        assertTrue(containsStyle(editText, italicAfterApply, android.graphics.Typeface.ITALIC, 6, 11));

        // Toggle again to remove italic
        boolean removed = TextSpanUtils.toggleItalic(editText);
        assertTrue(removed);

        StyleSpan[] spansAfterRemove = editText.getText().getSpans(6, 11, StyleSpan.class);
        assertFalse(containsStyle(editText, spansAfterRemove, android.graphics.Typeface.ITALIC, 6, 11));
    }

    private static boolean containsStyle(EditText et, StyleSpan[] spans, int style, int start, int end) {
        for (StyleSpan s : spans) {
            if (s.getStyle() == style) {
                int ss = et.getText().getSpanStart(s);
                int se = et.getText().getSpanEnd(s);
                if (ss <= start && se >= end) return true;
            }
        }
        return false;
    }
}


