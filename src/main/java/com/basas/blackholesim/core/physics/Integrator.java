package com.basas.blackholesim.core.physics;

import com.basas.blackholesim.core.SimulationEngine;


public interface Integrator {

    String name();

    
    void step(SimulationEngine engine, double dt, boolean pushTrails);
}
