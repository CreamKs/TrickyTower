package com.example.trickytower.objects;

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
import org.jbox2d.dynamics.World;

import com.example.trickytower.util.RectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * ComplexBlock: 이미지 대신 기본 도형을 사용하여 테트로미노를 그립니다.
 * 각 셀에 맞는 히트박스를 생성해 회전 시에도 모양이 유지됩니다.
 */
public class ComplexBlock extends Sprite implements IBoxCollidable {
    private static final float PPM = 50f;
    public static final int GRID_SIZE = 4;

    private final ShapeType type;
    private final float cellSize;
    private static final float HITBOX_SCALE = 0.9f; // 히트박스 축소 비율
    private final boolean[][] cells;
    private final int rows, cols;

    private final List<RectF> localBoxes = new ArrayList<>();
    private final List<RectF> boxes = new ArrayList<>();
    private final Paint fillPaint = new Paint();
    private static final Paint debugPaint = new Paint();
    static {
        debugPaint.setStyle(Paint.Style.STROKE);
        debugPaint.setColor(Color.BLUE);
        debugPaint.setStrokeWidth(2f);
    }

    private Body body;

    public ComplexBlock(ShapeType type, float x, float y, float cellSize) {
        super(0); // 이미지 사용 안 함
        this.type = type;
        this.cellSize = cellSize;
        this.fillPaint.setColor(type.color);

        // 마스크의 실제 영역 계산
        int minR = GRID_SIZE, minC = GRID_SIZE, maxR = -1, maxC = -1;
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (type.mask[r][c]) {
                    if (r < minR) minR = r;
                    if (c < minC) minC = c;
                    if (r > maxR) maxR = r;
                    if (c > maxC) maxC = c;
                }
            }
        }
        rows = maxR - minR + 1;
        cols = maxC - minC + 1;
        cells = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = type.mask[minR + r][minC + c];
            }
        }
        setPosition(x, y, cols * cellSize, rows * cellSize);
        buildLocalBoxes();
    }

    private void buildLocalBoxes() {
        localBoxes.clear();
        float centerHalf = cellSize / 2f;
        float hitHalf = cellSize * HITBOX_SCALE / 2f;
        float left0 = -cols * cellSize / 2f;
        float top0 = -rows * cellSize / 2f;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!cells[r][c]) continue;
                float cx = left0 + c * cellSize + centerHalf;
                float cy = top0 + r * cellSize + centerHalf;
                localBoxes.add(new RectF(cx - hitHalf, cy - hitHalf, cx + hitHalf, cy + hitHalf));
            }
        }
    }

    public void createPhysicsBody(World world) {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(x / PPM, y / PPM);
        bd.fixedRotation = false; // 중력에 의해 블록이 회전할 수 있도록 함
        body = world.createBody(bd);
        body.setUserData(this);

        float half = cellSize * HITBOX_SCALE / 2f / PPM;
        for (RectF r : localBoxes) {
            PolygonShape shape = new PolygonShape();
            Vec2 center = new Vec2(r.centerX() / PPM, r.centerY() / PPM);
            shape.setAsBox(half, half, center, 0);
            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 1f;
            fd.friction = 0.1f; // 약한 마찰력으로 경사면에서 미끄러지도록
            body.createFixture(fd);
        }
        initBoxes();
    }

    @Override
    public void update() {
        if (body == null) return;
        Vec2 pos = body.getPosition();
        RectUtil.setRect(dstRect, pos.x * PPM, pos.y * PPM, width, height);
        initBoxes();
    }

    @Override
    public void draw(Canvas canvas) {
        if (body == null) return;
        Vec2 pos = body.getPosition();
        float angleDeg = (float) Math.toDegrees(body.getAngle());
        float px = pos.x * PPM;
        float py = pos.y * PPM;
        canvas.save();
        canvas.rotate(angleDeg, px, py);
        float left0 = px - width / 2f;
        float top0 = py - height / 2f;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!cells[r][c]) continue;
                float left = left0 + c * cellSize;
                float top = top0 + r * cellSize;
                canvas.drawRect(left, top, left + cellSize, top + cellSize, fillPaint);
            }
        }
        canvas.restore();
        if (GameView.drawsDebugStuffs) {
            for (RectF r : boxes) canvas.drawRect(r, debugPaint);
        }
    }

    public void rotate90() {
        if (body == null) return;
        Vec2 pos = body.getPosition();
        float newAngle = body.getAngle() + (float) (Math.PI / 2);
        body.setTransform(pos, newAngle);
        initBoxes();
    }

    public void rotate180() {
        rotate90();
        rotate90();
    }

    private void initBoxes() {
        boxes.clear();
        float px = body.getPosition().x * PPM;
        float py = body.getPosition().y * PPM;
        float angle = body.getAngle();
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        for (RectF r : localBoxes) {
            float[] xs = {r.left, r.right, r.right, r.left};
            float[] ys = {r.top, r.top, r.bottom, r.bottom};
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                float wx = px + xs[i] * cos - ys[i] * sin;
                float wy = py + xs[i] * sin + ys[i] * cos;
                if (wx < minX) minX = wx;
                if (wy < minY) minY = wy;
                if (wx > maxX) maxX = wx;
                if (wy > maxY) maxY = wy;
            }
            boxes.add(new RectF(minX, minY, maxX, maxY));
        }
    }

    public List<RectF> getCellBoxes() {
        return boxes;
    }

    public float getBottomOffset() {
        float angle = body.getAngle();
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float half = cellSize / 2f;
        float left0 = -cols * cellSize / 2f;
        float top0 = -rows * cellSize / 2f;
        float max = Float.NEGATIVE_INFINITY;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!cells[r][c]) continue;
                float cx = left0 + c * cellSize + half;
                float cy = top0 + r * cellSize + half;
                float bottom = cy + half;
                float wy = cx * sin + bottom * cos;
                if (wy > max) max = wy;
            }
        }
        return max == Float.NEGATIVE_INFINITY ? 0f : max;
    }

    public Body getBody() {
        return body;
    }

    @Override
    public RectF getCollisionRect() {
        return dstRect;
    }
}
