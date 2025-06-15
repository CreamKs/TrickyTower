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

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import com.example.trickytower.objects.TextButton;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

import com.example.trickytower.scene.StageSelectScene;
import com.example.trickytower.scene.PauseScene;

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
    private final Random rand = new Random();

    private final int stageIndex;
    private int missedCount;
    private RectF platformBox;
    private RectF goalBox;

    public GameScene() {
        this(1);
    }

    public GameScene(int stageIndex) {
        this.stageIndex = stageIndex;
    }

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
        createStageObjects();
        initLayers(SceneLayer.values().length);
        add(SceneLayer.BACKGROUND, new Sprite(
            R.drawable.bg_game,
            Metrics.width/2f, Metrics.height/2f,
            Metrics.width, Metrics.height
        ));
        // 일시정지 버튼 추가
        float btnSize = 100f;
        TextButton pause = new TextButton(
                "ll",
                Metrics.width - btnSize,
                btnSize,
                btnSize,
                btnSize,
                pressed -> {
                    if (!pressed) {
                        new PauseScene().push();
                    }
                    return true;
                }
        );
        add(SceneLayer.UI, pause);
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

    private void createStageObjects() {
        float groundHeight = CELL_SIZE / 2f;
        // 작은 발판 생성 (중력 영향 없음)
        float platformWidth = CELL_SIZE * 3f;
        float platformY = Metrics.height - groundHeight * 3f;
        BodyDef bd = new BodyDef();
        bd.type = BodyType.STATIC;
        bd.position.set(Metrics.width / 2f / PPM, platformY / PPM);
        Body platform = world.createBody(bd);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(platformWidth / 2f / PPM, groundHeight / 2f / PPM);
        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        platform.createFixture(fd);
        platform.setUserData("PLATFORM");
        platformBox = new RectF(
                Metrics.width / 2f - platformWidth / 2f,
                platformY - groundHeight / 2f,
                Metrics.width / 2f + platformWidth / 2f,
                platformY + groundHeight / 2f);

        // 도착 지점 생성 (센서)
        float goalWidth = CELL_SIZE * 2f;
        float goalY = Metrics.height / 2f - (stageIndex - 1) * CELL_SIZE * 2f;
        BodyDef gbd = new BodyDef();
        gbd.type = BodyType.STATIC;
        gbd.position.set(Metrics.width / 2f / PPM, goalY / PPM);
        Body goal = world.createBody(gbd);
        PolygonShape gshape = new PolygonShape();
        gshape.setAsBox(goalWidth / 2f / PPM, groundHeight / 2f / PPM);
        FixtureDef gfd = new FixtureDef();
        gfd.shape = gshape;
        gfd.isSensor = true;
        goal.createFixture(gfd);
        goal.setUserData("GOAL");
        goalBox = new RectF(
                Metrics.width / 2f - goalWidth / 2f,
                goalY - groundHeight / 2f,
                Metrics.width / 2f + goalWidth / 2f,
                goalY + groundHeight / 2f);
    }

    private void spawnBlock() {
        touchEnabled = false;
        isFastDropping = false;
        ShapeType type = ShapeType.values()[rand.nextInt(ShapeType.values().length)];
        float gridStep = CELL_SIZE * ComplexBlock.getHitboxScale();
        float startX = Metrics.width/2f;
        if (type == ShapeType.O) {
            // O 블록은 실제 히트박스가 셀 크기보다 작아 중앙 정렬 시 살짝 왼쪽으로
            // 치우쳐 보인다. 이를 보정하기 위해 절반의 차이만큼 우측으로 이동한다.
            startX += (CELL_SIZE * (1f - ComplexBlock.getHitboxScale())) / 2f;
        }
        // 최초 생성 위치도 격자에 맞추어 정렬한다
        startX = Math.round(startX / gridStep) * gridStep;
        float startY = - type.getHeightCells() * CELL_SIZE;
        current = new ComplexBlock(type, startX, startY, CELL_SIZE);
        current.createPhysicsBody(world);
        // 새로 생성된 블록은 중력의 영향을 받지 않고 일정 속도로 떨어지도록 설정
        Body body = current.getBody();
        body.setBullet(true); // 빠른 낙하 시 충돌 누락 방지
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
            if ("GROUND".equals(data) || data instanceof ComplexBlock || "PLATFORM".equals(data)) {
                float contactY = BlockCollisionHelper.getCollisionContactY(current, landedBlocks, platformBox);
                if (Float.isNaN(contactY)) return; // 계산 실패 시 무시
                float bottomOffset = current.getBottomOffset();
                float centerY = contactY - bottomOffset;
                body.setLinearVelocity(new Vec2(0, 0));
                body.setAngularVelocity(0f);
                body.setGravityScale(1f); // 착지 후에는 중력 적용
                body.setTransform(new Vec2(body.getPosition().x, centerY / PPM), body.getAngle());
                // 착지 후에도 동적으로 유지하되 속도를 0으로 리셋하여 중력만 적용되도록 한다
                body.setType(BodyType.DYNAMIC);

                if ("GROUND".equals(data)) {
                    // 바닥에 닿았으면 블록을 삭제하고 카운트 증가
                    remove(SceneLayer.BLOCK, current);
                    world.destroyBody(body);
                    current = null;
                    missedCount++;
                    if (missedCount >= 3) {
                        stageFail();
                        return;
                    }
                    spawnBlock();
                    break;
                }

                landedBlocks.add(current);
                checkGoal(current); // 착지한 블록이 목표 지점에 도달했는지 확인
                spawnBlock();
                break;
            }
        }
    }

    private void checkLandedBlocksOnGround() {
        List<ComplexBlock> toRemove = new ArrayList<>();
        for (ComplexBlock b : landedBlocks) {
            Body body = b.getBody();
            for (ContactEdge ce = body.getContactList(); ce != null; ce = ce.next) {
                if (!ce.contact.isTouching()) continue;
                if ("GROUND".equals(ce.other.getUserData())) {
                    remove(SceneLayer.BLOCK, b);
                    world.destroyBody(body);
                    toRemove.add(b);
                    missedCount++;
                    if (missedCount >= 3) {
                        stageFail();
                        return;
                    }
                    break;
                }
            }
        }
        landedBlocks.removeAll(toRemove);
    }

    private void alignCurrentToGrid() {
        if (current == null) return;
        Body body = current.getBody();
        if (body.getGravityScale() != 0f) return;
        float hitStep = CELL_SIZE * ComplexBlock.getHitboxScale() / PPM;
        float x = body.getPosition().x;
        float newX = Math.round(x / hitStep) * hitStep;
        float halfW = current.getWidth() / 2f / PPM;
        float minX = halfW;
        float maxX = Metrics.width / PPM - halfW;
        if (newX < minX) newX = minX;
        if (newX > maxX) newX = maxX;
        if (Math.abs(newX - x) > 1e-4f) {
            body.setTransform(new Vec2(newX, body.getPosition().y), body.getAngle());
        }
    }

    private void checkGoal(ComplexBlock block) {
        if (goalBox == null) return;
        for (RectF r : block.getCellBoxes()) {
            if (RectF.intersects(r, goalBox)) {
                stageClear();
                break;
            }
        }
    }

    private void stageClear() {
        GameView.view.changeScene(new StageSelectScene());
    }

    private void stageFail() {
        GameView.view.changeScene(new StageSelectScene());
    }

    @Override
    public void update() {
        if (current != null) {
            float speed = isFastDropping ? FAST_DROP_SPEED : DROP_SPEED;
            current.getBody().setLinearVelocity(new Vec2(0, speed));
            alignCurrentToGrid();
        }
        world.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS);
        super.update();
        if (current != null) current.update();
        for (ComplexBlock b : landedBlocks) b.update();

        checkLandedBlocksOnGround();

        if (current != null) {
            checkForLanding();
        }
        for (ComplexBlock b : landedBlocks) {
            checkGoal(b);
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
                    float hitStep = CELL_SIZE * ComplexBlock.getHitboxScale();
                    float step = (hitStep / PPM) * (dx > 0 ? 1 : -1);
                    float halfW = current.getWidth() / 2f / PPM;
                    float newX = pos.x + step;
                    float minX = halfW;
                    float maxX = Metrics.width / PPM - halfW;
                    if (newX < minX) newX = minX;
                    if (newX > maxX) newX = maxX;
                    float gridStepWorld = hitStep / PPM;
                    newX = Math.round(newX / gridStepWorld) * gridStepWorld;
                    current.getBody().setTransform(new Vec2(newX, pos.y), current.getBody().getAngle());
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
            if (platformBox != null) canvas.drawRect(platformBox, debugPaint);
            if (goalBox != null) canvas.drawRect(goalBox, debugPaint);
        }
    }
}
