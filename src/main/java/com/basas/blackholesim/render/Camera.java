package com.basas.blackholesim.render;

import com.basas.blackholesim.core.math.Vec2;


public class Camera {
    private final Vec2 centerWorld = new Vec2(0, 0);
    private double zoom = 1.0;

    
    private double viewportW = 100;
    private double viewportH = 100;

    public void setViewport(double w, double h) {
        this.viewportW = Math.max(1, w);
        this.viewportH = Math.max(1, h);
    }

    public Vec2 getCenterWorld() {
        return centerWorld;
    }

    public void setCenterWorld(double x, double y) {
        centerWorld.set(x, y);
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = Math.max(0.15, Math.min(zoom, 8.0));
    }

    public Vec2 worldToScreen(Vec2 world) {
        
        double sx = (world.x - centerWorld.x) * zoom + viewportW / 2.0;
        double sy = (world.y - centerWorld.y) * zoom + viewportH / 2.0;
        return new Vec2(sx, sy);
    }

    public Vec2 screenToWorld(double sx, double sy) {
        
        double wx = (sx - viewportW / 2.0) / zoom + centerWorld.x;
        double wy = (sy - viewportH / 2.0) / zoom + centerWorld.y;
        return new Vec2(wx, wy);
    }

    public double worldToScreenScalar(double worldValue) {
        return worldValue * zoom;
    }

    public double worldToScreenLength(double worldLength) {
        return worldLength * zoom;
    }
}
