package com.example.anchornotes_team3.util;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.example.anchornotes_team3.R;

public class ColorPickerDialog {
    public interface OnColorPickedListener {
        void onPicked(String hexColor);
    }

    public static void show(Context context, String initialHex, OnColorPickedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        ImageView palette = view.findViewById(R.id.image_palette);
        View preview = view.findViewById(R.id.view_preview);
        SeekBar hueSeek = view.findViewById(R.id.seek_hue);

        final float[] hsv = new float[]{30f, 1f, 1f};
        if (initialHex != null && !initialHex.isEmpty()) {
            try {
                int c = Color.parseColor(initialHex);
                Color.colorToHSV(c, hsv);
            } catch (Exception ignored) {}
        }
        final float[] selectedSV = new float[]{hsv[1], hsv[2]};
        final String[] selectedHex = new String[]{toHex(Color.HSVToColor(hsv))};

        preview.setBackgroundColor(Color.parseColor(selectedHex[0]));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Choose Tag Color")
                .setView(view)
                .setPositiveButton("Select", (d, which) -> {
                    if (listener != null) listener.onPicked(selectedHex[0]);
                })
                .setNegativeButton("Cancel", null)
                .create();

        palette.post(() -> {
            Bitmap bmp = createSVBitmap(hsv[0], palette.getWidth(), palette.getHeight());
            palette.setImageBitmap(bmp);
        });

        hueSeek.setProgress((int) hsv[0]);
        hueSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                hsv[0] = progress;
                Bitmap bmp = createSVBitmap(hsv[0], palette.getWidth(), palette.getHeight());
                palette.setImageBitmap(bmp);
                int color = Color.HSVToColor(new float[]{hsv[0], selectedSV[0], selectedSV[1]});
                selectedHex[0] = toHex(color);
                preview.setBackgroundColor(color);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        palette.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                int w = v.getWidth();
                int h = v.getHeight();
                float x = clamp(event.getX(), 0, w - 1);
                float y = clamp(event.getY(), 0, h - 1);
                selectedSV[0] = x / (float) (w - 1); // saturation
                selectedSV[1] = 1f - (y / (float) (h - 1)); // value
                int color = Color.HSVToColor(new float[]{hsv[0], selectedSV[0], selectedSV[1]});
                selectedHex[0] = toHex(color);
                preview.setBackgroundColor(color);
                return true;
            }
            return false;
        });

        dialog.show();
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static String toHex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    private static Bitmap createSVBitmap(float hue, int width, int height) {
        if (width <= 0) width = 240;
        if (height <= 0) height = 240;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] line = new int[width];
        for (int y = 0; y < height; y++) {
            float v = 1f - (y / (float) (height - 1));
            for (int x = 0; x < width; x++) {
                float s = x / (float) (width - 1);
                int color = Color.HSVToColor(new float[]{hue, s, v});
                line[x] = color;
            }
            bmp.setPixels(line, 0, width, 0, y, width, 1);
        }
        return bmp;
    }
}
