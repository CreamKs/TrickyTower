package com.example.trickytower.scene;

import android.graphics.Canvas;
import android.view.MotionEvent;

import com.example.trickytower.R;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Button;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class StageSelectScene extends Scene {
    private static final int STAGE_COUNT = 5;

    @Override
    public void onEnter() {
        initLayers(SceneLayer.values().length);
        Sprite bg = new Sprite(
                R.drawable.bg_title,
                Metrics.width / 2f,
                Metrics.height / 2f,
                Metrics.width,
                Metrics.height
        );
        add(SceneLayer.BACKGROUND, bg);

        float btnW = 250f;
        float btnH = 90f;
        float startY = Metrics.height * 0.3f;
        float gap = btnH * 1.3f;
        for (int i = 0; i < STAGE_COUNT; i++) {
            final int stage = i + 1;
            Button btn = new Button(
                    R.mipmap.btn_start,
                    Metrics.width / 2f,
                    startY + i * gap,
                    btnW,
                    btnH,
                    pressed -> {
                        if (!pressed) changeScene(new GameScene(stage));
                        return true;
                    }
            );
            add(SceneLayer.UI, btn);
        }
    }

    private void changeScene(Scene next) {
        GameView.view.changeScene(next);
    }

    @Override
    protected int getTouchLayerIndex() {
        return SceneLayer.UI.ordinal();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }
}
