package com.example.anchornotes_team3;

import android.content.Intent;
import android.graphics.Bitmap;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for drawing on a canvas.
 * Allows users to create freehand drawings that are saved as images.
 */
public class DrawingActivity extends AppCompatActivity {

    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        drawingView = findViewById(R.id.drawing_view);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
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

