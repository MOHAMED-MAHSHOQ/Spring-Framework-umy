# Docker Guide for This Project (Beginner-Friendly + Industry Style)

This guide shows a clean, repeatable way to run your Spring Boot app with PostgreSQL using Docker.

If you only remember one rule, remember this:

- `localhost` inside a container means that same container, not your Postgres container.
- Use Docker service names (for example `postgres`) when containers talk to each other.

---

## 1) What is failing now and why

Your error says:

- `Connection to 127.0.0.1:5432 refused`

In your `localpostgres` profile, datasource URL points to `127.0.0.1`. That works only when app and DB run on the same machine process/network context. In Docker, app and DB are separate containers.

So the app should use:

- `jdbc:postgresql://postgres:5432/beerDB`

where `postgres` is the Docker Compose service name.

---

## 2) Prerequisites

Install and verify:

- Docker Desktop (running)
- Java 21
- Maven Wrapper (`mvnw.cmd`) already in project

Run these checks in PowerShell:

```powershell
docker --version
docker compose version
java -version
```

---

## 3) Recommended project files for Docker

## 3.1 Dockerfile (multi-stage build)

Replace your current `Dockerfile` with this:

```dockerfile
# ---------- build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom first for better layer caching
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./
COPY mvnw.cmd ./
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN ./mvnw -DskipTests clean package

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy Spring Boot fat jar produced by maven build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Container-friendly defaults (can still be overridden in compose)
ENV SPRING_PROFILES_ACTIVE=localpostgres

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Why this is industry standard:

- Multi-stage keeps final image smaller and cleaner
- Reproducible build inside Docker
- No need to pre-build jar manually on host

## 3.2 compose.yaml

Replace your current `compose.yaml` with this:

```yaml
services:
  postgres:
    image: postgres:16.3
    container_name: spring7-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: beerDB
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: mahshoq
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d beerDB"]
      interval: 10s
      timeout: 5s
      retries: 10

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: spring7-app
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: localpostgres
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/beerDB
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: mahshoq
      SPRING_FLYWAY_URL: jdbc:postgresql://postgres:5432/beerDB
      SPRING_FLYWAY_USER: postgres
      SPRING_FLYWAY_PASSWORD: mahshoq
      # Use host machine auth server if needed:
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://host.docker.internal:9000
    ports:
      - "8080:8080"

volumes:
  postgres_data:
```

Why this is industry standard:

- App + DB lifecycle in one command
- Healthcheck prevents app from starting before DB is ready
- Named volume keeps DB data between restarts
- Config is externalized via environment variables

---

## 4) Run commands (PowerShell)

From project root:

```powershell
cd "C:\Users\mohamed.ms\IdeaProjects\udemy Follow Up\spring-7-rest-mvc"

docker compose down

docker compose up --build -d

docker compose ps

docker compose logs -f app
```

Expected:

- `postgres` becomes healthy
- app starts without Flyway connection refused

Open:

- App: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## 5) Useful day-to-day commands

```powershell
# Show logs
docker compose logs -f

# Restart only app
docker compose restart app

# Enter postgres shell
docker compose exec postgres psql -U postgres -d beerDB

# Stop (keep data)
docker compose down

# Stop and remove data volume (full reset)
docker compose down -v
```

---

## 6) Recommended one-time property improvement

Your `application-localpostgres.properties` currently hardcodes `127.0.0.1`.

A safer pattern is to allow env overrides with localhost fallback:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://127.0.0.1:5432/beerDB}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:mahshoq}

spring.flyway.url=${SPRING_FLYWAY_URL:${SPRING_DATASOURCE_URL:jdbc:postgresql://127.0.0.1:5432/beerDB}}
spring.flyway.user=${SPRING_FLYWAY_USER:${SPRING_DATASOURCE_USERNAME:postgres}}
spring.flyway.password=${SPRING_FLYWAY_PASSWORD:${SPRING_DATASOURCE_PASSWORD:mahshoq}}
```

This keeps local non-Docker dev working, while Docker can override cleanly.

---

## 7) Common problems and fixes

## 7.1 Flyway / Postgres connection refused

Symptoms:

- `Connection to 127.0.0.1:5432 refused`

Fix:

- Ensure app uses `postgres` host, not `127.0.0.1`
- Ensure both services are in same `docker compose` project/network
- Check DB health:

```powershell
docker compose logs postgres --tail 100
docker compose ps
```

## 7.2 JWT issuer unreachable in Docker

If auth server is running on your host machine, use:

- `http://host.docker.internal:9000`

Do not use `http://localhost:9000` from inside the app container unless auth service is in the same container.

## 7.3 Swagger mapping pattern error

If you see this error:

- `Invalid mapping pattern detected: /swagger-ui/**/*swagger-initializer.js`

Use modern path patterns like:

- `/swagger-ui/**`

