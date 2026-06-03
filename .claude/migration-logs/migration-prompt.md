# Migration Prompt: Spring Boot → Quarkus

**Generated**: 2026-06-03  
**Project**: todo-demo-jws  
**Branch**: migrate-to-quarkus

---

## Current Application Summary

This is a Spring Boot 3.2.5 application implementing a TODO management system with a classic 3-layer architecture. The backend consists of only 6 Java files providing a REST API for CRUD operations on TODO items, with JPA/Hibernate persistence to PostgreSQL. The frontend is a pre-built Angular TodoMVC application served as static resources. The application currently deploys as a WAR file to JBoss Web Server (Tomcat) on OpenShift.

Knowledge graph analysis reveals a clean, well-structured codebase with zero circular dependencies and a cohesive backend community (Community 6: TodoController, TodoService, Todo, TodoRepository). The application has been designed with separation of concerns: Controller → Service → Repository → Entity, with minimal coupling between layers.

**Technology Stack:**
- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Persistence**: Spring Data JPA with Hibernate
- **Database**: PostgreSQL (runtime), H2 (dev/test)
- **REST API**: Spring Web with Spring MVC annotations
- **Validation**: Jakarta Validation API (Bean Validation)
- **Monitoring**: Spring Boot Actuator
- **Packaging**: WAR (for JBoss Web Server deployment)
- **Build**: Maven
- **Frontend**: Pre-built Angular TodoMVC (static resources)

**Architecture:**
- **Pattern**: Monolithic 3-layer REST API
- **Controller Layer**: TodoController - 5 REST endpoints (GET all, GET by ID, POST, PUT, DELETE)
- **Service Layer**: TodoService - business logic and transaction coordination
- **Repository Layer**: TodoRepository - Spring Data JPA interface (no custom queries)
- **Entity Layer**: Todo - JPA entity with Jakarta annotations
- **Frontend**: Static Angular resources served from classpath

**Scale:**
- **Backend Code**: 6 Java source files (~500 LOC)
- **Total Project**: 13 files (backend + frontend resources)
- **Dependencies**: 3522 edges in knowledge graph (mostly Angular frontend)
- **REST Endpoints**: 5 (all CRUD operations)
- **JPA Entities**: 1 (Todo)
- **Database Tables**: 1 (todos)

**Current Deployment:**
- **Platform**: Red Hat OpenShift
- **Application Server**: JBoss Web Server 6 (Tomcat-based)
- **Container Image**: registry.redhat.io/jboss-webserver-6/jws60-openjdk17-openshift-rhel8
- **Database**: PostgreSQL deployed via `oc new-app openshift/postgresql:latest`

---

## Target Platform and Technologies

**Target Stack:**
- **Framework**: Quarkus 3.x (latest stable)
- **Language**: Java 17 (maintain current version)
- **Persistence**: Hibernate ORM with Panache (Quarkus extension)
- **Database**: PostgreSQL (same as current)
- **REST API**: RESTEasy Reactive with JAX-RS annotations
- **Validation**: Hibernate Validator (Jakarta Validation)
- **Monitoring**: SmallRye Health (Quarkus extension)
- **Packaging**: JVM JAR (or optionally native binary for faster startup)
- **Build**: Maven with Quarkus plugin

**Migration Rationale:**
- **Performance**: Quarkus offers faster startup (milliseconds vs seconds) and lower memory footprint (~70% reduction)
- **Cloud-Native**: Built for containers and Kubernetes from the ground up
- **Developer Experience**: Live reload, unified configuration, extensive extensions ecosystem
- **Standards-Based**: Implements Jakarta EE and MicroProfile specifications (minimal vendor lock-in)
- **Red Hat Support**: Enterprise support available, strong OpenShift integration
- **Modern Java**: Embraces reactive programming, GraalVM native compilation

**Deployment Platform:**
- **Platform**: Red Hat OpenShift 4.x (current namespace: to be determined)
- **Container Runtime**: Podman (or Docker)
- **Orchestration**: Kubernetes on OpenShift
- **CI/CD**: OpenShift Pipelines (Tekton) or existing pipeline integration
- **Registry**: OpenShift internal registry or Quay.io

**Infrastructure Requirements:**
- **Container Images**: Red Hat Universal Base Image (UBI) for Quarkus
- **Database**: Existing PostgreSQL service (todos-database) - reuse current deployment
- **Persistent Storage**: None required (stateless application)
- **External Dependencies**: None (self-contained application)

---

## Migration Approach

**Strategy:** Big Bang Migration (all components at once)

