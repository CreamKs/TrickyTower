package com.example.trickytower.objects;

import android.graphics.Color;

/**
 * 7가지 테트로미노 블록 타입을 4×4 매트릭스 형태로 정의합니다.
 * true = 블록 셀, false = 빈 칸.
 */
public enum ShapeType {
    I(Color.CYAN, new boolean[][] {
        {false, false, false, false},
        { true,  true,  true,  true},
        {false, false, false, false},
        {false, false, false, false}
    }),
    J(Color.BLUE, new boolean[][] {
        { true, false, false, false},
        { true,  true,  true, false},
        {false, false, false, false},
        {false, false, false, false}
    }),
    L(0xFFFFA500, new boolean[][] {
        {false, false,  true, false},
        { true,  true,  true, false},
        {false, false, false, false},
        {false, false, false, false}
    }),
    O(Color.YELLOW, new boolean[][] {
        {false,  true,  true, false},
        {false,  true,  true, false},
        {false, false, false, false},
        {false, false, false, false}
    }),
    S(Color.GREEN, new boolean[][] {
        {false,  true,  true, false},
        { true,  true, false, false},
        {false, false, false, false},
        {false, false, false, false}
    }),
    T(0xFF800080, new boolean[][] {
        {false,  true, false, false},
        { true,  true,  true, false},
        {false, false, false, false},
        {false, false, false, false}
    }),
    Z(Color.RED, new boolean[][] {
        { true,  true, false, false},
        {false,  true,  true, false},
        {false, false, false, false},
        {false, false, false, false}
    });

    public final int color;
    public final boolean[][] mask;

    ShapeType(int color, boolean[][] mask) {
        this.color = color;
        this.mask = mask;
    }

    /** 전체 매트릭스에서 true인 셀의 최소행과 최대행, 최소열과 최대열을 계산해 실제 블록 크기를 리턴합니다. */
    public int getWidthCells() {
        int width = 0;
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                if (mask[r][c]) {
                    width = Math.max(width, c + 1);
                }
            }
        }
        return width;
    }

    public int getHeightCells() {
        int height = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (mask[r][c]) {
                    height = Math.max(height, r + 1);
                }
            }
        }
        return height;
    }
}
