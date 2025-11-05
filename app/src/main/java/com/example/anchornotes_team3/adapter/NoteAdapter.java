package com.example.anchornotes_team3.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anchornotes_team3.R;
import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.model.Tag;
import com.google.android.material.chip.ChipGroup;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying notes in RecyclerView
 * Supports both horizontal (pinned/relevant) and vertical (all notes) layouts
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    
    private List<Note> notes;
    private final boolean isHorizontal; // true for horizontal RecyclerView, false for vertical
    private OnNoteClickListener listener;
    
    public NoteAdapter(boolean isHorizontal) {
        this.notes = new ArrayList<>();
        this.isHorizontal = isHorizontal;
    }
    
    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onAddTagClick(Note note);
        void onDeleteClick(Note note);
        void onPinClick(Note note);
    }
    
    public void setOnNoteClickListener(OnNoteClickListener listener) {
        this.listener = listener;
    }
    
    public void setNotes(List<Note> notes) {
        this.notes = notes != null ? notes : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addNote(Note note) {
        this.notes.add(note);
        notifyItemInserted(notes.size() - 1);
    }
    
    public void updateNote(Note note) {
        int position = -1;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId() != null && notes.get(i).getId().equals(note.getId())) {
                position = i;
                break;
            }
        }
        if (position >= 0) {
            notes.set(position, note);
            notifyItemChanged(position);
        }
    }

    /**
     * Get note at adapter position (helper for swipe actions)
     */
    public Note getNoteAt(int position) {
        if (position < 0 || position >= notes.size()) return null;
        return notes.get(position);
    }
    
    public void removeNote(String noteId) {
        int position = -1;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId() != null && notes.get(i).getId().equals(noteId)) {
                position = i;
                break;
            }
        }
        if (position >= 0) {
            notes.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isHorizontal ? R.layout.item_note_horizontal : R.layout.item_note_vertical;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new NoteViewHolder(view, isHorizontal);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note, listener);
    }
    
    @Override
    public int getItemCount() {
        return notes.size();
    }
    
    static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView noteTitle;
        private final TextView noteDate;
        private final ChipGroup noteTags;
        private final com.google.android.material.button.MaterialButton addTagButton;
        private final com.google.android.material.button.MaterialButton deleteButton;
        private final com.google.android.material.button.MaterialButton pinButton;
        private final boolean isHorizontal;

        public NoteViewHolder(@NonNull View itemView, boolean isHorizontal) {
            super(itemView);
            this.isHorizontal = isHorizontal;
            noteTitle = itemView.findViewById(R.id.noteTitle);
            noteDate = itemView.findViewById(R.id.noteDate);
            noteTags = itemView.findViewById(R.id.noteTags);
            addTagButton = itemView.findViewById(R.id.addTagButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            pinButton = itemView.findViewById(R.id.pinButton);
        }
        
        public void bind(Note note, OnNoteClickListener listener) {
            if (note == null) {
                return;
            }
            
            // Set title
            String title = note.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = "Untitled Note";
            }
            noteTitle.setText(title);
            
            // Set date
            Instant dateInstant = note.getLastEdited() != null ? note.getLastEdited() : note.getCreatedAt();
            String dateText = formatDate(dateInstant);
            noteDate.setText(dateText != null ? dateText : "");

            // Set tags
            if (noteTags != null) {
                noteTags.removeAllViews();
                if (note.getTags() != null && !note.getTags().isEmpty()) {
                    noteTags.setVisibility(View.VISIBLE);
                    for (Tag tag : note.getTags()) {
                        // Create a circular colored dot for each tag
                        View tagDot = new View(itemView.getContext());

                        // Set size based on layout type
                        int dotSize = isHorizontal ? 16 : 20; // Smaller for horizontal layout
                        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(dotSize, dotSize);
                        params.setMargins(0, 0, 8, 0); // Add spacing between dots
                        tagDot.setLayoutParams(params);

                        // Parse and set tag color
                        try {
                            int color = Color.parseColor(tag.getColor());
                            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
                            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                            drawable.setColor(color);
                            tagDot.setBackground(drawable);
                        } catch (Exception e) {
                            // Fallback to default color if parsing fails
                            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
                            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                            drawable.setColor(itemView.getContext().getColor(R.color.orange_primary));
                            tagDot.setBackground(drawable);
                        }

                        noteTags.addView(tagDot);
                    }
                } else {
                    noteTags.setVisibility(View.GONE);
                }
            }

            // Set up add tag button - always visible on all layouts
            if (addTagButton != null) {
                addTagButton.setVisibility(View.VISIBLE);
                addTagButton.setEnabled(true);
                addTagButton.setOnClickListener(v -> {
                    try {
                        // Prevent card click
                        itemView.setEnabled(false);
                        if (listener != null && note != null) {
                            listener.onAddTagClick(note);
                        }
                        itemView.postDelayed(() -> itemView.setEnabled(true), 100);
                    } catch (Exception e) {
                        android.util.Log.e("NoteAdapter", "Error in add tag click listener", e);
                    }
                });
            } else {
                android.util.Log.w("NoteAdapter", "addTagButton is null!");
            }
            
            // Set up delete button - always visible on all layouts
            if (deleteButton != null) {
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setEnabled(true);
                deleteButton.setOnClickListener(v -> {
                    try {
                        // Prevent card click
                        itemView.setEnabled(false);
                        if (listener != null && note != null) {
                            listener.onDeleteClick(note);
                        }
                        itemView.postDelayed(() -> itemView.setEnabled(true), 100);
                    } catch (Exception e) {
                        android.util.Log.e("NoteAdapter", "Error in delete click listener", e);
                    }
                });
            } else {
                android.util.Log.w("NoteAdapter", "deleteButton is null!");
            }
            
            // Set up pin button
            if (pinButton != null) {
                // Show pin button for horizontal layout (pinned/relevant) or for unpinned notes in vertical layout
                if (isHorizontal) {
                    // Always show for horizontal cards (pinned and relevant sections)
                    // For pinned notes, show down arrow (↓) to unpin
                    // For unpinned notes, show up arrow (↑) to pin
                    pinButton.setVisibility(View.VISIBLE);
                    if (note.isPinned()) {
                        pinButton.setText("↓"); // Down arrow for unpinning
                    } else {
                        pinButton.setText("↑"); // Up arrow for pinning
                    }
                } else {
                    // Only show for unpinned notes in vertical layout (All Notes section)
                    // This allows pinning notes from the All Notes section
                    pinButton.setVisibility(note.isPinned() ? View.GONE : View.VISIBLE);
                    if (!note.isPinned()) {
                        pinButton.setText("↑"); // Up arrow for pinning
                    }
                }
                
                pinButton.setOnClickListener(v -> {
                    try {
                        // Prevent card click
                        itemView.setEnabled(false);
                        if (listener != null && note != null) {
                            listener.onPinClick(note);
                        }
                        itemView.postDelayed(() -> itemView.setEnabled(true), 100);
                    } catch (Exception e) {
                        android.util.Log.e("NoteAdapter", "Error in pin click listener", e);
                    }
                });
            }
            
            // Set click listener on entire card (excluding buttons)
            itemView.setOnClickListener(v -> {
                try {
                    if (listener != null && note != null && note.getId() != null && !note.getId().isEmpty()) {
                        listener.onNoteClick(note);
                    }
                } catch (Exception e) {
                    android.util.Log.e("NoteAdapter", "Error in note click listener", e);
                }
            });
        }
        
        private String formatDate(Instant instant) {
            if (instant == null) {
                return "";
            }
            
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d")
                    .withZone(ZoneId.systemDefault());
                return formatter.format(instant);
            } catch (Exception e) {
                // Fallback to simple format
                return instant.toString().substring(0, Math.min(10, instant.toString().length()));
            }
        }
    }
}

