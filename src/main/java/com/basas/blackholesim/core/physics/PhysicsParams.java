package com.basas.blackholesim.core.physics;


public class PhysicsParams {

    public double G = 520.0;

    public double c = 1200.0;

    public double softening = 18.0;

    public double maxAcceleration = 18000.0;

    public double killDistance = 120_000.0;

    public RelativityMode relativityMode = RelativityMode.SCHWARZSCHILD;

    public boolean enableAccretionDisk = true;

    public boolean enableBHDynamics = true;

    public double gwLossStrength = 0.35;

    public PhysicsParams copy() {
        PhysicsParams p = new PhysicsParams();
        p.G = this.G;
        p.c = this.c;
        p.softening = this.softening;
        p.maxAcceleration = this.maxAcceleration;
        p.killDistance = this.killDistance;
        p.relativityMode = this.relativityMode;
        p.enableAccretionDisk = this.enableAccretionDisk;
        p.enableBHDynamics = this.enableBHDynamics;
        p.gwLossStrength = this.gwLossStrength;
        return p;
    }
}
