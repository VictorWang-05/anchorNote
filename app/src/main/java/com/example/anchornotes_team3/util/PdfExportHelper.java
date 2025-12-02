package com.example.anchornotes.team3.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.example.anchornotes_team3.model.Attachment;
import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.model.Tag;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for exporting notes to PDF format
 * Uses iText7 library for PDF generation
 */
public class PdfExportHelper {

    private static final String PDF_FOLDER = "AnchorNotes_PDFs";
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")
            .withZone(ZoneId.systemDefault());

    /**
     * Export a note to PDF file
     * @param context Application context
     * @param note Note to export
     * @param callback Callback for success/failure
     */
    public static void exportNoteToPdf(Context context, Note note, ExportCallback callback) {
        new Thread(() -> {
            try {
                String fileName = getFileName(note);
                String successMessage;
                File resultFile = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ - Use MediaStore API
                    Uri pdfUri = createPdfUriWithMediaStore(context, fileName);
                    generatePdfToUri(context, note, pdfUri);
                    successMessage = "PDF exported to Downloads/" + PDF_FOLDER + "/" + fileName;
                } else {
                    // Android 9 and below - Use legacy File API
                    File pdfFile = createPdfFileLegacy(context, fileName);
                    generatePdfToFile(context, note, pdfFile);
                    successMessage = "PDF exported to: " + pdfFile.getAbsolutePath();
                    resultFile = pdfFile;
                }

                // Notify success on main thread
                final File finalResultFile = resultFile;
                ((android.app.Activity) context).runOnUiThread(() -> {
                    callback.onSuccess(finalResultFile);
                    Toast.makeText(context, successMessage, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Notify error on main thread
                ((android.app.Activity) context).runOnUiThread(() -> {
                    callback.onError(e.getMessage());
                    Toast.makeText(context, "Error exporting PDF: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Generate filename from note title or timestamp
     */
    private static String getFileName(Note note) {
        if (note.getTitle() != null && !note.getTitle().trim().isEmpty()) {
            return sanitizeFileName(note.getTitle()) + ".pdf";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            return "Note_" + sdf.format(new Date()) + ".pdf";
        }
    }

    /**
     * Create PDF Uri using MediaStore API (Android 10+)
     */
    private static Uri createPdfUriWithMediaStore(Context context, String fileName) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS + "/" + PDF_FOLDER);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
        if (uri == null) {
            throw new Exception("Failed to create MediaStore entry for PDF");
        }
        return uri;
    }

    /**
     * Create PDF file using legacy File API (Android 9 and below)
     */
    private static File createPdfFileLegacy(Context context, String fileName) throws Exception {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File pdfDir = new File(downloadsDir, PDF_FOLDER);

        if (!pdfDir.exists()) {
            boolean created = pdfDir.mkdirs();
            if (!created && !pdfDir.exists()) {
                throw new Exception("Failed to create directory: " + pdfDir.getAbsolutePath());
            }
        }

        return new File(pdfDir, fileName);
    }

    /**
     * Generate PDF content to Uri (MediaStore - Android 10+)
     */
    private static void generatePdfToUri(Context context, Note note, Uri uri) throws Exception {
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                throw new Exception("Failed to open output stream for URI");
            }
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            writePdfContent(context, note, document);
            document.close();
        }
    }

    /**
     * Generate PDF content to File (Legacy - Android 9 and below)
     */
    private static void generatePdfToFile(Context context, Note note, File file) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            PdfWriter writer = new PdfWriter(fos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            writePdfContent(context, note, document);
            document.close();
        }
    }

    /**
     * Write PDF content (common for both methods)
     */
    private static void writePdfContent(Context context, Note note, Document document) throws Exception {
        // Add title
        String title = note.getTitle() != null && !note.getTitle().isEmpty()
            ? note.getTitle() : "Untitled Note";
        Paragraph titlePara = new Paragraph(title)
            .setFontSize(24)
            .setBold()
            .setMarginBottom(10);
        document.add(titlePara);

        // Add metadata (date, tags)
        addMetadata(document, note);

        // Add separator line
        document.add(new Paragraph("_".repeat(80))
            .setFontColor(ColorConstants.LIGHT_GRAY)
            .setMarginBottom(15));

        // Add note content
        String content = note.getText() != null ? note.getText() : "";
        if (!content.isEmpty()) {
            // Parse markdown-style formatting
            addFormattedContent(document, content);
        } else {
            document.add(new Paragraph("(No content)")
                .setFontColor(ColorConstants.GRAY)
                .setItalic());
        }

        // Add attachments section if present
        if (note.getAttachments() != null && !note.getAttachments().isEmpty()) {
            addAttachmentsSection(document, note);
        }

        // Add reminders section if present
        addRemindersSection(document, note);

        // Add footer
        addFooter(document);
    }

    /**
     * Add metadata section (creation date, tags)
     */
    private static void addMetadata(Document document, Note note) {
        Paragraph metadata = new Paragraph();
        metadata.setFontSize(10);
        metadata.setFontColor(ColorConstants.DARK_GRAY);

        // Add creation date
        if (note.getCreatedAt() != null) {
            String dateStr = DATE_FORMATTER.format(note.getCreatedAt());
            metadata.add(new Text("Created: " + dateStr + "\n"));
        }

        // Add last edited date
        if (note.getLastEdited() != null) {
            String dateStr = DATE_FORMATTER.format(note.getLastEdited());
            metadata.add(new Text("Last edited: " + dateStr + "\n"));
        }

        // Add tags
        if (note.getTags() != null && !note.getTags().isEmpty()) {
            StringBuilder tagsStr = new StringBuilder("Tags: ");
            for (int i = 0; i < note.getTags().size(); i++) {
                Tag tag = note.getTags().get(i);
                tagsStr.append(tag.getName());
                if (i < note.getTags().size() - 1) {
                    tagsStr.append(", ");
                }
            }
            metadata.add(new Text(tagsStr.toString() + "\n"));
        }

        // Add pinned status
        if (note.isPinned()) {
            metadata.add(new Text("★ Pinned\n").setBold());
        }

        metadata.setMarginBottom(10);
        document.add(metadata);
    }

    /**
     * Add formatted note content
     * Handles basic markdown: **bold**, *italic*, etc.
     */
    private static void addFormattedContent(Document document, String content) {
        // Split by lines to preserve formatting
        String[] lines = content.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                document.add(new Paragraph("\n").setMarginTop(0).setMarginBottom(0));
                continue;
            }

            Paragraph para = new Paragraph();
            para.setFontSize(12);

            // Simple markdown parsing
            // Note: This is basic - for full markdown, you'd use a proper parser
            if (line.startsWith("# ")) {
                // Header
                para.add(new Text(line.substring(2)).setBold().setFontSize(18));
            } else if (line.startsWith("## ")) {
                para.add(new Text(line.substring(3)).setBold().setFontSize(16));
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                // Bullet point
                para.add(new Text("  • " + line.substring(2)));
            } else {
                // Regular text - handle inline formatting
                para.add(parseInlineFormatting(line));
            }

            document.add(para);
        }
    }

    /**
     * Parse inline formatting like **bold** and *italic*
     * Basic implementation - doesn't handle nested or complex formatting
     */
    private static Text parseInlineFormatting(String text) {
        // For simplicity, just return plain text
        // Full implementation would parse **bold**, *italic*, etc.
        return new Text(text);
    }

    /**
     * Add attachments section
     */
    private static void addAttachmentsSection(Document document, Note note) {
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Attachments:")
            .setBold()
            .setFontSize(14)
            .setMarginBottom(5));

        for (Attachment att : note.getAttachments()) {
            if (att.getType() == Attachment.AttachmentType.PHOTO) {
                String displayName = att.getDisplayName() != null && !att.getDisplayName().isEmpty()
                    ? att.getDisplayName() : "Photo";
                document.add(new Paragraph("📷 Photo: " + displayName)
                    .setFontSize(10));
                // Note: Downloading and embedding images would require network calls
                // which we skip for simplicity
            } else if (att.getType() == Attachment.AttachmentType.AUDIO) {
                String displayName = att.getDisplayName() != null && !att.getDisplayName().isEmpty()
                    ? att.getDisplayName() : "Audio";
                String duration = att.getFormattedDuration();
                document.add(new Paragraph("🎵 Audio: " + displayName +
                    (duration != null && !duration.isEmpty() ? " (" + duration + ")" : ""))
                    .setFontSize(10));
            }
        }
    }

    /**
     * Add reminders section if note has reminders
     */
    private static void addRemindersSection(Document document, Note note) {
        boolean hasReminders = false;

        Paragraph remindersPara = new Paragraph();
        remindersPara.setFontSize(10);
        remindersPara.setFontColor(new DeviceRgb(0, 100, 200));

        if (note.getReminderTime() != null) {
            hasReminders = true;
            String dateStr = DATE_FORMATTER.format(note.getReminderTime());
            remindersPara.add(new Text("\n⏰ Time Reminder: " + dateStr + "\n"));
        }

        if (note.getGeofence() != null) {
            hasReminders = true;
            String locationName = note.getGeofence().getAddressName() != null
                ? note.getGeofence().getAddressName()
                : "Location";
            remindersPara.add(new Text("\n📍 Location Reminder: " + locationName + "\n"));
        }

        if (hasReminders) {
            document.add(remindersPara);
        }
    }

    /**
     * Add footer
     */
    private static void addFooter(Document document) {
        document.add(new Paragraph("\n"));
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        document.add(new Paragraph("Exported from AnchorNotes on " + sdf.format(new Date()))
            .setFontSize(8)
            .setFontColor(ColorConstants.GRAY)
            .setTextAlignment(TextAlignment.CENTER));
    }

    /**
     * Sanitize filename to remove invalid characters
     */
    private static String sanitizeFileName(String fileName) {
        // Remove invalid characters for filenames
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
        // Limit length
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        return sanitized;
    }

    /**
     * Callback interface for export operations
     */
    public interface ExportCallback {
        void onSuccess(File pdfFile);
        void onError(String error);
    }
}
