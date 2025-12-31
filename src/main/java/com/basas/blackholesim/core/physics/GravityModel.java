package com.basas.blackholesim.core.physics;

import com.basas.blackholesim.core.entities.BlackHole;
import com.basas.blackholesim.core.math.Vec2;

import java.util.List;

public interface GravityModel {

    String name();

    void acceleration(List<BlackHole> blackHoles, Vec2 pos, Vec2 vel, PhysicsParams params, Vec2 outAcc);

    double eventHorizonRadius(BlackHole bh, PhysicsParams params);

    double potential(List<BlackHole> blackHoles, Vec2 pos, PhysicsParams params);
}
