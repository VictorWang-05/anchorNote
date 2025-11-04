package com.example.anchornotes_team3.util;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.widget.EditText;

/**
 * TextWatcher that applies formatting styles (bold, italic, size) to newly typed text
 * when formatting buttons are in "toggle" mode.
 */
public class FormattingTextWatcher implements TextWatcher {
    
    private final EditText editText;
    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private Float activeSizeScale = null; // null = normal size
    private int lastCursorPosition = 0;
    private int textLengthBefore = 0;
    
    public FormattingTextWatcher(EditText editText) {
        this.editText = editText;
    }
    
    public void toggleBold() {
        isBoldActive = !isBoldActive;
    }
    
    public void toggleItalic() {
        isItalicActive = !isItalicActive;
    }
    
    public void setSize(TextSpanUtils.TextSize size) {
        // Toggle: if same size clicked again, turn off
        if (activeSizeScale != null && activeSizeScale.equals(size.scale)) {
            activeSizeScale = null;
        } else {
            activeSizeScale = size.scale;
        }
    }
    
    public boolean isBoldActive() {
        return isBoldActive;
    }
    
    public boolean isItalicActive() {
        return isItalicActive;
    }
    
    public boolean isSizeActive() {
        return activeSizeScale != null;
    }
    
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        textLengthBefore = s.length();
        lastCursorPosition = editText.getSelectionStart();
    }
    
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Do nothing here
    }
    
    @Override
    public void afterTextChanged(Editable s) {
        // Only apply formatting if text was added (not deleted)
        if (s.length() > textLengthBefore) {
            int insertedCount = s.length() - textLengthBefore;
            int insertStart = lastCursorPosition;
            int insertEnd = insertStart + insertedCount;
            
            // Make sure we're within bounds
            if (insertStart >= 0 && insertEnd <= s.length() && insertStart < insertEnd) {
                applyActiveFormatting(s, insertStart, insertEnd);
            }
        }
    }
    
    private void applyActiveFormatting(Editable editable, int start, int end) {
        // Apply bold if active
        if (isBoldActive) {
            editable.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        // Apply italic if active
        if (isItalicActive) {
            editable.setSpan(new StyleSpan(Typeface.ITALIC), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        // Apply size if active
        if (activeSizeScale != null) {
            editable.setSpan(new RelativeSizeSpan(activeSizeScale), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}

