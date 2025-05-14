package com.example.trickytower.objects;

import android.graphics.RectF;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.AnimSprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

/**
 * Player 클래스는 플레이어 캐릭터를 나타냅니다.
 * AnimSprite를 상속하여 프레임 기반 애니메이션 지원,
 * IBoxCollidable을 구현하여 충돌 영역 제공.
 */
public class Player extends AnimSprite implements IBoxCollidable {
    // 충돌 판정을 위한 재사용 RectF
    private RectF rect = new RectF();
    // 수직 속도 및 중력 가속도
    private float dy;
    private final float gravity = 800f;  // 픽셀/초^2

    /**
     * 생성자: 애니메이션 프레임 속도와 초기 위치/크기 설정
     * @param resId 스프라이트 시트 리소스
     * @param x 초기 x 좌표 (중앙 기준)
     * @param y 초기 y 좌표 (중앙 기준)
     * @param width 객체 너비
     * @param height 객체 높이
     */
    public Player(int resId, float x, float y, float width, float height) {
        super(resId, 10);  // 10fps로 애니메이션 재생
        setPosition(x, y, width, height);
    }

    /**
     * 매 프레임 호출: 중력 적용, 위치 갱신, 바닥 충돌 처리
     */
    @Override
    public void update() {
        // 중력 가속도 적용
        dy += gravity * GameView.frameTime;
        // y 좌표에 속도 반영
        y += dy * GameView.frameTime;
        // dstRect 위치 갱신 (Sprite 내부)
        dstRect.offsetTo(x - width/2f, y - height/2f);

        // TODO: 조이스틱 입력에 따른 이동(dx) 처리

        // 화면 하단(바닥) 충돌 처리
        if (y + height/2f > Metrics.height) {
            y = Metrics.height - height/2f;
            dy = 0f;
        }
    }

    /**
     * IBoxCollidable 구현: 현재 dstRect를 반환하여 충돌 판정에 사용
     */
    @Override
    public RectF getCollisionRect() {
        rect.set(dstRect);
        return rect;
    }
}
