package com.example.trickytower.scene;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.trickytower.R;
import com.example.trickytower.objects.Block;
import com.example.trickytower.objects.StaticBlock;
import com.example.trickytower.util.BlockCollisionHelper;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

/**
 * GameScene은 BlockCollisionHelper를 이용해 블록 간 충돌 판정으로
 * 블록이 정확히 쌓이도록 하는 게임 로직을 담당합니다.
 */
public class GameScene extends Scene {
    private Block currentBlock;
    private float nextSpawnX;
    private boolean touchEnabled;
    private boolean isFastDropping;

    private float touchStartX, touchStartY;
    private float blockStartX, blockStartY;

    private static final float BLOCK_WIDTH = 100f;
    private static final float BLOCK_HEIGHT = 40f;

    // 고정된 블록 리스트
    private final List<IBoxCollidable> landedBlocks = new ArrayList<>();
    private final Random rand = new Random();

    @Override
    public void onEnter() {
        initLayers(SceneLayer.values().length);
        // 배경
        add(SceneLayer.BACKGROUND, new Sprite(
                R.drawable.bg_game,
                Metrics.width/2f, Metrics.height/2f,
                Metrics.width, Metrics.height
        ));
        // 첫 블록 스폰 위치는 화면 중앙
        nextSpawnX = Metrics.width/2f;
        spawnBlock();
    }

    /**
     * nextSpawnX 위치에서 새로운 블록 생성
     */
    private void spawnBlock() {
        touchEnabled = false;
        isFastDropping = false;
        float halfW = BLOCK_WIDTH/2f;
        float spawnX = Math.max(halfW, Math.min(Metrics.width-halfW, nextSpawnX));
        currentBlock = new Block(
                R.drawable.block,
                spawnX, -BLOCK_HEIGHT,
                BLOCK_WIDTH, BLOCK_HEIGHT
        );
        add(SceneLayer.BLOCK, currentBlock);
    }

    @Override
    public void update() {
        super.update();
        if (currentBlock == null) return;
        // 빠른 낙하 모드
        if (isFastDropping) currentBlock.update();

        // 충돌 지점 계산
        float contactY = BlockCollisionHelper.getCollisionContactY(currentBlock, landedBlocks);
        if (!Float.isNaN(contactY)) {
            // 낙하 중인 블록 제거
            remove(SceneLayer.BLOCK, currentBlock);
            RectF rect = currentBlock.getCollisionRect();
            float landedX = rect.centerX();
            nextSpawnX = landedX;
            float landedY = contactY - rect.height()/2f;
            // StaticBlock으로 고정 블록 생성
            StaticBlock landed = new StaticBlock(
                    R.drawable.block,
                    landedX, landedY,
                    rect.width(), rect.height()
            );
            add(SceneLayer.BLOCK, landed);
            landedBlocks.add(landed);
            // 다음 블록 스폰
            spawnBlock();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!touchEnabled && event.getAction() != MotionEvent.ACTION_DOWN) return false;
        if (currentBlock == null) return super.onTouchEvent(event);
        RectF rect = currentBlock.getCollisionRect();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchEnabled = true;
                touchStartX = event.getX();
                touchStartY = event.getY();
                blockStartX = rect.centerX();
                blockStartY = rect.centerY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - touchStartX;
                float dy = event.getY() - touchStartY;
                // 좌우 드래그
                if (Math.abs(dx) > Math.abs(dy)) {
                    float newX = blockStartX + dx;
                    float halfW = rect.width()/2f;
                    newX = Math.max(halfW, Math.min(Metrics.width-halfW, newX));
                    currentBlock.setPosition(newX, rect.centerY(), rect.width(), rect.height());
                } else if (dy > 50) {
                    // 하드 드랍 모드
                    isFastDropping = true;
                }
                return true;
            case MotionEvent.ACTION_UP:
                float updx = event.getX() - touchStartX;
                float updy = event.getY() - touchStartY;
                if (Math.hypot(updx, updy) < 20) {
                    // 탭: 90도 회전
                    currentBlock.setPosition(
                            rect.centerX(), rect.centerY(),
                            rect.height(), rect.width()
                    );
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }
}