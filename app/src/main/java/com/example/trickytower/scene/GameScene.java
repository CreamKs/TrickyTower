package com.example.trickytower.scene;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
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
import org.jbox2d.dynamics.contacts.ContactEdge;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.joints.WeldJoint;
import org.jbox2d.dynamics.joints.WeldJointDef;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class GameScene extends Scene {
    private static final float CELL_SIZE = 40f;
    private static final float PPM = 50f;
    private static final Vec2 GRAVITY = new Vec2(0, 9.8f);
    private static final float TIME_STEP = 1/60f;
    private static final int VELOCITY_ITERS = 6, POSITION_ITERS = 2;
    private static final float DROP_SPEED = 2f; // 블록이 기본적으로 떨어지는 속도 (m/s)
    private static final float FAST_DROP_SPEED = 10f; // m/s
    private static final long MOVE_DELAY_MS = 100;

    private World world;
    private ComplexBlock current;
    private final List<ComplexBlock> landedBlocks = new ArrayList<>();
    private final List<WeldJoint> joints = new ArrayList<>();
    private final Random rand = new Random();

    private boolean touchEnabled;
    private boolean isFastDropping;
    private float touchStartX, touchStartY;
    private long lastMoveTime;
    private RectF groundBox;
    private RectF leftWallBox;
    private RectF rightWallBox;
    private static final Paint debugPaint = new Paint();
    static {
        debugPaint.setStyle(Paint.Style.STROKE);
        debugPaint.setColor(android.graphics.Color.BLUE);
        debugPaint.setStrokeWidth(2f);
    }

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
        float groundHeight = CELL_SIZE / 2f;
        BodyDef bd = new BodyDef();
        bd.type = BodyType.STATIC;
        bd.position.set(Metrics.width / 2f / PPM, (Metrics.height + groundHeight / 2f) / PPM);
        Body ground = world.createBody(bd);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(Metrics.width / 2f / PPM, groundHeight / 2f / PPM);
        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        ground.createFixture(fd);
        ground.setUserData("GROUND");
        groundBox = new RectF(0, Metrics.height, Metrics.width, Metrics.height + groundHeight);

        // 양쪽 벽 생성
        float wallWidth = CELL_SIZE / 2f;
        BodyDef wallBd = new BodyDef();
        wallBd.type = BodyType.STATIC;
        // 왼쪽 벽
        wallBd.position.set(-wallWidth / 2f / PPM, Metrics.height / 2f / PPM);
        Body leftWall = world.createBody(wallBd);
        PolygonShape wallShape = new PolygonShape();
        wallShape.setAsBox(wallWidth / 2f / PPM, Metrics.height / 2f / PPM);
        FixtureDef wallFd = new FixtureDef();
        wallFd.shape = wallShape;
        leftWall.createFixture(wallFd);
        leftWall.setUserData("WALL");

        // 오른쪽 벽
        wallBd.position.set((Metrics.width + wallWidth / 2f) / PPM, Metrics.height / 2f / PPM);
        Body rightWall = world.createBody(wallBd);
        rightWall.createFixture(wallFd);
        rightWall.setUserData("WALL");

        leftWallBox = new RectF(-wallWidth, 0, 0, Metrics.height);
        rightWallBox = new RectF(Metrics.width, 0, Metrics.width + wallWidth, Metrics.height);
    }

    private void spawnBlock() {
        touchEnabled = false;
        isFastDropping = false;
        ShapeType type = ShapeType.values()[rand.nextInt(ShapeType.values().length)];
        float startX = Metrics.width/2f;
        float startY = - type.getHeightCells() * CELL_SIZE;
        current = new ComplexBlock(type, startX, startY, CELL_SIZE);
        current.createPhysicsBody(world);
        // 새로 생성된 블록은 중력의 영향을 받지 않고 일정 속도로 떨어지도록 설정
        Body body = current.getBody();
        body.setGravityScale(0f);
        body.setLinearVelocity(new Vec2(0, DROP_SPEED));
        add(SceneLayer.BLOCK, current);
        Log.d("GameScene", "spawnBlock: " + type + " at (" + startX + ", " + startY + ")");
    }

    private void checkForLanding() {
        Body body = current.getBody();
        for (ContactEdge ce = body.getContactList(); ce != null; ce = ce.next) {
            if (!ce.contact.isTouching()) continue;
            Body other = ce.other;
            Object data = other.getUserData();
            if ("GROUND".equals(data) || data instanceof ComplexBlock) {
                float contactY = BlockCollisionHelper.getCollisionContactY(current, landedBlocks);
                if (Float.isNaN(contactY)) return; // 계산 실패 시 무시
                float bottomOffset = current.getBottomOffset();
                float centerY = contactY - bottomOffset;
                body.setLinearVelocity(new Vec2(0, 0));
                body.setAngularVelocity(0f);
                body.setGravityScale(1f); // 착지 후에는 중력 적용
                body.setTransform(new Vec2(body.getPosition().x, centerY / PPM), body.getAngle());
                // 착지 후에도 동적으로 유지하되 속도를 0으로 리셋하여 중력만 적용되도록 한다
                body.setType(BodyType.DYNAMIC);
                landedBlocks.add(current);

                // 다른 블록 위에 착지한 경우 약한 응집력을 위한 웰드 조인트 생성
                if (data instanceof ComplexBlock) {
                    ComplexBlock landedOn = (ComplexBlock) data;
                    WeldJointDef jd = new WeldJointDef();
                    jd.initialize(landedOn.getBody(), body, body.getWorldCenter());
                    jd.frequencyHz = 2f;  // 조금 흔들리도록 낮은 주파수 설정
                    jd.dampingRatio = 0.5f;
                    joints.add((WeldJoint) world.createJoint(jd));
                }

                spawnBlock();
                break;
            }
        }
    }

    @Override
    public void update() {
        if (current != null) {
            float speed = isFastDropping ? FAST_DROP_SPEED : DROP_SPEED;
            current.getBody().setLinearVelocity(new Vec2(0, speed));
        }
        world.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS);
        super.update();
        if (current != null) current.update();
        for (ComplexBlock b : landedBlocks) b.update();

        if (current != null) checkForLanding();
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
        if (groundBox != null && kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView.drawsDebugStuffs) {
            canvas.drawRect(groundBox, debugPaint);
            if (leftWallBox != null) canvas.drawRect(leftWallBox, debugPaint);
            if (rightWallBox != null) canvas.drawRect(rightWallBox, debugPaint);
        }
    }
}
