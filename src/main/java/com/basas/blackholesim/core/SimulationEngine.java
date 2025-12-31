package com.basas.blackholesim.core;

import com.basas.blackholesim.core.entities.BlackHole;
import com.basas.blackholesim.core.entities.Particle;
import com.basas.blackholesim.core.math.Vec2;
import com.basas.blackholesim.core.physics.*;
import javafx.scene.paint.Color;

import java.util.*;


public class SimulationEngine {

    private final List<BlackHole> blackHoles = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private final Random random = new Random();

    private final PhysicsParams params = new PhysicsParams();

    private GravityModel gravityModel = new PaczynskiWiitaGravityModel();
    private Integrator integrator = new VelocityVerletIntegrator();

    
    private double initialTotalEnergy = Double.NaN;

    public SimulationEngine() { }

    public List<BlackHole> getBlackHoles() { return blackHoles; }

    

public List<Particle> getParticles() { return particles; }

    public PhysicsParams getParams() { return params; }

    public GravityModel getGravityModel() { return gravityModel; }

    public void setGravityModel(GravityModel gravityModel) {
        this.gravityModel = Objects.requireNonNull(gravityModel);
        resetEnergyBaseline();
    }

    public Integrator getIntegrator() { return integrator; }

    public void setIntegrator(Integrator integrator) {
        this.integrator = Objects.requireNonNull(integrator);
        resetEnergyBaseline();
    }

    public void resetEnergyBaseline() {
        this.initialTotalEnergy = Double.NaN;
    }

    public void clearParticles() {
        particles.clear();
        resetEnergyBaseline();
    }

    public void clearBlackHoles() {
        blackHoles.clear();
        resetEnergyBaseline();
    }

    public void addBlackHole(BlackHole bh) {
        blackHoles.add(bh);
        resetEnergyBaseline();
    }

    public boolean removeBlackHoleById(String id) {
        boolean removed = blackHoles.removeIf(bh -> bh.getId().equals(id));
        if (removed) resetEnergyBaseline();
        return removed;
    }

    public void addParticle(Particle p) {
        particles.add(p);
        resetEnergyBaseline();
    }

    public String nextBlackHoleId() {
        return "BH-" + (blackHoles.size() + 1);
    }

    public BlackHole getNearestBlackHole(Vec2 worldPos) {
        if (blackHoles.isEmpty()) return null;
        BlackHole nearest = blackHoles.get(0);
        double best = distSq(nearest.getPosition(), worldPos);
        for (int i = 1; i < blackHoles.size(); i++) {
            BlackHole bh = blackHoles.get(i);
            double d = distSq(bh.getPosition(), worldPos);
            if (d < best) {
                best = d;
                nearest = bh;
            }
        }
        return nearest;
    }

    public double eventHorizonRadius(BlackHole bh) {
        if (params.relativityMode == RelativityMode.NEWTONIAN) {
            return gravityModel.eventHorizonRadius(bh, params);
        }
        return Relativity.eventHorizonRadius(bh, params);
    }

