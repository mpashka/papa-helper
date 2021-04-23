package org.mpashka.findme.server;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

import java.util.stream.StreamSupport;

public class LocationEntity {
    private long time;
    private double latitude;
    private double longitude;
    private double accuracy;
    private int battery;

    public LocationEntity() {
    }

    public LocationEntity(long time, double latitude, double longitude, double accuracy, int battery) {
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.battery = battery;
    }

    private static LocationEntity from(Row row) {
        return new LocationEntity(row.getLong("time"),
                row.getDouble("lat"),
                row.getDouble("long"),
                row.getDouble("accuracy"),
                row.getInteger("battery"));
    }

    public static Multi<LocationEntity> findAll(PgPool client) {
        return client.query("SELECT time, lat, long, accuracy, battery FROM location ORDER BY time ASC").execute()
                // Create a Multi from the set of rows:
                .onItem().transformToMulti(set -> Multi.createFrom().items(() -> StreamSupport.stream(set.spliterator(), false)))
                // For each row create a fruit instance
                .onItem().transform(LocationEntity::from);
    }

    public Uni<Void> save(PgPool client) {
        return client.preparedQuery("INSERT INTO location (time, lat, long, accuracy, battery) VALUES ($1, $2, $3, $4, $5)")
                .execute(Tuple.of(time, latitude, longitude, accuracy, battery))
                .onItem().transform(i -> null);
        /*
        return client.preparedQuery("INSERT INTO fruits (name) VALUES ($1) RETURNING (id)").execute(Tuple.of(name))
                .onItem().transform(pgRowSet -> pgRowSet.iterator().next().getLong("id"));
         */
    }
}
