package com.example.anchornotes_team3.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom view for drawing on a canvas.
 * Supports pen mode with color selection and eraser mode.
 */
public class DrawingView extends View {
    public enum DrawingMode {
        PEN,
        ERASER
    }

    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;
    private Paint eraserPaint;
    private Path currentPath;
    private DrawingMode currentMode = DrawingMode.PEN;
    private int currentColor = Color.BLACK;

    // Drawing tool sizes
    private static final float PEN_WIDTH_DP = 4f;
    private static final float ERASER_WIDTH_DP = 40f;

    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize pen paint
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(currentColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dpToPx(PEN_WIDTH_DP));

        // Initialize eraser paint
        eraserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eraserPaint.setColor(Color.WHITE);
        eraserPaint.setStyle(Paint.Style.STROKE);
        eraserPaint.setStrokeJoin(Paint.Join.ROUND);
        eraserPaint.setStrokeCap(Paint.Cap.ROUND);
        eraserPaint.setStrokeWidth(dpToPx(ERASER_WIDTH_DP));
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (bitmap != null) {
            bitmap.recycle();
        }
        // Create a bitmap with white background
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        
        // Enable layer for eraser to work properly
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
        if (currentPath != null) {
            // Draw the current path using the active paint based on mode
            Paint activePaint = (currentMode == DrawingMode.ERASER) ? eraserPaint : paint;
            canvas.drawPath(currentPath, activePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                currentPath.lineTo(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                // Commit the path to our off-screen bitmap using current mode's paint
                Paint activePaint = (currentMode == DrawingMode.ERASER) ? eraserPaint : paint;
                canvas.drawPath(currentPath, activePaint);
                currentPath = null;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Get the current drawing as a bitmap.
     * @return The bitmap containing the drawing
     */
    public Bitmap getBitmap() {
        return bitmap;
    }

    /**
     * Clear the drawing canvas.
     */
    public void clear() {
        if (bitmap != null) {
            canvas.drawColor(Color.WHITE);
            invalidate();
        }
    }

    /**
     * Set the drawing mode (pen or eraser).
     */
    public void setDrawingMode(DrawingMode mode) {
        this.currentMode = mode;
    }

    /**
     * Get the current drawing mode.
     */
    public DrawingMode getDrawingMode() {
        return currentMode;
    }

    /**
     * Set the pen color.
     */
    public void setPenColor(int color) {
        this.currentColor = color;
        paint.setColor(color);
    }

    /**
     * Get the current pen color.
     */
    public int getPenColor() {
        return currentColor;
    }
}

