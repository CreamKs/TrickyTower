package com.example.trickytower.scene;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import java.util.Random;

import com.example.trickytower.R;
import com.example.trickytower.objects.Block;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

/**
 * GameScene은 블록이 쌓이는 게임 플레이를 담당합니다.
 * 플레이어와 UI를 제거하고, 블록이 스택 위에 순차적으로 쌓이도록 구현합니다.
 */
public class GameScene extends Scene {
    private Block currentBlock;
    private float stackTopY;
    private static final float BLOCK_WIDTH = 100f;
    private static final float BLOCK_HEIGHT = 40f;
    private Random rand = new Random();

    @Override
    public void onEnter() {
        // 레이어 2개: BACKGROUND, BLOCK
        initLayers(SceneLayer.values().length);

        // 배경 추가
        Sprite bg = new Sprite(
                R.drawable.bg_game,
                Metrics.width / 2f,
                Metrics.height / 2f,
                Metrics.width,
                Metrics.height
        );
        add(SceneLayer.BACKGROUND, bg);

        // 초기 스택 탑 위치는 화면 하단
        stackTopY = Metrics.height;

        // 첫 블록 스폰
        spawnBlock();
    }

    /**
     * 화면 상단에서 새로운 Block 객체를 생성하고 BLOCK 레이어에 추가
     */
    private void spawnBlock() {
        float x = Metrics.width / 2f;    // 화면 중앙
        float y = -BLOCK_HEIGHT;         // 화면 위에 시작
        currentBlock = new Block(
                R.drawable.block,
                x, y,
                BLOCK_WIDTH, BLOCK_HEIGHT
        );
        add(SceneLayer.BLOCK, currentBlock);
    }

    @Override
    public void update() {
        super.update();
        if (currentBlock != null) {
            // 현재 블록의 위치 확인
            RectF rect = currentBlock.getCollisionRect();
            // 블록이 스택 탑에 닿았을 때
            if (rect.bottom >= stackTopY) {
                // 낙하 중인 블록 제거
                remove(SceneLayer.BLOCK, currentBlock);
                // 블록 쌓기: 중심 X 좌표 사용
                float landedX = rect.centerX();
                float landedY = stackTopY - BLOCK_HEIGHT / 2f;
                Sprite landed = new Sprite(
                        R.drawable.block,
                        landedX, landedY,
                        BLOCK_WIDTH, BLOCK_HEIGHT
                );
                add(SceneLayer.BLOCK, landed);
                // 스택 탑 위치 업데이트
                stackTopY -= BLOCK_HEIGHT;
                // 다음 블록 스폰
                spawnBlock();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 터치 비활성
        return super.onTouchEvent(event);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }
}