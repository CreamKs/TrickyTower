package com.example.trickytower.objects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;

/**
 * Simple text-based score display.
 */
public class ScoreLabel implements IGameObject {
    private final Paint paint = new Paint();
    private final float x;
    private final float y;
    private int score;

    public ScoreLabel(float x, float y, float textSize) {
        this.x = x;
        this.y = y;
        paint.setColor(Color.WHITE);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /** Increase the score by the given amount. */
    public void add(int amount) {
        score += amount;
    }

    public int getScore() {
        return score;
    }

    @Override
    public void update() {
        // nothing to update
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawText("Score: " + score, x, y, paint);
    }
}
