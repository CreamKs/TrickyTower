package com.example.trickytower.objects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.ITouchable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.Sound;
import com.example.trickytower.R;

public class TextButton implements IGameObject, ITouchable {
    public interface OnTouchListener {
        boolean onTouch(boolean pressed);
    }

    private final RectF rect = new RectF();
    private final Paint bgPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final String text;
    private final OnTouchListener listener;
    private boolean captures;

    public TextButton(String text, float cx, float cy, float width, float height, OnTouchListener listener) {
        this.text = text;
        this.listener = listener;
        rect.set(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f);

        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.BLACK);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(height * 0.5f);
        textPaint.setAntiAlias(true);
    }

    @Override
    public void update() { }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(rect, bgPaint);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = rect.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, rect.centerX(), textY, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            float[] pts = Metrics.fromScreen(e.getX(), e.getY());
            if (!rect.contains(pts[0], pts[1])) return false;
            captures = true;
            return listener.onTouch(true);
        } else if (action == MotionEvent.ACTION_UP) {
            if (!captures) return false;
            captures = false;
            Sound.playEffect(R.raw.click);
            return listener.onTouch(false);
        }
        return captures;
    }
}
