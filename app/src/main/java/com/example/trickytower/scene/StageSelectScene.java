package com.example.trickytower.scene;

import android.graphics.Canvas;
import android.view.MotionEvent;

import com.example.trickytower.R;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Button;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

import com.example.trickytower.scene.TitleScene;

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
        float gap = btnH * 1.3f;
        float startY = Metrics.height * 0.3f + gap * 5f; // 첫 버튼을 6번째 위치에 떨어진다

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

        // 뒤로 가기 버튼
        Button back = new Button(
                R.mipmap.btn_start,
                btnW * 0.6f,
                Metrics.height * 0.1f,
                btnW * 0.8f,
                btnH * 0.8f,
                pressed -> {
                    if (!pressed) changeScene(new TitleScene());
                    return true;
                }
        );
        add(SceneLayer.UI, back);
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
