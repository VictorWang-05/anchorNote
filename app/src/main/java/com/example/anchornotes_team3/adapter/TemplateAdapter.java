package com.example.anchornotes_team3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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
    private java.util.Set<String> activeGeofenceIds; // Track which geofences user is inside
    
    public TemplateAdapter() {
        this.templates = new ArrayList<>();
        this.activeGeofenceIds = new java.util.HashSet<>();
    }
    
    /**
     * Update which geofences are currently active
     * @param activeGeofenceIds Set of active geofence IDs (e.g., "note_123")
     */
    public void setActiveGeofenceIds(java.util.Set<String> activeGeofenceIds) {
        this.activeGeofenceIds = activeGeofenceIds != null ? activeGeofenceIds : new java.util.HashSet<>();
        notifyDataSetChanged();
    }
    
    public interface OnTemplateClickListener {
        void onUseTemplate(Template template);
        void onDeleteTemplate(Template template);
        void onEditTemplate(Template template);
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
        
        // Check if template's geofence is currently active
        boolean isLocationRelevant = false;
        if (template.getGeofence() != null && template.getGeofence().getId() != null) {
            String templateGeofenceId = template.getGeofence().getId();
            isLocationRelevant = activeGeofenceIds.contains(templateGeofenceId);
        }
        
        // Check if this is the example template
        boolean isExample = "example_template_hardcoded".equals(template.getId());
        
        holder.bind(template, listener, isLocationRelevant, isExample);
    }
    
    @Override
    public int getItemCount() {
        return templates.size();
    }
    
    static class TemplateViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTemplateName;
        private TextView tvTemplatePreview;
        private TextView tvLocationIndicator;
        private TextView tvExampleIndicator;
        private ChipGroup chipGroupTags;
        private MaterialButton btnUseTemplate;
        private MaterialButton btnEditTemplate;
        private MaterialButton btnDeleteTemplate;
        
        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTemplateName = itemView.findViewById(R.id.tv_template_name);
            tvTemplatePreview = itemView.findViewById(R.id.tv_template_preview);
            tvLocationIndicator = itemView.findViewById(R.id.tv_location_indicator);
            tvExampleIndicator = itemView.findViewById(R.id.tv_example_indicator);
            chipGroupTags = itemView.findViewById(R.id.chip_group_tags);
            btnUseTemplate = itemView.findViewById(R.id.btn_use_template);
            btnEditTemplate = itemView.findViewById(R.id.btn_edit_template);
            btnDeleteTemplate = itemView.findViewById(R.id.btn_delete_template);
        }
        
        public void bind(Template template, OnTemplateClickListener listener, boolean isLocationRelevant, boolean isExample) {
            // Apply background color if available
            if (itemView instanceof CardView) {
                CardView cardView = (CardView) itemView;
                if (template.getBackgroundColor() != null && !template.getBackgroundColor().isEmpty()) {
                    try {
                        int color = android.graphics.Color.parseColor(template.getBackgroundColor());
                        cardView.setCardBackgroundColor(color);
                    } catch (Exception e) {
                        // Use default background if parsing fails
                        cardView.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.note_card_beige, null));
                    }
                } else {
                    // Use default background
                    cardView.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.note_card_beige, null));
                }
            }

            // Set template name
            tvTemplateName.setText(template.getName() != null ? template.getName() : "Untitled Template");

            // Show/hide location indicator
            if (tvLocationIndicator != null) {
                tvLocationIndicator.setVisibility(isLocationRelevant ? View.VISIBLE : View.GONE);
            }
            
            // Show/hide example indicator
            if (tvExampleIndicator != null) {
                tvExampleIndicator.setVisibility(isExample ? View.VISIBLE : View.GONE);
            }
            
            // Set preview text (first 150 characters, stripped of markdown)
            String preview = template.getText();
            if (preview != null && !preview.isEmpty()) {
                // Strip markdown syntax for cleaner preview
                preview = stripMarkdown(preview);
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
            btnEditTemplate.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditTemplate(template);
                }
            });
            
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
        
        /**
         * Strip markdown syntax from text for cleaner preview display
         * Removes bold (**), italic (*), big (<big>), small (<small>), etc.
         */
        private String stripMarkdown(String text) {
            if (text == null) return "";
            
            return text
                // Remove bold (***text*** or **text**)
                .replaceAll("\\*\\*\\*([^*]+)\\*\\*\\*", "$1")
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                // Remove italic (*text*)
                .replaceAll("\\*([^*]+)\\*", "$1")
                // Remove big/small tags
                .replaceAll("<big>", "")
                .replaceAll("</big>", "")
                .replaceAll("<small>", "")
                .replaceAll("</small>", "")
                // Remove any remaining asterisks
                .replaceAll("\\*", "")
                // Clean up multiple spaces
                .replaceAll("\\s+", " ")
                .trim();
        }
    }
}

