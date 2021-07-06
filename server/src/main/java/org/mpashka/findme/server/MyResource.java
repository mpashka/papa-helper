package org.mpashka.findme.server;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/findme")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MyResource {

    private static final Logger log = LoggerFactory.getLogger(MyResource.class);

    @Inject
    MyService service;

    @Inject
    PgPool client;

    @GET
    @Path("/info")
    @Produces(MediaType.TEXT_PLAIN)
    public String info() {
        return "Server works";
    }

    @POST
    @Path("/save")
    public Uni<Response> save(SaveEntity saveEntity) {
        log.debug("Add {}", saveEntity);
        return service.saveEntity(saveEntity)
                .onItem().transform(i -> Response.ok().build());
    }

    @POST
    @Path("/location/add")
    public Uni<Response> locationAdd(LocationEntity location) {
        log.debug("Add {}", location);
        return location.save(client)
                .onItem().transform(i -> Response.ok().build());
    }

    @GET
    @Path("/location/get")
    public Multi<LocationEntity> locationGet(@QueryParam("start") long start, @QueryParam("stop") long stop) {
        return LocationEntity.findAll(client, start, stop);
    }
}