**Reasoning:**
1. **Small Codebase**: Only 6 Java files makes big bang feasible in 1-2 days
2. **No Complex Features**: No Spring Security, Cloud, Batch, or other complex Spring features that would require careful phased migration
3. **Clean Architecture**: Zero circular dependencies allows straightforward layer-by-layer migration
4. **Low Risk**: Migration complexity assessed as LOW with high confidence (98% extracted edges in knowledge graph)
5. **Direct Equivalents**: All Spring Boot dependencies have 1:1 Quarkus equivalents
6. **Team Expertise**: Team is experienced with Quarkus, reducing learning curve risk
7. **Timeline**: ASAP requirement (1-2 days) favors big bang over phased approach overhead

**Migration Sequence:**
1. **Project Setup** (2-3 hours): Create Quarkus project, configure extensions, set up build
2. **Entity Layer** (1 hour): Migrate Todo entity (minimal changes - already Jakarta-compliant)
3. **Repository Layer** (1 hour): Convert to Panache repository pattern or maintain JPA interface
4. **Service Layer** (1 hour): Replace @Service with @ApplicationScoped, switch to @Inject from @Autowired
5. **Controller Layer** (2-3 hours): Convert Spring MVC to JAX-RS annotations, replace ResponseEntity pattern
6. **Configuration** (1 hour): Migrate application.properties to Quarkus property format
7. **Static Resources** (30 min): Move Angular frontend to META-INF/resources
8. **Testing** (2-4 hours): Add Quarkus tests, validate all endpoints, integration testing
9. **Containerization** (2-3 hours): Build container image, test locally
10. **OpenShift Deployment** (2-3 hours): Deploy to current namespace, configure routes, validate

**Total Estimated Duration**: 12-20 hours of development work (1.5-2.5 days)

**Key Considerations:**
- **Transaction Management**: Move `@Transactional` from controller to service layer (Quarkus best practice)
- **Dependency Injection**: Replace field injection (`@Autowired`) with constructor injection (`@Inject`)
- **Response Handling**: Replace `ResponseEntity<T>` with direct returns or JAX-RS `Response`
- **CORS Configuration**: Move from `@CrossOrigin` annotation to `application.properties`
- **Static Resources**: Copy from `src/main/resources/static/` to `src/main/resources/META-INF/resources/`
- **Database Connection**: Verify PostgreSQL datasource configuration in Quarkus format
- **Packaging**: Switch from WAR to JAR (Quarkus embeds HTTP server)

---

## Required Deliverables

### 1. Containerization

**Requirements:**
- All components must be containerized using Podman (or Docker)
- Use Red Hat Universal Base Image (UBI) as base: `registry.access.redhat.com/ubi9/openjdk-17`
- Multi-stage builds for optimized image sizes (separate build and runtime stages)
- Container images must pass security scanning (no critical vulnerabilities)
- Minimize image layers and use .dockerignore to exclude unnecessary files

**Deliverables:**
- `src/main/docker/Dockerfile.jvm` - JVM mode container image
- `src/main/docker/Dockerfile.native` - Native mode container image (optional but recommended for production)
- `.dockerignore` file to optimize build context
- Container image pushed to Quay.io or OpenShift internal registry
- Image tag strategy: `quay.io/${USER}/todo-demo-quarkus:latest` and semantic versions

**Best Practices:**
- Run container as non-root user (Quarkus default)
- Set resource limits (memory, CPU) in Dockerfile
- Include health check commands
- Use immutable image tags for production deployments
- Layer caching optimization for faster rebuilds

### 2. OpenShift Deployment

**Requirements:**
- Kubernetes manifests for all components (Deployment, Service, Route)
- Deployment strategy: Rolling update with zero downtime
- Health checks: Liveness and readiness probes configured
- Resource management: CPU/memory requests and limits defined
- ConfigMaps for application configuration (database URL, application properties)
- Secrets for sensitive data (database credentials)
- Horizontal Pod Autoscaler (HPA) for production scalability (optional)

**Deliverables:**
- `k8s/deployment.yaml` - Quarkus application deployment manifest
- `k8s/service.yaml` - Service to expose application pods
- `k8s/route.yaml` - OpenShift route for external access
- `k8s/configmap.yaml` - Application configuration
- `k8s/secret.yaml` - Database credentials (reference existing todos-database secret)
- Deployment instructions in README.md

