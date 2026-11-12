package com.earthquakeready.engine;

import com.earthquakeready.domain.SimulationEvent;

@FunctionalInterface
public interface SimulationEventHandler {
    void handle(SimulationEvent event);
}
