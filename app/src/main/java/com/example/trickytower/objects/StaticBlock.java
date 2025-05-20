package com.example.trickytower.objects;

import android.graphics.RectF;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;

/**
 * StaticBlock 클래스는 고정된 블록으로, 떨어지지 않고
 * IBoxCollidable을 구현해 충돌판정을 지원합니다.
 */
public class StaticBlock extends Sprite implements IBoxCollidable {
    private RectF rect = new RectF();

    /**
     * @param resId   이미지 리소스 ID
     * @param x       중앙 X 좌표
     * @param y       중앙 Y 좌표
     * @param width   블록 너비
     * @param height  블록 높이
     */
    public StaticBlock(int resId, float x, float y, float width, float height) {
        super(resId, x, y, width, height);
    }

    /**
     * 고정 블록은 update가 필요 없으므로 빈 구현
     */
    @Override
    public void update() {
        // no-op
    }

    /**
     * 충돌영역 제공을 위해 RectF 반환
     */
    @Override
    public RectF getCollisionRect() {
        rect.set(dstRect);
        return rect;
    }
}