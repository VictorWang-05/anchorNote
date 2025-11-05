package com.example.anchornotes_team3.util;

import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
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
        
        // Store selection to restore after modification
        final int selectionStart = start;
        final int selectionEnd = end;
        
        // Verify we have valid text
        if (editable == null || editable.length() == 0) {
            return false;
        }
        
        // Verify selection is within bounds
        if (start < 0 || end < 0 || start > editable.length() || end > editable.length()) {
            return false;
        }
        
        // Check what styles are present in the selection
        boolean hasBold = false;
        boolean hasItalic = false;
        
        StyleSpan[] spans = editable.getSpans(start, end, StyleSpan.class);
        for (StyleSpan span : spans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            
            // Check if this span affects the selection
            if (spanStart < end && spanEnd > start) {
                int style = span.getStyle();
                if (style == Typeface.BOLD || style == Typeface.BOLD_ITALIC) {
                    hasBold = true;
                }
                if (style == Typeface.ITALIC || style == Typeface.BOLD_ITALIC) {
                    hasItalic = true;
                }
            }
        }
        
        // Remove all style spans that overlap with the selection
        // We'll reapply them properly after
        StyleSpan[] allSpans = editable.getSpans(0, editable.length(), StyleSpan.class);
        for (StyleSpan span : allSpans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            
            // Remove spans that overlap with selection
            if (spanStart < end && spanEnd > start) {
                editable.removeSpan(span);
                
                // Reapply span for parts outside selection
                int style = span.getStyle();
                if (spanStart < start) {
                    // Part before selection
                    if (style == Typeface.BOLD) {
                        editable.setSpan(new StyleSpan(Typeface.BOLD), spanStart, start, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == Typeface.ITALIC) {
                        editable.setSpan(new StyleSpan(Typeface.ITALIC), spanStart, start, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == Typeface.BOLD_ITALIC) {
                        editable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), spanStart, start, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                if (spanEnd > end) {
                    // Part after selection
                    if (style == Typeface.BOLD) {
                        editable.setSpan(new StyleSpan(Typeface.BOLD), end, spanEnd, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == Typeface.ITALIC) {
                        editable.setSpan(new StyleSpan(Typeface.ITALIC), end, spanEnd, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == Typeface.BOLD_ITALIC) {
                        editable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), end, spanEnd, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }

        // Apply new formatting to selection
        if (!hasBold) {
            // Add bold - preserve italic if present
            if (hasItalic) {
                editable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                editable.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            // Remove bold - keep italic if present
            if (hasItalic) {
                editable.setSpan(new StyleSpan(Typeface.ITALIC), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            // If no italic, text becomes normal (no span needed)
        }
        
        // Restore selection - use post to ensure it happens after layout
        editText.post(() -> {
            // Ensure text is still there
            if (editText.getText() != null && editText.getText().length() > 0) {
                // Clamp selection to valid range
                int maxLength = editText.getText().length();
                int safeStart = Math.max(0, Math.min(selectionStart, maxLength));
                int safeEnd = Math.max(0, Math.min(selectionEnd, maxLength));
                if (safeStart <= safeEnd) {
                    editText.setSelection(safeStart, safeEnd);
                }
            }
        });
        
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
        
        // Store selection to restore after modification
        final int selectionStart = start;
        final int selectionEnd = end;
        
        // Verify we have valid text
        if (editable == null || editable.length() == 0) {
            return false;
        }
        
        // Verify selection is within bounds
        if (start < 0 || end < 0 || start > editable.length() || end > editable.length()) {
            return false;
        }
        
        // Check what styles are present in the selection
        boolean hasBold = false;
        boolean hasItalic = false;
        
        StyleSpan[] spans = editable.getSpans(start, end, StyleSpan.class);
        for (StyleSpan span : spans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            
            // Check if this span affects the selection
            if (spanStart < end && spanEnd > start) {
                int style = span.getStyle();
                if (style == Typeface.BOLD || style == Typeface.BOLD_ITALIC) {
                    hasBold = true;
                }
                if (style == Typeface.ITALIC || style == Typeface.BOLD_ITALIC) {
                    hasItalic = true;
                }
            }
        }
        
        // Remove all style spans that overlap with the selection
        StyleSpan[] allSpans = editable.getSpans(0, editable.length(), StyleSpan.class);
        for (StyleSpan span : allSpans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            
            // Remove spans that overlap with selection
            if (spanStart < end && spanEnd > start) {
                editable.removeSpan(span);
                
                // Reapply span for parts outside selection
                int style = span.getStyle();
                if (spanStart < start) {
                    // Part before selection
                    if (style == Typeface.BOLD) {
                        editable.setSpan(new StyleSpan(Typeface.BOLD), spanStart, start, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == Typeface.ITALIC) {
                        editable.setSpan(new StyleSpan(Typeface.ITALIC), spanStart, start, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == Typeface.BOLD_ITALIC) {
                        editable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), spanStart, start, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                if (spanEnd > end) {
                    // Part after selection
                    if (style == Typeface.BOLD) {
                        editable.setSpan(new StyleSpan(Typeface.BOLD), end, spanEnd, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == Typeface.ITALIC) {
                        editable.setSpan(new StyleSpan(Typeface.ITALIC), end, spanEnd, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == Typeface.BOLD_ITALIC) {
                        editable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), end, spanEnd, 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }

        // Apply new formatting to selection
        if (!hasItalic) {
            // Add italic - preserve bold if present
            if (hasBold) {
                editable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                editable.setSpan(new StyleSpan(Typeface.ITALIC), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            // Remove italic - keep bold if present
            if (hasBold) {
                editable.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            // If no bold, text becomes normal (no span needed)
        }
        
        // Restore selection - use post to ensure it happens after layout
        editText.post(() -> {
            // Ensure text is still there
            if (editText.getText() != null && editText.getText().length() > 0) {
                // Clamp selection to valid range
                int maxLength = editText.getText().length();
                int safeStart = Math.max(0, Math.min(selectionStart, maxLength));
                int safeEnd = Math.max(0, Math.min(selectionEnd, maxLength));
                if (safeStart <= safeEnd) {
                    editText.setSelection(safeStart, safeEnd);
                }
            }
        });
        
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
        
        // Store selection to restore after modification
        final int selectionStart = start;
        final int selectionEnd = end;
        
        // Verify we have valid text
        if (editable == null || editable.length() == 0) {
            return false;
        }
        
        // Verify selection is within bounds
        if (start < 0 || end < 0 || start > editable.length() || end > editable.length()) {
            return false;
        }

        // Remove existing size spans that overlap with the selection
        RelativeSizeSpan[] allSpans = editable.getSpans(0, editable.length(), RelativeSizeSpan.class);
        for (RelativeSizeSpan span : allSpans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            
            // Remove spans that overlap with selection
            if (spanStart < end && spanEnd > start) {
                float originalScale = span.getSizeChange();
                editable.removeSpan(span);
                
                // Reapply span for parts outside selection
                if (spanStart < start) {
                    editable.setSpan(new RelativeSizeSpan(originalScale), spanStart, start, 
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (spanEnd > end) {
                    editable.setSpan(new RelativeSizeSpan(originalScale), end, spanEnd, 
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        // Apply new size span
        editable.setSpan(new RelativeSizeSpan(size.scale), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        // Restore selection - use post to ensure it happens after layout
        editText.post(() -> {
            // Ensure text is still there
            if (editText.getText() != null && editText.getText().length() > 0) {
                // Clamp selection to valid range
                int maxLength = editText.getText().length();
                int safeStart = Math.max(0, Math.min(selectionStart, maxLength));
                int safeEnd = Math.max(0, Math.min(selectionEnd, maxLength));
                if (safeStart <= safeEnd) {
                    editText.setSelection(safeStart, safeEnd);
                }
            }
        });
        
        return true;
    }

}
