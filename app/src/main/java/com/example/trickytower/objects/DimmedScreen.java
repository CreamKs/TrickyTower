package com.example.trickytower.objects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

/**
 * 화면을 어둡게 덮어주는 단순한 오브젝트
 */
public class DimmedScreen implements IGameObject {
    private final Paint paint = new Paint();

    public DimmedScreen(float alpha) {
        int a = Math.round(alpha * 255);
        paint.setColor(Color.argb(a, 0, 0, 0));
    }

    @Override
    public void update() { }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(0, 0, Metrics.width, Metrics.height, paint);
    }
}
