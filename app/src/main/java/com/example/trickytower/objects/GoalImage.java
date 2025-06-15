package com.example.trickytower.objects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;

/** 골인 지점을 체커 무늬로 그려주는 오브젝트 */
public class GoalImage implements IGameObject {
    private final RectF rect;
    private static final Paint blackPaint = new Paint();
    private static final Paint whitePaint = new Paint();
    static {
        blackPaint.setColor(Color.BLACK);
        whitePaint.setColor(Color.WHITE);
    }

    public GoalImage(RectF rect) {
        this.rect = rect;
    }

    @Override
    public void update() { }

    @Override
    public void draw(Canvas canvas) {
        int cols = 10;
        int rows = 2;
        float cellW = rect.width() / cols;
        float cellH = rect.height() / rows;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Paint p = ((r + c) % 2 == 0) ? whitePaint : blackPaint;
                float l = rect.left + c * cellW;
                float t = rect.top + r * cellH;
                canvas.drawRect(l, t, l + cellW, t + cellH, p);
            }
        }
    }
}
