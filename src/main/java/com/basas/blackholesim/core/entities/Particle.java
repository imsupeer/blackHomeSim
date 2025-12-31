package com.basas.blackholesim.core.entities;

import com.basas.blackholesim.core.math.Vec2;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.Deque;

public class Particle {
    private final Vec2 position;
    private final Vec2 velocity;
    private double radius = 2.3;
    private Color color = Color.rgb(180, 220, 255, 0.92);

    private final Deque<Vec2> trail = new ArrayDeque<>();
    private int maxTrailPoints = 120;

    private boolean alive = true;

    private boolean photon = false;
    private boolean geodesic = false;

    private String centralBhId = null;
    private double E = 1.0;
    private double L = 0.0;
    private double r = 0.0;
    private double phi = 0.0;
    private double pr = 0.0;

    public Particle(Vec2 position, Vec2 velocity) {
        this.position = position;
        this.velocity = velocity;
    }

    public Vec2 getPosition() { return position; }

    public Vec2 getVelocity() { return velocity; }

    public double getRadius() { return radius; }

    public void setRadius(double radius) { this.radius = radius; }

    public Color getColor() { return color; }

    public void setColor(Color color) { this.color = color; }

    public Deque<Vec2> getTrail() { return trail; }

    public int getMaxTrailPoints() { return maxTrailPoints; }

    public void setMaxTrailPoints(int maxTrailPoints) {
        this.maxTrailPoints = maxTrailPoints;
        while (trail.size() > this.maxTrailPoints) trail.removeFirst();
    }

    public boolean isAlive() { return alive; }

    public void kill() { this.alive = false; }

    public boolean isPhoton() { return photon; }

    public void setPhoton(boolean photon) { this.photon = photon; }

    public boolean isGeodesic() { return geodesic; }

    public void setGeodesic(boolean geodesic) { this.geodesic = geodesic; }

    public String getCentralBhId() { return centralBhId; }

    public void setCentralBhId(String centralBhId) { this.centralBhId = centralBhId; }

    public double getE() { return E; }

    public void setE(double e) { E = e; }

    public double getL() { return L; }

    public void setL(double l) { L = l; }

    public double getR() { return r; }

    public void setR(double r) { this.r = r; }

    public double getPhi() { return phi; }

    public void setPhi(double phi) { this.phi = phi; }

    public double getPr() { return pr; }

    public void setPr(double pr) { this.pr = pr; }

    public void clearTrail() { trail.clear(); }

    public void pushTrailPoint() {
        if (maxTrailPoints <= 0) return;
        trail.addLast(position.copy());
        while (trail.size() > maxTrailPoints) trail.removeFirst();
    }
}
