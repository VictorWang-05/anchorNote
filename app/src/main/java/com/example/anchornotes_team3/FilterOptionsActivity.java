package com.example.anchornotes_team3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.anchornotes_team3.model.FilterCriteria;
import com.example.anchornotes_team3.model.Tag;
import com.example.anchornotes_team3.repository.NoteRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class FilterOptionsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ChipGroup chipGroupTags;
    private TextView tvNoTags;
    private MaterialCheckBox checkboxHasPhoto;
    private MaterialCheckBox checkboxHasAudio;
    private MaterialButton btnClearFilter;
    private MaterialButton btnApplyFilter;

    private NoteRepository noteRepository;
    private List<Tag> availableTags = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_options);

        noteRepository = NoteRepository.getInstance(this);

        initializeViews();
        setupToolbar();
        loadAvailableTags();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        chipGroupTags = findViewById(R.id.chip_group_tags);
        tvNoTags = findViewById(R.id.tv_no_tags);
        checkboxHasPhoto = findViewById(R.id.checkbox_has_photo);
        checkboxHasAudio = findViewById(R.id.checkbox_has_audio);
        btnClearFilter = findViewById(R.id.btn_clear_filter);
        btnApplyFilter = findViewById(R.id.btn_apply_filter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadAvailableTags() {
        noteRepository.getAllTags(new NoteRepository.TagsCallback() {
            @Override
            public void onSuccess(List<Tag> tags) {
                availableTags = tags;
                populateTagChips();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(FilterOptionsActivity.this, "Failed to load tags: " + error, Toast.LENGTH_SHORT).show();
                tvNoTags.setVisibility(View.VISIBLE);
            }
        });
    }

    private void populateTagChips() {
        chipGroupTags.removeAllViews();

        if (availableTags.isEmpty()) {
            tvNoTags.setVisibility(View.VISIBLE);
            return;
        }

        tvNoTags.setVisibility(View.GONE);

        for (Tag tag : availableTags) {
            Chip chip = new Chip(this);
            chip.setText(tag.getName());
            chip.setCheckable(true);
            chip.setClickable(true);

            if (tag.getColor() != null && !tag.getColor().isEmpty()) {
                try {
                    int color = android.graphics.Color.parseColor(tag.getColor());
                    int lighterColor = lightenColor(color, 0.3f);

                    // Create color state list for checked/unchecked states
                    int[][] states = new int[][] {
                        new int[] { android.R.attr.state_checked },
                        new int[] { -android.R.attr.state_checked }
                    };
                    int[] colors = new int[] {
                        color,
                        lighterColor
                    };
                    android.content.res.ColorStateList chipColorStateList = new android.content.res.ColorStateList(states, colors);

                    chip.setChipBackgroundColor(chipColorStateList);
                    chip.setTextColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));

                    // Add checkmark icon when selected
                    chip.setCheckedIconVisible(true);
                    chip.setChipStrokeWidth(4f);
                    chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                } catch (Exception e) {
                    // Use default color
                }
            }

            chip.setTag(tag);
            chipGroupTags.addView(chip);
        }
    }

    private int lightenColor(int color, float factor) {
        int red = android.graphics.Color.red(color);
        int green = android.graphics.Color.green(color);
        int blue = android.graphics.Color.blue(color);

        red = (int) (red + (255 - red) * factor);
        green = (int) (green + (255 - green) * factor);
        blue = (int) (blue + (255 - blue) * factor);

        return android.graphics.Color.rgb(red, green, blue);
    }

    private void setupClickListeners() {
        btnClearFilter.setOnClickListener(v -> clearAllFilters());
        btnApplyFilter.setOnClickListener(v -> applyFilter());
    }

    private void clearAllFilters() {
        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            View child = chipGroupTags.getChildAt(i);
            if (child instanceof Chip) {
                ((Chip) child).setChecked(false);
            }
        }

        checkboxHasPhoto.setChecked(false);
        checkboxHasAudio.setChecked(false);

        Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show();
    }

    private void applyFilter() {
        FilterCriteria criteria = new FilterCriteria();

        List<String> selectedTagIds = new ArrayList<>();
        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            View child = chipGroupTags.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked() && chip.getTag() instanceof Tag) {
                    Tag tag = (Tag) chip.getTag();
                    selectedTagIds.add(tag.getId());
                }
            }
        }
        criteria.setTagIds(selectedTagIds);

        if (checkboxHasPhoto.isChecked()) {
            criteria.setHasPhoto(true);
        }

        if (checkboxHasAudio.isChecked()) {
            criteria.setHasAudio(true);
        }

        if (criteria.isEmpty()) {
            Toast.makeText(this, "Please select at least one filter", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, FilterResultsActivity.class);
        intent.putExtra(FilterResultsActivity.EXTRA_FILTER_CRITERIA, criteria);
        startActivity(intent);
    }
}
