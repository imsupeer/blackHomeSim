package com.basas.blackholesim.render;

import com.basas.blackholesim.core.SimulationEngine;
import com.basas.blackholesim.core.entities.BlackHole;
import com.basas.blackholesim.core.entities.Particle;
import com.basas.blackholesim.core.math.Vec2;
import com.basas.blackholesim.core.physics.PhysicsParams;
import com.basas.blackholesim.core.physics.Relativity;
import com.basas.blackholesim.core.physics.RelativityMode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Deque;


public class CanvasRenderer {

    private final SimulationEngine engine;
    private final Camera camera;

    private boolean drawTrails = true;
    private boolean drawGridDistortion = true;
    private boolean drawVelocityVectors = false;

    
    private double gridStep = 46.0;
    private double gridStrength = 1800;

    
    private Vec2 mouseScreen = new Vec2(0, 0);
    private boolean mouseValid = false;

    public CanvasRenderer(SimulationEngine engine, Camera camera) {
        this.engine = engine;
        this.camera = camera;
    }

    public void setDrawTrails(boolean drawTrails) { this.drawTrails = drawTrails; }
    public void setDrawGridDistortion(boolean drawGridDistortion) { this.drawGridDistortion = drawGridDistortion; }
    public void setDrawVelocityVectors(boolean drawVelocityVectors) { this.drawVelocityVectors = drawVelocityVectors; }

    public void setMouse(double sx, double sy, boolean valid) {
        this.mouseScreen.set(sx, sy);
        this.mouseValid = valid;
    }

    public void render(GraphicsContext g, double w, double h) {
        camera.setViewport(w, h);

        
        g.setFill(Color.rgb(10, 12, 24));
        g.fillRect(0, 0, w, h);

        
        g.setFill(Color.rgb(255, 255, 255, 0.02));
        g.fillOval(-w * 0.25, -h * 0.1, w * 1.5, h * 1.2);

        if (drawGridDistortion) drawDistortedGrid(g, w, h);

        
        for (BlackHole bh : engine.getBlackHoles()) {
            if (engine.getParams().enableAccretionDisk && engine.getParams().relativityMode != RelativityMode.NEWTONIAN) {
                drawAccretionDisk(g, bh, engine.getParams());
            }
            drawBlackHole(g, bh);
        }

        
        if (drawTrails) {
            for (Particle p : engine.getParticles()) {
                drawTrail(g, p);
            }
        }

        
        for (Particle p : engine.getParticles()) {
            drawParticle(g, p);
            if (drawVelocityVectors) drawVelocity(g, p);
        }

        drawHud(g, w, h);
    }

    
    private void drawAccretionDisk(GraphicsContext g, BlackHole bh, PhysicsParams p) {
        Vec2 center = bh.getPosition();
        double rin = Math.max(Relativity.iscoRadius(bh, p), Relativity.eventHorizonRadius(bh, p) * 1.06);
        double rout = rin * 6.5;

        int rings = 26;
        int segs = 96;
        double pIndex = 3.0;
        double baseAlpha = 0.22;

        for (int ri = 0; ri < rings; ri++) {
            double t = (ri + 0.5) / rings;
            double r = rin + t * (rout - rin);
            double emiss = Math.pow(r / rin, -pIndex);
            double alpha = Math.min(0.65, baseAlpha * emiss);

            for (int si = 0; si < segs; si++) {
                double a0 = (si / (double) segs) * Math.PI * 2.0;
                double a1 = ((si + 1) / (double) segs) * Math.PI * 2.0;

                double gFactor = redshiftFactor(bh, p, r, a0);
                Color c = diskColor(gFactor, alpha);

                Vec2 p0 = camera.worldToScreen(new Vec2(center.x + r * Math.cos(a0), center.y + r * Math.sin(a0)));
                Vec2 p1 = camera.worldToScreen(new Vec2(center.x + r * Math.cos(a1), center.y + r * Math.sin(a1)));

                g.setStroke(c);
                g.setLineWidth(Math.max(1.0, camera.worldToScreenLength(0.9)));
                g.strokeLine(p0.x, p0.y, p1.x, p1.y);
            }
        }
    }

    private double redshiftFactor(BlackHole bh, PhysicsParams p, double r, double phi) {
        double grav = Relativity.gravitationalRedshiftFactor(bh, p, r);

        double v = Relativity.keplerianSpeed(bh, p, r);
        double gamma = 1.0 / Math.sqrt(Math.max(1e-9, 1.0 - v * v));

        double vx = -Math.sin(phi);
        double vy = Math.cos(phi);

        double cos = vx; 
        double doppler = 1.0 / (gamma * (1.0 - v * cos));

        double gFactor = grav * doppler;
        return Math.max(0.05, Math.min(3.0, gFactor));
    }

