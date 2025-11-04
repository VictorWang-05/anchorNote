package com.example.anchornotes_team3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anchornotes_team3.R;
import com.example.anchornotes_team3.model.Template;
import com.example.anchornotes_team3.model.Tag;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying templates in RecyclerView
 */
public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder> {
    
    private List<Template> templates;
    private OnTemplateClickListener listener;
    
    public TemplateAdapter() {
        this.templates = new ArrayList<>();
    }
    
    public interface OnTemplateClickListener {
        void onUseTemplate(Template template);
        void onDeleteTemplate(Template template);
    }
    
    public void setOnTemplateClickListener(OnTemplateClickListener listener) {
        this.listener = listener;
    }
    
    public void setTemplates(List<Template> templates) {
        this.templates = templates != null ? templates : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_template, parent, false);
        return new TemplateViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        Template template = templates.get(position);
        holder.bind(template, listener);
    }
    
    @Override
    public int getItemCount() {
        return templates.size();
    }
    
    static class TemplateViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTemplateName;
        private TextView tvTemplatePreview;
        private ChipGroup chipGroupTags;
        private MaterialButton btnUseTemplate;
        private MaterialButton btnDeleteTemplate;
        
        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTemplateName = itemView.findViewById(R.id.tv_template_name);
            tvTemplatePreview = itemView.findViewById(R.id.tv_template_preview);
            chipGroupTags = itemView.findViewById(R.id.chip_group_tags);
            btnUseTemplate = itemView.findViewById(R.id.btn_use_template);
            btnDeleteTemplate = itemView.findViewById(R.id.btn_delete_template);
        }
        
        public void bind(Template template, OnTemplateClickListener listener) {
            // Set template name
            tvTemplateName.setText(template.getName() != null ? template.getName() : "Untitled Template");
            
            // Set preview text (first 150 characters)
            String preview = template.getText();
            if (preview != null && !preview.isEmpty()) {
                if (preview.length() > 150) {
                    preview = preview.substring(0, 150) + "...";
                }
                tvTemplatePreview.setText(preview);
                tvTemplatePreview.setVisibility(View.VISIBLE);
            } else {
                tvTemplatePreview.setText("No content");
                tvTemplatePreview.setVisibility(View.VISIBLE);
            }
            
            // Clear and populate tags
            chipGroupTags.removeAllViews();
            if (template.getTags() != null && !template.getTags().isEmpty()) {
                for (Tag tag : template.getTags()) {
                    Chip chip = new Chip(itemView.getContext());
                    chip.setText(tag.getName());
                    chip.setClickable(false);
                    chip.setCheckable(false);
                    
                    // Set tag color if available
                    if (tag.getColor() != null && !tag.getColor().isEmpty()) {
                        try {
                            int color = android.graphics.Color.parseColor(tag.getColor());
                            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));
                            chip.setTextColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                        } catch (Exception e) {
                            // Use default color if parsing fails
                        }
                    }
                    
                    chipGroupTags.addView(chip);
                }
            }
            
            // Set up click listeners
            btnUseTemplate.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUseTemplate(template);
                }
            });
            
            btnDeleteTemplate.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteTemplate(template);
                }
            });
        }
    }
}

