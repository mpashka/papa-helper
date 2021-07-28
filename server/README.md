https://quarkus.io/guides/getting-started-reactive
https://vertx.io/get-started/
https://smallrye.io/smallrye-mutiny/
https://smallrye.io/smallrye-mutiny/guides/converters


pg_ctlcluster 13 main start
Ver Cluster Port Status Owner    Data directory              Log file
13  main    5432 down   postgres /var/lib/postgresql/13/main /var/log/postgresql/postgresql-13-main.log

```
sudo -u postgres psql
create database papa_helper;
create user quarkus_test with encrypted password 'quarkus_test';
grant all privileges on database quarkus_test to quarkus_test;

docker exec -it papa-postgresql psql
\c papa_helper # connect to db
\dt            # Show tables
\dt+ 
```

flutter
https://stackoverflow.com/questions/57201412/any-way-to-add-google-maps-in-flutter-for-web

# server project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
./mvnw package && docker build -f src/main/docker/Dockerfile.jvm -t mpashka/papa-helper.server:jvm-alpha1 . && docker push mpashka/papa-helper.server:jvm-alpha1
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/server-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.html.

## Docker
This uses Dockerfile.jvm
```
./mvnw package -Dquarkus.native.container-build=true [-Dquarkus.container-image.builder=docker]
# docker build -f src/main/docker/Dockerfile.jvm -t mpashka/papa-helper.server:jvm-alpha1 .
# docker push mpashka/papa-helper.server:jvm-alpha1 
```

```
docker run -it -p 8080:8080 --env JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -Dquarkus.datasource.reactive.url=postgresql://192.168.2.52:5432/quarkus_test" m_pashka/server:1.0.0-SNAPSHOT
```

## Provided examples

### RESTEasy JAX-RS example

REST is easy peasy with this Hello World RESTEasy resource.

[Related guide section...](https://quarkus.io/guides/getting-started#the-jax-rs-resources)

