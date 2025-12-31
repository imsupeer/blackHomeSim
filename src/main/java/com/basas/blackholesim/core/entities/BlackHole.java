package com.basas.blackholesim.core.entities;

import com.basas.blackholesim.core.math.Vec2;

public class BlackHole {
    private final String id;
    private final Vec2 position;
    private final Vec2 velocity;
    private double mass;
    private double spin;

    public BlackHole(String id, Vec2 position, double mass) {
        this(id, position, new Vec2(0, 0), mass, 0.0);
    }

    public BlackHole(String id, Vec2 position, Vec2 velocity, double mass, double spin) {
        this.id = id;
        this.position = position;
        this.velocity = velocity;
        this.mass = mass;
        this.spin = spin;
    }

    public String getId() { return id; }

    public Vec2 getPosition() { return position; }

    public Vec2 getVelocity() { return velocity; }

    public double getMass() { return mass; }

    public void setMass(double mass) { this.mass = mass; }

    public double getSpin() { return spin; }

    public void setSpin(double spin) { this.spin = spin; }
}
