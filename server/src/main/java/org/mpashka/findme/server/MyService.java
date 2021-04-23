package org.mpashka.findme.server;

import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyService {
    public Uni<String> greeting(String name) {
        return Uni.createFrom().item(name)
                .onItem().transform(n -> String.format("hello %s", n));
    }
}
