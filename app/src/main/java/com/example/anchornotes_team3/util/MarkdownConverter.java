package com.example.anchornotes_team3.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.noties.markwon.Markwon;

/**
 * Utility class to convert between Android Spanned text and Markdown format.
 * This allows formatted text (bold, italic, sizes) to be persisted in the database.
 */
public class MarkdownConverter {

    /**
     * Represents a formatting span with its position and type
     */
    private static class SpanInfo implements Comparable<SpanInfo> {
        int start;
        int end;
        SpanType type;

        enum SpanType {
            BOLD, ITALIC, BOLD_ITALIC, SMALL, LARGE
        }

        SpanInfo(int start, int end, SpanType type) {
            this.start = start;
            this.end = end;
            this.type = type;
        }

        @Override
        public int compareTo(SpanInfo other) {
            // Sort by start position, then by end position (descending for nesting)
            if (this.start != other.start) {
                return Integer.compare(this.start, other.start);
            }
            return Integer.compare(other.end, this.end); // Longer spans first
        }
    }

    /**
     * Convert formatted Spanned text to Markdown string
     */
    public static String toMarkdown(Spanned spanned) {
        if (spanned == null || spanned.length() == 0) {
            return "";
        }

        String text = spanned.toString();
        android.util.Log.d("MarkdownConverter", "📤 toMarkdown input text: " + text);

        List<SpanInfo> spans = extractSpans(spanned);
        android.util.Log.d("MarkdownConverter", "🎨 Found " + spans.size() + " spans");

        if (spans.isEmpty()) {
            return text;
        }

        // Log span details
        for (SpanInfo span : spans) {
            android.util.Log.d("MarkdownConverter", "  Span: " + span.type + " from " + span.start + " to " + span.end);
        }

        // Merge overlapping spans of the same type to prevent duplicate markers
        spans = mergeOverlappingSpans(spans);
        android.util.Log.d("MarkdownConverter", "🔗 After merge: " + spans.size() + " spans");

        // Sort spans by position
        Collections.sort(spans);

        StringBuilder markdown = new StringBuilder();
        int currentPos = 0;

        // Build markdown by inserting markers
        List<MarkdownMarker> markers = createMarkers(spans, text.length());
        Collections.sort(markers);

        for (MarkdownMarker marker : markers) {
            if (marker.position > currentPos) {
                markdown.append(text, currentPos, marker.position);
            }
            markdown.append(marker.text);
            currentPos = marker.position;
        }

        if (currentPos < text.length()) {
            markdown.append(text.substring(currentPos));
        }

        String result = markdown.toString();
        android.util.Log.d("MarkdownConverter", "📤 toMarkdown output: " + result);
        return result;
    }

    /**
     * Merge overlapping spans of the same type to prevent duplicate markdown markers
     */
    private static List<SpanInfo> mergeOverlappingSpans(List<SpanInfo> spans) {
        if (spans.isEmpty()) return spans;

        List<SpanInfo> merged = new ArrayList<>();
        Collections.sort(spans);

        SpanInfo current = null;

        for (SpanInfo span : spans) {
            if (current == null) {
                current = new SpanInfo(span.start, span.end, span.type);
            } else if (current.type == span.type && span.start <= current.end) {
                // Overlapping or adjacent spans of same type - merge them
                current.end = Math.max(current.end, span.end);
            } else {
                // Different type or non-overlapping - add current and start new
                merged.add(current);
                current = new SpanInfo(span.start, span.end, span.type);
            }
        }

        if (current != null) {
            merged.add(current);
        }

        return merged;
    }

