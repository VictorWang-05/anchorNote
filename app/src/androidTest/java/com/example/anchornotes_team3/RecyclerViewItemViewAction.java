package com.example.anchornotes_team3;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

/**
 * Helper class to perform actions on child views within RecyclerView items
 *
 * This allows us to click on specific buttons or views inside RecyclerView item layouts,
 * which is necessary for testing interactions with individual template items.
 */
public class RecyclerViewItemViewAction implements ViewAction {

    private final int viewId;
    private final ViewAction viewAction;

    public RecyclerViewItemViewAction(int viewId, ViewAction viewAction) {
        this.viewId = viewId;
        this.viewAction = viewAction;
    }

    @Override
    public Matcher<View> getConstraints() {
        return viewAction.getConstraints();
    }

    @Override
    public String getDescription() {
        return "Perform action on view with id: " + viewId;
    }

    @Override
    public void perform(UiController uiController, View view) {
        View childView = view.findViewById(viewId);
        if (childView != null) {
            viewAction.perform(uiController, childView);
        }
    }
}
