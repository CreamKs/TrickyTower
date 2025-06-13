package com.example.trickytower.scene;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.trickytower.R;
import com.example.trickytower.objects.ComplexBlock;
import com.example.trickytower.objects.ShapeType;
import com.example.trickytower.util.BlockCollisionHelper;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.collision.shapes.EdgeShape;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class GameScene extends Scene {
    private static final float CELL_SIZE = 40f;
    private static final float PPM = 50f;
    private static final int GRID_SIZE = ComplexBlock.GRID_SIZE;
    private static final Vec2 GRAVITY = new Vec2(0, 9.8f);
    private static final float TIME_STEP = 1/60f;
    private static final int VELOCITY_ITERS = 6, POSITION_ITERS = 2;
    private static final float FAST_DROP_SPEED = 10f; // m/s
    private static final long MOVE_DELAY_MS = 100;

    private World world;
    private ComplexBlock current;
    private final List<ComplexBlock> landedBlocks = new ArrayList<>();
    private final Random rand = new Random();

    private boolean touchEnabled;
    private boolean isFastDropping;
    private float touchStartX, touchStartY;
    private long lastMoveTime;

    @Override
    public void onEnter() {
        // Debug bounding boxes
        kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView.drawsDebugStuffs = true;

        world = new World(GRAVITY);
        createGround();
        initLayers(SceneLayer.values().length);
        add(SceneLayer.BACKGROUND, new Sprite(
            R.drawable.bg_game,
            Metrics.width/2f, Metrics.height/2f,
            Metrics.width, Metrics.height
        ));
        spawnBlock();
    }

    private void createGround() {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.STATIC;
        bd.position.set(0, Metrics.height/PPM);
        Body ground = world.createBody(bd);
        EdgeShape shape = new EdgeShape();
        shape.set(new Vec2(0, 0), new Vec2(Metrics.width/PPM, 0));
        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        ground.createFixture(fd);
    }

    private void spawnBlock() {
        touchEnabled = false;
        isFastDropping = false;
        ShapeType type = ShapeType.values()[rand.nextInt(ShapeType.values().length)];
        float startX = Metrics.width/2f;
        float startY = - GRID_SIZE * CELL_SIZE;
        current = new ComplexBlock(type, startX, startY, CELL_SIZE);
        current.createPhysicsBody(world);
        add(SceneLayer.BLOCK, current);
        Log.d("GameScene", "spawnBlock: " + type + " at (" + startX + ", " + startY + ")");
    }

    @Override
    public void update() {
        if (current != null && isFastDropping) {
            current.getBody().setLinearVelocity(new Vec2(0, FAST_DROP_SPEED));
        }
        world.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS);
        super.update();
        if (current != null) current.update();
        for (ComplexBlock b : landedBlocks) b.update();

        if (current != null) {
            float contactY = BlockCollisionHelper.getCollisionContactY(current, landedBlocks);
            if (!Float.isNaN(contactY)) {
                float bottomOffset = current.getBottomOffset();
                float centerYPixel = contactY - bottomOffset;
                Vec2 pos = current.getBody().getPosition();
                Body body = current.getBody();
                body.setLinearVelocity(new Vec2(0, 0));
                body.setType(BodyType.STATIC);
                body.setTransform(new Vec2(pos.x, centerYPixel/PPM), body.getAngle());
                landedBlocks.add(current);
                spawnBlock();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float[] pts = Metrics.fromScreen(event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchEnabled = true;
                touchStartX = pts[0];
                touchStartY = pts[1];
                lastMoveTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!touchEnabled || current == null) break;
                float dx = pts[0] - touchStartX;
                float dy = pts[1] - touchStartY;
                long now = System.currentTimeMillis();
                if (Math.abs(dx) > Math.abs(dy) && now - lastMoveTime > MOVE_DELAY_MS) {
                    Vec2 pos = current.getBody().getPosition();
                    float step = (CELL_SIZE / PPM) * (dx > 0 ? 1 : -1);
                    current.getBody().setTransform(new Vec2(pos.x + step, pos.y), current.getBody().getAngle());
                    lastMoveTime = now;
                    touchStartX = pts[0];
                }
                if (dy > CELL_SIZE/2f) {
                    isFastDropping = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!touchEnabled) break;
                touchEnabled = false;
                if (Math.hypot(pts[0]-touchStartX, pts[1]-touchStartY) < CELL_SIZE/4f) {
                    current.rotate90();
                }
                isFastDropping = false;
                break;
        }
        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        for (ComplexBlock b : landedBlocks) b.draw(canvas);
        if (current != null) current.draw(canvas);
    }
}
