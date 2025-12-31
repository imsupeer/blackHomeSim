package com.basas.blackholesim.core.math;


public class Vec2 {
    public double x;
    public double y;

    public Vec2() {
        this(0, 0);
    }

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2 set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vec2 copy() {
        return new Vec2(x, y);
    }

    public Vec2 add(Vec2 o) {
        this.x += o.x;
        this.y += o.y;
        return this;
    }

    public Vec2 addScaled(Vec2 o, double s) {
        this.x += o.x * s;
        this.y += o.y * s;
        return this;
    }

    public Vec2 sub(Vec2 o) {
        this.x -= o.x;
        this.y -= o.y;
        return this;
    }

    public Vec2 scale(double s) {
        this.x *= s;
        this.y *= s;
        return this;
    }

    public Vec2 mul(double s) {
        return scale(s);
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public double lengthSq() {
        return x * x + y * y;
    }

    public Vec2 normalize() {
        double len = length();
        if (len > 1e-12) {
            x /= len;
            y /= len;
        }
        return this;
    }

    public static Vec2 add(Vec2 a, Vec2 b) {
        return new Vec2(a.x + b.x, a.y + b.y);
    }

    public static Vec2 sub(Vec2 a, Vec2 b) {
        return new Vec2(a.x - b.x, a.y - b.y);
    }

    public static Vec2 scaled(Vec2 a, double s) {
        return new Vec2(a.x * s, a.y * s);
    }
}
