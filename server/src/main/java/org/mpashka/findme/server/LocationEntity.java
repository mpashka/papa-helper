package org.mpashka.findme.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;

import java.util.stream.StreamSupport;

public class LocationEntity {

    @JsonProperty("time")
    public long time;

    @JsonProperty("lat")
    public double latitude;

    @JsonProperty("long")
    public double longitude;

    @JsonProperty("accuracy")
    public double accuracy;

    @JsonProperty("battery")
    public int battery;

    @JsonProperty("mi_battery")
    public int miBattery;

    @JsonProperty("mi_steps")
    public int miSteps;

    @JsonProperty("mi_heart")
    public int miHeart;

    public LocationEntity() {
    }

    public LocationEntity(long time, double latitude, double longitude, double accuracy, int battery, int miBattery, int miSteps, int miHeart) {
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.battery = battery;
        this.miBattery = miBattery;
        this.miSteps = miSteps;
        this.miHeart = miHeart;
    }

    public static void init(PgPool client) {
        Uni.createFrom().item(1)
//                .flatMap(u -> client.query("DROP TABLE IF EXISTS location").execute())
                .flatMap(r -> client.query("CREATE TABLE IF NOT EXISTS location (" +
                        "time NUMERIC PRIMARY KEY, " +
                        "lat NUMERIC(14,11) NOT NULL, " +
                        "long NUMERIC(14,11) NOT NULL, " +
                        "accuracy NUMERIC(6,3) NOT NULL," +
                        "battery NUMERIC," +
                        "mi_battery NUMERIC," +
                        "mi_steps NUMERIC," +
                        "mi_heart NUMERIC" +
                        ")").execute())
                .await().indefinitely();
    }

    private static LocationEntity from(Row row) {
        return new LocationEntity(row.getLong("time"),
                row.getDouble("lat"),
                row.getDouble("long"),
                row.getDouble("accuracy"),
                row.getInteger("battery"),
                row.getInteger("mi_battery"),
                row.getInteger("mi_steps"),
                row.getInteger("mi_heart")
                );
    }

    public static Multi<LocationEntity> findAll(PgPool client, long start, long stop) {
        return client.preparedQuery("SELECT time, lat, long, accuracy, battery " +
                "FROM location " +
                "WHERE time >= $1 and time <= $2" +
                "ORDER BY time ASC")
                .execute(Tuple.of(start, stop))
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

    @Override
    public String toString() {
        return "LocationEntity{" +
                "time=" + time +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                ", battery=" + battery +
                ", miBattery=" + miBattery +
                ", miSteps=" + miSteps +
                ", miHeart=" + miHeart +
                '}';
    }
}
