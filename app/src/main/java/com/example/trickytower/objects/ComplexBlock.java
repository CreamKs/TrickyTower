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
    private static final float PIVOT = GRID_SIZE / 2f;

    private final ShapeType type;
    private final float cellSize;

    // 4x4 그리드 마스크
    private boolean[][] mask;

    // 현재 모양의 최소 영역 정보
    private int minRow, minCol, widthCells, heightCells;

    // 피벗(바디 중심)에서 실제 그리기 영역 중심까지의 오프셋
    private float offsetX, offsetY;

    private final List<RectF> boxes = new ArrayList<>();
    private final Paint paint = new Paint();
    private Body body;
    private float pivotX, pivotY;

    public ComplexBlock(ShapeType type, float x, float y, float cellSize) {
        super(type.resId);
        this.type = type;
        this.cellSize = cellSize;
        this.pivotX = x;
        this.pivotY = y;

        paint.setFilterBitmap(false);

        mask = new boolean[GRID_SIZE][GRID_SIZE];
        boolean[][] orig = type.mask;
        for (int r = 0; r < GRID_SIZE; r++) {
            System.arraycopy(orig[r], 0, mask[r], 0, GRID_SIZE);
        }

        recalcBounds();
        setPosition(pivotX + offsetX, pivotY + offsetY, widthCells * cellSize, heightCells * cellSize);
        initBoxes();
    }

    /** 생성 시 호출: 각 셀에 대응하는 fixture 추가 */
    public void createPhysicsBody(World world) {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(pivotX / PPM, pivotY / PPM);
        bd.fixedRotation = true;
        body = world.createBody(bd);

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (!mask[r][c]) continue;
                PolygonShape shape = new PolygonShape();
                float half = cellSize / 2f / PPM;
                Vec2 center = new Vec2(
                        ((c + 0.5f) - PIVOT) * cellSize / PPM,
                        ((r + 0.5f) - PIVOT) * cellSize / PPM
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
        float px = pos.x * PPM;
        float py = pos.y * PPM;
        float angle = body.getAngle();
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float ox = offsetX * cos - offsetY * sin;
        float oy = offsetX * sin + offsetY * cos;
        RectUtil.setRect(dstRect, px + ox, py + oy, widthCells * cellSize, heightCells * cellSize);
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

        recalcBounds();

        float cos = (float) Math.cos(newAngle);
        float sin = (float) Math.sin(newAngle);
        float ox = offsetX * cos - offsetY * sin;
        float oy = offsetX * sin + offsetY * cos;
        RectUtil.setRect(dstRect, pos.x * PPM + ox, pos.y * PPM + oy, widthCells * cellSize, heightCells * cellSize);
        initBoxes();
    }

    private void recalcBounds() {
        minRow = GRID_SIZE; minCol = GRID_SIZE;
        int maxRow = -1, maxCol = -1;
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (!mask[r][c]) continue;
                if (r < minRow) minRow = r;
                if (c < minCol) minCol = c;
                if (r > maxRow) maxRow = r;
                if (c > maxCol) maxCol = c;
            }
        }
        if (maxRow < minRow) { // all empty, fallback
            minRow = minCol = 0;
            maxRow = maxCol = 0;
        }
        widthCells = maxCol - minCol + 1;
        heightCells = maxRow - minRow + 1;

        this.width = widthCells * cellSize;
        this.height = heightCells * cellSize;

        // 피벗과 그리기 영역 중심 사이의 오프셋 계산
        float centerCol = minCol + widthCells / 2f;
        float centerRow = minRow + heightCells / 2f;
        offsetX = (centerCol - PIVOT) * cellSize;
        offsetY = (centerRow - PIVOT) * cellSize;
    }

    private void initBoxes() {
        boxes.clear();
        float px = body != null ? body.getPosition().x * PPM : pivotX;
        float py = body != null ? body.getPosition().y * PPM : pivotY;
        float angle = body != null ? body.getAngle() : 0f;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float baseHalf = cellSize / 2f;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (!mask[r][c]) continue;
                float localX = ((c + 0.5f) - PIVOT) * cellSize;
                float localY = ((r + 0.5f) - PIVOT) * cellSize;

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
