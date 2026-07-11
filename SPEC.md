# Spec: TaskHub — Team Task-Management Service

## Objective

TaskHub is a team task-management platform, built incrementally across **8 course sessions**, each shipping a working, demoable increment. The end state is a small but production-shaped microservice system:

- A **REST API** (`task-service`) for managing Projects, Tasks, Tags, and Users, secured with JWT + role-based access.
- A separate **gRPC notification-service** that emits task-lifecycle events (unary calls + server-streaming subscriptions).
- Both services are backed by **PostgreSQL**, tested at multiple levels, containerized, deployed to a local **kind** Kubernetes cluster via **Flux CD GitOps**, and observable via structured logs, metrics, dashboards, and traces.

**Target user:** the person taking the course (learning Spring Boot / gRPC / K8s / GitOps end-to-end) — so the spec favors realistic, idiomatic patterns over shortcuts, even where a shortcut would be faster to demo.

**Definition of success:** by the end of session 8, `git clone` → `kind create cluster` → Flux reconciles → both services are running in `dev` and promotable to `prod`, with dashboards showing live traffic and traces for a request that flows from the REST API into a notification event.

## Assumptions

Stated so they can be corrected before session 1 starts:

1. **Monorepo, two independent Maven projects** — `task-service/` and `notification-service/` are separate Spring Boot apps (own `pom.xml`, own Dockerfile, own deployment) living in one git repo, not a Maven multi-module parent/aggregator. This matches how they'll actually be deployed (independently) in Kubernetes.
2. **JWT is self-issued**, not delegated to an external IdP (no Keycloak/Auth0/OAuth2 provider) — `task-service` issues and validates its own JWTs. Simpler for a course context; can be swapped later.
3. **SonarCloud** (not self-hosted SonarQube) is the static-analysis target in CI, since it's free for this kind of repo and needs no extra infra.
4. **Container registry:** GitHub Container Registry (GHCR), since it needs no extra account setup beyond the GitHub repo already backing CI.
5. **kind** is local-only for this course (not a managed cloud cluster) — "dev" and "prod" are two overlays/namespaces on the *same* local kind cluster, not two real environments.
6. **Observability stack runs in-cluster** (kube-prometheus-stack for Prometheus/Grafana/Alertmanager, Loki + Promtail/Grafana Agent for logs, Tempo or Zipkin for traces) rather than a hosted SaaS APM.
7. **Coverage gate:** 80% instruction coverage via JaCoCo, enforced in the Maven `verify` phase and re-checked in CI — adjustable per module if a module has a good reason (e.g., gRPC generated code excluded).
8. DB migrations are **Flyway only** (no Hibernate `ddl-auto: update` in any environment beyond local scratch experiments).

## Tech Stack

