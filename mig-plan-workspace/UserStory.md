# User Stories: Spring Boot → Quarkus Migration

**Project**: todo-demo-jws  
**Branch**: migrate-to-quarkus  
**Generated**: 2026-06-03  
**Total Stories**: 8

---

## User Story 1: Quarkus Project Foundation

**As a:** Platform Engineer  
**I want:** A fully configured Quarkus project with all necessary extensions and build setup  
**So that:** The development team has a solid foundation for migrating the application code

---

### Tasks
- **Task Group 1: Project Setup and Configuration** (from tasks.md)
  - 1.1 Create new Quarkus project with required extensions
  - 1.2 Configure Maven build in pom.xml
  - 1.3 Migrate application.properties configuration
  - 1.4 Set up H2 configuration for dev/test
  - 1.5 Verify Quarkus dev mode works
  - 1.6 Configure Git for new structure

### Acceptance Criteria
- [ ] Quarkus project builds successfully (`mvn clean package` succeeds)
- [ ] Dev mode starts without errors (`mvn quarkus:dev` works)
- [ ] Dev UI accessible at http://localhost:8080/q/dev
- [ ] Health check endpoint returns UP status
- [ ] Database configuration migrated from Spring Boot format to Quarkus format
- [ ] CORS configuration present in application.properties
- [ ] Project committed to migrate-to-quarkus branch

---

### Details
- **Priority:** High (Foundation for all other work)
- **Estimate:** 3 story points (2-3 hours)

---

