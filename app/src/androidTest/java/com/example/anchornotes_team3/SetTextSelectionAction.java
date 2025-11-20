package com.example.anchornotes_team3;

import android.view.View;
import android.widget.EditText;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.Matchers.allOf;

/**
 * Custom Espresso ViewAction to set text selection in an EditText
 * This is used for testing text formatting operations like bold and italic
 */
public class SetTextSelectionAction implements ViewAction {

    private final int start;
    private final int end;

    public SetTextSelectionAction(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public Matcher<View> getConstraints() {
        return allOf(isDisplayed(), isAssignableFrom(EditText.class));
    }

    @Override
    public String getDescription() {
        return "Set text selection from " + start + " to " + end;
    }

    @Override
    public void perform(UiController uiController, View view) {
        EditText editText = (EditText) view;
        editText.setSelection(start, end);
        uiController.loopMainThreadUntilIdle();
    }

    public static ViewAction setSelection(int start, int end) {
        return new SetTextSelectionAction(start, end);
    }
}

