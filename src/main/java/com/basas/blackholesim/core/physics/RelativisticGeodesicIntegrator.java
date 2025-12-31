package com.basas.blackholesim.core.physics;

import com.basas.blackholesim.core.SimulationEngine;
import com.basas.blackholesim.core.entities.BlackHole;
import com.basas.blackholesim.core.entities.Particle;
import com.basas.blackholesim.core.math.Vec2;

import java.util.Iterator;

public class RelativisticGeodesicIntegrator implements Integrator {

    private int substeps = 2;

    public void setSubsteps(int substeps) {
        this.substeps = Math.max(1, substeps);
    }

    @Override
    public void step(SimulationEngine engine, double dt, boolean pushTrail) {
        if (dt <= 0) return;
        if (engine.getParticles().isEmpty()) return;

        double h = dt / substeps;
        for (int s = 0; s < substeps; s++) {
            Iterator<Particle> it = engine.getParticles().iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                if (!p.isAlive()) { it.remove(); continue; }

                boolean dead = stepParticle(engine, p, h, pushTrail);
                if (dead) {
                    p.kill();
                    it.remove();
                }
            }
        }
    }

    private boolean stepParticle(SimulationEngine engine, Particle p, double dt, boolean pushTrail) {
        BlackHole bh = engine.getNearestBlackHole(p.getPosition());
        if (bh == null) return false;

        PhysicsParams params = engine.getParams();
        double rg = Relativity.massLength(bh, params);
        if (rg <= 1e-9) return false;

        if (!p.isGeodesic()) {
            initializeConstants(p, bh, params);
        }

        double r0 = p.getR();
        double phi0 = p.getPhi();
        double pr0 = p.getPr();

        if (r0 <= 0.0) {
            Vec2 rel = p.getPosition().copy().sub(bh.getPosition());
            r0 = Math.max(1e-6, rel.length());
            phi0 = Math.atan2(rel.y, rel.x);
            p.setR(r0);
            p.setPhi(phi0);
        }

        double lambdaStep = dt * (params.c / rg);

        rk4Integrate(p, bh, params, lambdaStep);

        double r = p.getR();
        double phi = p.getPhi();

        double horizon = Relativity.eventHorizonRadius(bh, params);
        if (r <= horizon * 1.0005) {
            return true;
        }

        double x = bh.getPosition().x + r * Math.cos(phi);
        double y = bh.getPosition().y + r * Math.sin(phi);
        p.getPosition().x = x;
        p.getPosition().y = y;

        double dr = p.getPr();
        double dphi = dPhi(p, bh, params);
        double vx = dr * Math.cos(phi) - r * Math.sin(phi) * dphi;
        double vy = dr * Math.sin(phi) + r * Math.cos(phi) * dphi;
        p.getVelocity().x = vx;
        p.getVelocity().y = vy;

        if (Math.abs(p.getPosition().x) > params.killDistance || Math.abs(p.getPosition().y) > params.killDistance) return true;
        if (pushTrail) p.pushTrailPoint();
        return false;
    }

    private void initializeConstants(Particle p, BlackHole bh, PhysicsParams params) {
        Vec2 rel = p.getPosition().copy().sub(bh.getPosition());
        double r = Math.max(1e-6, rel.length());
        double phi = Math.atan2(rel.y, rel.x);

        double vx = p.getVelocity().x;
        double vy = p.getVelocity().y;

        double rg = Relativity.massLength(bh, params);
        double c = Math.max(1e-9, params.c);

        double v = Math.sqrt(vx * vx + vy * vy);
        double vbar = Math.min(0.999, v / c);

        double vr = (vx * Math.cos(phi) + vy * Math.sin(phi));
        double vrbar = vr / c;

        double omega = (rel.x * vy - rel.y * vx) / (r * r + 1e-9);
        double L = (r * r) * (omega / c);

        double mu = p.isPhoton() ? 0.0 : 1.0;
        double gamma = 1.0 / Math.sqrt(Math.max(1e-9, 1.0 - vbar * vbar));
        double E = mu == 0.0 ? 1.0 : gamma;

        p.setR(r);
        p.setPhi(phi);
        p.setPr(vrbar * rg); 
        p.setL(L * rg);
        p.setE(E);
        p.setCentralBhId(bh.getId());
        p.setGeodesic(true);
    }

    private void rk4Integrate(Particle p, BlackHole bh, PhysicsParams params, double h) {
        double r = p.getR();
        double phi = p.getPhi();
        double pr = p.getPr();

        double[] k1 = deriv(p, bh, params, r, phi, pr);
        double[] k2 = deriv(p, bh, params, r + 0.5 * h * k1[0], phi + 0.5 * h * k1[1], pr + 0.5 * h * k1[2]);
        double[] k3 = deriv(p, bh, params, r + 0.5 * h * k2[0], phi + 0.5 * h * k2[1], pr + 0.5 * h * k2[2]);
        double[] k4 = deriv(p, bh, params, r + h * k3[0], phi + h * k3[1], pr + h * k3[2]);

        double nr = r + (h / 6.0) * (k1[0] + 2.0 * k2[0] + 2.0 * k3[0] + k4[0]);
        double nphi = phi + (h / 6.0) * (k1[1] + 2.0 * k2[1] + 2.0 * k3[1] + k4[1]);
        double npr = pr + (h / 6.0) * (k1[2] + 2.0 * k2[2] + 2.0 * k3[2] + k4[2]);

        if (!Double.isFinite(nr) || !Double.isFinite(nphi) || !Double.isFinite(npr)) return;

        p.setR(Math.max(1e-6, nr));
        p.setPhi(wrapAngle(nphi));
        p.setPr(npr);
    }

    private double[] deriv(Particle p, BlackHole bh, PhysicsParams params, double r, double phi, double pr) {
        double dr = pr;
        double dphi = dPhiFor(p, bh, params, r);
        double dpr = dPr(p, bh, params, r);
        return new double[]{dr, dphi, dpr};
    }

    private double dPhi(Particle p, BlackHole bh, PhysicsParams params) {
        return dPhiFor(p, bh, params, p.getR());
    }

    private double dPhiFor(Particle p, BlackHole bh, PhysicsParams params, double r) {
        double L = p.getL();
        if (params.relativityMode == RelativityMode.KERR) {
            double a = Relativity.massLength(bh, params) * Math.max(0.0, Math.min(0.999, bh.getSpin()));
            double M = Relativity.massLength(bh, params);
            double Delta = r * r - 2.0 * M * r + a * a;
            double E = p.getE();
            double P = E * (r * r + a * a) - a * L;
            return - ( (a * P) / Math.max(1e-9, Delta) - a * E + L ) / Math.max(1e-9, r * r);
        }
        return L / Math.max(1e-9, r * r);
    }

    private double dPr(Particle p, BlackHole bh, PhysicsParams params, double r) {
        if (params.relativityMode == RelativityMode.KERR) {
            return 0.5 * dFdrKerr(p, bh, params, r);
        }
        double M = Relativity.massLength(bh, params);
        double L = p.getL();
        double r2 = r * r;
        double r3 = r2 * r;
        double r4 = r2 * r2;
        double termL = L * L * (1.0 / Math.max(1e-12, r3) - 3.0 * M / Math.max(1e-12, r4));
        if (p.isPhoton()) return termL;
        return termL - M / Math.max(1e-12, r2);
    }

    private double dFdrKerr(Particle p, BlackHole bh, PhysicsParams params, double r) {
        double eps = Math.max(1e-5, 1e-4 * r);
        double f1 = Fkerr(p, bh, params, r + eps);
        double f0 = Fkerr(p, bh, params, r - eps);
        return (f1 - f0) / (2.0 * eps);
    }

    private double Fkerr(Particle p, BlackHole bh, PhysicsParams params, double r) {
        double M = Relativity.massLength(bh, params);
        double a = M * Math.max(0.0, Math.min(0.999, bh.getSpin()));
        double E = p.getE();
        double L = p.getL();
        double mu = p.isPhoton() ? 0.0 : 1.0;

        double Delta = r * r - 2.0 * M * r + a * a;
        double P = E * (r * r + a * a) - a * L;
        double R = P * P - Delta * ((L - a * E) * (L - a * E) + mu * mu * r * r);
        return R / Math.max(1e-12, r * r * r * r);
    }

    private double wrapAngle(double a) {
        double twoPi = Math.PI * 2.0;
        a = a % twoPi;
        if (a < -Math.PI) a += twoPi;
        if (a > Math.PI) a -= twoPi;
        return a;
    }

    @Override
    public String name() {
        return "Relativistic Geodesics";
    }
}
