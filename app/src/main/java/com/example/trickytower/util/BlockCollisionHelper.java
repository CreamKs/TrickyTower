package com.example.trickytower.util;

import android.graphics.RectF;
import java.util.List;

import com.example.trickytower.objects.Block;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

/**
 * BlockCollisionHelper
 *
 * 블록(current)과 쌓인 블록들 및 화면 바닥 간 충돌을 처리합니다.
 */
public class BlockCollisionHelper {
    /**
     * 블록과 바닥 혹은 쌓인 블록 간의 충돌을 검사하고,
     * 충돌 시 contactY(충돌 지점 Y 좌표)를 반환합니다.
     * @param current 검사항목 블록
     * @param landedBlocks IBoxCollidable을 구현한 쌓인 블록 리스트
     * @return 충돌 시 contactY (>=0), 충돌 없으면 Float.NaN
     */
    public static float getCollisionContactY(Block current, List<IBoxCollidable> landedBlocks) {
        RectF rect = current.getCollisionRect();
        float bottom = rect.bottom;
        // 1) 화면 바닥과 충돌
        if (bottom >= Metrics.height) {
            return Metrics.height;
        }
        // 2) 쌓인 블록들과 충돌 검사
        float closestY = Float.NaN;
        for (IBoxCollidable landed : landedBlocks) {
            RectF lr = landed.getCollisionRect();
            float cx = rect.centerX();
            if (cx >= lr.left && cx <= lr.right && bottom >= lr.top) {
                if (Float.isNaN(closestY) || lr.top > closestY) {
                    closestY = lr.top;
                }
            }
        }
        return closestY;
    }

    /**
     * 두 블록(Landed 블록과 현재 블록)의 X축 겹침 여부 반환
     */
    public static boolean isXOverlap(Block block, IBoxCollidable landed) {
        RectF r1 = block.getCollisionRect();
        RectF r2 = landed.getCollisionRect();
        float cx = r1.centerX();
        return cx >= r2.left && cx <= r2.right;
    }
}
