package com.example.trickytower.scene;

import android.graphics.Canvas;
import android.view.MotionEvent;

import com.example.trickytower.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Button;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

/**
 * TitleScene은 게임의 첫 화면(타이틀)을 담당합니다.
 * 배경과 시작 버튼을 그리고, 버튼 터치 시 GameScene으로 전환합니다.
 */
public class TitleScene extends Scene {
    @Override
    public void onEnter() {
        // 레이어 하나로 배경과 버튼 관리
        initLayers(SceneLayer.values().length);

        // 배경 이미지 추가
        Sprite bg = new Sprite(
                R.drawable.bg_title,
                Metrics.width / 2f,
                Metrics.height / 2f,
                Metrics.width,
                Metrics.height
        );
        add(SceneLayer.BACKGROUND, bg);

        // 시작 버튼 추가
        float btnWidth = 300f;
        float btnHeight = 100f;
        Button startButton = new Button(
                R.drawable.btn_start,
                Metrics.width / 2f,
                Metrics.height * 0.75f,
                btnWidth,
                btnHeight,
                pressed -> {
                    if (!pressed) {
                        // 버튼에서 손을 뗄 때 다음 씬으로 전환
                        changeScene(new GameScene());
                    }
                    return true;
                }
        );
        add(SceneLayer.UI, startButton);
    }

    @Override
    protected int getTouchLayerIndex() {
        // UI 레이어를 터치 대상으로 지정
        return SceneLayer.UI.ordinal();
    }


    /**
     * changeScene 헬퍼: GameView를 통해 씬 전환
     * @param nextScene 전환할 씬 객체
     */
    private void changeScene(Scene nextScene) {
        GameView.view.changeScene(nextScene);
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