package com.example.trickytower.scene;

import android.graphics.Canvas;
import android.view.MotionEvent;

import com.example.trickytower.R;
import com.example.trickytower.objects.Block;
import com.example.trickytower.objects.Player;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Score;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.JoyStick;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.util.CollisionHelper;

import java.util.Random;

/**
 * GameScene은 실제 게임 플레이를 담당합니다.
 * 플레이어, 블록 스폰, 충돌 처리, 점수판, 조이스틱 입력 등을 관리합니다.
 */
public class GameScene extends Scene {
    private Player player;
    private Score score;
    private JoyStick joystick;
    private Random rand = new Random();

    @Override
    public void onEnter() {
        // 레이어 3개: BACKGROUND, CHARACTER, UI
        initLayers(SceneLayer.values().length);

        // 1) 배경 추가
        Sprite bg = new Sprite(
                R.drawable.bg_game,
                Metrics.width / 2f,
                Metrics.height / 2f,
                Metrics.width,
                Metrics.height
        );
        add(SceneLayer.BACKGROUND, bg);

        // 2) 플레이어 추가
        player = new Player(
                R.drawable.player,
                Metrics.width / 2f,
                200,
                80,
                80
        );
        add(SceneLayer.CHARACTER, player);

        // 3) 블록 여러 개 스폰
        for (int i = 0; i < 5; i++) {
            spawnBlock();
        }

        // 4) 점수판 추가
        score = new Score(
                R.drawable.numbers,
                Metrics.width - 20,
                20,
                40
        );
        score.setScore(0);
        add(SceneLayer.UI, score);

        // 5) 조이스틱 추가
        joystick = new JoyStick(
                R.drawable.joy_bg,
                R.drawable.joy_thumb,
                200,
                Metrics.height - 200,
                150,
                60,
                90
        );
        add(SceneLayer.UI, joystick);
    }

    /**
     * 화면 밖 위에서 블록을 랜덤 위치에 스폰합니다.
     */
    private void spawnBlock() {
        float x = rand.nextFloat() * (Metrics.width - 100) + 50;
        float y = Metrics.height + 50;
        Block block = new Block(
                R.drawable.block,
                x,
                y,
                100,
                40
        );
        add(SceneLayer.CHARACTER, block);
    }

    @Override
    public void update() {
        super.update();
        // 충돌 처리: 블록과 플레이어
        for (var obj : objectsAt(SceneLayer.CHARACTER)) {
            if (obj instanceof Block && CollisionHelper.collides((Block) obj, player)) {
                score.add(10);
                remove(SceneLayer.CHARACTER, obj);
            }
        }
        // 추가 블록 스폰 로직은 필요 시 구현
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 조이스틱 터치 우선 처리
        return joystick.onTouch(event) || super.onTouchEvent(event);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }
}


