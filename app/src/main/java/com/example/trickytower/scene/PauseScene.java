package com.example.trickytower.scene;

import com.example.trickytower.R;
import com.example.trickytower.objects.DimmedScreen;
import com.example.trickytower.scene.TitleScene;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Button;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

/**
 * 일시정지 상태를 표시하는 씬
 */
public class PauseScene extends Scene {
    @Override
    public boolean isTransparent() {
        // 기존 게임 화면 위에 투명하게 그리기
        return true;
    }

    @Override
    public void onEnter() {
        initLayers(SceneLayer.values().length);
        add(SceneLayer.BACKGROUND, new DimmedScreen(0.5f));

        float btnW = 220f;
        float btnH = 90f;
        // 계속하기 버튼
        Button resume = new Button(
                R.mipmap.btn_resume_n,
                Metrics.width / 2f,
                Metrics.height * 0.45f,
                btnW,
                btnH,
                pressed -> {
                    if (!pressed) {
                        Scene.pop();
                    }
                    return true;
                }
        );
        add(SceneLayer.UI, resume);

        // 나가기 버튼
        Button exit = new Button(
                R.mipmap.btn_exit_n,
                Metrics.width / 2f,
                Metrics.height * 0.6f,
                btnW,
                btnH,
                pressed -> {
                    if (!pressed) {
                        Scene.pop();
                        GameView.view.changeScene(new TitleScene());
                    }
                    return true;
                }
        );
        add(SceneLayer.UI, exit);
    }

    @Override
    protected int getTouchLayerIndex() {
        return SceneLayer.UI.ordinal();
    }
}