    private Color diskColor(double g, double alpha) {
        double k = (g - 1.0);
        double blue = clamp01(0.20 + Math.max(0.0, k) * 0.65);
        double red = clamp01(0.85 + Math.max(0.0, -k) * 0.65);
        double green = clamp01(0.50 + Math.max(0.0, -k) * 0.15);

        return Color.rgb((int)(255 * red), (int)(255 * green), (int)(255 * blue), clamp01(alpha));
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

private void drawBlackHole(GraphicsContext g, BlackHole bh) {
        Vec2 c = camera.worldToScreen(bh.getPosition());
        double rWorld = engine.eventHorizonRadius(bh);
        double r = Math.max(2.0, camera.worldToScreenScalar(rWorld));

        
        g.setFill(Color.rgb(120, 170, 255, 0.10));
        g.fillOval(c.x - r * 2.4, c.y - r * 2.4, r * 4.8, r * 4.8);

        
        g.setStroke(Color.rgb(170, 210, 255, 0.75));
        g.setLineWidth(Math.max(1.0, 1.5 * camera.getZoom()));
        g.strokeOval(c.x - r, c.y - r, r * 2, r * 2);

        
        g.setFill(Color.rgb(0, 0, 0, 0.96));
        g.fillOval(c.x - r * 0.86, c.y - r * 0.86, r * 1.72, r * 1.72);

        
        g.setFont(Font.font(12));
        g.setTextAlign(TextAlignment.CENTER);

        g.setFill(Color.rgb(232, 234, 246, 0.70));
        g.fillText("Event Horizon (r≈" + String.format("%.2f", rWorld) + ")", c.x, c.y - r - 10);

        g.setFill(Color.rgb(232, 234, 246, 0.85));
        g.fillText(bh.getId(), c.x, c.y + r + 16);
    }

    private void drawParticle(GraphicsContext g, Particle p) {
        Vec2 s = camera.worldToScreen(p.getPosition());
        double r = Math.max(1.0, camera.worldToScreenScalar(p.getRadius()));

        g.setFill(p.getColor());
        g.fillOval(s.x - r, s.y - r, r * 2, r * 2);
    }

    private void drawVelocity(GraphicsContext g, Particle p) {
        Vec2 s = camera.worldToScreen(p.getPosition());

        
        double scale = 0.08 * camera.getZoom();
        double vx = p.getVelocity().x * scale;
        double vy = p.getVelocity().y * scale;

        g.setStroke(Color.rgb(255, 255, 255, 0.35));
        g.setLineWidth(Math.max(1.0, 1.1 * camera.getZoom()));
        g.strokeLine(s.x, s.y, s.x + vx, s.y + vy);
    }

    private void drawTrail(GraphicsContext g, Particle p) {
        Deque<Vec2> t = p.getTrail();
        if (t.isEmpty()) return;

        Vec2 prev = null;
        int i = 0;
        int n = t.size();

        for (Vec2 wp : t) {
            Vec2 sp = camera.worldToScreen(wp);
            if (prev != null) {
                double alpha = Math.max(0.05, (double) i / (double) n);
                g.setStroke(Color.rgb(200, 210, 255, 0.25 * alpha));
                g.setLineWidth(Math.max(1.0, 1.0 * camera.getZoom()));
                g.strokeLine(prev.x, prev.y, sp.x, sp.y);
            }
            prev = sp;
            i++;
        }
    }

    private void drawHud(GraphicsContext g, double w, double h) {
        g.setFont(Font.font(12));
        g.setTextAlign(TextAlignment.LEFT);

        int particles = engine.getParticles().size();
        int holes = engine.getBlackHoles().size();

        g.setFill(Color.rgb(232, 234, 246, 0.85));
        g.fillText("Particles: " + particles + "   Black holes: " + holes, 14, 18);

        g.setFill(Color.rgb(232, 234, 246, 0.70));
        g.fillText("Model: " + engine.getGravityModel().name() + "   Integrator: " + engine.getIntegrator().name(), 14, 36);

        double drift = engine.energyDriftRatio() * 100.0;
        g.setFill(Color.rgb(232, 234, 246, 0.60));
        g.fillText(String.format("Energy drift: %+,.4f%%   (Click: spawn • Shift+Drag BH • RMB/MMB pan • Wheel zoom)", drift), 14, 54);

        if (mouseValid) {
            Vec2 world = camera.screenToWorld(mouseScreen.x, mouseScreen.y);
            double gMag = engine.gravitationalFieldAt(world);
            double vEsc = engine.escapeVelocityAt(world);

            g.setFill(Color.rgb(232, 234, 246, 0.75));
            g.fillText(String.format("At cursor: |g|=%.3f   v_esc≈%.2f", gMag, vEsc), 14, 72);
        }
    }

    private void drawDistortedGrid(GraphicsContext g, double w, double h) {
        g.setLineWidth(1.0);
        g.setStroke(Color.rgb(120, 140, 190, 0.15));

        double step = gridStep;

        for (double y = 0; y <= h; y += step) {
            Vec2 prev = null;
            for (double x = 0; x <= w; x += step / 2.0) {
                Vec2 p = distortScreenPointToScreen(x, y);
                if (prev != null) g.strokeLine(prev.x, prev.y, p.x, p.y);
                prev = p;
            }
        }

        for (double x = 0; x <= w; x += step) {
            Vec2 prev = null;
            for (double y = 0; y <= h; y += step / 2.0) {
                Vec2 p = distortScreenPointToScreen(x, y);
                if (prev != null) g.strokeLine(prev.x, prev.y, p.x, p.y);
                prev = p;
            }
        }
    }

    private Vec2 distortScreenPointToScreen(double sx, double sy) {
        Vec2 world = camera.screenToWorld(sx, sy);
        Vec2 deflection = new Vec2(0, 0);

        for (BlackHole bh : engine.getBlackHoles()) {
            Vec2 dir = Vec2.sub(bh.getPosition(), world);
            double distSq = dir.lengthSq() + 50.0;
            double strength = (gridStrength * bh.getMass()) / distSq;

            dir.normalize().scale(strength);
            deflection.add(dir);
        }

        Vec2 warpedWorld = Vec2.add(world, deflection);
        return camera.worldToScreen(warpedWorld);
    }
}
