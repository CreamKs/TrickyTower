package com.example.trickytower.util;

import android.graphics.RectF;

/**
 * RectF 유틸리티 클래스
 * 중심 좌표와 너비/높이를 이용해 RectF의 영역을 설정합니다.
 */
public class RectUtil {
    private RectUtil() {
        // 인스턴스화 방지
    }

    /**
     * 기존 RectF 객체를 중심 좌표(cx, cy)와 주어진 너비(width), 높이(height)에 맞게 설정합니다.
     * @param rect  설정할 RectF 객체
     * @param cx    중심 X 좌표
     * @param cy    중심 Y 좌표
     * @param width  전체 너비
     * @param height 전체 높이
     */
    public static void setRect(RectF rect, float cx, float cy, float width, float height) {
        float halfW = width  / 2f;
        float halfH = height / 2f;
        rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
    }
}