### Preconditions
- Development machine has Maven, Java 17, and Quarkus CLI installed
- Access to Quarkus project generator (https://code.quarkus.io or CLI)

---

### Steps to Reproduce / Implementation Notes
1. Generate Quarkus project skeleton with extensions: resteasy-reactive-jackson, hibernate-orm-panache, jdbc-postgresql, jdbc-h2, hibernate-validator, smallrye-health
2. Configure pom.xml with project metadata and build settings
3. Create application.properties with Quarkus-format database config and CORS settings
4. Create test-specific application.properties with H2 in-memory database
5. Verify project builds, dev mode works, and health checks pass
6. Commit project structure to Git

---

### Dependencies
- None (first story to implement)

---

### Test Cases
- Test 1: Run `mvn clean package` - expected: BUILD SUCCESS
- Test 2: Run `mvn quarkus:dev` - expected: Application starts on port 8080
- Test 3: Access http://localhost:8080/q/dev - expected: Dev UI loads
- Test 4: Access http://localhost:8080/q/health - expected: {"status":"UP"}

---

### Notes / Comments
- This is the foundation for all migration work - must be solid before proceeding
- Dev mode with live reload significantly improves developer productivity
- CORS configuration in properties (not annotations) is Quarkus best practice

---

## User Story 2: Data Model Migration

**As a:** Backend Developer  
**I want:** The Todo entity migrated to Quarkus with Panache pattern  
**So that:** We have a clean, testable domain model foundation for the application

---

### Tasks
- **Task Group 2: Entity Layer Migration** (from tasks.md)
  - 2.1 Copy Todo entity to Quarkus project
  - 2.2 Modify Todo entity for Panache pattern
  - 2.3 Create entity unit tests
  - 2.4 Test Generation: Use mig-test-gen for comprehensive entity tests
  - 2.5 Documentation: Document entity migration decisions

---

### Acceptance Criteria
- [ ] Todo entity compiles successfully
- [ ] Entity extends PanacheEntity (id field inherited)
- [ ] All fields use public visibility (Panache convention)
- [ ] Jakarta validation annotations present and working (`@NotBlank`, `@Column(unique=true)`)
- [ ] All entity tests pass (validation, field access)
- [ ] Generated tests from mig-test-gen pass
- [ ] Documentation explains Panache entity pattern

---

### Details
- **Priority:** High (Foundation for repository and service layers)
- **Estimate:** 2 story points (1 hour)

---

### Preconditions
- User Story 1 (Project Foundation) completed
- Understand Panache entity pattern (public fields, inherited id)

---

### Steps to Reproduce / Implementation Notes
1. Copy Todo.java from Spring Boot project to Quarkus project (org.acme.todo.model package)
2. Add `extends PanacheEntity` to class declaration
3. Remove `@Id` and `@GeneratedValue` annotations (inherited from PanacheEntity)
4. Remove `private Long id;` field (inherited from PanacheEntity)
5. Change all field visibility from `private` to `public`
6. Remove all getter and setter methods (Panache auto-generates at build time)
7. Create TodoTest.java with validation tests
8. Invoke `/skill mig-test-gen` for comprehensive test generation
9. Verify all tests pass: `mvn test`
10. Document Panache entity pattern in README.md

---

### Dependencies
- User Story 1 (Project Foundation)

---

### Test Cases
- Test 1: Create valid Todo with title "Test" - expected: no validation errors
- Test 2: Create Todo with blank title "" - expected: validation error "@NotBlank"
- Test 3: Access todo.id field - expected: PanacheEntity provides id getter
- Test 4: Run `mvn test` - expected: all entity tests pass

---

### Notes / Comments
- Panache entity pattern uses public fields - this is intentional, not a mistake
- Getters/setters are generated at compile time via bytecode manipulation
- Already using Jakarta annotations (not javax) - no changes needed

---

## User Story 3: Data Access Layer Migration

**As a:** Backend Developer  
**I want:** The TodoRepository migrated to Panache repository pattern with comprehensive tests  
**So that:** Database operations work correctly and are well-tested

---

### Tasks
- **Task Group 3: Repository Layer Migration** (from tasks.md)
  - 3.1 Create TodoRepository with Panache
  - 3.2 Create repository integration tests
  - 3.3 Test PostgreSQL connection (local or OpenShift)
  - 3.4 Test Generation: Use mig-test-gen for repository tests
  - 3.5 Documentation: Document repository pattern

---

### Acceptance Criteria
- [ ] TodoRepository implements PanacheRepository<Todo>
- [ ] Repository annotated with @ApplicationScoped
- [ ] All CRUD operations work: persist(), listAll(), findByIdOptional(), deleteById()
- [ ] Integration tests pass against H2 in-memory database
- [ ] PostgreSQL connection works (local or OpenShift)
- [ ] Generated tests from mig-test-gen pass
- [ ] Documentation explains Panache repository methods

---

### Details
- **Priority:** High (Required for service layer)
- **Estimate:** 2 story points (1 hour)

---

### Preconditions
- User Story 2 (Data Model Migration) completed
- H2 database dependency present in pom.xml for testing
- PostgreSQL available (local or OpenShift) for integration testing

---

### Steps to Reproduce / Implementation Notes
1. Create TodoRepository.java class (not interface)
2. Implement `PanacheRepository<Todo>` interface
3. Add `@ApplicationScoped` annotation
4. No method implementations needed - all CRUD methods inherited from Panache
5. Create TodoRepositoryTest.java with `@QuarkusTest` and `@TestTransaction`
6. Test persist, listAll, findByIdOptional, deleteById, count operations
7. Start local PostgreSQL or connect to OpenShift service
8. Test against PostgreSQL in dev mode
9. Invoke `/skill mig-test-gen` for comprehensive repository tests
10. Document available Panache methods in README.md

---

### Dependencies
- User Story 2 (Data Model Migration)

---

### Test Cases
- Test 1: Persist Todo, verify id assigned - expected: todo.id not null after persist
- Test 2: Persist 2 Todos, call listAll() - expected: returns 2 Todos
- Test 3: Persist Todo, call findByIdOptional(id) - expected: Optional<Todo> present
- Test 4: Delete Todo by ID, verify deleted - expected: findByIdOptional returns empty
- Test 5: Connect to PostgreSQL, persist Todo - expected: record in database

---

### Notes / Comments
- Panache provides all CRUD methods automatically - no need to write any
- `persist()` modifies entity in-place and returns void (unlike Spring Data JPA `save()`)
- `@TestTransaction` ensures test isolation - each test rolls back

---

## User Story 4: Business Logic Migration

**As a:** Backend Developer  
**I want:** The TodoService migrated to Quarkus with proper transaction management  
**So that:** Business logic is correct and transactions are properly scoped

---

### Tasks
- **Task Group 4: Service Layer Migration** (from tasks.md)
  - 4.1 Copy TodoService to Quarkus project
  - 4.2 Migrate TodoService annotations and dependencies
  - 4.3 Create service unit tests (with mocked repository)
  - 4.4 Test transaction behavior (rollback on exception)
  - 4.5 Test Generation: Use mig-test-gen for service tests
  - 4.6 Documentation: Document service layer patterns

---

### Acceptance Criteria
- [ ] TodoService annotated with @ApplicationScoped (not @Service)
- [ ] Dependency injection uses @Inject (not @Autowired)
- [ ] @Transactional annotation on mutating methods (createOrUpdateTodo, deleteTodoById)
- [ ] Repository method calls updated (findAll → listAll, save → persist, etc.)
- [ ] All service unit tests pass (with mocked repository)
- [ ] Transaction rollback test passes
- [ ] Generated tests from mig-test-gen pass
- [ ] Documentation explains transaction management

---

### Details
- **Priority:** High (Required for REST API layer)
- **Estimate:** 2 story points (1 hour)

---

### Preconditions
- User Story 3 (Data Access Layer Migration) completed
- Understand CDI scopes and Jakarta Transactions

---

### Steps to Reproduce / Implementation Notes
1. Copy TodoService.java from Spring Boot project
2. Replace `@Service` with `@ApplicationScoped`
3. Replace `@Autowired` with `@Inject` (or use constructor injection)
4. Add `@Transactional` to createOrUpdateTodo() and deleteTodoById()
5. Update repository method calls:
   - `findAll()` → `listAll()`
   - `findById(id)` → `findByIdOptional(id)`
   - `save(todo)` → `persist(todo)` then `return todo`
   - `deleteById(id)` → `deleteById(id)` (same)
6. Create TodoServiceTest.java with `@InjectMock` for mocking repository
7. Test all service methods with mocked repository responses
8. Test transaction rollback on exception
9. Invoke `/skill mig-test-gen` for comprehensive service tests
10. Document transaction boundaries in README.md

---

### Dependencies
- User Story 3 (Data Access Layer Migration)

---

### Test Cases
- Test 1: Call getAllTodos(), mock returns 2 todos - expected: service returns 2 todos
- Test 2: Call getTodoById(1L), mock returns todo - expected: Optional<Todo> present
- Test 3: Call createOrUpdateTodo(), verify persist() called - expected: repository.persist() invoked
- Test 4: Mock throws exception in persist(), call createOrUpdateTodo() - expected: transaction rolls back

---

### Notes / Comments
- Transaction boundaries moved from controller to service layer (best practice)
- Panache persist() returns void, so service returns the passed-in todo object
- @ApplicationScoped creates one instance per application lifecycle

---

## User Story 5: REST API Migration

**As a:** Backend Developer  
**I want:** The REST API migrated from Spring MVC to JAX-RS with comprehensive tests  
**So that:** The API contract remains identical and frontend continues to work

---

### Tasks
- **Task Group 5: REST API Layer Migration** (from tasks.md)
  - 5.1 Create TodoResource (JAX-RS Resource class)
  - 5.2 Migrate GET /api/todos endpoint (getAllTodos)
  - 5.3 Migrate GET /api/todos/{id} endpoint (getTodoById)
  - 5.4 Migrate POST /api/todos endpoint (createTodo)
  - 5.5 Migrate PUT /api/todos/{id} endpoint (updateTodo)
  - 5.6 Migrate DELETE /api/todos/{id} endpoint (deleteTodoById)
  - 5.7 Remove @CrossOrigin annotation (CORS now in application.properties)
  - 5.8 Create REST API integration tests
  - 5.9 Test CORS headers
  - 5.10 Test Generation: Use mig-test-gen for REST API tests
  - 5.11 Documentation: Document REST API migration

- **Task Group 6: Static Resources Migration** (from tasks.md)
  - 6.1 Create META-INF/resources directory
  - 6.2 Copy Angular frontend files
  - 6.3 Test static resource serving in dev mode
  - 6.4 Test frontend-backend integration
  - 6.5 Documentation: Document static resource serving

---

### Acceptance Criteria
- [ ] TodoResource class created with @Path("/api/todos")
- [ ] All 5 REST endpoints functional (GET all, GET by ID, POST, PUT, DELETE)
- [ ] JAX-RS annotations used (@GET, @POST, @PUT, @DELETE, @PathParam)
- [ ] ResponseEntity replaced with JAX-RS Response
- [ ] @Transactional removed from controller (now in service layer)
- [ ] CORS headers present in responses (configured in application.properties)
- [ ] All REST API tests pass (REST Assured)
- [ ] Angular frontend accessible at root path /
- [ ] Frontend can successfully call backend API
- [ ] Generated tests from mig-test-gen pass
- [ ] Documentation complete (JAX-RS vs Spring MVC comparison)

---

### Details
- **Priority:** High (User-facing API)
- **Estimate:** 5 story points (3-4 hours)

---

### Preconditions
- User Story 4 (Business Logic Migration) completed
- Understand JAX-RS resource classes and annotations
- Angular frontend files available from Spring Boot project

---

### Steps to Reproduce / Implementation Notes
1. Create TodoResource.java in `org.acme.todo.resource` package
2. Add class-level annotations: `@Path("/api/todos")`, `@Produces(JSON)`, `@Consumes(JSON)`
3. Inject TodoService with `@Inject`
4. Migrate each endpoint:
   - Replace Spring annotations with JAX-RS equivalents
   - Replace @PathVariable with @PathParam
   - Remove @RequestBody (automatic in JAX-RS)
   - Replace ResponseEntity with Response builder pattern
5. Remove @CrossOrigin annotation (CORS in application.properties)
6. Create TodoResourceTest.java with REST Assured
7. Test all endpoints: GET, POST, PUT, DELETE, 404 scenarios
8. Test CORS headers in OPTIONS preflight request
9. Copy Angular files from `src/main/resources/static/` to `src/main/resources/META-INF/resources/`
10. Test frontend loads and can call backend API
11. Invoke `/skill mig-test-gen` for comprehensive API tests
12. Document JAX-RS migration in README.md

---

### Dependencies
- User Story 4 (Business Logic Migration)

---

### Test Cases
- Test 1: GET /api/todos - expected: 200 OK with JSON array
- Test 2: POST /api/todos with valid todo - expected: 201 Created with todo in body
- Test 3: GET /api/todos/{id} for existing - expected: 200 OK with todo
- Test 4: GET /api/todos/99999 - expected: 404 Not Found
- Test 5: PUT /api/todos/{id} with update - expected: 200 OK with updated todo
- Test 6: DELETE /api/todos/{id} - expected: 204 No Content
- Test 7: OPTIONS /api/todos with Origin header - expected: CORS headers present
- Test 8: Load frontend at / - expected: Angular app loads
- Test 9: Create todo via frontend UI - expected: appears in list

---

### Notes / Comments
- Biggest migration effort due to annotation changes and response handling
- API contract must remain 100% identical for frontend compatibility
- CORS configuration critical for frontend-backend communication

---

## User Story 6: Comprehensive Testing and Quality Assurance

**As a:** QA Engineer  
**I want:** A comprehensive test suite with high coverage and performance validation  
**So that:** We can confidently deploy Quarkus to production knowing it works correctly

---

### Tasks
- **Task Group 7: Testing and Quality Assurance** (from tasks.md)
  - 7.1 Run full test suite
  - 7.2 Measure test coverage
  - 7.3 Create characterization tests (compare Spring Boot vs Quarkus)
  - 7.4 Performance baseline testing
  - 7.5 Native compilation test (optional)
  - 7.6 Documentation: Document testing approach

---

### Acceptance Criteria
- [ ] All tests pass (100% passing rate)
- [ ] Code coverage >80% line coverage, >70% branch coverage
- [ ] Characterization tests confirm Quarkus responses match Spring Boot exactly
- [ ] Performance tests show Quarkus startup <1 second (vs Spring Boot 3-5 seconds)
- [ ] Performance tests show Quarkus memory <150 MB (vs Spring Boot 200-300 MB)
- [ ] Throughput and latency equal to or better than Spring Boot
- [ ] Native compilation succeeds (optional)
- [ ] Testing documentation complete

---

### Details
- **Priority:** High (Required before deployment)
- **Estimate:** 5 story points (2-4 hours)

---

### Preconditions
- User Story 5 (REST API Migration) completed
- Spring Boot application available for comparison testing
- Apache Bench (ab) or similar load testing tool installed

---

### Steps to Reproduce / Implementation Notes
1. Run full test suite: `mvn clean test`
2. Verify all tests pass (entity, repository, service, resource)
3. Generate coverage report: `mvn clean verify`
4. Review coverage in `target/site/jacoco/index.html`, ensure >80%
5. Run Spring Boot app locally, record API responses for all endpoints
6. Run Quarkus app, execute same API calls, compare responses (should be identical)
7. Measure Spring Boot performance: startup time, memory (RSS), throughput (ab), latency
8. Measure Quarkus performance: same metrics
9. Compare results: Quarkus should be faster startup, lower memory, equal/better throughput
10. (Optional) Build native executable: `mvn package -Pnative`, test performance
11. Document testing approach in README.md

---

### Dependencies
- User Story 5 (REST API Migration)

---

### Test Cases
- Test 1: Run `mvn test` - expected: all tests pass
- Test 2: Check coverage report - expected: >80% line coverage
- Test 3: Compare API response: Spring vs Quarkus - expected: identical JSON
- Test 4: Measure Quarkus startup - expected: <1 second
- Test 5: Measure Quarkus memory - expected: <150 MB RSS
- Test 6: Load test with ab -n 1000 -c 10 - expected: throughput >= Spring Boot
- Test 7: Build native executable - expected: BUILD SUCCESS, binary <100ms startup

---

### Notes / Comments
- Characterization tests critical to ensure functional equivalence
- Performance improvements (faster startup, less memory) are key migration benefits
- Native compilation is optional but highly recommended for production

---

## User Story 7: Containerization and OpenShift Deployment

**As a:** DevOps Engineer  
**I want:** Quarkus application containerized and deployed to OpenShift with health checks and monitoring  
**So that:** The application runs in production with high availability and observability

---

### Tasks
- **Task Group 8: Containerization** (from tasks.md)
  - 8.1 Containerization: Use mig-containerize for Dockerfile generation
  - 8.2 Build JVM container image
  - 8.3 Test container image locally
  - 8.4 Security scan container image
  - 8.5 Push image to container registry
  - 8.6 Documentation: Document containerization

- **Task Group 9: OpenShift Deployment** (from tasks.md)
  - 9.1 Determine current OpenShift namespace
  - 9.2 Deployment: Use mig-deploy for Kubernetes manifest generation
  - 9.3 Create ConfigMap for application configuration (optional)
  - 9.4 Deploy to OpenShift
  - 9.5 Create and expose Service
  - 9.6 Create OpenShift Route for external access
  - 9.7 Validate health checks
  - 9.8 Validate database connectivity
  - 9.9 Test high availability (2 replicas)
  - 9.10 Documentation: Document OpenShift deployment

---

### Acceptance Criteria
- [ ] JVM container image builds successfully
- [ ] Container image security scan passes (no critical vulnerabilities)
- [ ] Container runs locally with PostgreSQL connection working
- [ ] Container image pushed to Quay.io registry
- [ ] Kubernetes manifests generated (Deployment, Service, Route, ConfigMap)
- [ ] Application deployed to OpenShift with 2 replicas
- [ ] Health checks passing (liveness and readiness probes)
- [ ] Application accessible via HTTPS route
- [ ] Database operations work in OpenShift
- [ ] High availability confirmed (pod restart doesn't cause downtime)
- [ ] Documentation complete (build, deployment, troubleshooting)

---

### Details
- **Priority:** High (Required for production deployment)
- **Estimate:** 8 story points (4-6 hours)

---

### Preconditions
- User Story 6 (Testing and Quality Assurance) completed
- Access to OpenShift cluster and namespace
- Existing PostgreSQL service (todos-database) in OpenShift
- Access to Quay.io or OpenShift internal registry

---

### Steps to Reproduce / Implementation Notes

**Containerization:**
1. Invoke `/skill mig-containerize` to generate Dockerfiles
2. Build JVM image: `mvn clean package && podman build -f src/main/docker/Dockerfile.jvm -t quay.io/${USER}/todo-demo-quarkus:latest .`
3. Test locally: `podman run -p 8080:8080 -e QUARKUS_DATASOURCE_JDBC_URL=... quay.io/${USER}/todo-demo-quarkus:latest`
4. Security scan: `trivy image quay.io/${USER}/todo-demo-quarkus:latest`
5. Push to registry: `podman push quay.io/${USER}/todo-demo-quarkus:latest`

**OpenShift Deployment:**
1. Determine namespace: `oc project`
2. Invoke `/skill mig-deploy` to generate Kubernetes manifests
3. (Optional) Create ConfigMap: `oc apply -f k8s/configmap.yaml`
4. Deploy: `oc apply -f k8s/deployment.yaml`
5. Monitor: `oc rollout status deployment/todo-app-quarkus`
6. Create service: `oc apply -f k8s/service.yaml`
7. Create route: `oc apply -f k8s/route.yaml`
8. Get route URL: `oc get route todo-app-quarkus -o jsonpath='{.spec.host}'`
9. Test health: `curl https://$ROUTE_URL/q/health`
10. Test API: `curl https://$ROUTE_URL/api/todos`
11. Test frontend: Open `https://$ROUTE_URL/` in browser
12. Document deployment in README.md

---

### Dependencies
- User Story 6 (Testing and Quality Assurance)

---

### Test Cases
- Test 1: Build container image - expected: BUILD SUCCESS
- Test 2: Security scan - expected: no critical vulnerabilities
- Test 3: Run container locally - expected: app starts, health returns UP
- Test 4: Push to registry - expected: image visible at quay.io
- Test 5: Deploy to OpenShift - expected: 2 pods running and Ready
- Test 6: Access health endpoint - expected: {"status":"UP","checks":[...]}
- Test 7: Create todo via API - expected: 201 Created, record in database
- Test 8: Delete 1 pod - expected: OpenShift creates replacement, app stays accessible

---

### Notes / Comments
- Multi-stage Dockerfile reduces image size significantly
- Red Hat UBI images ensure security and support
- 2 replicas provide high availability and zero-downtime deployments
- Health probes (liveness/readiness) critical for Kubernetes orchestration

---

## User Story 8: Production Cutover and Validation

**As a:** Platform Engineer  
**I want:** Traffic switched from Spring Boot to Quarkus with monitoring and rollback capability  
**So that:** Users experience zero downtime and improved performance

---

### Tasks
- **Task Group 10: Migration Validation and Cutover** (from tasks.md)
  - 10.1 Compare Spring Boot and Quarkus deployments
  - 10.2 Performance comparison in OpenShift
  - 10.3 Execute end-to-end smoke tests
  - 10.4 Soak test (extended load)
  - 10.5 Plan cutover to Quarkus
  - 10.6 Execute cutover (switch route)
  - 10.7 Monitor post-cutover (24-48 hours)
  - 10.8 Decommission Spring Boot deployment (after monitoring period)

- **Task Group 11: Documentation and Knowledge Transfer** (from tasks.md)
  - 11.1 Update README.md with final documentation
  - 11.2 Generate API documentation (OpenAPI)
  - 11.3 Create migration retrospective document
  - 11.4 Archive migration artifacts
  - 11.5 Knowledge transfer (if team-based)
  - 11.6 Update project dependencies
  - 11.7 Final code cleanup
  - 11.8 Create git tag for final migration

---

### Acceptance Criteria
- [ ] Functional equivalence confirmed (Quarkus responses match Spring Boot)
- [ ] Performance improvements validated (faster startup, lower memory)
- [ ] End-to-end smoke tests pass (all user journeys work)
- [ ] Soak test passes (extended load with no errors or memory leaks)
- [ ] Cutover procedure documented and approved
- [ ] Traffic switched to Quarkus successfully (route pointing to Quarkus service)
- [ ] 24-48 hours of stable operation in production
- [ ] Spring Boot deployment decommissioned (after monitoring period)
- [ ] README.md updated with complete Quarkus documentation
- [ ] OpenAPI documentation generated and accessible
- [ ] Migration retrospective complete
- [ ] All artifacts archived in docs/migration/
- [ ] Team familiar with Quarkus codebase (if team-based)
- [ ] Git tag created: quarkus-v1.0.0

---

### Details
- **Priority:** High (Final production deployment)
- **Estimate:** 8 story points (3-5 hours active work + 24-48 hours monitoring)

---

### Preconditions
- User Story 7 (Containerization and OpenShift Deployment) completed
- Both Spring Boot and Quarkus deployments running in OpenShift
- Stakeholders notified of upcoming cutover (if applicable)

---

### Steps to Reproduce / Implementation Notes

**Validation:**
1. Get route URLs for Spring Boot and Quarkus
2. Test same API calls against both, compare responses (should be identical)
3. Performance comparison: ab -n 1000 -c 10 against both, compare metrics
4. Execute end-to-end smoke tests via frontend UI (create, read, update, delete todos)
5. Soak test: ab -n 10000 -c 20 -t 300 against Quarkus, monitor for errors/leaks

**Cutover:**
1. Document cutover procedure and rollback steps
2. Verify Quarkus health checks passing
3. Switch route to Quarkus: `oc patch route jws-app -p '{"spec":{"to":{"name":"todo-app-quarkus"}}}'`
4. Verify route now serves from Quarkus backend
5. Monitor logs: `oc logs -f deployment/todo-app-quarkus`
6. Monitor health checks in OpenShift console
7. If issues, rollback: `oc patch route jws-app -p '{"spec":{"to":{"name":"jws-app"}}}'`

**Post-Cutover:**
1. Monitor for 24-48 hours (logs, health, user reports, database)
2. After stable operation, decommission Spring Boot: `oc delete deployment jws-app`
3. Archive Spring Boot code: `git tag spring-boot-final`
4. Keep Spring Boot image in registry for 30 days (rollback option)

**Documentation:**
1. Update README.md with complete Quarkus documentation
2. Generate OpenAPI spec: add quarkus-smallrye-openapi extension
3. Create MIGRATION.md retrospective document
4. Archive all migration artifacts to docs/migration/
5. (If team) Schedule knowledge transfer meeting and walkthrough
6. Update dependencies: `mvn versions:display-dependency-updates`
7. Code cleanup: format, remove unused imports, remove comments
8. Commit and tag: `git tag -a quarkus-v1.0.0 -m "Quarkus migration complete"`

---

### Dependencies
- User Story 7 (Containerization and OpenShift Deployment)

---

### Test Cases
- Test 1: Compare API responses (Spring vs Quarkus) - expected: identical JSON
- Test 2: Performance test Quarkus - expected: faster startup, lower memory than Spring
- Test 3: Smoke test via UI - expected: all CRUD operations work
- Test 4: Soak test 10000 requests - expected: no errors, stable memory
- Test 5: Switch route - expected: route serves from Quarkus, no downtime
- Test 6: Monitor 48 hours - expected: stable operation, no issues
- Test 7: Decommission Spring Boot - expected: only Quarkus deployment remains

---

### Notes / Comments
- Cutover uses route switching for zero downtime (blue/green deployment)
- 24-48 hour monitoring period provides confidence before decommissioning Spring Boot
- Rollback capability preserved for 30 days (Spring Boot image and code tagged)
- Migration retrospective captures learnings for future migrations

---

## Summary

**Total User Stories**: 8  
**Total Story Points**: 33 (approximately 14-21 hours of active development work)

**Story Sequencing** (in order of implementation):
1. Quarkus Project Foundation (3 points)
2. Data Model Migration (2 points)
3. Data Access Layer Migration (2 points)
4. Business Logic Migration (2 points)
5. REST API Migration (5 points)
6. Comprehensive Testing and Quality Assurance (5 points)
7. Containerization and OpenShift Deployment (8 points)
8. Production Cutover and Validation (8 points)

**Critical Path**:
Stories 1→2→3→4→5 are sequential (each depends on the previous)  
Stories 6→7→8 build on story 5 completion

**Value Delivery**:
- Stories 1-5: Enable development (technical foundation)
- Story 6: Ensure quality (comprehensive testing)
- Story 7: Enable deployment (containerization and OpenShift)
- Story 8: Deliver value to production (cutover and validation)

**Risk Mitigation**:
- Each story has clear acceptance criteria
- Comprehensive testing throughout (not just at the end)
- Integration skills invoked for automation (mig-test-gen, mig-containerize, mig-deploy)
- Blue/green deployment with rollback capability
- Extended monitoring period before decommissioning Spring Boot

---

**End of User Stories**
