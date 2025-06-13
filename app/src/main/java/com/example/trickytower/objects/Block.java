// Block.java
package com.example.trickytower.objects;

import android.graphics.RectF;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class Block extends Sprite implements IBoxCollidable {
    private RectF collisionRect;

    public Block(int resId, float x, float y, float width, float height) {
        super(resId, x, y, width, height);
        this.collisionRect = new RectF();
        updateCollisionRect();
    }

    private void updateCollisionRect() {
        collisionRect.set(dstRect);
    }

    @Override
    public void update() {
        super.update();
        updateCollisionRect();
    }

    @Override
    public RectF getCollisionRect() {
        return collisionRect;
    }
}
