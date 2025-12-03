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
import com.example.anchornotes_team3.PomodoroActivity;

public class BottomNavigationHelper {

    public enum NavItem {
        HOME,
        FILTER,
        TEMPLATES,
        STATS,
        POMODORO
    }

    public static void setupBottomNavigation(Activity activity, AuthManager authManager, NavItem activeItem) {
        View navHome = activity.findViewById(R.id.nav_home);
        View navFilter = activity.findViewById(R.id.nav_filter);
        View navTemplates = activity.findViewById(R.id.nav_templates);
        View navStats = activity.findViewById(R.id.nav_stats);
        View navPomodoro = activity.findViewById(R.id.nav_pomodoro);

        // Set active state for the current page
        setActiveState(activity, navHome, navFilter, navTemplates, navStats, navPomodoro, activeItem);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                if (!(activity instanceof MainActivity)) {
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                    applyTransition(activity, activeItem, NavItem.HOME);
                }
            });
        }

        if (navFilter != null) {
            navFilter.setOnClickListener(v -> {
                if (!(activity instanceof FilterOptionsActivity)) {
                    if (authManager.isLoggedIn()) {
                        Intent intent = new Intent(activity, FilterOptionsActivity.class);
                        activity.startActivity(intent);
                        applyTransition(activity, activeItem, NavItem.FILTER);
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
                        applyTransition(activity, activeItem, NavItem.TEMPLATES);
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
                        applyTransition(activity, activeItem, NavItem.STATS);
                    } else {
                        Toast.makeText(activity, "Please login first", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (navPomodoro != null) {
            navPomodoro.setOnClickListener(v -> {
                if (!(activity instanceof PomodoroActivity)) {
                    Intent intent = new Intent(activity, PomodoroActivity.class);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
        }
    }

    /**
     * Apply directional slide animation based on navigation order
     * Order: FILTER -> TEMPLATES -> HOME -> STATS
     */
    private static void applyTransition(Activity activity, NavItem from, NavItem to) {
        int fromIndex = getNavIndex(from);
        int toIndex = getNavIndex(to);

        if (toIndex > fromIndex) {
            // Moving right in the navigation bar
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            // Moving left in the navigation bar
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    /**
     * Get the position index of each nav item (left to right)
     */
    private static int getNavIndex(NavItem item) {
        switch (item) {
            case FILTER: return 0;
            case TEMPLATES: return 1;
            case HOME: return 2;
            case STATS: return 3;
            default: return 2; // Default to HOME
        }
    }

    private static void setActiveState(Activity activity, View navHome, View navFilter,
                                      View navTemplates, View navStats, View navPomodoro, NavItem activeItem) {
        // Reset all buttons to inactive state
        resetButton(activity, navHome);
        resetButton(activity, navFilter);
        resetButton(activity, navTemplates);
        resetButton(activity, navStats);
        resetButton(activity, navPomodoro);

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
            case POMODORO:
                activateButton(activity, navPomodoro);
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
            button.getId() == R.id.nav_stats ? R.id.nav_stats_icon :
            R.id.nav_pomodoro_icon
        );
        TextView label = container.findViewById(
            button.getId() == R.id.nav_home ? R.id.nav_home_label :
            button.getId() == R.id.nav_filter ? R.id.nav_filter_label :
            button.getId() == R.id.nav_templates ? R.id.nav_templates_label :
            button.getId() == R.id.nav_stats ? R.id.nav_stats_label :
            R.id.nav_pomodoro_label
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
            button.getId() == R.id.nav_stats ? R.id.nav_stats_icon :
            R.id.nav_pomodoro_icon
        );
        TextView label = container.findViewById(
            button.getId() == R.id.nav_home ? R.id.nav_home_label :
            button.getId() == R.id.nav_filter ? R.id.nav_filter_label :
            button.getId() == R.id.nav_templates ? R.id.nav_templates_label :
            button.getId() == R.id.nav_stats ? R.id.nav_stats_label :
            R.id.nav_pomodoro_label
        );

        if (icon != null) {
            icon.setColorFilter(activity.getResources().getColor(R.color.accent_blue, null));
        }
        if (label != null) {
            label.setTextColor(activity.getResources().getColor(R.color.accent_blue, null));
        }
    }
}
