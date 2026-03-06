package edu.ds503.project2.model;

public class Rectangle2D {
    private final String id;
    private final int x;
    private final int y;
    private final int h;
    private final int w;

    public Rectangle2D(String id, int x, int y, int h, int w) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.h = h;
        this.w = w;
    }

    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getH() {
        return h;
    }

    public int getW() {
        return w;
    }

    public boolean contains(Point2D point) {
        int x2 = x + w;
        int y2 = y + h;
        return point.getX() >= x && point.getX() <= x2 && point.getY() >= y && point.getY() <= y2;
    }

    public boolean intersects(Rectangle2D other) {
        int thisX2 = x + w;
        int thisY2 = y + h;
        int otherX2 = other.x + other.w;
        int otherY2 = other.y + other.h;
        return x <= otherX2 && thisX2 >= other.x && y <= otherY2 && thisY2 >= other.y;
    }

    @Override
    public String toString() {
        return id + "," + x + "," + y + "," + h + "," + w;
    }
}
