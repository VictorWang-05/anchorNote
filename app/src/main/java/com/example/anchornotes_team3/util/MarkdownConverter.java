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

/**
 * Utility class to convert between Android Spanned text and Markdown format.
 * This allows formatted text (bold, italic, sizes) to be persisted in the database.
 *
 * Uses manual parsing instead of Markwon to avoid round-trip corruption issues.
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

        List<SpanInfo> spans = extractSpans(spanned);

        if (spans.isEmpty()) {
            return text;
        }

        // Merge overlapping spans of the same type to prevent duplicate markers
        spans = mergeOverlappingSpans(spans);

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
        return result;
    }

    /**
     * Merge overlapping and adjacent spans of the same type to prevent duplicate markdown markers
     * Also handles character-by-character spans created by FormattingTextWatcher
     */
    private static List<SpanInfo> mergeOverlappingSpans(List<SpanInfo> spans) {
        if (spans.isEmpty()) return spans;

        // Group spans by type for more effective merging
        List<SpanInfo>[] spansByType = new List[SpanInfo.SpanType.values().length];
        for (int i = 0; i < spansByType.length; i++) {
            spansByType[i] = new ArrayList<>();
        }

        for (SpanInfo span : spans) {
            spansByType[span.type.ordinal()].add(span);
        }

        List<SpanInfo> merged = new ArrayList<>();

        // Merge each type separately
        for (List<SpanInfo> typeSpans : spansByType) {
            if (typeSpans.isEmpty()) continue;

            // Sort by start position
            Collections.sort(typeSpans, (a, b) -> {
                if (a.start != b.start) return Integer.compare(a.start, b.start);
                return Integer.compare(a.end, b.end);
            });

            SpanInfo current = typeSpans.get(0);

            for (int i = 1; i < typeSpans.size(); i++) {
                SpanInfo next = typeSpans.get(i);

                // Check if spans are adjacent or overlapping
                // Adjacent means: current.end == next.start (no gap)
                // Overlapping means: next.start < current.end
                if (next.start <= current.end) {
                    // Merge: extend current to include next
                    current = new SpanInfo(current.start, Math.max(current.end, next.end), current.type);
                } else {
                    // Non-adjacent, save current and start new
                    merged.add(current);
                    current = next;
                }
            }

            // Add the last span
            merged.add(current);
        }

        return merged;
    }

    /**
     * Extract all formatting spans from Spanned text
     * Handles StyleSpan (bold/italic) and RelativeSizeSpan (sizes)
     * Splits spans at newline boundaries to prevent malformed markdown
     */
    private static List<SpanInfo> extractSpans(Spanned spanned) {
        List<SpanInfo> result = new ArrayList<>();
        String text = spanned.toString();

        // Get all spans
        Object[] allSpans = spanned.getSpans(0, spanned.length(), Object.class);

        for (Object span : allSpans) {
            int start = spanned.getSpanStart(span);
            int end = spanned.getSpanEnd(span);

            if (start >= end) continue;

            SpanInfo.SpanType type = null;

            // Handle StyleSpan (bold/italic)
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
            // Handle RelativeSizeSpan (sizes)
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
                // Split span at newline boundaries to prevent tags spanning multiple lines
                int currentStart = start;
                for (int i = start; i <= end; i++) {
                    boolean isNewline = (i < end && text.charAt(i) == '\n');
                    boolean isEnd = (i == end);
                    
                    if (isNewline || isEnd) {
                        if (currentStart < i) {
                            result.add(new SpanInfo(currentStart, i, type));
                        }
                        if (isNewline) {
                            currentStart = i + 1; // Start after the newline
                        }
                    }
                }
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
            // For opening tags: lower priority first (size, then italic, then bold)
            // For closing tags: REVERSE order (bold, then italic, then size)
            // This ensures proper nesting: <small>**text**</small> not <small>**text</small>**
            if (this.isOpening) {
                return Integer.compare(this.priority, other.priority);
            } else {
                return Integer.compare(other.priority, this.priority); // Reversed!
            }
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
     * @param context Unused, kept for backward compatibility
     * @param markdown The markdown string to parse
     * @return Formatted Spanned text
     */
    public static Spanned fromMarkdown(Context context, String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return new SpannableStringBuilder("");
        }

        // Parse markdown manually to avoid Markwon round-trip issues
        SpannableStringBuilder builder = parseMarkdownManually(markdown);
        return builder;
    }

    /**
     * Manually parse markdown to create a Spanned text
     * This avoids round-trip corruption issues with Markwon
     */
    private static SpannableStringBuilder parseMarkdownManually(String markdown) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        parseMarkdownRecursive(markdown, builder, 0);
        return builder;
    }

    /**
     * Recursively parse markdown and apply formatting
     */
    private static int parseMarkdownRecursive(String markdown, SpannableStringBuilder builder, int pos) {
        while (pos < markdown.length()) {
            // Check for size tags
            if (markdown.startsWith("<small>", pos)) {
                int closePos = findClosingTag(markdown, pos + 7, "</small>");
                if (closePos >= 0) {
                    String content = markdown.substring(pos + 7, closePos);
                    int startPos = builder.length();
                    // Recursively parse the content inside <small>
                    SpannableStringBuilder inner = new SpannableStringBuilder();
                    parseMarkdownRecursive(content, inner, 0);
                    builder.append(inner);
                    builder.setSpan(new RelativeSizeSpan(0.8f), startPos, builder.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    pos = closePos + 8;  // 8 = length of "</small>"
                    continue;
                }
            } else if (markdown.startsWith("<big>", pos)) {
                int closePos = findClosingTag(markdown, pos + 5, "</big>");
                if (closePos >= 0) {
                    String content = markdown.substring(pos + 5, closePos);
                    int startPos = builder.length();
                    // Recursively parse the content inside <big>
                    SpannableStringBuilder inner = new SpannableStringBuilder();
                    parseMarkdownRecursive(content, inner, 0);
                    builder.append(inner);
                    builder.setSpan(new RelativeSizeSpan(1.3f), startPos, builder.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    pos = closePos + 6;  // 6 = length of "</big>"
                    continue;
                }
            }
            // Check for bold-italic ***
            else if (markdown.startsWith("***", pos) && !markdown.startsWith("****", pos)) {
                int closePos = findClosingMarker(markdown, pos + 3, "***");
                if (closePos >= 0) {
                    String content = markdown.substring(pos + 3, closePos);
                    int startPos = builder.length();
                    builder.append(content);
                    builder.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), startPos, builder.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    pos = closePos + 3;
                    continue;
                }
            }
            // Check for bold **
            else if (markdown.startsWith("**", pos) && !markdown.startsWith("***", pos)) {
                int closePos = findClosingMarker(markdown, pos + 2, "**");
                if (closePos >= 0) {
                    String content = markdown.substring(pos + 2, closePos);
                    int startPos = builder.length();
                    // Recursively parse content inside ** (e.g., for nested italic)
                    SpannableStringBuilder inner = new SpannableStringBuilder();
                    parseMarkdownRecursive(content, inner, 0);
                    builder.append(inner);
                    builder.setSpan(new StyleSpan(Typeface.BOLD), startPos, builder.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    pos = closePos + 2;
                    continue;
                }
            }
            // Check for italic *
            else if (markdown.startsWith("*", pos) && !markdown.startsWith("**", pos)) {
                int closePos = findClosingMarker(markdown, pos + 1, "*");
                if (closePos >= 0) {
                    String content = markdown.substring(pos + 1, closePos);
                    int startPos = builder.length();
                    builder.append(content);
                    builder.setSpan(new StyleSpan(Typeface.ITALIC), startPos, builder.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    pos = closePos + 1;
                    continue;
                }
            }

            // Regular character
            builder.append(markdown.charAt(pos));
            pos++;
        }

        return pos;
    }

    /**
     * Find the closing tag (for <small> and <big>)
     */
    private static int findClosingTag(String text, int start, String closeTag) {
        return text.indexOf(closeTag, start);
    }

    /**
     * Find the closing marker for a formatting tag (* or **)
     */
    private static int findClosingMarker(String text, int start, String marker) {
        int pos = text.indexOf(marker, start);
        // Make sure we don't match a longer marker (e.g., *** when looking for **)
        while (pos >= 0) {
            if (marker.equals("*") && pos + 1 < text.length() && text.charAt(pos + 1) == '*') {
                // This is ** or ***, not a single *
                pos = text.indexOf(marker, pos + 1);
                continue;
            }
            if (marker.equals("**") && pos + 2 < text.length() && text.charAt(pos + 2) == '*') {
                // This is ***, not **
                pos = text.indexOf(marker, pos + 1);
                continue;
            }
            return pos;
        }
        return -1;
    }

}
