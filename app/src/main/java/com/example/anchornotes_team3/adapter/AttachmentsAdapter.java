package com.example.anchornotes_team3.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.anchornotes_team3.R;
import com.example.anchornotes_team3.model.Attachment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying attachments (photos and audio) in a RecyclerView
 */
public class AttachmentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_PHOTO = 1;
    private static final int VIEW_TYPE_AUDIO = 2;

    private final List<Attachment> attachments;
    private OnAttachmentActionListener listener;

    public interface OnAttachmentActionListener {
        void onRemoveAttachment(Attachment attachment);
        void onPlayAudio(Attachment attachment);
        void onPauseAudio(Attachment attachment);
    }

    public AttachmentsAdapter() {
        this.attachments = new ArrayList<>();
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments.clear();
        this.attachments.addAll(attachments);
        notifyDataSetChanged();
    }

    public void setOnAttachmentActionListener(OnAttachmentActionListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Attachment attachment = attachments.get(position);
        return attachment.getType() == Attachment.AttachmentType.PHOTO 
                ? VIEW_TYPE_PHOTO 
                : VIEW_TYPE_AUDIO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_PHOTO) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attachment_photo, parent, false);
            return new PhotoViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attachment_audio, parent, false);
            return new AudioViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Attachment attachment = attachments.get(position);
        
        if (holder instanceof PhotoViewHolder) {
            ((PhotoViewHolder) holder).bind(attachment);
        } else if (holder instanceof AudioViewHolder) {
            ((AudioViewHolder) holder).bind(attachment);
        }
    }

    @Override
    public int getItemCount() {
        return attachments.size();
    }

    // ViewHolder for Photo attachments
    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivPhoto;
        private final MaterialButton btnRemove;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }

        public void bind(Attachment attachment) {
            // Load image from URI (local) or mediaUrl (backend)
            if (attachment.getUri() != null) {
                // Local file - load from URI
                Glide.with(itemView.getContext())
                        .load(attachment.getUri())
                        .centerCrop()
                        .into(ivPhoto);
            } else if (attachment.getMediaUrl() != null) {
                // Backend file - load from URL
                Glide.with(itemView.getContext())
                        .load(attachment.getMediaUrl())
                        .centerCrop()
                        .into(ivPhoto);
            }
            
            // Handle remove click
            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveAttachment(attachment);
                }
            });
        }
    }

    // ViewHolder for Audio attachments
    class AudioViewHolder extends RecyclerView.ViewHolder {
        private final MaterialButton btnPlay;
        private final TextView tvAudio;
        private final TextView tvDuration;
        private final MaterialButton btnRemove;
        private boolean isPlaying = false;

        public AudioViewHolder(@NonNull View itemView) {
            super(itemView);
            btnPlay = itemView.findViewById(R.id.btn_play);
            tvAudio = itemView.findViewById(R.id.tv_audio);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }

        public void bind(Attachment attachment) {
            // Display duration
            String duration = attachment.getFormattedDuration();
            if (!duration.isEmpty()) {
                tvDuration.setText(duration);
            } else {
                tvDuration.setText("0:00");
            }
            
            // Handle play/pause toggle
            btnPlay.setOnClickListener(v -> {
                isPlaying = !isPlaying;
                if (isPlaying) {
                    btnPlay.setIcon(itemView.getContext().getDrawable(android.R.drawable.ic_media_pause));
                    if (listener != null) {
                        listener.onPlayAudio(attachment);
                    }
                } else {
                    btnPlay.setIcon(itemView.getContext().getDrawable(android.R.drawable.ic_media_play));
                    if (listener != null) {
                        listener.onPauseAudio(attachment);
                    }
                }
            });
            
            // Handle remove click
            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveAttachment(attachment);
                }
            });
        }
    }
}

