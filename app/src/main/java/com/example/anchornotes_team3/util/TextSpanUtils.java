package com.example.anchornotes_team3.util;

import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.widget.EditText;

import android.graphics.Typeface;

/**
 * Utility class for applying/removing text spans (bold, italic, size) on EditText selections
 */
public class TextSpanUtils {

    public enum TextSize {
        SMALL(0.8f),
        MEDIUM(1.0f),
        LARGE(1.3f);

        public final float scale;

        TextSize(float scale) {
            this.scale = scale;
        }
    }

    /**
     * Toggle bold style on the current selection
     * Returns true if formatting was applied, false if no text was selected
     */
    public static boolean toggleBold(EditText editText) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start < 0 || end < 0 || start == end) {
            return false;
        }

        Editable editable = editText.getText();
        StyleSpan[] spans = editable.getSpans(start, end, StyleSpan.class);

        boolean hasBold = false;
        for (StyleSpan span : spans) {
            if (span.getStyle() == Typeface.BOLD || span.getStyle() == Typeface.BOLD_ITALIC) {
                editable.removeSpan(span);
                hasBold = true;
            }
        }

        if (!hasBold) {
            editable.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return true;
    }

    /**
     * Toggle italic style on the current selection
     * Returns true if formatting was applied, false if no text was selected
     */
    public static boolean toggleItalic(EditText editText) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start < 0 || end < 0 || start == end) {
            return false;
        }

        Editable editable = editText.getText();
        StyleSpan[] spans = editable.getSpans(start, end, StyleSpan.class);

        boolean hasItalic = false;
        for (StyleSpan span : spans) {
            if (span.getStyle() == Typeface.ITALIC || span.getStyle() == Typeface.BOLD_ITALIC) {
                editable.removeSpan(span);
                hasItalic = true;
            }
        }

        if (!hasItalic) {
            editable.setSpan(new StyleSpan(Typeface.ITALIC), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return true;
    }

    /**
     * Apply text size to the current selection
     * Returns true if formatting was applied, false if no text was selected
     */
    public static boolean applyTextSize(EditText editText, TextSize size) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start < 0 || end < 0 || start == end) {
            return false;
        }

        Editable editable = editText.getText();

        // Remove existing size spans in the selection
        RelativeSizeSpan[] spans = editable.getSpans(start, end, RelativeSizeSpan.class);
        for (RelativeSizeSpan span : spans) {
            editable.removeSpan(span);
        }

        // Apply new size span
        editable.setSpan(new RelativeSizeSpan(size.scale), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return true;
    }

}

