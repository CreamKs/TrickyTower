package com.example.trickytower.util;

import android.graphics.RectF;
import java.util.List;

import com.example.trickytower.objects.ComplexBlock;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

/**
 * BlockCollisionHelper: 셀 단위 충돌 감지
 */
public class BlockCollisionHelper {
    /**
     * 현재 블록이 충돌할 Y 좌표(픽셀 단위) 반환
     * NaN이면 충돌 없음
     */
    public static float getCollisionContactY(ComplexBlock block, List<ComplexBlock> landedBlocks) {
        float contactY = Float.NaN;
        List<RectF> cells = block.getCellBoxes();
        for (RectF cell : cells) {
            float bottom = cell.bottom;
            // 바닥 충돌
            if (bottom >= Metrics.height) {
                contactY = Float.isNaN(contactY) ? Metrics.height : Math.min(contactY, Metrics.height);
            }
            // 이미 착지한 블록의 각 셀과 비교
            for (ComplexBlock landed : landedBlocks) {
                for (RectF landedCell : landed.getCellBoxes()) {
                    // 가로 범위 겹치면서 현재 셀의 아래쪽이 상대 셀 위쪽 이하이고
                    // 동시에 현재 셀의 윗면이 상대 셀 윗면보다 위에 있어야 "아래에서 위로" 충돌로 인정한다
                    if (landedCell.left < cell.right && landedCell.right > cell.left
                            && bottom >= landedCell.top && cell.top <= landedCell.top) {
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
