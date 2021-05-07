package org.mpashka.findme.server;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class MyService {

    private static final Logger log = LoggerFactory.getLogger(MyResource.class);

    @Inject
    PgPool client;

    public Uni<Void> saveEntity(SaveEntity saveEntity) {
        log.debug("Add {}", saveEntity);
        return saveLocations(saveEntity.getLocations()).onItem()
                .transformToUni(l -> saveAccelerations(saveEntity.getAccelerations()))
                .onItem().invoke(l -> log.debug("All saved"));
    }

    private Uni<Void> saveLocations(List<LocationEntity> locationEntities) {
        if (locationEntities == null || locationEntities.isEmpty()) {
            log.debug("No locations");
            return Uni.createFrom().voidItem();
        }
        return Multi.createFrom().iterable(locationEntities).onItem()
                .transformToUni(l -> l.save(client))
                .merge().collect().asList()
                .onItem().invoke(l -> log.debug("Locations saved"))
                .map(l -> null);
    }

    private Uni<Void> saveAccelerations(List<AccelerometerEntity> accelerometerEntities) {
        if (accelerometerEntities == null || accelerometerEntities.isEmpty()) {
            log.debug("No accelerations");
            return Uni.createFrom().voidItem();
        }
        return Multi.createFrom().iterable(accelerometerEntities).onItem()
                .transformToUni(l -> l.save(client))
                .merge().collect().asList()
                .onItem().invoke(l -> log.debug("Accelerations saved"))
                .map(l -> null);
    }
}