**OpenShift Configuration:**
- **Namespace**: Deploy to current namespace (reuse existing todos-database service)
- **Service Name**: `todo-app-quarkus`
- **Route**: Publicly accessible URL via OpenShift router
- **Replicas**: 2 (for high availability)
- **Health Endpoints**:
  - Liveness: `/q/health/live`
  - Readiness: `/q/health/ready`
- **Resource Limits**:
  - Memory: 512Mi request, 1Gi limit
  - CPU: 250m request, 500m limit

**Database Integration:**
- Reuse existing `todos-database` PostgreSQL service
- Connect via Kubernetes service DNS: `todos-database:5432`
- Database name: `todos`
- Credentials: Inject from ConfigMap/Secret

### 3. Test Coverage

**Characterization Tests** (Capture current behavior before migration):
- **API Contract Tests**: All 5 REST endpoints with expected request/response payloads
  - `GET /api/todos` - Returns list of all todos
  - `GET /api/todos/{id}` - Returns single todo or 404
  - `POST /api/todos` - Creates new todo, returns 201 with created entity
  - `PUT /api/todos/{id}` - Updates existing todo or 404
  - `DELETE /api/todos/{id}` - Deletes todo, returns 204 No Content
- **Data Validation Tests**: Test Jakarta validation constraints on Todo entity
  - `@NotBlank` on title field
  - Unique constraint on title column
- **Database Integration**: Verify JPA entity mappings and persistence operations
- **CORS Behavior**: Verify cross-origin requests are handled correctly
- **Transaction Behavior**: Verify rollback on exceptions

**Unit Tests** (Minimum 80% coverage for migrated code):
- **Entity Tests**: Todo entity getters/setters, validation
- **Repository Tests**: All CRUD operations against in-memory H2 database
- **Service Tests**: Business logic with mocked repository
- **Controller Tests**: REST endpoint behavior with mocked service

**Integration Tests** (`@QuarkusTest` annotation):
- **Full Stack API Tests**: Using REST Assured library
  - Test all endpoints against running Quarkus application
  - Test database persistence (use `@TestTransaction` for isolation)
  - Test error handling (404, 400, 500 scenarios)
- **Database Migration Tests**: Verify schema creation and data integrity
- **Health Check Tests**: Verify `/q/health` endpoints return correct status

**End-to-End Tests** (Complete business workflows):
- Create multiple todos → List all → Update one → Delete one → Verify final state
- Test concurrent requests (multiple users creating todos simultaneously)
- Test frontend integration (Angular app successfully calls backend API)

**Performance Tests** (Baseline and target metrics):
- **Startup Time**:
  - Spring Boot baseline: ~3-5 seconds
  - Quarkus JVM target: <1 second
  - Quarkus native target: <0.1 seconds
- **Memory Footprint**:
  - Spring Boot baseline: ~200-300 MB RSS
  - Quarkus JVM target: ~100-150 MB RSS
  - Quarkus native target: ~30-50 MB RSS
- **Request Throughput**: Maintain or exceed Spring Boot throughput (measure with Apache Bench or similar)
- **Response Time**: P50, P95, P99 latency for all endpoints (should be similar or better)

**Test Deliverables:**
- `src/test/java/` - All unit and integration tests
- Test coverage report (JaCoCo) - minimum 80% line coverage
- Performance test results comparing Spring Boot vs Quarkus
- Test execution documentation in README.md

### 4. Detailed Migration Plan

**Requirements:**
- Phase-by-phase breakdown with estimated durations
- Task lists with clear ownership and dependencies
- Risk assessment for each phase with mitigation strategies
- Rollback procedures for each deployment phase
- Monitoring and observability setup (logs, metrics, traces)
- Documentation requirements (README, API docs, deployment guides)
- Training plan for team (if needed - may skip given Quarkus expertise)

**Plan Structure:**
1. **Specification Document** (`spec.md`):
   - Current state analysis (Spring Boot architecture, dependencies, deployment)
   - Target state definition (Quarkus architecture, extensions, deployment)
   - Gap analysis (what changes between current and target)
   - Migration scope and out-of-scope items

2. **Design Document** (`design.md`):
   - Quarkus project structure
   - Extension selection and configuration
   - Database access pattern (Panache vs traditional JPA)
   - REST API design (JAX-RS resource classes)
   - Configuration management strategy
   - Container and OpenShift architecture

3. **Task Breakdown** (`tasks.md`):
   - Hierarchical task list with checkboxes
   - Task groupings by layer or component
   - Dependencies between tasks clearly marked
   - Time estimates for each task
   - Owner assignments (if team-based)
   - Acceptance criteria for each task

