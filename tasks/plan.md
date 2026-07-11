# TaskHub — Full 8-Session Task Breakdown

Source of truth: `../SPEC.md`. This document is the human-readable overview; the actionable
units are tracked as GitHub Issues (one per task below), grouped into 8 Milestones (one per
session). See `todo.md` for a flat checklist mirroring this breakdown.

No implementation exists yet. Session 1 starts only after explicit confirmation from the
developer, per the "confirm scope before starting each new session" boundary in `SPEC.md`.

## Key decision

The `users.role` column (`USER`/`ADMIN`) is added to **Session 1**'s `V1__init.sql` baseline,
not deferred to a Session-3 migration. Chosen explicitly by the developer to avoid altering an
already-shipped Flyway migration later (Flyway migrations are append-only per `SPEC.md`
boundaries).

## Dependency graph

Sessions are strictly sequential (single developer):

```
S1 Foundations → S2 CRUD API → S3 JWT+RBAC+CORS → S4 gRPC notifications
   → S5 Full test strategy → S6 Docker+CI → S7 K8s+Flux → S8 Observability
```

Within S1 only: T1 (task-service boot) and T2 (notification-service boot) are independent of
each other. Every other task lists what it depends on inline.

## Session 1 — Foundations
*(role column included in V1 per developer's decision)*

- **S1-T1** — Bootstrap `task-service` against Dockerized Postgres. Spring Initializr (Java 25,
  Boot 4.1.x, `com.taskhub`), remove placeholder `src/Main.java`, `deploy/docker-compose.yml`
  with Postgres 16, datasource wired.
  - Verify: `docker compose up -d postgres && ./mvnw spring-boot:run`; `/actuator/health` → UP.
- **S1-T2** — Bootstrap `notification-service` skeleton. Initializr (actuator, lombok only),
  distinct port from task-service, no DB dependency. Independent of S1-T1.
  - Verify: boots standalone, health UP.
- **S1-T3** — Flyway baseline migration `V1__init.sql`: `users` (incl. `role` column),
  `projects`, `tasks`, `tags`, `task_tags`, FKs per spec relationships. Depends on S1-T1.
  - Verify: `psql \dt` shows all 5 tables + `flyway_schema_history` with `V1` success.
- **S1-T4** — JPA entities + persistence slice test. `User`/`Project`/`Task`/`Tag` (Lombok),
  `@DataJpaTest` round-tripping the full graph. Depends on S1-T3.
  - Verify: slice test passes, no Hibernate schema-mismatch warnings.

**Checkpoint:** compose up → both services boot, V1 applied with zero manual steps, persistence
test green, placeholder removed.

## Session 2 — Task & Project CRUD REST API

- **S2-T1** — Project CRUD vertical slice. DTOs+validation+repo+service+controller under
  `/api/v1/projects`, paginated list, slice test (happy path + validation failure).
- **S2-T2** — Task CRUD nested under project (with tag association).
  `/api/v1/projects/{projectId}/tasks`, create/update accepts `tagNames`. Depends on S2-T1.
- **S2-T3** — Tag read endpoints. `GET /api/v1/tags`, `GET /api/v1/tags/{id}`, paginated.
  Depends on S2-T2 (tags exist once tasks can create them).
- **S2-T4** — Swagger UI + global RFC 7807 error handling. `springdoc-openapi` wired,
  `@Operation` annotations, `GlobalExceptionHandler`. Depends on S2-T1..T3 (validates real
  endpoint surface).

**Checkpoint:** full CRUD demoable via Swagger UI, invalid payloads → `ProblemDetail` 400s with
field errors.

## Session 3 — JWT authentication + RBAC + CORS

- **S3-T1** — Register/login issuing JWT (BCrypt). No schema change needed — `role` already
  exists from S1.
- **S3-T2** — Security filter chain enforcing 401 on protected endpoints. Depends on S3-T1.
- **S3-T3** — Role-based authorization + ownership rules. `@PreAuthorize`, ADMIN vs
  USER-owns-only. Depends on S3-T2.
- **S3-T4** — CORS allow-list + JWT expiry enforcement. Depends on S3-T2.

**Checkpoint:** unauthenticated → 401, USER outside own project → 403, ADMIN unrestricted, JWT
expiry enforced, CORS allow-list (no `*`).

## Session 4 — gRPC notification-service

- **S4-T1** — `.proto` contract + unary `SendNotification`. Add `grpc-spring-boot-starter` +
  `protobuf-maven-plugin`.
- **S4-T2** — Server-streaming `SubscribeToTaskEvents` + logging interceptor. Depends on S4-T1.
- **S4-T3** — Auth-token propagation interceptor — rejects calls without valid token metadata.
  Depends on S4-T2.
- **S4-T4** — `task-service` gRPC client — real end-to-end notification on task
  create/status-change. Depends on S4-T3 and S2/S3.

**Checkpoint:** `grpcurl` calls both RPCs successfully; auth interceptor rejects unauthenticated
calls; task create triggers a real, observable notification.

## Session 5 — Full test strategy

- **S5-T1** — Backfill unit tests: `JwtService`, ownership logic, MapStruct mappers.
- **S5-T2** — Testcontainers integration tests: real Postgres, real Flyway, full request→DB
  round trip.
- **S5-T3** — gRPC contract tests: in-process server, unary+streaming+interceptor rejection.
- **S5-T4** — Cross-service E2E happy-path test: register→login→create project→create
  task→assert notification received. Depends on S5-T2 and S5-T3.
- **S5-T5** — Wire JaCoCo 80% coverage gate into `mvn verify` for both modules. Depends on
  S5-T1..T4.

**Checkpoint:** `./mvnw verify` fails below 80% coverage (sanity-checked); all test levels pass.

## Session 6 — Multi-stage Docker + GitHub Actions CI

- **S6-T1** — Multi-stage `Dockerfile` for `task-service` — non-root, slim JRE runtime stage.
- **S6-T2** — Multi-stage `Dockerfile` for `notification-service`.
- **S6-T3** — `[ask-first]` GitHub Actions CI for `task-service` — build→test→SonarCloud→publish
  to GHCR. Touches CI files + `SONAR_TOKEN`/GHCR secrets. Depends on S6-T1.
- **S6-T4** — `[ask-first]` GitHub Actions CI for `notification-service` — same pattern, same
  secrets. Depends on S6-T2.

**Checkpoint:** both Dockerfiles build/run locally; PR triggers build+test+sonar; merge to
`main` publishes both images to GHCR.

## Session 7 — Kubernetes (kind) + Flux CD GitOps

- **S7-T1** — Kustomize base manifests + `kind-config.yaml` — Deployment/Service/
  ConfigMap+SecretRefs for all 3 workloads. Depends on S6 (images exist).
- **S7-T2** — `overlays/dev` + manual smoke test on `kind` — proves manifests work before
  handing to Flux. Depends on S7-T1.
- **S7-T3** — `[ask-first]` Bootstrap Flux CD watching `deploy/k8s/clusters/dev` — needs
  kubeconfig + GitHub deploy token. Depends on S7-T2 proven manually first.
- **S7-T4** — Rollback demonstration — bad commit → Flux reconciles failure → `git revert` →
  recovery, documented. Depends on S7-T3.
- **S7-T5** — `[ask-first]` `overlays/prod` scaffold — render-only, never applied/wired to Flux.

**Checkpoint:** `kind create` + Flux bootstrap → both services healthy in `dev`; rollback demo
works; `prod` overlay exists but is never deployed.

## Session 8 — Observability

- **S8-T1** — Structured JSON logs → Loki — `logstash-logback-encoder`, log shipper deployed.
  Depends on S7 (running cluster).
- **S8-T2** — Prometheus scraping both services — `/actuator/prometheus`, Targets page shows
  both UP. Depends on S7.
- **S8-T3** — Grafana dashboards as code — latency/error-rate/JVM/gRPC-call-count panels.
  Depends on S8-T2.
- **S8-T4** — Distributed tracing `task-service`→`notification-service` — Micrometer
  Tracing+OTel, one trace ID spans both. Depends on S8-T1, S8-T2.
- **S8-T5** — Alertmanager rule + manual trigger demo. Depends on S8-T2, S8-T3.

**Checkpoint (project completion):** Grafana Explore trace crosses both services;
manually-triggered alert fires; all prior checkpoints hold.

## Ask-first tasks

Require explicit confirmation before executing, even though the roadmap itself is pre-approved:

| Task | Why |
|---|---|
| S6-T3 | New CI workflow file + `SONAR_TOKEN`/GHCR secrets |
| S6-T4 | New CI workflow file + shared secrets |
| S7-T3 | Flux bootstrap — kubeconfig + GitHub deploy token |
| S7-T5 | Touches the `prod` overlay |
