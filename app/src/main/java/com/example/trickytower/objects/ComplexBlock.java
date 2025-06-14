package com.example.trickytower.objects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.Path;

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
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

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

    private static class IntPoint {
        final int x, y;
        IntPoint(int x, int y) { this.x = x; this.y = y; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntPoint p = (IntPoint) o;
            return x == p.x && y == p.y;
        }
        @Override public int hashCode() { return 31 * x + y; }
    }

    private final List<Vec2[]> localTris = new ArrayList<>();
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

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        boolean[][] mask = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (bitmap.getPixel(x, y) >>> 24) & 0xFF;
                mask[y][x] = alpha > 0;
            }
        }

        Map<IntPoint, IntPoint> edges = new HashMap<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!mask[y][x]) continue;
                if (x == 0 || !mask[y][x - 1])
                    edges.put(new IntPoint(x, y), new IntPoint(x, y + 1));
                if (y == 0 || !mask[y - 1][x])
                    edges.put(new IntPoint(x, y), new IntPoint(x + 1, y));
                if (x == w - 1 || !mask[y][x + 1])
                    edges.put(new IntPoint(x + 1, y), new IntPoint(x + 1, y + 1));
                if (y == h - 1 || !mask[y + 1][x])
                    edges.put(new IntPoint(x + 1, y + 1), new IntPoint(x, y + 1));
            }
        }

        if (edges.isEmpty()) return;
        IntPoint start = edges.keySet().iterator().next();
        List<Vec2> polygon = new ArrayList<>();
        IntPoint p = start;
        do {
            polygon.add(pointToVec2(p, w, h));
            IntPoint next = edges.remove(p);
            if (next == null) break;
            p = next;
        } while (!p.equals(start) && polygon.size() < edges.size() + 2);

        List<Vec2[]> tris = triangulate(polygon);
        localTris.addAll(tris);
    }

    private Vec2 pointToVec2(IntPoint p, int imgW, int imgH) {
        float lx = (p.x - imgW / 2f) * scale;
        float ly = (p.y - imgH / 2f) * scale;
        return new Vec2(lx, ly);
    }

    private static boolean isConvex(Vec2 a, Vec2 b, Vec2 c) {
        float cross = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
        return cross < 0; // clockwise polygon
    }

    private static boolean pointInTri(Vec2 p, Vec2 a, Vec2 b, Vec2 c) {
        float area = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
        float s = ((a.y - c.y) * (p.x - c.x) + (c.x - a.x) * (p.y - c.y)) / area;
        float t = ((c.y - b.y) * (p.x - c.x) + (b.x - c.x) * (p.y - c.y)) / area;
        float u = 1 - s - t;
        return s >= 0 && t >= 0 && u >= 0;
    }

    private List<Vec2[]> triangulate(List<Vec2> poly) {
        List<Vec2[]> result = new ArrayList<>();
        List<Vec2> verts = new ArrayList<>(poly);
        if (verts.size() < 3) return result;
        int n = verts.size();
        int guard = 0;
        while (n >= 3 && guard < 1000) { // safety
            boolean earFound = false;
            for (int i = 0; i < n; i++) {
                Vec2 prev = verts.get((i + n - 1) % n);
                Vec2 curr = verts.get(i);
                Vec2 next = verts.get((i + 1) % n);
                if (!isConvex(prev, curr, next)) continue;
                boolean hasInner = false;
                for (int j = 0; j < n; j++) {
                    if (j == i || j == (i + 1) % n || j == (i + n - 1) % n) continue;
                    if (pointInTri(verts.get(j), prev, curr, next)) {
                        hasInner = true; break;
                    }
                }
                if (!hasInner) {
                    result.add(new Vec2[]{prev, curr, next});
                    verts.remove(i);
                    n--;
                    earFound = true;
                    break;
                }
            }
            if (!earFound) {
                Vec2 first = verts.get(0);
                for (int i = 1; i + 1 < n; i++) {
                    result.add(new Vec2[]{first, verts.get(i), verts.get(i + 1)});
                }
                break;
            }
            guard++;
        }
        return result;
    }

    /* no-op: fixtures rotate automatically with the body */


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
        for (Vec2[] tri : localTris) {
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            for (Vec2 v : tri) {
                float wx = px + v.x * cos - v.y * sin;
                float wy = py + v.x * sin + v.y * cos;
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
