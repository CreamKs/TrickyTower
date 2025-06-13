package com.example.trickytower.objects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;

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
 * ComplexBlock: 테트리스 4x4 그리드 중심 피벗 회전 및 셀 단위 충돌 처리
 */
public class ComplexBlock extends Sprite implements IBoxCollidable {
    private static final float PPM = 50f;
    public static final int GRID_SIZE = 4;
    private final ShapeType type;
    private final float cellSize;
    private boolean[][] mask;  // 4x4 그리드
    private final List<RectF> boxes = new ArrayList<>();
    private final Paint paint = new Paint();
    private Body body;

    public ComplexBlock(ShapeType type, float x, float y, float cellSize) {
        super(type.resId, x, y, cellSize * GRID_SIZE, cellSize * GRID_SIZE);
        this.type = type;
        this.cellSize = cellSize;
        paint.setFilterBitmap(false);
        // mask 초기화 (4x4)
        mask = new boolean[GRID_SIZE][GRID_SIZE];
        boolean[][] orig = type.mask;
        for (int r = 0; r < GRID_SIZE; r++) {
            System.arraycopy(orig[r], 0, mask[r], 0, GRID_SIZE);
        }
        initBoxes();
    }

    /** 생성 시 호출: 각 셀에 대응하는 fixture 추가 */
    public void createPhysicsBody(World world) {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(x / PPM, y / PPM);
        bd.fixedRotation = false;
        body = world.createBody(bd);
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (!mask[r][c]) continue;
                PolygonShape shape = new PolygonShape();
                float half = cellSize / 2f / PPM;
                // 4x4 피벗(중앙) 기준 상대 좌표
                Vec2 center = new Vec2(
                    ((c + 0.5f) - GRID_SIZE / 2f) * cellSize / PPM,
                    ((r + 0.5f) - GRID_SIZE / 2f) * cellSize / PPM
                );
                shape.setAsBox(half, half, center, 0);
                FixtureDef fd = new FixtureDef();
                fd.shape = shape;
                fd.density = 1f;
                body.createFixture(fd);
            }
        }
    }

    @Override
    public void update() {
        if (body == null) return;
        Vec2 pos = body.getPosition();
        float centerX = pos.x * PPM;
        float centerY = pos.y * PPM;
        // dstRect를 body 위치와 크기에 맞춰 설정
        RectUtil.setRect(dstRect, centerX, centerY, cellSize * GRID_SIZE, cellSize * GRID_SIZE);
        initBoxes();
    }

    @Override
    public void draw(Canvas canvas) {
        // 회전을 Canvas 자체에 적용하여 깨짐 방지
        float angleDeg = (float) Math.toDegrees(body.getAngle());
        float px = dstRect.centerX();
        float py = dstRect.centerY();
        canvas.save();
        canvas.rotate(angleDeg, px, py);
        canvas.drawBitmap(bitmap, null, dstRect, paint);
        canvas.restore();
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
        initBoxes();
    }

    private void initBoxes() {
        boxes.clear();
        float cx = dstRect.centerX();
        float cy = dstRect.centerY();
        float angle = body != null ? body.getAngle() : 0f;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (!mask[r][c]) continue;
                // 셀 중심의 로컬 좌표(피벗 기준)
                float localX = ((c + 0.5f) - GRID_SIZE / 2f) * cellSize;
                float localY = ((r + 0.5f) - GRID_SIZE / 2f) * cellSize;

                // 회전 변환 후 월드 좌표
                float worldX = cx + localX * cos - localY * sin;
                float worldY = cy + localX * sin + localY * cos;

                boxes.add(new RectF(
                        worldX - cellSize / 2f,
                        worldY - cellSize / 2f,
                        worldX + cellSize / 2f,
                        worldY + cellSize / 2f
                ));
            }
        }
    }

    public List<RectF> getCellBoxes() {
        return boxes;
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
