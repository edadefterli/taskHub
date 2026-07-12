# TaskHub — Task Checklist

Mirrors `plan.md`. `[ask-first]` = confirm with developer before executing, even post-approval.

## Session 1 — Foundations
- [x] S1-T1 Bootstrap task-service against Dockerized Postgres
- [x] S1-T2 Bootstrap notification-service skeleton
- [x] S1-T3 Flyway baseline migration V1__init.sql (incl. users.role)
- [x] S1-T4 JPA entities + persistence slice test

## Session 2 — Task & Project CRUD REST API
- [x] S2-T1 Project CRUD vertical slice
- [ ] S2-T2 Task CRUD nested under project (with tag association)
- [ ] S2-T3 Tag read endpoints
- [ ] S2-T4 Swagger UI + global RFC 7807 error handling

## Session 3 — JWT authentication + RBAC + CORS
- [ ] S3-T1 Register/login issuing JWT (BCrypt)
- [ ] S3-T2 Security filter chain enforcing 401
- [ ] S3-T3 Role-based authorization + ownership rules
- [ ] S3-T4 CORS allow-list + JWT expiry enforcement

## Session 4 — gRPC notification-service
- [ ] S4-T1 .proto contract + unary SendNotification
- [ ] S4-T2 Server-streaming SubscribeToTaskEvents + logging interceptor
- [ ] S4-T3 Auth-token propagation interceptor
- [ ] S4-T4 task-service gRPC client (real end-to-end notification)

## Session 5 — Full test strategy
- [ ] S5-T1 Backfill unit tests
- [ ] S5-T2 Testcontainers integration tests
- [ ] S5-T3 gRPC contract tests
- [ ] S5-T4 Cross-service E2E happy-path test
- [ ] S5-T5 Wire JaCoCo 80% coverage gate

## Session 6 — Multi-stage Docker + GitHub Actions CI
- [ ] S6-T1 Multi-stage Dockerfile for task-service
- [ ] S6-T2 Multi-stage Dockerfile for notification-service
- [ ] S6-T3 [ask-first] GitHub Actions CI for task-service
- [ ] S6-T4 [ask-first] GitHub Actions CI for notification-service

## Session 7 — Kubernetes (kind) + Flux CD GitOps
- [ ] S7-T1 Kustomize base manifests + kind-config.yaml
- [ ] S7-T2 overlays/dev + manual smoke test on kind
- [ ] S7-T3 [ask-first] Bootstrap Flux CD
- [ ] S7-T4 Rollback demonstration
- [ ] S7-T5 [ask-first] overlays/prod scaffold

## Session 8 — Observability
- [ ] S8-T1 Structured JSON logs → Loki
- [ ] S8-T2 Prometheus scraping both services
- [ ] S8-T3 Grafana dashboards as code
- [ ] S8-T4 Distributed tracing across both services
- [ ] S8-T5 Alertmanager rule + manual trigger demo
