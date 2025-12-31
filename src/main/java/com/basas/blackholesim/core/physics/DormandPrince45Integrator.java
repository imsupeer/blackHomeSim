package com.basas.blackholesim.core.physics;

import com.basas.blackholesim.core.SimulationEngine;
import com.basas.blackholesim.core.entities.Particle;
import com.basas.blackholesim.core.math.Vec2;

import java.util.Iterator;

public class DormandPrince45Integrator implements Integrator {

    private double tolerance = 1e-3;
    private int maxSubstepsPerFrame = 32;

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = Math.max(1e-6, Math.min(1e-1, tolerance));
    }

    public int getMaxSubstepsPerFrame() {
        return maxSubstepsPerFrame;
    }

    public void setMaxSubstepsPerFrame(int maxSubstepsPerFrame) {
        this.maxSubstepsPerFrame = Math.max(8, Math.min(128, maxSubstepsPerFrame));
    }

    @Override
    public String name() {
        return "Dormand–Prince RK45";
    }

    @Override
    public void step(SimulationEngine engine, double dt, boolean pushTrails) {
        if (dt <= 0)
            return;

        GravityModel model = engine.getGravityModel();
        PhysicsParams params = engine.getParams();

        Iterator<Particle> it = engine.getParticles().iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            if (!p.isAlive()) {
                it.remove();
                continue;
            }

            if (pushTrails)
                p.pushTrailPoint();

            if (engine.isInsideAnyEventHorizon(p.getPosition())) {
                p.kill();
                it.remove();
                continue;
            }

            boolean alive = integrateAdaptive(model, engine, params, p, dt);
            if (!alive) {
                it.remove();
                continue;
            }

            if (Math.abs(p.getPosition().x) > params.killDistance
                    || Math.abs(p.getPosition().y) > params.killDistance) {
                p.kill();
                it.remove();
            }
        }
    }

    private boolean integrateAdaptive(GravityModel model, SimulationEngine engine, PhysicsParams params, Particle p,
            double dtTotal) {
        double remaining = dtTotal;
        double h = dtTotal;

        int sub = 0;

        while (remaining > 1e-12 && sub < maxSubstepsPerFrame) {
            h = Math.min(h, remaining);

            StepResult r = dopriAttempt(model, engine, params, p, h);

            if (!r.accepted) {
                h *= 0.5;
                sub++;
                continue;
            }

            p.getPosition().x = r.nx;
            p.getPosition().y = r.ny;
            p.getVelocity().x = r.nvx;
            p.getVelocity().y = r.nvy;

            if (engine.isInsideAnyEventHorizon(p.getPosition())) {
                p.kill();
                return false;
            }

            remaining -= h;

            if (r.error < tolerance * 0.125)
                h *= 1.8;
            else if (r.error > tolerance * 0.8)
                h *= 0.75;

            sub++;
        }

        return p.isAlive();
    }

    private StepResult dopriAttempt(GravityModel model, SimulationEngine engine, PhysicsParams params, Particle p,
            double h) {

        double x = p.getPosition().x;
        double y = p.getPosition().y;
        double vx = p.getVelocity().x;
        double vy = p.getVelocity().y;

        Vec2 a = new Vec2();

        model.acceleration(engine.getBlackHoles(), new Vec2(x, y), new Vec2(vx, vy), params, a);
        double k1x = vx;
        double k1y = vy;
        double k1vx = a.x;
        double k1vy = a.y;

        double x2 = x + h * (1.0 / 5.0) * k1x;
        double y2 = y + h * (1.0 / 5.0) * k1y;
        double vx2 = vx + h * (1.0 / 5.0) * k1vx;
        double vy2 = vy + h * (1.0 / 5.0) * k1vy;

        model.acceleration(engine.getBlackHoles(), new Vec2(x2, y2), new Vec2(vx2, vy2), params, a);
        double k2x = vx2;
        double k2y = vy2;
        double k2vx = a.x;
        double k2vy = a.y;

        double x3 = x + h * (3.0 / 40.0 * k1x + 9.0 / 40.0 * k2x);
        double y3 = y + h * (3.0 / 40.0 * k1y + 9.0 / 40.0 * k2y);
        double vx3 = vx + h * (3.0 / 40.0 * k1vx + 9.0 / 40.0 * k2vx);
        double vy3 = vy + h * (3.0 / 40.0 * k1vy + 9.0 / 40.0 * k2vy);

        model.acceleration(engine.getBlackHoles(), new Vec2(x3, y3), new Vec2(vx3, vy3), params, a);
        double k3x = vx3;
        double k3y = vy3;
        double k3vx = a.x;
        double k3vy = a.y;

        double x4 = x + h * (44.0 / 45.0 * k1x - 56.0 / 15.0 * k2x + 32.0 / 9.0 * k3x);
        double y4 = y + h * (44.0 / 45.0 * k1y - 56.0 / 15.0 * k2y + 32.0 / 9.0 * k3y);
        double vx4 = vx + h * (44.0 / 45.0 * k1vx - 56.0 / 15.0 * k2vx + 32.0 / 9.0 * k3vx);
        double vy4 = vy + h * (44.0 / 45.0 * k1vy - 56.0 / 15.0 * k2vy + 32.0 / 9.0 * k3vy);

        model.acceleration(engine.getBlackHoles(), new Vec2(x4, y4), new Vec2(vx4, vy4), params, a);
        double k4x = vx4;
        double k4y = vy4;
        double k4vx = a.x;
        double k4vy = a.y;

        double x5 = x
                + h * (19372.0 / 6561.0 * k1x - 25360.0 / 2187.0 * k2x + 64448.0 / 6561.0 * k3x - 212.0 / 729.0 * k4x);
        double y5 = y
                + h * (19372.0 / 6561.0 * k1y - 25360.0 / 2187.0 * k2y + 64448.0 / 6561.0 * k3y - 212.0 / 729.0 * k4y);
        double vx5 = vx + h
                * (19372.0 / 6561.0 * k1vx - 25360.0 / 2187.0 * k2vx + 64448.0 / 6561.0 * k3vx - 212.0 / 729.0 * k4vx);
        double vy5 = vy + h
                * (19372.0 / 6561.0 * k1vy - 25360.0 / 2187.0 * k2vy + 64448.0 / 6561.0 * k3vy - 212.0 / 729.0 * k4vy);

        model.acceleration(engine.getBlackHoles(), new Vec2(x5, y5), new Vec2(vx5, vy5), params, a);
        double k5x = vx5;
        double k5y = vy5;
        double k5vx = a.x;
        double k5vy = a.y;

        double x6 = x + h * (9017.0 / 3168.0 * k1x - 355.0 / 33.0 * k2x + 46732.0 / 5247.0 * k3x + 49.0 / 176.0 * k4x
                - 5103.0 / 18656.0 * k5x);
        double y6 = y + h * (9017.0 / 3168.0 * k1y - 355.0 / 33.0 * k2y + 46732.0 / 5247.0 * k3y + 49.0 / 176.0 * k4y
                - 5103.0 / 18656.0 * k5y);
        double vx6 = vx + h * (9017.0 / 3168.0 * k1vx - 355.0 / 33.0 * k2vx + 46732.0 / 5247.0 * k3vx
                + 49.0 / 176.0 * k4vx - 5103.0 / 18656.0 * k5vx);
        double vy6 = vy + h * (9017.0 / 3168.0 * k1vy - 355.0 / 33.0 * k2vy + 46732.0 / 5247.0 * k3vy
                + 49.0 / 176.0 * k4vy - 5103.0 / 18656.0 * k5vy);

        model.acceleration(engine.getBlackHoles(), new Vec2(x6, y6), new Vec2(vx6, vy6), params, a);
        double k6x = vx6;
        double k6y = vy6;
        double k6vx = a.x;
        double k6vy = a.y;

        double x7 = x + h * (35.0 / 384.0 * k1x + 500.0 / 1113.0 * k3x + 125.0 / 192.0 * k4x - 2187.0 / 6784.0 * k5x
                + 11.0 / 84.0 * k6x);
        double y7 = y + h * (35.0 / 384.0 * k1y + 500.0 / 1113.0 * k3y + 125.0 / 192.0 * k4y - 2187.0 / 6784.0 * k5y
                + 11.0 / 84.0 * k6y);
        double vx7 = vx + h * (35.0 / 384.0 * k1vx + 500.0 / 1113.0 * k3vx + 125.0 / 192.0 * k4vx
                - 2187.0 / 6784.0 * k5vx + 11.0 / 84.0 * k6vx);
        double vy7 = vy + h * (35.0 / 384.0 * k1vy + 500.0 / 1113.0 * k3vy + 125.0 / 192.0 * k4vy
                - 2187.0 / 6784.0 * k5vy + 11.0 / 84.0 * k6vy);

        model.acceleration(engine.getBlackHoles(), new Vec2(x7, y7), new Vec2(vx7, vy7), params, a);
        double k7x = vx7;
        double k7y = vy7;
        double k7vx = a.x;
        double k7vy = a.y;

        double nx5 = x + h * (35.0 / 384.0 * k1x + 500.0 / 1113.0 * k3x + 125.0 / 192.0 * k4x - 2187.0 / 6784.0 * k5x
                + 11.0 / 84.0 * k6x);
        double ny5 = y + h * (35.0 / 384.0 * k1y + 500.0 / 1113.0 * k3y + 125.0 / 192.0 * k4y - 2187.0 / 6784.0 * k5y
                + 11.0 / 84.0 * k6y);
        double nvx5 = vx + h * (35.0 / 384.0 * k1vx + 500.0 / 1113.0 * k3vx + 125.0 / 192.0 * k4vx
                - 2187.0 / 6784.0 * k5vx + 11.0 / 84.0 * k6vx);
        double nvy5 = vy + h * (35.0 / 384.0 * k1vy + 500.0 / 1113.0 * k3vy + 125.0 / 192.0 * k4vy
                - 2187.0 / 6784.0 * k5vy + 11.0 / 84.0 * k6vy);

        double nx4 = x + h * (5179.0 / 57600.0 * k1x + 7571.0 / 16695.0 * k3x + 393.0 / 640.0 * k4x
                - 92097.0 / 339200.0 * k5x + 187.0 / 2100.0 * k6x + 1.0 / 40.0 * k7x);
        double ny4 = y + h * (5179.0 / 57600.0 * k1y + 7571.0 / 16695.0 * k3y + 393.0 / 640.0 * k4y
                - 92097.0 / 339200.0 * k5y + 187.0 / 2100.0 * k6y + 1.0 / 40.0 * k7y);
        double nvx4 = vx + h * (5179.0 / 57600.0 * k1vx + 7571.0 / 16695.0 * k3vx + 393.0 / 640.0 * k4vx
                - 92097.0 / 339200.0 * k5vx + 187.0 / 2100.0 * k6vx + 1.0 / 40.0 * k7vx);
        double nvy4 = vy + h * (5179.0 / 57600.0 * k1vy + 7571.0 / 16695.0 * k3vy + 393.0 / 640.0 * k4vy
                - 92097.0 / 339200.0 * k5vy + 187.0 / 2100.0 * k6vy + 1.0 / 40.0 * k7vy);

        double ex = Math.abs(nx5 - nx4);
        double ey = Math.abs(ny5 - ny4);
        double evx = Math.abs(nvx5 - nvx4);
        double evy = Math.abs(nvy5 - nvy4);

        double scale = 1.0 + Math.max(Math.abs(x), Math.abs(y)) + Math.max(Math.abs(vx), Math.abs(vy));
        double err = Math.max(Math.max(ex, ey), Math.max(evx, evy)) / scale;

        StepResult out = new StepResult();
        out.nx = nx5;
        out.ny = ny5;
        out.nvx = nvx5;
        out.nvy = nvy5;
        out.error = err;
        out.accepted = err <= tolerance;

        return out;
    }

    private static class StepResult {
        boolean accepted;
        double error;
        double nx, ny, nvx, nvy;
    }
}
