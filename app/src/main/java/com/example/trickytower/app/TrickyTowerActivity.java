package com.example.trickytower.app;

import android.os.Bundle;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.activity.GameActivity;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import com.example.trickytower.scene.TitleScene;

/**
 * TrickyTowerActivity는 GameActivity를 상속받아
 * 앱 시작 시 자동으로 GameView를 생성·설정하고,
 * 최초 씬(TitleScene)을 푸시하여 타이틀 화면을 띄웁니다.
 */
public class TrickyTowerActivity extends GameActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // GameActivity가 GameView를 setContentView 해주므로
        // 별도의 레이아웃 설정 없이 바로 씬을 푸시합니다.
        GameView view = GameView.view;
        // TitleScene을 푸시하여 게임의 첫 화면을 설정
        view.pushScene(new TitleScene());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 앱이 포그라운드로 돌아올 때 추가 로직이 필요하면 여기에
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 앱이 백그라운드로 갈 때 리소스 해제나 저장이 필요하면 여기에
    }
}
