package com.example.anchornotes_team3;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.anchornotes_team3.view.DrawingView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for drawing on a canvas.
 * Allows users to create freehand drawings that are saved as images.
 * Supports pen mode with color selection and eraser mode.
 */
public class DrawingActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private MaterialButton btnPen;
    private MaterialButton btnEraser;
    private MaterialButton btnColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_drawing);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        drawingView = findViewById(R.id.drawing_view);
        btnPen = findViewById(R.id.btn_pen);
        btnEraser = findViewById(R.id.btn_eraser);
        btnColor = findViewById(R.id.btn_color);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        setupDrawingTools();
        updateToolButtonStates();
        setupWindowInsets();
    }

    /**
     * Setup window insets to handle system bars (navigation bar)
     */
    private void setupWindowInsets() {
        android.view.View toolsMenu = findViewById(R.id.drawing_tools_menu);
        
        // Store original padding
        final int originalPaddingBottom = toolsMenu.getPaddingBottom();
        final int originalPaddingTop = toolsMenu.getPaddingTop();
        final int originalPaddingLeft = toolsMenu.getPaddingLeft();
        final int originalPaddingRight = toolsMenu.getPaddingRight();
        
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(toolsMenu, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars());
            
            // Apply bottom padding to account for navigation bar
            v.setPadding(
                    originalPaddingLeft,
                    originalPaddingTop,
                    originalPaddingRight,
                    originalPaddingBottom + systemBars.bottom
            );
            
            return insets;
        });
    }

    /**
     * Setup click listeners for drawing tool buttons
     */
    private void setupDrawingTools() {
        // Pen button - switches to pen mode
        btnPen.setOnClickListener(v -> {
            drawingView.setDrawingMode(DrawingView.DrawingMode.PEN);
            updateToolButtonStates();
        });

        // Eraser button - switches to eraser mode
        btnEraser.setOnClickListener(v -> {
            drawingView.setDrawingMode(DrawingView.DrawingMode.ERASER);
            updateToolButtonStates();
        });

        // Color button - shows color picker
        btnColor.setOnClickListener(v -> showColorPicker());
    }

    /**
     * Update button appearances to reflect current tool
     */
    private void updateToolButtonStates() {
        boolean isPenMode = drawingView.getDrawingMode() == DrawingView.DrawingMode.PEN;
        
        // Highlight active tool
        if (isPenMode) {
            btnPen.setIconTint(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.orange_primary, null)));
            btnPen.setTextColor(getResources().getColor(R.color.orange_primary, null));
            btnEraser.setIconTint(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.text_dark, null)));
            btnEraser.setTextColor(getResources().getColor(R.color.text_dark, null));
        } else {
            btnEraser.setIconTint(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.orange_primary, null)));
            btnEraser.setTextColor(getResources().getColor(R.color.orange_primary, null));
            btnPen.setIconTint(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.text_dark, null)));
            btnPen.setTextColor(getResources().getColor(R.color.text_dark, null));
        }

        // Update color button icon to show current pen color as a filled circle
        updateColorButtonIcon();
    }

    /**
     * Update the color button icon to display a circle filled with the current pen color
     */
    private void updateColorButtonIcon() {
        int currentColor = drawingView.getPenColor();
        
        // Create a circular drawable with the current color
        android.graphics.drawable.GradientDrawable colorCircle = new android.graphics.drawable.GradientDrawable();
        colorCircle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        colorCircle.setColor(currentColor);
        colorCircle.setStroke(
            (int) (2 * getResources().getDisplayMetrics().density), 
            Color.parseColor("#757575")
        );
        colorCircle.setSize(
            (int) (28 * getResources().getDisplayMetrics().density),
            (int) (28 * getResources().getDisplayMetrics().density)
        );
        
        btnColor.setIcon(colorCircle);
    }

    /**
     * Show color picker dialog using MaterialColorPickerDialog
     */
    private void showColorPicker() {
        int currentColor = drawingView.getPenColor();

        com.github.dhaval2404.colorpicker.MaterialColorPickerDialog colorPicker = 
            new com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
                .Builder(this)
                .setTitle("Choose Pen Color")
                .setColorShape(com.github.dhaval2404.colorpicker.model.ColorShape.CIRCLE)
                .setDefaultColor(currentColor)
                .setColorListener(new com.github.dhaval2404.colorpicker.listener.ColorListener() {
                    @Override
                    public void onColorSelected(int color, String colorHex) {
                        drawingView.setPenColor(color);
                        // Switch to pen mode when color is selected
                        drawingView.setDrawingMode(DrawingView.DrawingMode.PEN);
                        updateToolButtonStates();
                    }
                })
                .build();
        
        colorPicker.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_drawing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_save_drawing) {
            saveDrawingAndFinish();
            return true;
        } else if (id == R.id.action_clear_drawing) {
            drawingView.clear();
            Toast.makeText(this, R.string.btn_clear, Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    /**
     * Save the current drawing as a PNG file and return its URI.
     */
    private void saveDrawingAndFinish() {
        Bitmap bitmap = drawingView.getBitmap();
        if (bitmap == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        try {
            File file = createDrawingFile();
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

            Intent result = new Intent();
            result.setData(uri);
            setResult(RESULT_OK, result);
            finish();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save drawing", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     * Create a file for storing the drawing.
     * Uses the same storage location as camera photos for consistency.
     */
    private File createDrawingFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "DRAW_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("Pictures");
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(imageFileName, ".png", storageDir);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}

