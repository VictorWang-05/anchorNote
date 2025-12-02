package com.example.anchornotes_team3.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.Stack;

/**
 * Helper class to manage undo/redo functionality for EditText fields.
 * Tracks text changes and allows reverting to previous states.
 */
public class UndoRedoHelper {

    /**
     * Represents a snapshot of text state at a point in time
     */
    private static class TextState {
        final String text;
        final int cursorPosition;

        TextState(String text, int cursorPosition) {
            this.text = text;
            this.cursorPosition = cursorPosition;
        }
    }

    private final EditText editText;
    private final Stack<TextState> undoStack = new Stack<>();
    private final Stack<TextState> redoStack = new Stack<>();

    private boolean isUndoingOrRedoing = false;
    private boolean isEnabled = true;
    private TextWatcher textWatcher;

    // Configuration
    private static final int MAX_HISTORY_SIZE = 100; // Maximum number of states to keep
    private static final long CHANGE_DELAY_MS = 500; // Minimum time between saves (ms)

    private long lastChangeTime = 0;
    private String pendingText = null;
    private int pendingCursor = 0;

    public UndoRedoHelper(EditText editText) {
        this.editText = editText;
        setupTextWatcher();
    }

    /**
     * Set up the text watcher to track changes
     */
    private void setupTextWatcher() {
        textWatcher = new TextWatcher() {
            private String textBefore = "";
            private int cursorBefore = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUndoingOrRedoing && isEnabled) {
                    textBefore = s.toString();
                    cursorBefore = editText.getSelectionStart();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUndoingOrRedoing && isEnabled) {
                    String textAfter = s.toString();
                    int cursorAfter = editText.getSelectionStart();

                    // Only save if text actually changed
                    if (!textBefore.equals(textAfter)) {
                        long currentTime = System.currentTimeMillis();

                        // Debounce: wait for pause in typing before saving state
                        if (currentTime - lastChangeTime < CHANGE_DELAY_MS) {
                            // User is still typing, store pending state
                            pendingText = textBefore;
                            pendingCursor = cursorBefore;
                        } else {
                            // Typing paused, save the state
                            if (pendingText != null) {
                                saveState(pendingText, pendingCursor);
                                pendingText = null;
                            }
                            saveState(textBefore, cursorBefore);
                        }

                        lastChangeTime = currentTime;

                        // Clear redo stack when new change is made
                        redoStack.clear();
                    }
                }
            }
        };

        editText.addTextChangedListener(textWatcher);
    }

    /**
     * Save the current state to undo stack
     */
    private void saveState(String text, int cursor) {
        // Don't save duplicate states
        if (!undoStack.isEmpty()) {
            TextState last = undoStack.peek();
            if (last.text.equals(text)) {
                return;
            }
        }

        undoStack.push(new TextState(text, cursor));

        // Limit stack size to prevent memory issues
        if (undoStack.size() > MAX_HISTORY_SIZE) {
            undoStack.remove(0);
        }
    }

    /**
     * Save the current state immediately (used when manually triggering save)
     */
    public void saveCurrentState() {
        if (!isEnabled || isUndoingOrRedoing) {
            return;
        }

        String currentText = editText.getText().toString();
        int currentCursor = editText.getSelectionStart();

        // Save any pending state first
        if (pendingText != null && !pendingText.equals(currentText)) {
            saveState(pendingText, pendingCursor);
            pendingText = null;
        }

        saveState(currentText, currentCursor);
    }

    /**
     * Perform undo operation
     * @return true if undo was performed, false if nothing to undo
     */
    public boolean undo() {
        if (!canUndo()) {
            return false;
        }

        // Save any pending changes first
        if (pendingText != null) {
            saveState(pendingText, pendingCursor);
            pendingText = null;
        }

        // Save current state to redo stack
        String currentText = editText.getText().toString();
        int currentCursor = editText.getSelectionStart();
        redoStack.push(new TextState(currentText, currentCursor));

        // Restore previous state
        TextState previousState = undoStack.pop();
        restoreState(previousState);

        return true;
    }

    /**
     * Perform redo operation
     * @return true if redo was performed, false if nothing to redo
     */
    public boolean redo() {
        if (!canRedo()) {
            return false;
        }

        // Save current state to undo stack
        String currentText = editText.getText().toString();
        int currentCursor = editText.getSelectionStart();
        undoStack.push(new TextState(currentText, currentCursor));

        // Restore next state
        TextState nextState = redoStack.pop();
        restoreState(nextState);

        return true;
    }

    /**
     * Restore a text state to the EditText
     */
    private void restoreState(TextState state) {
        isUndoingOrRedoing = true;

        try {
            editText.setText(state.text);

            // Restore cursor position safely
            int cursorPos = Math.min(state.cursorPosition, state.text.length());
            cursorPos = Math.max(0, cursorPos);
            editText.setSelection(cursorPos);
        } finally {
            isUndoingOrRedoing = false;
        }
    }

    /**
     * Check if undo is available
     */
    public boolean canUndo() {
        return isEnabled && !undoStack.isEmpty();
    }

    /**
     * Check if redo is available
     */
    public boolean canRedo() {
        return isEnabled && !redoStack.isEmpty();
    }

    /**
     * Clear all undo/redo history
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        pendingText = null;
    }

    /**
     * Enable or disable undo/redo tracking
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    /**
     * Check if undo/redo is enabled
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Clean up resources
     */
    public void destroy() {
        if (textWatcher != null) {
            editText.removeTextChangedListener(textWatcher);
        }
        clearHistory();
    }
}
