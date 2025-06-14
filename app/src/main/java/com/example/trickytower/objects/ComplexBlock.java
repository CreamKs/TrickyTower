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
        boolean[][] mask = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (bitmap.getPixel(x, y) >>> 24) & 0xFF;
                mask[y][x] = alpha > 0;
            }
        }

        List<Vec2> poly = traceOutline(mask);
        List<Vec2[]> tris = triangulatePolygon(poly);
        for (Vec2[] t : tris) {
            Vec2[] scaled = new Vec2[3];
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < 3; i++) {
                float sx = (t[i].x - w / 2f) * scale;
                float sy = (t[i].y - h / 2f) * scale;
                scaled[i] = new Vec2(sx, sy);
                if (sx < minX) minX = sx;
                if (sy < minY) minY = sy;
                if (sx > maxX) maxX = sx;
                if (sy > maxY) maxY = sy;
            }
            localTris.add(scaled);
            localBoxes.add(new RectF(minX, minY, maxX, maxY));
        }
    }

    /** Marching Squares 방식으로 외곽선 점들을 추출 */
    private List<Vec2> traceOutline(boolean[][] mask) {
        int h = mask.length;
        int w = mask[0].length;
        class Edge { int sx, sy, ex, ey; Edge(int sx,int sy,int ex,int ey){this.sx=sx;this.sy=sy;this.ex=ex;this.ey=ey;} }
        List<Edge> edges = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!mask[y][x]) continue;
                if (x == 0 || !mask[y][x-1]) edges.add(new Edge(x, y+1, x, y));
                if (y == 0 || !mask[y-1][x]) edges.add(new Edge(x, y, x+1, y));
                if (x == w-1 || !mask[y][x+1]) edges.add(new Edge(x+1, y, x+1, y+1));
                if (y == h-1 || !mask[y+1][x]) edges.add(new Edge(x+1, y+1, x, y+1));
            }
        }
        if (edges.isEmpty()) return new ArrayList<>();
        // 연결 구조 만들기
        java.util.Map<String, Edge> map = new java.util.HashMap<>();
        for (Edge e : edges) {
            map.put(e.sx + "," + e.sy, e);
        }
        Edge first = edges.get(0);
        List<Vec2> poly = new ArrayList<>();
        int cx = first.sx, cy = first.sy;
        poly.add(new Vec2(cx, cy));
        while (true) {
            Edge e = map.remove(cx + "," + cy);
            if (e == null) break;
            cx = e.ex; cy = e.ey;
            poly.add(new Vec2(cx, cy));
            if (cx == first.sx && cy == first.sy) break;
        }
        return poly;
    }

    /** 다각형을 Ear clipping으로 삼각형 분해 */
    private static List<Vec2[]> triangulatePolygon(List<Vec2> poly) {
        List<Vec2[]> result = new ArrayList<>();
        if (poly.size() < 3) return result;
        List<Vec2> verts = new ArrayList<>(poly);
        float area = 0f;
        for (int i = 0; i < verts.size(); i++) {
            Vec2 a = verts.get(i);
            Vec2 b = verts.get((i + 1) % verts.size());
            area += a.x * b.y - a.y * b.x;
        }
        final boolean ccw = area > 0f; // 화면 좌표 기준
        int guard = 0;
        while (verts.size() >= 3 && guard++ < 1000) {
            int n = verts.size();
            boolean earFound = false;
            for (int i = 0; i < n; i++) {
                Vec2 prev = verts.get((i + n - 1) % n);
                Vec2 curr = verts.get(i);
                Vec2 next = verts.get((i + 1) % n);
                float cross = (curr.x - prev.x) * (next.y - prev.y) - (curr.y - prev.y) * (next.x - prev.x);
                if (ccw ? cross <= 0f : cross >= 0f) continue; // convex check
                boolean contains = false;
                for (int j = 0; j < n; j++) {
                    if (j == i || j == (i + n - 1) % n || j == (i + 1) % n) continue;
                    if (pointInTriangle(verts.get(j), prev, curr, next)) { contains = true; break; }
                }
                if (contains) continue;
                result.add(new Vec2[] { prev.clone(), curr.clone(), next.clone() });
                verts.remove(i);
                earFound = true;
                break;
            }
            if (!earFound) break; // 실패
        }
        return result;
    }

    private static boolean pointInTriangle(Vec2 p, Vec2 a, Vec2 b, Vec2 c) {
        float ab = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
        float bc = (c.x - b.x) * (p.y - b.y) - (c.y - b.y) * (p.x - b.x);
        float ca = (a.x - c.x) * (p.y - c.y) - (a.y - c.y) * (p.x - c.x);
        boolean hasNeg = (ab < 0) || (bc < 0) || (ca < 0);
        boolean hasPos = (ab > 0) || (bc > 0) || (ca > 0);
        return !(hasNeg && hasPos);
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