    public boolean isInsideAnyEventHorizon(Vec2 pos) {
        for (BlackHole bh : blackHoles) {
            double dx = bh.getPosition().x - pos.x;
            double dy = bh.getPosition().y - pos.y;
            double r = Math.sqrt(dx * dx + dy * dy);
            if (r < eventHorizonRadius(bh)) return true;
        }
        return false;
    }

    
    public void addRandomBurst(Vec2 center, int count, double spawnRadius) {
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double r = spawnRadius * (0.25 + 0.75 * random.nextDouble());
            Vec2 pos = new Vec2(center.x + Math.cos(a) * r, center.y + Math.sin(a) * r);

            Vec2 vel = makeTangentialOrbitVelocity(pos, 0.65 + random.nextDouble() * 0.8);
            Particle p = new Particle(pos, vel);

            double hue = 200 + random.nextDouble() * 60;
            p.setColor(Color.hsb(hue, 0.35, 1.0, 0.9));
            p.setRadius(1.8 + random.nextDouble() * 2.2);

            particles.add(p);
        }
        resetEnergyBaseline();
    }

    
    public Vec2 makeTangentialOrbitVelocity(Vec2 worldPos, double factor) {
        BlackHole nearest = getNearestBlackHole(worldPos);
        if (nearest == null) return new Vec2(0, 0);

        double dx = nearest.getPosition().x - worldPos.x;
        double dy = nearest.getPosition().y - worldPos.y;

        double r = Math.sqrt(Math.max(1e-6, dx * dx + dy * dy));

        
        Vec2 acc = new Vec2();
        gravityModel.acceleration(List.of(nearest), worldPos, new Vec2(0,0), params, acc);
        double aMag = Math.sqrt(acc.x*acc.x + acc.y*acc.y);

        
        double v = Math.sqrt(Math.max(0.0, aMag * r));

        
        double tx = -dy / r;
        double ty = dx / r;

        return new Vec2(tx * v * factor, ty * v * factor);
    }

    
    public void update(double dt, boolean pushTrail) {
        if (dt <= 0 || blackHoles.isEmpty()) return;
        if (params.enableBHDynamics && blackHoles.size() > 1) {
            stepBlackHoles(dt);
            mergeBlackHolesIfNeeded();
        }
        integrator.step(this, dt, pushTrail);
    }

    public double gravitationalFieldAt(Vec2 worldPos) {
        if (blackHoles.isEmpty()) return 0.0;
        Vec2 a = new Vec2();
        gravityModel.acceleration(blackHoles, worldPos, new Vec2(0,0), params, a);
        return a.length();
    }

    
    public double escapeVelocityAt(Vec2 worldPos) {
        double phi = gravityModel.potential(blackHoles, worldPos, params);
        return Math.sqrt(Math.max(0.0, 2.0 * Math.abs(phi)));
    }

    
    
    private void stepBlackHoles(double dt) {
        int n = blackHoles.size();
        if (n < 2) return;

        Vec2[] acc = new Vec2[n];
        for (int i = 0; i < n; i++) acc[i] = new Vec2(0, 0);

        for (int i = 0; i < n; i++) {
            BlackHole a = blackHoles.get(i);
            for (int j = i + 1; j < n; j++) {
                BlackHole b = blackHoles.get(j);

                double dx = b.getPosition().x - a.getPosition().x;
                double dy = b.getPosition().y - a.getPosition().y;
                double r2 = dx * dx + dy * dy + params.softening * params.softening;
                double r = Math.sqrt(r2);
                double invR3 = 1.0 / (r2 * r);

                double f = params.G * b.getMass() * invR3;
                acc[i].x += dx * f;
                acc[i].y += dy * f;

                f = params.G * a.getMass() * invR3;
                acc[j].x -= dx * f;
                acc[j].y -= dy * f;
            }
        }

        for (int i = 0; i < n; i++) {
            BlackHole bh = blackHoles.get(i);
            bh.getVelocity().x += acc[i].x * dt;
            bh.getVelocity().y += acc[i].y * dt;
        }

        if (params.gwLossStrength > 0 && n == 2) {
            applyGWRadiationReaction(dt);
        }

        for (BlackHole bh : blackHoles) {
            bh.getPosition().x += bh.getVelocity().x * dt;
            bh.getPosition().y += bh.getVelocity().y * dt;
        }
    }

    private void applyGWRadiationReaction(double dt) {
        BlackHole b1 = blackHoles.get(0);
        BlackHole b2 = blackHoles.get(1);

        Vec2 r12 = b2.getPosition().copy().sub(b1.getPosition());
        double r = Math.max(1e-6, r12.length());

        double m1 = b1.getMass();
        double m2 = b2.getMass();
        double mu = (m1 * m2) / Math.max(1e-9, (m1 + m2));

        double daDt = -(64.0 / 5.0) * params.gwLossStrength
                * Math.pow(params.G, 3.0) * m1 * m2 * (m1 + m2)
                / (Math.pow(params.c, 5.0) * Math.pow(r, 3.0) + 1e-9);

        double dr = daDt * dt;

        Vec2 n = r12.copy().mul(1.0 / r);
        double w1 = m2 / (m1 + m2);
        double w2 = m1 / (m1 + m2);

        b1.getPosition().x += n.x * (-dr) * w1 * 0.5;
        b1.getPosition().y += n.y * (-dr) * w1 * 0.5;
        b2.getPosition().x -= n.x * (-dr) * w2 * 0.5;
        b2.getPosition().y -= n.y * (-dr) * w2 * 0.5;

        Vec2 vrel = b2.getVelocity().copy().sub(b1.getVelocity());
        double drag = Math.min(0.15, Math.abs(dr) / Math.max(1e-6, r));
        b1.getVelocity().x += vrel.x * drag * w1 * 0.5;
        b1.getVelocity().y += vrel.y * drag * w1 * 0.5;
        b2.getVelocity().x -= vrel.x * drag * w2 * 0.5;
        b2.getVelocity().y -= vrel.y * drag * w2 * 0.5;
    }

    private void mergeBlackHolesIfNeeded() {
        if (blackHoles.size() < 2) return;
        if (blackHoles.size() != 2) return;

        BlackHole a = blackHoles.get(0);
        BlackHole b = blackHoles.get(1);

        double dx = b.getPosition().x - a.getPosition().x;
        double dy = b.getPosition().y - a.getPosition().y;
        double d = Math.sqrt(dx * dx + dy * dy);

        double ra = eventHorizonRadius(a);
        double rb = eventHorizonRadius(b);

        if (d > (ra + rb) * 1.15) return;

        double m1 = a.getMass();
        double m2 = b.getMass();
        double frac = Relativity.gwMassLossFraction(m1, m2);
        double dm = frac * (m1 + m2);
        double mf = Math.max(1e-9, (m1 + m2) - dm);

        double px = m1 * a.getVelocity().x + m2 * b.getVelocity().x;
        double py = m1 * a.getVelocity().y + m2 * b.getVelocity().y;

        Vec2 pos = new Vec2(
                (m1 * a.getPosition().x + m2 * b.getPosition().x) / (m1 + m2),
                (m1 * a.getPosition().y + m2 * b.getPosition().y) / (m1 + m2)
        );

        Vec2 vel = new Vec2(px / mf, py / mf);

        double spin = Math.min(0.999, (m1 * a.getSpin() + m2 * b.getSpin()) / (m1 + m2) + 0.35 * Relativity.symmetricMassRatio(m1, m2));

        blackHoles.clear();
        blackHoles.add(new BlackHole("BH-MERGED", pos, vel, mf, spin));
        resetEnergyBaseline();
    }

public double totalEnergy() {
        if (blackHoles.isEmpty()) return 0.0;

        double total = 0.0;
        for (Particle p : particles) {
            if (!p.isAlive()) continue;

            double v2 = p.getVelocity().x * p.getVelocity().x + p.getVelocity().y * p.getVelocity().y;
            double kin = 0.5 * v2;
            double pot = gravityModel.potential(blackHoles, p.getPosition(), params);

            total += (kin + pot);
        }
        return total;
    }

    public double energyDriftRatio() {
        double e = totalEnergy();
        if (Double.isNaN(initialTotalEnergy)) {
            initialTotalEnergy = e;
            return 0.0;
        }
        if (Math.abs(initialTotalEnergy) < 1e-9) return 0.0;
        return (e - initialTotalEnergy) / initialTotalEnergy;
    }

    private static double distSq(Vec2 a, Vec2 b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return dx*dx + dy*dy;
    }
}