and avoid patterns that place extra text after `**`.

---

## 8) Optional production hardening (next level)

When you are ready:

- Move passwords to `.env` or Docker secrets
- Pin image versions (already done for Postgres)
- Add resource limits and JVM memory flags
- Add Actuator health endpoint and monitor it
- Use CI pipeline to build and scan Docker image

---

## 9) Quick start (copy-paste)

```powershell
cd "C:\Users\mohamed.ms\IdeaProjects\udemy Follow Up\spring-7-rest-mvc"
docker compose down -v
docker compose up --build -d
docker compose logs -f app
```

If startup is successful, test:

```powershell
curl http://localhost:8080/v3/api-docs
```

You now have a clean Docker baseline for local development that matches common industry practice.

---

## 10) Final: command-only run (no `compose.yaml`, no `Dockerfile` edits)

Use this when you want to run everything with raw Docker commands only.

### Step A: go to project folder

Why: all next commands expect you to be in this project.

```powershell
cd "C:\Users\mohamed.ms\IdeaProjects\udemy Follow Up\spring-7-rest-mvc"
```

### Step B: build the Spring Boot jar on your machine

Why: without using a Dockerfile build, we need a jar first.

```powershell
.\mvnw.cmd -DskipTests clean package
```

### Step C: find the jar path and save it in a variable

Why: jar name can change, so this avoids hardcoding it.

```powershell
$JAR = (Get-ChildItem ".\target\*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1).FullName
$JAR
```

### Step D: create a private Docker network

Why: app and database containers can find each other by name.

```powershell
docker network create spring7net
```

If it already exists, Docker will print an error; that is okay.

### Step E: run PostgreSQL container

Why: your app profile `localpostgres` needs Postgres + Flyway migrations.

```powershell
docker run -d --name postgres --network spring7net -e POSTGRES_DB=beerDB -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=mahshoq -p 5432:5432 postgres:16.3
```

### Step F: check PostgreSQL is ready

Why: starting app before DB is ready causes connection refused.

```powershell
docker logs postgres --tail 100
```

You want to see a line similar to "database system is ready to accept connections".

### Step G: run the app container from Java image and mount your jar

Why: this starts your app without any custom Dockerfile. We pass env variables so datasource/flyway use `postgres` hostname (not `127.0.0.1`).

```powershell
docker run -d --name spring7-app --network spring7net -p 8080:8080 -v "${JAR}:/app/app.jar" -e SPRING_PROFILES_ACTIVE=localpostgres -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/beerDB -e SPRING_DATASOURCE_USERNAME=postgres -e SPRING_DATASOURCE_PASSWORD=mahshoq -e SPRING_FLYWAY_URL=jdbc:postgresql://postgres:5432/beerDB -e SPRING_FLYWAY_USER=postgres -e SPRING_FLYWAY_PASSWORD=mahshoq -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://host.docker.internal:9000 eclipse-temurin:21-jre java -jar /app/app.jar
```

### Step H: watch app logs

Why: confirm Flyway migration + Spring Boot startup is successful.

```powershell
docker logs -f spring7-app
```

### Step I: test from browser

Why: quick functional check.

- App: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`

### Step J: day-2 commands (stop, start, clean)

Why: this is the minimum you need for normal daily use.

```powershell
# Stop containers (keeps data)
docker stop spring7-app postgres

# Start again
docker start postgres

docker start spring7-app

# See running containers
docker ps

# Remove only app container (safe)
docker rm -f spring7-app

# Full reset: removes app + db containers + db data volume in container
# (data is deleted because container is removed and we did not attach a named volume)
docker rm -f spring7-app postgres

# Optional: remove network
docker network rm spring7net
```

### One-screen command list (copy in order)

```powershell
cd "C:\Users\mohamed.ms\IdeaProjects\udemy Follow Up\spring-7-rest-mvc"
.\mvnw.cmd -DskipTests clean package
$JAR = (Get-ChildItem ".\target\*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1).FullName
docker network create spring7net
docker run -d --name postgres --network spring7net -e POSTGRES_DB=beerDB -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=mahshoq -p 5432:5432 postgres:16.3
docker logs postgres --tail 100
docker run -d --name spring7-app --network spring7net -p 8080:8080 -v "${JAR}:/app/app.jar" -e SPRING_PROFILES_ACTIVE=localpostgres -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/beerDB -e SPRING_DATASOURCE_USERNAME=postgres -e SPRING_DATASOURCE_PASSWORD=mahshoq -e SPRING_FLYWAY_URL=jdbc:postgresql://postgres:5432/beerDB -e SPRING_FLYWAY_USER=postgres -e SPRING_FLYWAY_PASSWORD=mahshoq -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://host.docker.internal:9000 eclipse-temurin:21-jre java -jar /app/app.jar
docker logs -f spring7-app
```