    /**
     * Extract all formatting spans from Spanned text
     * Handles both manually created spans (StyleSpan, RelativeSizeSpan)
     * and Markwon-generated spans
     */
    private static List<SpanInfo> extractSpans(Spanned spanned) {
        List<SpanInfo> result = new ArrayList<>();

        // Get all spans to handle both manual and Markwon spans
        Object[] allSpans = spanned.getSpans(0, spanned.length(), Object.class);

        for (Object span : allSpans) {
            int start = spanned.getSpanStart(span);
            int end = spanned.getSpanEnd(span);

            if (start >= end) continue;

            String spanClassName = span.getClass().getName();
            SpanInfo.SpanType type = null;

            // Handle StyleSpan (manually created bold/italic)
            if (span instanceof StyleSpan) {
                StyleSpan styleSpan = (StyleSpan) span;
                switch (styleSpan.getStyle()) {
                    case Typeface.BOLD:
                        type = SpanInfo.SpanType.BOLD;
                        break;
                    case Typeface.ITALIC:
                        type = SpanInfo.SpanType.ITALIC;
                        break;
                    case Typeface.BOLD_ITALIC:
                        type = SpanInfo.SpanType.BOLD_ITALIC;
                        break;
                }
            }
            // Handle Markwon's StrongEmphasisSpan (bold from markdown **)
            else if (spanClassName.contains("StrongEmphasisSpan")) {
                type = SpanInfo.SpanType.BOLD;
            }
            // Handle Markwon's EmphasisSpan (italic from markdown *)
            else if (spanClassName.contains("EmphasisSpan")) {
                type = SpanInfo.SpanType.ITALIC;
            }
            // Handle RelativeSizeSpan (manually created sizes)
            else if (span instanceof RelativeSizeSpan) {
                RelativeSizeSpan sizeSpan = (RelativeSizeSpan) span;
                float size = sizeSpan.getSizeChange();

                // Match the size scales from TextSpanUtils.TextSize
                if (Math.abs(size - 0.8f) < 0.1f) {
                    type = SpanInfo.SpanType.SMALL;
                } else if (Math.abs(size - 1.3f) < 0.1f) {
                    type = SpanInfo.SpanType.LARGE;
                }
                // Skip MEDIUM (1.0f) as it's the default
            }

            if (type != null) {
                result.add(new SpanInfo(start, end, type));
            }
        }

        return result;
    }

    /**
     * Represents a markdown marker to be inserted
     */
    private static class MarkdownMarker implements Comparable<MarkdownMarker> {
        int position;
        String text;
        boolean isOpening;
        int priority; // For sorting when positions are equal

        MarkdownMarker(int position, String text, boolean isOpening, int priority) {
            this.position = position;
            this.text = text;
            this.isOpening = isOpening;
            this.priority = priority;
        }

        @Override
        public int compareTo(MarkdownMarker other) {
            if (this.position != other.position) {
                return Integer.compare(this.position, other.position);
            }
            // At the same position: opening tags go before closing tags
            if (this.isOpening != other.isOpening) {
                return this.isOpening ? -1 : 1;
            }
            return Integer.compare(this.priority, other.priority);
        }
    }

    /**
     * Create markdown markers for all spans
     */
    private static List<MarkdownMarker> createMarkers(List<SpanInfo> spans, int textLength) {
        List<MarkdownMarker> markers = new ArrayList<>();

        for (SpanInfo span : spans) {
            String openTag, closeTag;
            int priority = 0;

            switch (span.type) {
                case BOLD:
                    openTag = "**";
                    closeTag = "**";
                    priority = 2;
                    break;
                case ITALIC:
                    openTag = "*";
                    closeTag = "*";
                    priority = 1;
                    break;
                case BOLD_ITALIC:
                    openTag = "***";
                    closeTag = "***";
                    priority = 3;
                    break;
                case SMALL:
                    openTag = "<small>";
                    closeTag = "</small>";
                    priority = 0;
                    break;
                case LARGE:
                    openTag = "<big>";
                    closeTag = "</big>";
                    priority = 0;
                    break;
                default:
                    continue;
            }

            markers.add(new MarkdownMarker(span.start, openTag, true, priority));
            markers.add(new MarkdownMarker(span.end, closeTag, false, priority));
        }

        return markers;
    }

    /**
     * Convert Markdown string to formatted Spanned text
     */
    public static Spanned fromMarkdown(Context context, String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return new SpannableStringBuilder("");
        }

        android.util.Log.d("MarkdownConverter", "📥 fromMarkdown input: " + markdown);

