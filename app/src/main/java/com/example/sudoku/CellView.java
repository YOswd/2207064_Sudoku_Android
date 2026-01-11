package com.example.sudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.EditText;

public class CellView extends EditText {

    private Paint borderPaint;
    private int row, col;

    public CellView(Context context, int r, int c) {
        super(context);
        this.row = r;
        this.col = c;
        init();
    }

    public CellView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(0xFF000000); // black
        borderPaint.setStrokeWidth(2f);

        setGravity(android.view.Gravity.CENTER);
        setTextSize(18);
        setPadding(4, 4, 4, 4);
        setTextColor(0xFF000000);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        float thick = 6f;
        float thin = 2f;

        borderPaint.setStrokeWidth((col % 3 == 0) ? thick : thin);
        canvas.drawLine(0, 0, 0, h, borderPaint);

        borderPaint.setStrokeWidth((row % 3 == 0) ? thick : thin);
        canvas.drawLine(0, 0, w, 0, borderPaint);

        borderPaint.setStrokeWidth((col == 8) ? thick : thin);
        canvas.drawLine(w, 0, w, h, borderPaint);

        borderPaint.setStrokeWidth((row == 8) ? thick : thin);
        canvas.drawLine(0, h, w, h, borderPaint);
    }
}
