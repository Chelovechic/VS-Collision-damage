package org.collisionmod.collision.forge;

import org.valkyrienskies.core.api.events.CollisionEvent;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import java.util.concurrent.ConcurrentLinkedQueue;


public final class VSCollisionEvents {

    private VSCollisionEvents() {}

    public static final ConcurrentLinkedQueue<CollisionEvent> QUEUE = new ConcurrentLinkedQueue<>();

    public enum BackendMode {
        PHYSX,
        KRUNCH
    }


    public static volatile BackendMode MODE = BackendMode.PHYSX;

    private static volatile boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        var api = ValkyrienSkiesMod.getApi();


        api.getCollisionStartEvent().on(ev -> {
            if (MODE != BackendMode.PHYSX) return;
            QUEUE.add(ev);
        });


        api.getCollisionPersistEvent().on(ev -> {
            if (MODE != BackendMode.KRUNCH) return;
            QUEUE.add(ev);
        });
    }
}

