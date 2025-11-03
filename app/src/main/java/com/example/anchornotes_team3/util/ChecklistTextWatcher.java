package com.example.anchornotes_team3.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * TextWatcher that automatically continues checklist bullets when user presses Enter
 */
public class ChecklistTextWatcher implements TextWatcher {

    private final EditText editText;
    private boolean isProcessing = false;

    public ChecklistTextWatcher(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Not needed
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Not needed
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (isProcessing) {
            return;
        }

        isProcessing = true;

        try {
            int cursorPos = editText.getSelectionStart();
            if (cursorPos > 0 && cursorPos <= s.length()) {
                // Check if user just pressed Enter (newline character)
                if (cursorPos >= 1 && s.charAt(cursorPos - 1) == '\n') {
                    // Look backwards to find if previous line started with checklist bullet
                    int lineStart = findLineStart(s, cursorPos - 2);
                    if (lineStart >= 0 && lineStart < s.length()) {
                        String lineText = s.subSequence(lineStart, cursorPos - 1).toString().trim();
                        if (lineText.startsWith("☐") || lineText.startsWith("☑")) {
                            // Auto-insert bullet on new line
                            s.insert(cursorPos, "☐ ");
                            editText.setSelection(cursorPos + 2);
                        }
                    }
                }
            }
        } finally {
            isProcessing = false;
        }
    }

    private int findLineStart(Editable s, int fromPos) {
        for (int i = fromPos; i >= 0; i--) {
            if (s.charAt(i) == '\n') {
                return i + 1;
            }
        }
        return 0; // Beginning of text
    }
}