4. **User Stories** (`UserStory.md`):
   - Business-focused user stories mapping technical work to value
   - Acceptance criteria for each story
   - Story point estimates
   - Dependencies and priority ordering

**Monitoring & Observability:**
- **Logging**: Configure Quarkus logging to output JSON format for log aggregation
- **Metrics**: Enable Micrometer metrics for Prometheus scraping
- **Health Checks**: SmallRye Health endpoints for Kubernetes liveness/readiness
- **Tracing**: (Optional) OpenTelemetry for distributed tracing

**Rollback Procedures:**
- Keep Spring Boot deployment running during initial Quarkus testing
- Blue/Green deployment strategy: new Quarkus deployment alongside existing Spring Boot
- Route switch: OpenShift route can quickly revert to Spring Boot service if issues arise
- Database compatibility: Ensure schema changes (if any) are backward compatible
- Automated rollback trigger: If health checks fail for >5 minutes, auto-rollback

---

## Success Criteria

**Functional Success:**
- ✅ All 5 REST API endpoints functional and passing tests
- ✅ Database operations (CRUD) working correctly against PostgreSQL
- ✅ Angular frontend served successfully and able to interact with backend API
- ✅ CORS configuration allows frontend cross-origin requests
- ✅ Data validation (Jakarta Validation) working as expected

**Non-Functional Success:**
- ✅ Application starts in <1 second (JVM mode) or <100ms (native mode)
- ✅ Memory footprint <150 MB RSS (JVM mode) or <50 MB RSS (native mode)
- ✅ Zero downtime during deployment cutover
- ✅ Health checks (liveness/readiness) pass consistently
- ✅ All tests passing with >80% code coverage

**Deployment Success:**
- ✅ Container image built and pushed to registry
- ✅ Application deployed to OpenShift current namespace
- ✅ Accessible via OpenShift route (HTTPS)
- ✅ Database connection working (using existing todos-database service)
- ✅ Logs visible in OpenShift console
- ✅ Metrics exposed and scrapeable

**Documentation Success:**
- ✅ README.md updated with Quarkus build and deployment instructions
- ✅ API documentation (OpenAPI spec) generated and accessible
- ✅ Migration report documenting what changed and why
- ✅ Rollback procedure documented

**Team Success:**
- ✅ Team confident in Quarkus codebase (given existing Quarkus expertise)
- ✅ No blockers or unresolved issues
- ✅ Knowledge transfer complete (if new team members involved)

---

## Constraints and Risks

### Constraints

**Timeline Constraints:**
- **Hard Deadline**: Complete migration within 1-2 days (ASAP requirement)
- **Reason**: Business need for rapid modernization
- **Impact**: Limits scope for exploratory work or advanced optimization

**Technical Constraints:**
- **Database**: Must reuse existing PostgreSQL service (todos-database) - no database migration
- **Namespace**: Deploy to current OpenShift namespace - no new project creation
- **Java Version**: Maintain Java 17 - no upgrade to Java 21+ in this migration
- **API Contract**: REST API must remain compatible (frontend expects same endpoints/payloads)

**Resource Constraints:**
- **Team Size**: Unknown (assuming small team given small codebase)
- **OpenShift Resources**: Shared namespace - coordinate resource limits to avoid conflicts

### Risks

**High Risk (Likelihood: Low, Impact: High):**

**Risk 1: Database Connection Issues**
- **Description**: Quarkus datasource configuration differs from Spring Boot auto-configuration
- **Likelihood**: Low (standard PostgreSQL JDBC, well-documented)
- **Impact**: High (blocks all CRUD operations)
- **Mitigation**:
  - Test database connection early in migration (Phase 2)
  - Validate connection pool settings match Spring Boot behavior
  - Use Quarkus dev mode to test against actual PostgreSQL (not just H2)
- **Rollback**: Revert to Spring Boot deployment if DB issues persist

**Risk 2: OpenShift Deployment Failure**
- **Description**: Container image fails to deploy due to permissions, resource limits, or configuration errors
- **Likelihood**: Low (experienced team, standard OpenShift deployment)
- **Impact**: High (application not accessible)
- **Mitigation**:
  - Test container image locally with Podman before pushing to registry
  - Validate Kubernetes manifests with `oc apply --dry-run`
  - Deploy to non-production namespace first if available
  - Keep Spring Boot deployment running during initial rollout
- **Rollback**: Delete Quarkus deployment, revert route to Spring Boot service

**Medium Risk (Likelihood: Medium, Impact: Medium):**

