package org.mpashka.findme.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;

import java.util.stream.StreamSupport;

public class AccelerometerEntity {
    @JsonProperty("time")
    public long time;

    @JsonProperty("avg")
    public double average;

    @JsonProperty("max")
    public double maximum;

    @JsonProperty("battery")
    public int battery;

    public AccelerometerEntity() {
    }

    public AccelerometerEntity(long time, double average, double maximum, int battery) {
        this.time = time;
        this.average = average;
        this.maximum = maximum;
        this.battery = battery;
    }

    public static void init(PgPool client) {
        Uni.createFrom().item(1)
//                .flatMap(u -> client.query("DROP TABLE IF EXISTS location").execute())
                .flatMap(r -> client.query("CREATE TABLE IF NOT EXISTS accelerometer (" +
                        "time NUMERIC PRIMARY KEY, " +
                        "avg NUMERIC(14,11) NOT NULL, " +
                        "max NUMERIC(14,11) NOT NULL, " +
                        "battery NUMERIC" +
                        ")").execute())
                .await().indefinitely();
    }

    private static AccelerometerEntity from(Row row) {
        return new AccelerometerEntity(row.getLong("time"),
                row.getDouble("avg"),
                row.getDouble("max"),
                row.getInteger("battery"));
    }

    public static Multi<AccelerometerEntity> findAll(PgPool client, long start, long stop) {
        return client.preparedQuery("SELECT time, avg, max, battery " +
                "FROM accelerometer " +
                "WHERE time >= $1 and time <= $2" +
                "ORDER BY time ASC")
                .execute(Tuple.of(start, stop))
                // Create a Multi from the set of rows:
                .onItem().transformToMulti(set -> Multi.createFrom().items(() -> StreamSupport.stream(set.spliterator(), false)))
                // For each row create a fruit instance
                .onItem().transform(AccelerometerEntity::from);
    }

    public Uni<Void> save(PgPool client) {
        return client.preparedQuery("INSERT INTO accelerometer (time, avg, max, battery) VALUES ($1, $2, $3, $4)")
                .execute(Tuple.of(time, average, maximum, battery))
                .onItem().transform(i -> null);
        /*
        return client.preparedQuery("INSERT INTO fruits (name) VALUES ($1) RETURNING (id)").execute(Tuple.of(name))
                .onItem().transform(pgRowSet -> pgRowSet.iterator().next().getLong("id"));
         */
    }

    @Override
    public String toString() {
        return "AccelerometerEntity{" +
                "time=" + time +
                ", average=" + average +
                ", maximum=" + maximum +
                ", battery=" + battery +
                '}';
    }
}
