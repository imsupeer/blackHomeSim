package com.basas.blackholesim.core.physics;

import com.basas.blackholesim.core.SimulationEngine;
import com.basas.blackholesim.core.entities.Particle;
import com.basas.blackholesim.core.math.Vec2;

import java.util.Iterator;


public class VelocityVerletIntegrator implements Integrator {

    @Override
    public String name() {
        return "Velocity Verlet";
    }

    @Override
    public void step(SimulationEngine engine, double dt, boolean pushTrails) {
        if (dt <= 0) return;

        GravityModel model = engine.getGravityModel();
        PhysicsParams params = engine.getParams();

        Vec2 a0 = new Vec2();
        Vec2 a1 = new Vec2();

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

            model.acceleration(engine.getBlackHoles(), p.getPosition(), p.getVelocity(), params, a0);

            double x = p.getPosition().x;
            double y = p.getPosition().y;

            double vx = p.getVelocity().x;
            double vy = p.getVelocity().y;

            double halfDt2 = 0.5 * dt * dt;

            double nx = x + vx * dt + a0.x * halfDt2;
            double ny = y + vy * dt + a0.y * halfDt2;

            
            p.getPosition().x = nx;
            p.getPosition().y = ny;

            if (engine.isInsideAnyEventHorizon(p.getPosition())) {
                p.kill();
                it.remove();
                continue;
            }

            model.acceleration(engine.getBlackHoles(), p.getPosition(), p.getVelocity(), params, a1);

            double nvx = vx + 0.5 * (a0.x + a1.x) * dt;
            double nvy = vy + 0.5 * (a0.y + a1.y) * dt;

            p.getVelocity().x = nvx;
            p.getVelocity().y = nvy;

            if (Math.abs(nx) > params.killDistance || Math.abs(ny) > params.killDistance) {
                p.kill();
                it.remove();
            }
        }
    }
}