        // First, manually extract and track size tags since Markwon might not handle them
        List<SizeTag> sizeTags = extractSizeTags(markdown);
        android.util.Log.d("MarkdownConverter", "📏 Found " + sizeTags.size() + " size tags");

        // Remove size tags from markdown for Markwon parsing
        String cleanMarkdown = markdown.replaceAll("<small>|</small>|<big>|</big>", "");
        android.util.Log.d("MarkdownConverter", "🧹 Clean markdown: " + cleanMarkdown);

        // Create Markwon instance to handle bold/italic
        Markwon markwon = Markwon.builder(context)
                .build();

        // Parse and render markdown to Spanned
        SpannableStringBuilder builder = new SpannableStringBuilder(markwon.toMarkdown(cleanMarkdown));
        android.util.Log.d("MarkdownConverter", "📝 After Markwon: " + builder.toString());

        // Manually apply size spans based on extracted tags
        applySizeTags(builder, sizeTags);
        android.util.Log.d("MarkdownConverter", "✅ Final result: " + builder.toString());

        return builder;
    }

    /**
     * Represents a size tag in the markdown
     */
    private static class SizeTag {
        int start;
        int end;
        boolean isSmall; // true for <small>, false for <big>

        SizeTag(int start, int end, boolean isSmall) {
            this.start = start;
            this.end = end;
            this.isSmall = isSmall;
        }
    }

    /**
     * Extract size tags from markdown and calculate their positions in the clean text
     */
    private static List<SizeTag> extractSizeTags(String markdown) {
        List<SizeTag> tags = new ArrayList<>();

        // Build clean text and track positions simultaneously
        StringBuilder cleanText = new StringBuilder();
        int markdownPos = 0;

        while (markdownPos < markdown.length()) {
            int smallStart = markdown.indexOf("<small>", markdownPos);
            int bigStart = markdown.indexOf("<big>", markdownPos);

            // Find the next opening tag
            int nextTagPos = -1;
            boolean isSmall = false;
            String openTag, closeTag;

            if (smallStart >= 0 && (bigStart < 0 || smallStart < bigStart)) {
                nextTagPos = smallStart;
                isSmall = true;
                openTag = "<small>";
                closeTag = "</small>";
            } else if (bigStart >= 0) {
                nextTagPos = bigStart;
                isSmall = false;
                openTag = "<big>";
                closeTag = "</big>";
            } else {
                // No more tags, append remaining text
                cleanText.append(markdown.substring(markdownPos));
                break;
            }

            // Append text before the tag
            cleanText.append(markdown.substring(markdownPos, nextTagPos));

            // Find closing tag
            int closeTagPos = markdown.indexOf(closeTag, nextTagPos + openTag.length());

            if (closeTagPos < 0) {
                // No closing tag, treat opening tag as plain text
                cleanText.append(markdown.substring(nextTagPos));
                break;
            }

            // Record the position in clean text
            int contentStart = cleanText.length();

            // Extract and append the content between tags
            String content = markdown.substring(nextTagPos + openTag.length(), closeTagPos);
            cleanText.append(content);

            int contentEnd = cleanText.length();

            // Add the size tag
            tags.add(new SizeTag(contentStart, contentEnd, isSmall));

            // Move past the closing tag
            markdownPos = closeTagPos + closeTag.length();
        }

        return tags;
    }

    /**
     * Apply size spans to the builder based on extracted tags
     */
    private static void applySizeTags(SpannableStringBuilder builder, List<SizeTag> tags) {
        for (SizeTag tag : tags) {
            android.util.Log.d("MarkdownConverter", "📏 Applying size tag: " + (tag.isSmall ? "small" : "big")
                    + " from " + tag.start + " to " + tag.end + " (text length: " + builder.length() + ")");

            if (tag.start >= 0 && tag.end <= builder.length() && tag.start < tag.end) {
                float scale = tag.isSmall ? 0.8f : 1.3f;
                builder.setSpan(new RelativeSizeSpan(scale), tag.start, tag.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                android.util.Log.d("MarkdownConverter", "✅ Applied size span successfully");
            } else {
                android.util.Log.w("MarkdownConverter", "⚠️ Invalid size tag range, skipping");
            }
        }
    }

}
