package com.basas.blackholesim.core.physics;

import com.basas.blackholesim.core.entities.BlackHole;
import com.basas.blackholesim.core.math.Vec2;

import java.util.List;


public class NewtonianGravityModel implements GravityModel {

    @Override
    public String name() {
        return "Newtonian";
    }

    @Override
    public void acceleration(List<BlackHole> blackHoles, Vec2 pos, Vec2 vel, PhysicsParams params, Vec2 outAcc) {
        double ax = 0.0;
        double ay = 0.0;

        double soft2 = params.softening * params.softening;

        for (BlackHole bh : blackHoles) {
            double dx = bh.getPosition().x - pos.x;
            double dy = bh.getPosition().y - pos.y;

            double r2 = dx * dx + dy * dy;
            double r = Math.sqrt(Math.max(1e-12, r2));

            double denom = (r2 + soft2);
            double aMag = (params.G * bh.getMass()) / denom;

            double nx = dx / r;
            double ny = dy / r;

            ax += nx * aMag;
            ay += ny * aMag;
        }

        
        double aLen = Math.sqrt(ax * ax + ay * ay);
        if (aLen > params.maxAcceleration) {
            double k = params.maxAcceleration / Math.max(1e-12, aLen);
            ax *= k;
            ay *= k;
        }

        outAcc.x = ax;
        outAcc.y = ay;
    }

    @Override
    public double eventHorizonRadius(BlackHole bh, PhysicsParams params) {
        
        return (2.0 * params.G * bh.getMass()) / (params.c * params.c);
    }

    @Override
    public double potential(List<BlackHole> blackHoles, Vec2 pos, PhysicsParams params) {
        double soft2 = params.softening * params.softening;
        double phi = 0.0;

        for (BlackHole bh : blackHoles) {
            double dx = bh.getPosition().x - pos.x;
            double dy = bh.getPosition().y - pos.y;
            double r2 = dx * dx + dy * dy;
            double r = Math.sqrt(r2 + soft2);

            phi += -(params.G * bh.getMass()) / Math.max(1e-12, r);
        }
        return phi;
    }
}