| Concern | Choice |
|---|---|
| Language / JDK | Java 25 (LTS) |
| Framework | Spring Boot 4.1.x (requires Spring Framework 7 baseline — do not pin an exact patch version by hand, let Spring Initializr pick current default within the 4.1 line) |
| Build tool | Maven |
| REST | Spring Web MVC, springdoc-openapi (Swagger UI) |
| Persistence | Spring Data JPA + PostgreSQL 16 + Flyway |
| Auth | Spring Security + JWT (jjwt or Spring's own `NimbusJwtDecoder`), role-based (`USER`, `ADMIN`) |
| gRPC | `grpc-spring-boot-starter` (Yidongnan/grpc-ecosystem) + `protobuf-maven-plugin` |
| Mapping | MapStruct (entity ↔ DTO) |
| Boilerplate | Lombok |
| Unit/slice tests | JUnit 5, Mockito, `@WebMvcTest` / `@DataJpaTest` |
| Integration tests | Testcontainers (Postgres), gRPC in-process test server |
| E2E | REST Assured (or Spring's `TestRestTemplate`) against a docker-composed stack |
| Coverage | JaCoCo |
| Containerization | Docker, multi-stage builds |
| CI | GitHub Actions |
| Static analysis | SonarCloud |
| Orchestration | Kubernetes via `kind` |
| GitOps | Flux CD, Kustomize overlays (`dev`, `prod`) |
| Metrics | Micrometer → Prometheus |
| Dashboards | Grafana |
| Logs | Logback (JSON via `logstash-logback-encoder`) → Loki |
| Tracing | Micrometer Tracing + OpenTelemetry → Tempo |
| Alerting | Prometheus Alertmanager |

## Commands

Bootstrap (Session 1 — run once from repo root):

```bash
# Look up the current latest 4.1.x patch before generating (bootVersion must be an exact
# version the Initializr metadata knows about — there's no "4.1.x" wildcard):
curl -s https://start.spring.io/metadata/client | grep -o '"4\.1\.[0-9]*"' | sort -V | tail -1

# task-service — REST API (replace 4.1.0 below with the version found above)
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d javaVersion=25 \
  -d bootVersion=4.1.0 \
  -d groupId=com.taskhub \
  -d artifactId=task-service \
  -d name=task-service \
  -d packageName=com.taskhub.taskservice \
  -d dependencies=web,data-jpa,postgresql,flyway,validation,security,actuator,lombok,testcontainers \
  -o task-service.zip \
  && unzip task-service.zip -d task-service && rm task-service.zip

# notification-service — gRPC (gRPC itself isn't a start.spring.io dependency;
# added by hand to pom.xml in session 4: grpc-spring-boot-starter + protobuf-maven-plugin)
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d javaVersion=25 \
  -d bootVersion=4.1.0 \
  -d groupId=com.taskhub \
  -d artifactId=notification-service \
  -d name=notification-service \
  -d packageName=com.taskhub.notificationservice \
  -d dependencies=actuator,lombok \
  -o notification-service.zip \
  && unzip notification-service.zip -d notification-service && rm notification-service.zip
```

Per-module day-to-day commands (run from inside `task-service/` or `notification-service/`):

```bash
./mvnw spring-boot:run                 # run locally
./mvnw test                            # unit + slice tests
./mvnw verify                          # + integration tests + JaCoCo coverage gate
./mvnw verify -Dsonar.host.url=...      # + Sonar analysis (also runs in CI)
docker build -t taskhub/<module>:dev . # build image locally
```

Local full-stack dev:

```bash
docker compose -f deploy/docker-compose.yml up   # postgres + both services
```

Cluster (session 7+):

```bash
kind create cluster --config deploy/k8s/kind-config.yaml
flux bootstrap github --owner=<you> --repository=taskhub --path=deploy/k8s/clusters/dev
kubectl get kustomizations -n flux-system
```

## Project Structure

```
taskhub/
├── SPEC.md
├── README.md
├── task-service/                     # REST API (Spring Boot, Maven)
│   ├── pom.xml
│   ├── Dockerfile                    # multi-stage
│   └── src/
│       ├── main/java/com/taskhub/taskservice/
│       │   ├── project/              # package-by-feature: entity, repo, service, controller, dto, mapper
│       │   ├── task/
│       │   ├── tag/
│       │   ├── user/
│       │   ├── auth/                 # JWT filter, security config, roles
│       │   └── common/               # exception handling, pagination, config
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/         # Flyway: V1__init.sql, V2__..., etc.
│       └── test/java/com/taskhub/taskservice/
│           ├── unit/
│           ├── slice/
│           └── integration/
├── notification-service/             # gRPC (Spring Boot, Maven)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/proto/               # .proto definitions
│       ├── main/java/com/taskhub/notificationservice/
│       │   ├── grpc/                 # service impls, interceptors
│       │   └── config/
│       └── test/java/...
├── deploy/
│   ├── docker-compose.yml            # local dev stack
│   ├── k8s/
│   │   ├── base/                     # shared manifests (Deployment, Service, etc.)
│   │   ├── overlays/dev/             # Kustomize overlay
│   │   ├── overlays/prod/            # Kustomize overlay
│   │   ├── clusters/dev/             # Flux Kustomization + GitRepository CRs
│   │   └── observability/            # kube-prometheus-stack, Loki, Tempo values/manifests
│   └── kind-config.yaml
├── docs/                             # ADRs, diagrams, session notes
└── .github/workflows/
    ├── task-service-ci.yml
    └── notification-service-ci.yml
```

## Code Style

Package-by-feature, constructor injection, records for DTOs, MapStruct for mapping, `ProblemDetail` (RFC 7807) for errors. Example from `task-service`:

```java
// dto/TaskRequest.java
public record TaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotNull TaskStatus status,
    @FutureOrPresent LocalDate dueDate,
    Set<@NotBlank String> tagNames
) {}

// task/TaskController.java
@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks")
class TaskController {

    private final TaskService taskService;

    TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    Page<TaskResponse> list(@PathVariable UUID projectId, Pageable pageable) {
        return taskService.list(projectId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')")
    TaskResponse create(@PathVariable UUID projectId, @Valid @RequestBody TaskRequest request) {
        return taskService.create(projectId, request);
    }
}

// common/GlobalExceptionHandler.java
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleNotFound(TaskNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
```

Conventions:

- No field injection (`@Autowired` on fields) — constructor injection only, no Lombok `@RequiredArgsConstructor` needed for small classes; use it once a class has 4+ dependencies.
- DTOs are `record`s; entities use Lombok `@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor/@Builder`.
- Package-private classes/methods by default; `public` only when used outside the feature package.
- Every non-trivial endpoint has a matching OpenAPI `@Operation`/`@ApiResponse` annotation — Swagger UI must stay accurate, not an afterthought.
- Test method names: `should_doThing_when_condition()`.

## Testing Strategy

| Level | Tool | Scope | Where |
|---|---|---|---|
| Unit | JUnit 5 + Mockito | Services, mappers, JWT util — no Spring context | `*/test/.../unit` |
| Slice | `@WebMvcTest`, `@DataJpaTest` | Controllers (mocked service layer), repositories (H2 or Testcontainers) | `*/test/.../slice` |
| Integration | Testcontainers (Postgres) | Full Spring context, real DB, real Flyway migrations | `*/test/.../integration` |
| gRPC | in-process gRPC test server | Unary + streaming contract tests, interceptor behavior | `notification-service/test` |
| E2E | REST Assured / `TestRestTemplate` against `docker compose` stack | Auth → CRUD → notification event, cross-service happy path | `task-service/test/e2e` (or a top-level `e2e/` module if it grows) |
| Coverage gate | JaCoCo, bound to `verify` | 80% instruction coverage minimum, enforced in CI | `pom.xml` per module |

Every session that adds behavior adds tests in the *same* task, not a follow-up — a session isn't done until its tests pass and the coverage gate holds.

## Course Roadmap (8 Sessions)

Each session is a self-contained increment with its own scope, deliverables, and acceptance check. Per the agreed workflow, I will **stop and confirm scope with you before starting each new session**, even though the full roadmap is pre-approved here.

### Session 1 — Foundations: project bootstrap, PostgreSQL, Flyway, entities
- Generate `task-service` and `notification-service` via Spring Initializr (commands above); remove the current placeholder `src/Main.java`.
- Local Postgres via `deploy/docker-compose.yml`.
- Flyway baseline migration creating `users`, `projects`, `tasks`, `tags`, `task_tags` tables.
- JPA entities: `User`, `Project`, `Task`, `Tag` (Task ↔ Tag many-to-many; Task → Project many-to-one; Project → User many-to-one owner).
- **Acceptance:** `./mvnw spring-boot:run` in `task-service` boots cleanly against Dockerized Postgres; Flyway migration applies with no manual schema steps.

### Session 2 — Task & Project CRUD REST API
- DTOs (records) + validation for Project and Task create/update/read.
- Full CRUD endpoints under `/api/v1`, pagination (`Pageable`, `Page<T>` responses) on list endpoints.
- Springdoc-openapi wired up; Swagger UI reachable at `/swagger-ui.html`.
- **Acceptance:** CRUD flows demoable via Swagger UI; invalid payloads return RFC 7807 `ProblemDetail` 400s with field-level messages.

### Session 3 — JWT authentication + role-based authorization + CORS
- `/api/v1/auth/register`, `/api/v1/auth/login` issuing JWTs; password hashing (BCrypt).
- `USER` / `ADMIN` roles; `@PreAuthorize` on mutating endpoints; admins can act across all projects, users only on their own.
- CORS configured for a named allow-list of origins (not `*`).
- **Acceptance:** unauthenticated requests to protected endpoints get 401; a `USER` acting outside their project gets 403; JWT expiry is enforced.

### Session 4 — gRPC notification-service
- `.proto` contract: unary `SendNotification`, server-streaming `SubscribeToTaskEvents`.
- Server + interceptor (logging + auth-token propagation) in `notification-service`.
- `task-service` publishes a notification (via gRPC client stub) on task create/status-change.
- **Acceptance:** a gRPC client (e.g. `grpcurl` or a small test client) can call both RPCs against a running `notification-service`; `task-service` triggers a real notification end-to-end.

### Session 5 — Full test strategy: unit, slice, integration, E2E, coverage gate
- Backfill/extend unit + slice tests for anything under-tested from sessions 1–4.
- Testcontainers-based integration tests for repositories and the full request → DB round trip.
- gRPC contract tests (interceptor + streaming behavior).
- One E2E scenario spanning both services.
- JaCoCo gate wired into `mvn verify`, threshold enforced locally (CI wiring is session 6).
- **Acceptance:** `./mvnw verify` fails the build if coverage drops below 80%; all test levels pass locally.

### Session 6 — Multi-stage Docker + GitHub Actions CI
- Multi-stage `Dockerfile` per service (Maven build stage → slim JRE runtime stage, non-root user).
- `.github/workflows/*-ci.yml`: build → test (`verify`, coverage gate) → SonarCloud scan → publish image to GHCR (tagged by git SHA + `latest` on main).
- **Acceptance:** a PR triggers build+test+sonar; a merge to `main` publishes both images to GHCR.

### Session 7 — Kubernetes (kind) + Flux CD GitOps
- Kustomize `base` + `overlays/dev` + `overlays/prod` manifests (Deployment, Service, ConfigMap, Secret refs, HPA optional) for both services + Postgres.
- Flux bootstrapped against this repo, watching `deploy/k8s/clusters/dev`.
- Documented rollback procedure (git revert triggers Flux reconcile back to prior state; verify via `flux get kustomizations`).
- **Acceptance:** `kind create cluster` + Flux bootstrap results in both services healthy in the `dev` namespace, reachable via `kubectl port-forward`; a deliberate bad commit + revert demonstrates rollback.

### Session 8 — Observability: logging, metrics, dashboards, tracing, alerting
- Structured JSON logs (Logback + `logstash-logback-encoder`) shipped to Loki.
- Prometheus scraping both services via Actuator/Micrometer; Grafana dashboards (per-service latency, error rate, JVM, gRPC call counts) provisioned as code.
- Distributed tracing (Micrometer Tracing + OTel) across a request that spans `task-service` → `notification-service`, visualized in Tempo/Grafana.
- Alertmanager rule(s) for at least one meaningful condition (e.g. error-rate spike, pod crash-looping).
- **Acceptance:** a single Grafana "Explore" trace shows a request crossing both services; a manually-triggered condition fires a visible alert.

## Boundaries

**Always:**
- Write/extend tests in the same session that introduces the behavior; do not defer to "later."
- Run `./mvnw verify` (tests + coverage gate) before considering any session's code done.
- Use Flyway migrations for every schema change — never hand-edit the DB or rely on `ddl-auto`.
- Keep Swagger/OpenAPI annotations in sync with actual endpoints.
- Use constructor injection and package-by-feature structure consistently.
- Confirm scope with you before starting each new session (per your stated preference).

**Ask first:**
- Any change to the DB schema/migration files after they've shipped in an earlier session (Flyway migrations are append-only; fixing a mistake means a new migration, not editing history — confirm the approach before writing it).
- Adding a new third-party dependency not already listed in the Tech Stack table.
- Any change to CI workflow files, branch protection, or required secrets (`SONAR_TOKEN`, GHCR credentials, kubeconfig).
- Anything that touches the `prod` overlay or triggers a real (non-`kind`) deployment.
- Changing the coverage gate threshold or excluding a module from it.
- Introducing an external IdP/OAuth2 provider in place of self-issued JWT (a scope change from the Assumptions above).

**Never:**
- Commit secrets, `.env` files, JWT signing keys, or kubeconfig credentials to the repo.
- Disable or delete a failing test to make CI green — fix the code or the test, or flag it to you.
- Use `ddl-auto: update`/`create` against any environment beyond a throwaway local experiment.
- Force-push, rewrite history, or bypass branch protection / required CI checks.
- Deploy directly with `kubectl apply` against the GitOps-managed namespaces — all changes to `dev`/`prod` manifests go through git + Flux.
- Expose the gRPC notification-service or the Postgres instance outside the cluster/dev-network without an explicit request.

## Success Criteria

- All 8 sessions' acceptance checks (above) pass.
- `./mvnw verify` is green with ≥80% coverage on both modules.
- CI pipeline (build → test → sonar → publish) is green on `main` for both services.
- `kind` cluster + Flux reconciliation brings up a fully working `dev` environment from a clean clone.
- Grafana shows live metrics, logs (via Loki), and a cross-service trace; at least one alert rule is demonstrably functional.
- A documented, tested rollback (git revert + Flux reconcile) restores a prior known-good state.

## Open Questions

- Exact JWT library (`io.jsonwebtoken:jjwt` vs Spring Security's built-in resource-server JWT support) — default to Spring Security's own JWT decoder/encoder unless you'd rather use `jjwt` directly; can decide at session 3.
- Whether `notification-service` needs its own Postgres (e.g. to persist notification history) or stays stateless (in-memory/pass-through) — currently assumed **stateless**; raise this again at session 4 if persistence turns out to be needed.
- Whether the E2E suite (session 5) deserves its own top-level Maven module/directory once it exists, or stays inside `task-service` — defer until we see how large it gets.
