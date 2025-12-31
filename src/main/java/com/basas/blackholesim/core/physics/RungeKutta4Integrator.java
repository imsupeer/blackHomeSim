package com.basas.blackholesim.core.physics;

import com.basas.blackholesim.core.SimulationEngine;
import com.basas.blackholesim.core.entities.Particle;
import com.basas.blackholesim.core.math.Vec2;

import java.util.Iterator;


public class RungeKutta4Integrator implements Integrator {

    private int substeps = 2;

    public int getSubsteps() { return substeps; }

    public void setSubsteps(int substeps) {
        this.substeps = Math.max(1, Math.min(16, substeps));
    }

    @Override
    public String name() {
        return "Runge–Kutta 4";
    }

    @Override
    public void step(SimulationEngine engine, double dt, boolean pushTrails) {
        if (dt <= 0) return;

        double h = dt / substeps;

        GravityModel model = engine.getGravityModel();
        PhysicsParams params = engine.getParams();

        Vec2 a = new Vec2();

        Iterator<Particle> it = engine.getParticles().iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            if (!p.isAlive()) {
                it.remove();
                continue;
            }

            if (pushTrails) p.pushTrailPoint();

            if (engine.isInsideAnyEventHorizon(p.getPosition())) {
                p.kill();
                it.remove();
                continue;
            }

            for (int s = 0; s < substeps; s++) {
                if (engine.isInsideAnyEventHorizon(p.getPosition())) {
                    p.kill();
                    it.remove();
                    break;
                }

                rk4ParticleStep(model, engine, params, p, h, a);

                if (Math.abs(p.getPosition().x) > params.killDistance || Math.abs(p.getPosition().y) > params.killDistance) {
                    p.kill();
                    it.remove();
                    break;
                }
            }
        }
    }

    private void rk4ParticleStep(GravityModel model, SimulationEngine engine, PhysicsParams params, Particle p, double h, Vec2 a) {
        
        double x = p.getPosition().x;
        double y = p.getPosition().y;
        double vx = p.getVelocity().x;
        double vy = p.getVelocity().y;

        
        model.acceleration(engine.getBlackHoles(), new Vec2(x, y), new Vec2(vx, vy), params, a);
        double k1x = vx;
        double k1y = vy;
        double k1vx = a.x;
        double k1vy = a.y;

        
        double x2 = x + 0.5 * h * k1x;
        double y2 = y + 0.5 * h * k1y;
        double vx2 = vx + 0.5 * h * k1vx;
        double vy2 = vy + 0.5 * h * k1vy;

        model.acceleration(engine.getBlackHoles(), new Vec2(x2, y2), new Vec2(vx2, vy2), params, a);
        double k2x = vx2;
        double k2y = vy2;
        double k2vx = a.x;
        double k2vy = a.y;

        
        double x3 = x + 0.5 * h * k2x;
        double y3 = y + 0.5 * h * k2y;
        double vx3 = vx + 0.5 * h * k2vx;
        double vy3 = vy + 0.5 * h * k2vy;

        model.acceleration(engine.getBlackHoles(), new Vec2(x3, y3), new Vec2(vx3, vy3), params, a);
        double k3x = vx3;
        double k3y = vy3;
        double k3vx = a.x;
        double k3vy = a.y;

        
        double x4 = x + h * k3x;
        double y4 = y + h * k3y;
        double vx4 = vx + h * k3vx;
        double vy4 = vy + h * k3vy;

        model.acceleration(engine.getBlackHoles(), new Vec2(x4, y4), new Vec2(vx4, vy4), params, a);
        double k4x = vx4;
        double k4y = vy4;
        double k4vx = a.x;
        double k4vy = a.y;

        
        double nx = x + (h / 6.0) * (k1x + 2.0 * k2x + 2.0 * k3x + k4x);
        double ny = y + (h / 6.0) * (k1y + 2.0 * k2y + 2.0 * k3y + k4y);
        double nvx = vx + (h / 6.0) * (k1vx + 2.0 * k2vx + 2.0 * k3vx + k4vx);
        double nvy = vy + (h / 6.0) * (k1vy + 2.0 * k2vy + 2.0 * k3vy + k4vy);

        p.getPosition().x = nx;
        p.getPosition().y = ny;
        p.getVelocity().x = nvx;
        p.getVelocity().y = nvy;
    }
}
