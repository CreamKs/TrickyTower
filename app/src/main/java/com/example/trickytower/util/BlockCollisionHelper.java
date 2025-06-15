package com.example.trickytower.util;

import android.graphics.RectF;
import java.util.List;

import com.example.trickytower.objects.ComplexBlock;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

/**
 * BlockCollisionHelper: 셀 단위 충돌 감지
 */
public class BlockCollisionHelper {
    private static final float EPSILON = 1f; // 착지 판정 감도를 높이기 위한 여유값

    /**
     * 현재 블록이 충돌할 Y 좌표(픽셀 단위) 반환
     * NaN이면 충돌 없음
     */
    public static float getCollisionContactY(ComplexBlock block, List<ComplexBlock> landedBlocks, RectF platformBox) {
        float contactY = Float.NaN;
        List<RectF> cells = block.getCellBoxes();
        for (RectF cell : cells) {
            float bottom = cell.bottom;
            // 바닥 충돌
            if (bottom >= Metrics.height - EPSILON) {
                contactY = Float.isNaN(contactY) ? Metrics.height : Math.min(contactY, Metrics.height);
            }
            if (platformBox != null
                    && cell.right > platformBox.left && cell.left < platformBox.right
                    && bottom >= platformBox.top - EPSILON && cell.top <= platformBox.top + EPSILON) {
                contactY = Float.isNaN(contactY)
                        ? platformBox.top
                        : Math.min(contactY, platformBox.top);
            }
            // 이미 착지한 블록의 각 셀과 비교
            for (ComplexBlock landed : landedBlocks) {
                for (RectF landedCell : landed.getCellBoxes()) {
                    // 가로 범위가 겹치면서 현재 셀의 아래쪽이 상대 셀 위쪽보다 약간만 아래에 있어도
                    // 착지로 인식하도록 여유값(EPSILON)을 적용한다
                    if (landedCell.left < cell.right && landedCell.right > cell.left
                            && bottom >= landedCell.top - EPSILON && cell.top <= landedCell.top + EPSILON) {
                        contactY = Float.isNaN(contactY)
                                ? landedCell.top
                                : Math.min(contactY, landedCell.top);
                    }
                }
            }
        }
        return contactY;
    }
}
