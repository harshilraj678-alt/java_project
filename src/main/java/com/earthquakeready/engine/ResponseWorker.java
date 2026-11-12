package com.earthquakeready.engine;

import com.earthquakeready.domain.ResponseMission;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ResponseWorker implements Runnable {
    private final EventBuffer<ResponseMission> work;
    private final ResourceAllocator resources;
    private final Consumer<ResponseMission> completionHandler;
    private final Runnable snapshotPublisher;

    public ResponseWorker(EventBuffer<ResponseMission> work, ResourceAllocator resources,
                          Consumer<ResponseMission> completionHandler, Runnable snapshotPublisher) {
        this.work = work;
        this.resources = resources;
        this.completionHandler = completionHandler;
        this.snapshotPublisher = snapshotPublisher;
    }

    @Override public void run() {
        try {
            ResponseMission mission;
            while ((mission = work.take()) != null) {
                if (work.isClosed()) {
                    resources.release(mission.allocation());
                    continue;
                }
                if (mission.advance()) {
                    resources.release(mission.allocation());
                    completionHandler.accept(mission);
                }
                else {
                    TimeUnit.MILLISECONDS.sleep(700);
                    try {
                        if (work.isClosed()) resources.release(mission.allocation());
                        else work.put(mission);
                    } catch (IllegalStateException closedDuringProgress) {
                        resources.release(mission.allocation());
                        break;
                    }
                }
                snapshotPublisher.run();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
