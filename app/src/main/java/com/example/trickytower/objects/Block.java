package com.example.trickytower.objects;

import android.graphics.RectF;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IRecyclable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

/**
 * Block 클래스는 떨어지는 블록 하나를 표현합니다.
 * IBoxCollidable: 충돌 판정을 위한 경계 사각형 제공
 * IRecyclable: 사용 후 재활용 풀에 반환되는 오브젝트로 처리
 */
public class Block extends Sprite implements IBoxCollidable, IRecyclable {
    private RectF rect;  // 충돌 영역을 위해 재사용하는 RectF 객체

    /**
     * 생성자: 블록 스프라이트 초기화 및 충돌 영역 객체 생성
     * @param resId 이미지 리소스 ID
     * @param x 시작 x 위치
     * @param y 시작 y 위치
     * @param width 블록 너비
     * @param height 블록 높이
     */
    public Block(int resId, float x, float y, float width, float height) {
        super(resId, x, y, width, height);
        rect = new RectF();
    }

    /**
     * 매 프레임 호출: 블록을 아래로 이동시키고 dstRect를 갱신
     */
    @Override
    public void update() {
        // 중력 효과 없이 일정 속도로 아래로 낙하
        y += 300 * GameView.frameTime;
        // dstRect는 Sprite에서 관리하는 그리기 사각형
        dstRect.offsetTo(x - width/2, y - height/2);
    }

    /**
     * IBoxCollidable 구현: 충돌 판정을 위한 RectF 반환
     */
    @Override
    public RectF getCollisionRect() {
        rect.set(dstRect);
        return rect;
    }

    /**
     * IRecyclable 구현: 객체 풀에 반환될 때 호출
     * 필요 시 위치나 상태 초기화를 여기에 작성
     */
    @Override
    public void onRecycle() {
        // 예: y 위치 초기화, 속도 초기화 등이 필요하면 여기서 수행
    }
}