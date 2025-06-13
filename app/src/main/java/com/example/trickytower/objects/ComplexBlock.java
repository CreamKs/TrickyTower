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
 * ComplexBlock: 이미지 픽셀을 이용해 히트박스를 생성하고
 * 회전 시 이미지와 히트박스가 함께 회전합니다.
 */
public class ComplexBlock extends Sprite implements IBoxCollidable {
    private static final float PPM = 50f;
    /** 기본 너비를 맞추기 위한 기준 셀 크기 */
    private static final float DEFAULT_CELLS = 4f;
    private static final float IMAGE_SCALE = 0.8f;

    private final ShapeType type;
    private final float cellSize;
    private final float scale;

    private static class LocalBox {
        float cx, cy, halfW, halfH;
    }

    private final List<LocalBox> localBoxes = new ArrayList<>();
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
        this.pivotX = x;
        this.pivotY = y;
        paint.setFilterBitmap(false);

        // 셀 크기에 맞춰 이미지 스케일 결정
        float targetWidth = type.getWidthCells() * cellSize * IMAGE_SCALE;
        this.scale = targetWidth / bitmap.getWidth();
        float scaledW = bitmap.getWidth() * scale;
        float scaledH = bitmap.getHeight() * scale;
        setPosition(pivotX, pivotY, scaledW, scaledH);

        buildLocalBoxes();
        initBoxes();
    }

    /** 비투명 픽셀 영역을 찾아 localBoxes 리스트를 구축 */
    private void buildLocalBoxes() {
        localBoxes.clear();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        for (int row = 0; row < h; row++) {
            int start = -1;
            for (int col = 0; col < w; col++) {
                int alpha = (bitmap.getPixel(col, row) >>> 24) & 0xFF;
                boolean opaque = alpha > 0;
                if (opaque) {
                    if (start < 0) start = col;
                } else if (start >= 0) {
                    addBox(start, col - 1, row, w, h);
                    start = -1;
                }
            }
            if (start >= 0) {
                addBox(start, w - 1, row, w, h);
            }
        }
    }

    private void addBox(int sx, int ex, int row, int imgW, int imgH) {
        LocalBox b = new LocalBox();
        float centerXPixel = (sx + ex + 1) / 2f - imgW / 2f;
        float centerYPixel = (row + 0.5f) - imgH / 2f;
        b.cx = centerXPixel * scale;
        b.cy = centerYPixel * scale;
        b.halfW = (ex - sx + 1) * scale / 2f;
        b.halfH = scale / 2f;
        localBoxes.add(b);
    }

    /** 현 바디의 모든 피처를 제거 후 다시 생성 */
    private void recreateFixtures() {
        if (body == null) return;
        for (Fixture f : fixtures) {
            body.destroyFixture(f);
        }
        fixtures.clear();
        for (LocalBox lb : localBoxes) {
            PolygonShape shape = new PolygonShape();
            shape.setAsBox(lb.halfW / PPM, lb.halfH / PPM,
                    new Vec2(lb.cx / PPM, lb.cy / PPM), 0);
            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 1f;
            Fixture fixture = body.createFixture(fd);
            fixtures.add(fixture);
        }
    }

    /** localBoxes를 90도 회전시킨다 */
    private void rotateLocalBoxes90() {
        for (LocalBox lb : localBoxes) {
            float oldCx = lb.cx;
            lb.cx = -lb.cy;
            lb.cy = oldCx;
            float oldHalf = lb.halfW;
            lb.halfW = lb.halfH;
            lb.halfH = oldHalf;
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
        for (LocalBox lb : localBoxes) {
            PolygonShape shape = new PolygonShape();
            shape.setAsBox(lb.halfW / PPM, lb.halfH / PPM,
                    new Vec2(lb.cx / PPM, lb.cy / PPM), 0);
            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 1f;
            Fixture f = body.createFixture(fd);
            fixtures.add(f);
        }
    }

    @Override
    public void update() {
        if (body == null) return;
        Vec2 pos = body.getPosition();
        float px = pos.x * PPM;
        float py = pos.y * PPM;
        RectUtil.setRect(dstRect, px, py, bitmap.getWidth() * scale, bitmap.getHeight() * scale);
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
        rotateLocalBoxes90();
        recreateFixtures();
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
        for (LocalBox lb : localBoxes) {
            float worldX = px + lb.cx * cos - lb.cy * sin;
            float worldY = py + lb.cx * sin + lb.cy * cos;
            boxes.add(new RectF(
                    worldX - lb.halfW,
                    worldY - lb.halfH,
                    worldX + lb.halfW,
                    worldY + lb.halfH));
        }
    }

    public List<RectF> getCellBoxes() {
        return boxes;
    }

    /** 현재 형태의 가장 아래쪽 오프셋(px) 계산 */
    public float getBottomOffset() {
        float max = Float.NEGATIVE_INFINITY;
        for (LocalBox lb : localBoxes) {
            float bottom = lb.cy + lb.halfH;
            if (bottom > max) max = bottom;
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
