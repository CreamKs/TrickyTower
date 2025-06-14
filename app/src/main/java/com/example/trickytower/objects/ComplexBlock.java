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
import org.jbox2d.dynamics.World;

import com.example.trickytower.util.RectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * ComplexBlock: 이미지 픽셀을 이용해 히트박스를 생성하고
 * 회전 시 이미지와 히트박스가 함께 회전합니다.
 */
public class ComplexBlock extends Sprite implements IBoxCollidable {
    private static final float PPM = 50f;
    /** 기본 그리드 크기를 맞추기 위한 상수 */
    public static final int GRID_SIZE = 4;
    private static final float DEFAULT_CELLS = 4f;
    private static final float IMAGE_SCALE = 0.8f;

    private final ShapeType type;
    private final float cellSize;
    private final float scale;

    private final List<Vec2[]> localTris = new ArrayList<>();
    // 픽셀 단위 셀의 로컬 박스 목록
    private final List<RectF> localBoxes = new ArrayList<>();
    // 월드 좌표계에서 계산된 디버그 박스 목록
    private final List<RectF> boxes = new ArrayList<>();
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
        this.pivotX = x;
        this.pivotY = y;
        paint.setFilterBitmap(false);

        // 셀 크기에 맞춰 이미지 스케일 결정
        float targetWidth = type.getWidthCells() * cellSize * IMAGE_SCALE;
        this.scale = targetWidth / bitmap.getWidth();
        float scaledW = bitmap.getWidth() * scale;
        float scaledH = bitmap.getHeight() * scale;
        setPosition(pivotX, pivotY, scaledW, scaledH);

        buildTriangles();
        initBoxes();
    }

    /** 비트맵의 테두리를 따라 삼각형 목록을 생성 */
    private void buildTriangles() {
        localTris.clear();
        localBoxes.clear();

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (bitmap.getPixel(x, y) >>> 24) & 0xFF;
                if (alpha == 0) continue;
                float left = (x - w / 2f) * scale;
                float top = (y - h / 2f) * scale;
                float right = (x + 1 - w / 2f) * scale;
                float bottom = (y + 1 - h / 2f) * scale;
                localBoxes.add(new RectF(left, top, right, bottom));
                // 두 개의 삼각형으로 쪼개어 등록
                localTris.add(new Vec2[] {
                        new Vec2(left, top),
                        new Vec2(right, top),
                        new Vec2(right, bottom)
                });
                localTris.add(new Vec2[] {
                        new Vec2(left, top),
                        new Vec2(right, bottom),
                        new Vec2(left, bottom)
                });
            }
        }
    }



    /** 월드에 물리 바디 생성 */
    public void createPhysicsBody(World world) {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(pivotX / PPM, pivotY / PPM);
        bd.fixedRotation = true;
        body = world.createBody(bd);
        body.setUserData(this);
        for (Vec2[] tri : localTris) {
            PolygonShape shape = new PolygonShape();
            Vec2[] verts = new Vec2[3];
            for (int i = 0; i < 3; i++) {
                verts[i] = new Vec2(tri[i].x / PPM, tri[i].y / PPM);
            }
            shape.set(verts, verts.length);
            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 1f;
            body.createFixture(fd);
        }
    }

    @Override
    public void update() {
        if (body == null) return;
        Vec2 pos = body.getPosition();
        float px = pos.x * PPM;
        float py = pos.y * PPM;
        RectUtil.setRect(dstRect, px, py, width, height);
        initBoxes();
    }

    @Override
    public void draw(Canvas canvas) {
        if (body == null) return;
        float angleDeg = (float) Math.toDegrees(body.getAngle());
        Vec2 pos = body.getPosition();
        float px = pos.x * PPM;
        float py = pos.y * PPM;
        canvas.save();
        canvas.rotate(angleDeg, px, py);
        canvas.drawBitmap(bitmap, null, dstRect, paint);
        canvas.restore();
        if (GameView.drawsDebugStuffs) {
            for (RectF r : boxes) canvas.drawRect(r, debugPaint);
        }
    }

    /** 90도 회전 */
    public void rotate90() {
        if (body == null) return;
        Vec2 pos = body.getPosition();
        float newAngle = body.getAngle() + (float) (Math.PI / 2);
        body.setTransform(pos, newAngle);
        initBoxes();
    }

    /** 180도 회전 */
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

    /** 현재 형태의 가장 아래쪽 오프셋(px) 계산 */
    public float getBottomOffset() {
        float angle = body != null ? body.getAngle() : 0f;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float max = Float.NEGATIVE_INFINITY;
        for (Vec2[] tri : localTris) {
            for (Vec2 v : tri) {
                float wy = v.x * sin + v.y * cos;
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
