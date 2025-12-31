package com.basas.blackholesim.core.physics;

import com.basas.blackholesim.core.entities.BlackHole;

public final class Relativity {

    private Relativity() {}

    public static double massLength(BlackHole bh, PhysicsParams p) {
        double c2 = p.c * p.c;
        if (c2 <= 0) return 0;
        return (p.G * bh.getMass()) / c2;
    }

    public static double schwarzschildRadius(BlackHole bh, PhysicsParams p) {
        return 2.0 * massLength(bh, p);
    }

    public static double kerrHorizonRadius(BlackHole bh, PhysicsParams p) {
        double rg = massLength(bh, p);
        double a = clamp01(bh.getSpin());
        double term = Math.sqrt(Math.max(0.0, 1.0 - a * a));
        return rg * (1.0 + term);
    }

    public static double eventHorizonRadius(BlackHole bh, PhysicsParams p) {
        if (p.relativityMode == RelativityMode.KERR) return kerrHorizonRadius(bh, p);
        return schwarzschildRadius(bh, p);
    }

    public static double iscoRadius(BlackHole bh, PhysicsParams p) {
        double rg = massLength(bh, p);
        if (rg <= 0) return 0;
        double a = clamp01(bh.getSpin());
        if (p.relativityMode != RelativityMode.KERR || a == 0.0) return 6.0 * rg;

        double z1 = 1.0 + Math.cbrt(1.0 - a * a) * (Math.cbrt(1.0 + a) + Math.cbrt(1.0 - a));
        double z2 = Math.sqrt(3.0 * a * a + z1 * z1);
        double rI = (3.0 + z2 - Math.sqrt(Math.max(0.0, (3.0 - z1) * (3.0 + z1 + 2.0 * z2))));
        return rI * rg;
    }

    public static double keplerianSpeed(BlackHole bh, PhysicsParams p, double r) {
        double rg = massLength(bh, p);
        if (rg <= 0 || r <= 0) return 0;
        double x = r / rg;
        if (p.relativityMode == RelativityMode.KERR) {
            double a = clamp01(bh.getSpin());
            double omega = 1.0 / (Math.pow(x, 1.5) + a);
            double v = x * omega;
            return Math.min(0.999, Math.max(0.0, v / Math.sqrt(Math.max(1e-9, 1.0 - 2.0 / x))));
        } else {
            double v = Math.sqrt(1.0 / x);
            return Math.min(0.999, Math.max(0.0, v / Math.sqrt(Math.max(1e-9, 1.0 - 2.0 / x))));
        }
    }

    public static double gravitationalRedshiftFactor(BlackHole bh, PhysicsParams p, double r) {
        double rg = massLength(bh, p);
        if (rg <= 0 || r <= 0) return 1.0;
        double x = r / rg;
        return Math.sqrt(Math.max(1e-9, 1.0 - 2.0 / x));
    }

    public static double symmetricMassRatio(double m1, double m2) {
        double mt = m1 + m2;
        if (mt <= 0) return 0.0;
        return (m1 * m2) / (mt * mt);
    }

    public static double gwMassLossFraction(double m1, double m2) {
        double eta = symmetricMassRatio(m1, m2);
        return Math.max(0.0, Math.min(0.10, 0.20 * eta));
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(0.999, v));
    }
}
