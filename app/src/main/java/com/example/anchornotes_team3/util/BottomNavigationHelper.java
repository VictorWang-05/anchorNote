package com.example.anchornotes_team3.util;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.anchornotes_team3.FilterOptionsActivity;
import com.example.anchornotes_team3.MainActivity;
import com.example.anchornotes_team3.R;
import com.example.anchornotes_team3.StatisticsActivity;
import com.example.anchornotes_team3.TemplateActivity;
import com.example.anchornotes_team3.auth.AuthManager;

public class BottomNavigationHelper {

    public enum NavItem {
        HOME,
        FILTER,
        TEMPLATES,
        STATS
    }

    public static void setupBottomNavigation(Activity activity, AuthManager authManager, NavItem activeItem) {
        View navHome = activity.findViewById(R.id.nav_home);
        View navFilter = activity.findViewById(R.id.nav_filter);
        View navTemplates = activity.findViewById(R.id.nav_templates);
        View navStats = activity.findViewById(R.id.nav_stats);

        // Set active state for the current page
        setActiveState(activity, navHome, navFilter, navTemplates, navStats, activeItem);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                if (!(activity instanceof MainActivity)) {
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
        }

        if (navFilter != null) {
            navFilter.setOnClickListener(v -> {
                if (!(activity instanceof FilterOptionsActivity)) {
                    if (authManager.isLoggedIn()) {
                        Intent intent = new Intent(activity, FilterOptionsActivity.class);
                        activity.startActivity(intent);
                        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } else {
                        Toast.makeText(activity, "Please login to filter notes", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (navTemplates != null) {
            navTemplates.setOnClickListener(v -> {
                if (!(activity instanceof TemplateActivity)) {
                    if (authManager.isLoggedIn()) {
                        Intent intent = new Intent(activity, TemplateActivity.class);
                        activity.startActivity(intent);
                        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } else {
                        Toast.makeText(activity, "Please login first", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (navStats != null) {
            navStats.setOnClickListener(v -> {
                if (!(activity instanceof StatisticsActivity)) {
                    if (authManager.isLoggedIn()) {
                        Intent intent = new Intent(activity, StatisticsActivity.class);
                        activity.startActivity(intent);
                        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } else {
                        Toast.makeText(activity, "Please login first", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private static void setActiveState(Activity activity, View navHome, View navFilter,
                                      View navTemplates, View navStats, NavItem activeItem) {
        // Reset all buttons to inactive state
        resetButton(activity, navHome);
        resetButton(activity, navFilter);
        resetButton(activity, navTemplates);
        resetButton(activity, navStats);

        // Activate the current page's button
        switch (activeItem) {
            case HOME:
                activateButton(activity, navHome);
                break;
            case FILTER:
                activateButton(activity, navFilter);
                break;
            case TEMPLATES:
                activateButton(activity, navTemplates);
                break;
            case STATS:
                activateButton(activity, navStats);
                break;
        }
    }

    private static void resetButton(Activity activity, View button) {
        if (button == null) return;

        ViewGroup container = (ViewGroup) button;
        ImageView icon = container.findViewById(
            button.getId() == R.id.nav_home ? R.id.nav_home_icon :
            button.getId() == R.id.nav_filter ? R.id.nav_filter_icon :
            button.getId() == R.id.nav_templates ? R.id.nav_templates_icon :
            R.id.nav_stats_icon
        );
        TextView label = container.findViewById(
            button.getId() == R.id.nav_home ? R.id.nav_home_label :
            button.getId() == R.id.nav_filter ? R.id.nav_filter_label :
            button.getId() == R.id.nav_templates ? R.id.nav_templates_label :
            R.id.nav_stats_label
        );

        if (icon != null) {
            icon.setColorFilter(activity.getResources().getColor(R.color.text_secondary, null));
        }
        if (label != null) {
            label.setTextColor(activity.getResources().getColor(R.color.text_secondary, null));
        }
    }

    private static void activateButton(Activity activity, View button) {
        if (button == null) return;

        ViewGroup container = (ViewGroup) button;
        ImageView icon = container.findViewById(
            button.getId() == R.id.nav_home ? R.id.nav_home_icon :
            button.getId() == R.id.nav_filter ? R.id.nav_filter_icon :
            button.getId() == R.id.nav_templates ? R.id.nav_templates_icon :
            R.id.nav_stats_icon
        );
        TextView label = container.findViewById(
            button.getId() == R.id.nav_home ? R.id.nav_home_label :
            button.getId() == R.id.nav_filter ? R.id.nav_filter_label :
            button.getId() == R.id.nav_templates ? R.id.nav_templates_label :
            R.id.nav_stats_label
        );

        if (icon != null) {
            icon.setColorFilter(activity.getResources().getColor(R.color.accent_blue, null));
        }
        if (label != null) {
            label.setTextColor(activity.getResources().getColor(R.color.accent_blue, null));
        }
    }
}
