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

    public Uni<?> saveEntity(SaveEntity saveEntity) {
        log.debug("Add {}", saveEntity);
        return saveLocations(saveEntity.getLocations())
                .onItem().invoke(l -> log.debug("All saved"));
    }

    private Uni<?> saveLocations(List<LocationEntity> locationEntities) {
        if (locationEntities == null || locationEntities.isEmpty()) {
            log.debug("No locations");
            return Uni.createFrom().voidItem();
        }
        return Multi.createFrom().iterable(locationEntities).onItem()
                .transformToUni(l -> l.save(client))
                .merge().collect().asList()
                .invoke(l -> log.debug("Locations saved"));
    }
}