**Risk 3: Frontend Integration Issues**
- **Description**: Angular frontend cannot reach Quarkus backend due to CORS misconfiguration or API contract changes
- **Likelihood**: Medium (CORS configuration is a common migration pain point)
- **Impact**: Medium (frontend unusable, but backend still functional)
- **Mitigation**:
  - Characterization tests capture current CORS behavior
  - Test CORS configuration in Quarkus dev mode before deployment
  - Configure `quarkus.http.cors=true` and validate allowed origins
- **Rollback**: Update CORS configuration via ConfigMap without redeployment

**Risk 4: Performance Regression**
- **Description**: Quarkus application performs worse than Spring Boot (unexpected given Quarkus performance advantages)
- **Likelihood**: Low (Quarkus typically faster, but configuration matters)
- **Impact**: Medium (business impact if latency increases significantly)
- **Mitigation**:
  - Baseline Spring Boot performance before migration
  - Performance test each phase (entity, repository, service, controller)
  - Use Quarkus performance tuning guide (connection pools, thread pools)
  - Consider native compilation if JVM mode underperforms
- **Rollback**: Revert to Spring Boot if performance gap is unacceptable

**Low Risk (Likelihood: Low, Impact: Low):**

**Risk 5: Test Coverage Gaps**
- **Description**: Migrated tests don't catch regression due to framework differences
- **Likelihood**: Medium (test framework changes: JUnit/Mockito → JUnit/RestAssured)
- **Impact**: Low (can be addressed post-deployment)
- **Mitigation**:
  - Run Spring Boot tests before migration to establish baseline
  - Map each Spring Boot test to Quarkus equivalent
  - Add integration tests for critical paths
  - Manual testing of all endpoints before final deployment
- **Rollback**: Not applicable (fix tests in-place)

**Risk 6: Missing Quarkus Extension**
- **Description**: A Spring Boot feature doesn't have a direct Quarkus equivalent
- **Likelihood**: Very Low (simple CRUD app uses only standard features)
- **Impact**: Low (workarounds available for most scenarios)
- **Mitigation**:
  - Review Quarkus extension catalog early: https://quarkus.io/extensions/
  - All current Spring dependencies have known Quarkus equivalents
  - Fallback: Use standard Jakarta EE or MicroProfile APIs directly
- **Rollback**: Not applicable (find alternative extension or implementation)

### Risk Summary

**Overall Risk Level**: **LOW**

**Reasoning**:
- Small, well-understood codebase
- No complex Spring features to migrate
- Experienced team with Quarkus knowledge
- All Spring dependencies have direct Quarkus equivalents
- Clean architecture with zero circular dependencies
- Comprehensive testing strategy to catch issues early

---

## Request

**Create a comprehensive migration plan following the above requirements.**

The plan should include:

1. **Specification Document** (`spec.md`):
   - Detailed analysis of current Spring Boot application (architecture, dependencies, components)
   - Complete definition of target Quarkus application (extensions, architecture, deployment)
   - Comprehensive gap analysis identifying all changes required
   - Clear scope definition (what's in scope, what's out of scope)

2. **Design Document** (`design.md`):
   - Quarkus project structure and organization
   - Extension selection with justification for each
   - Database access pattern decision (Panache vs traditional JPA)
   - REST API resource class design (JAX-RS annotations and patterns)
   - Configuration management approach
   - Container architecture (Dockerfile strategy, image optimization)
   - OpenShift deployment architecture (manifests, services, routes, ConfigMaps, Secrets)

3. **Task Breakdown** (`tasks.md`):
   - Hierarchical task list organized by migration phase (setup, entity, repository, service, controller, config, deployment)
   - Each task with clear acceptance criteria and checkbox for tracking completion
   - Dependencies between tasks explicitly marked
   - Time estimates for each task
   - Total project timeline calculation
   - Critical path identification

4. **User Stories** (`UserStory.md`):
   - User stories mapping technical work to business value
   - Stories should be independently deployable where possible
   - Each story with acceptance criteria, story points, and priority
   - Dependencies between stories clearly indicated
   - Stories suitable for tracking in agile project management tools

**The plan must be immediately actionable by the development team** with clear step-by-step instructions, all necessary code changes identified, and validation steps for each phase.

**The plan must integrate with migration ecosystem skills**:
- Invoke `mig-test-gen` skill for comprehensive test generation based on graphify analysis
- Invoke `mig-containerize` skill for Dockerfile generation and container optimization
- Invoke `mig-deploy` skill for OpenShift deployment manifest generation and validation

**Timeline**: Complete all planning documents within 1-2 hours, enabling immediate start of 1-2 day migration implementation.
