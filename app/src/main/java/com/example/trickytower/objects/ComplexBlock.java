package com.example.trickytower.objects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Color;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;

import com.example.trickytower.util.RectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * ComplexBlock: 테트리스 4x4 그리드 중심 피벗 회전 및 셀 단위 충돌 처리
 */
public class ComplexBlock extends Sprite implements IBoxCollidable {
    private static final float PPM = 50f;
    public static final int GRID_SIZE = 4;
    private static final float PIVOT = GRID_SIZE / 2f;
    /** 이미지와 히트박스 크기를 결정하는 비율 */
    private static final float IMAGE_SCALE = 0.8f;

    private final ShapeType type;
    private final float cellSize;
    private final float scaledCellSize;

    // 4x4 그리드 마스크
    private boolean[][] mask;


    private final List<RectF> boxes = new ArrayList<>();
    private final List<Fixture> fixtures = new ArrayList<>();
    private final Paint paint = new Paint();
    private static final Paint debugPaint = new Paint();
    static {
        debugPaint.setStyle(Paint.Style.STROKE);
        debugPaint.setColor(Color.BLUE);
        debugPaint.setStrokeWidth(2f);
    }
    private Body body;
    private float pivotX, pivotY;

    public ComplexBlock(ShapeType type, float x, float y, float cellSize) {
        super(type.resId);
        this.type = type;
        this.cellSize = cellSize;
        this.scaledCellSize = cellSize * IMAGE_SCALE;
        this.pivotX = x;
        this.pivotY = y;

        paint.setFilterBitmap(false);

        mask = new boolean[GRID_SIZE][GRID_SIZE];
        boolean[][] orig = type.mask;
        for (int r = 0; r < GRID_SIZE; r++) {
            System.arraycopy(orig[r], 0, mask[r], 0, GRID_SIZE);
        }

        setPosition(pivotX, pivotY, GRID_SIZE * scaledCellSize, GRID_SIZE * scaledCellSize);
        initBoxes();
    }

    /** 생성 시 호출: 각 셀에 대응하는 fixture 추가 */
    public void createPhysicsBody(World world) {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(pivotX / PPM, pivotY / PPM);
        bd.fixedRotation = true;
        body = world.createBody(bd);
        body.setUserData(this);
        recreateFixtures();
    }

    /** 현재 mask에 맞춰 body의 fixture를 모두 새로 만든다 */
    private void recreateFixtures() {
        if (body == null) return;
        // 기존 fixture 제거
        for (Fixture f : fixtures) {
            body.destroyFixture(f);
        }
        fixtures.clear();

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (!mask[r][c]) continue;
                PolygonShape shape = new PolygonShape();
                float half = scaledCellSize / 2f / PPM;
                Vec2 center = new Vec2(
                        ((c + 0.5f) - PIVOT) * scaledCellSize / PPM,
                        ((r + 0.5f) - PIVOT) * scaledCellSize / PPM
                );
                shape.setAsBox(half, half, center, 0);
                FixtureDef fd = new FixtureDef();
                fd.shape = shape;
                fd.density = 1f;
                Fixture fixture = body.createFixture(fd);
                fixtures.add(fixture);
            }
        }
    }

    @Override
    public void update() {
        if (body == null) return;
        Vec2 pos = body.getPosition();
        float px = pos.x * PPM;
        float py = pos.y * PPM;
        RectUtil.setRect(dstRect, px, py, GRID_SIZE * scaledCellSize, GRID_SIZE * scaledCellSize);
        initBoxes();
    }

    @Override
    public void draw(Canvas canvas) {
        // 회전을 Canvas 자체에 적용하여 깨짐 방지
        float angleDeg = (float) Math.toDegrees(body.getAngle());
        Vec2 pos = body.getPosition();
        float px = pos.x * PPM;
        float py = pos.y * PPM;
        canvas.save();
        canvas.rotate(angleDeg, px, py);
        canvas.drawBitmap(bitmap, null, dstRect, paint);
        canvas.restore();
        if (GameView.drawsDebugStuffs) {
            for (RectF rect : boxes) {
                canvas.drawRect(rect, debugPaint);
            }
        }
    }

    /** 90° 회전: mask 업데이트 및 body transform */
    public void rotate90() {
        // mask 90° 회전
        boolean[][] newMask = new boolean[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                newMask[c][GRID_SIZE - 1 - r] = mask[r][c];
            }
        }
        mask = newMask;

        // 물리 body 회전
        Vec2 pos = body.getPosition();
        float newAngle = body.getAngle() + (float) (Math.PI / 2);
        body.setTransform(pos, newAngle);
        RectUtil.setRect(dstRect, pos.x * PPM, pos.y * PPM, GRID_SIZE * scaledCellSize, GRID_SIZE * scaledCellSize);
        recreateFixtures();
        initBoxes();
    }

    /** 180° 회전 */
    public void rotate180() {
        rotate90();
        rotate90();
    }

    private void initBoxes() {
        boxes.clear();
        float px = body != null ? body.getPosition().x * PPM : pivotX;
        float py = body != null ? body.getPosition().y * PPM : pivotY;
        float angle = body != null ? body.getAngle() : 0f;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float baseHalf = scaledCellSize / 2f;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (!mask[r][c]) continue;
                float localX = ((c + 0.5f) - PIVOT) * scaledCellSize;
                float localY = ((r + 0.5f) - PIVOT) * scaledCellSize;

                float worldX = px + localX * cos - localY * sin;
                float worldY = py + localX * sin + localY * cos;

                float half = baseHalf;

                RectF rect = new RectF(
                        worldX - half,
                        worldY - half,
                        worldX + half,
                        worldY + half
                );
                boxes.add(rect);
            }
        }
    }

    public List<RectF> getCellBoxes() {
        return boxes;
    }

    /** 현재 mask에서 피벗을 기준으로 가장 아래 셀의 bottom까지의 오프셋(px)을 구한다 */
    public float getBottomOffset() {
        float max = Float.NEGATIVE_INFINITY;
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (!mask[r][c]) continue;
                float bottom = ((r + 1f) - PIVOT) * scaledCellSize;
                if (bottom > max) max = bottom;
            }
        }
        return max == Float.NEGATIVE_INFINITY ? 0f : max;
    }

    public boolean[][] getMask() {
        return mask;
    }

    public Body getBody() {
        return body;
    }

    @Override
    public RectF getCollisionRect() {
        return dstRect;
    }
}
