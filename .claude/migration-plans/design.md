# Migration Design: Spring Boot → Quarkus

**Project**: todo-demo-jws  
**Branch**: migrate-to-quarkus  
**Generated**: 2026-06-03  
**Design Version**: 1.0

---

## Architecture Overview

### Target Architecture

**Pattern**: 3-layer monolithic REST API (unchanged from current)

```
┌─────────────────────────────────────────────────────────────┐
│                     Angular Frontend                        │
│                    (Static Resources)                       │
│              src/main/resources/META-INF/resources/         │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/REST
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Quarkus Application                      │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │              REST Layer (JAX-RS)                      │ │
│  │                  TodoResource                         │ │
│  │  @Path("/api/todos")                                  │ │
│  │  - GET /api/todos                                     │ │
│  │  - GET /api/todos/{id}                                │ │
│  │  - POST /api/todos                                    │ │
│  │  - PUT /api/todos/{id}                                │ │
│  │  - DELETE /api/todos/{id}                             │ │
│  └───────────────────┬───────────────────────────────────┘ │
│                      │ @Inject                             │
│                      ▼                                      │
│  ┌───────────────────────────────────────────────────────┐ │
│  │           Business Logic Layer (CDI)                  │ │
│  │                  TodoService                          │ │
│  │  @ApplicationScoped                                   │ │
│  │  - getAllTodos()                                      │ │
│  │  - getTodoById(id)                                    │ │
│  │  - @Transactional createOrUpdateTodo(todo)           │ │
│  │  - @Transactional deleteTodoById(id)                 │ │
│  └───────────────────┬───────────────────────────────────┘ │
│                      │ @Inject                             │
│                      ▼                                      │
│  ┌───────────────────────────────────────────────────────┐ │
│  │         Data Access Layer (Panache)                   │ │
│  │                TodoRepository                         │ │
│  │  implements PanacheRepository<Todo>                   │ │
│  │  - listAll()                                          │ │
│  │  - findByIdOptional(id)                               │ │
│  │  - persist(todo)                                      │ │
│  │  - deleteById(id)                                     │ │
│  └───────────────────┬───────────────────────────────────┘ │
│                      │ Hibernate ORM                       │
│                      ▼                                      │
│  ┌───────────────────────────────────────────────────────┐ │
│  │              Domain Model (JPA)                       │ │
│  │                    Todo                               │ │
│  │  extends PanacheEntity                                │ │
│  │  - Long id (inherited)                                │ │
│  │  - String title                                       │ │
│  │  - boolean completed                                  │ │
│  │  - int order                                          │ │
│  │  - String url                                         │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
└────────────────────────┬────────────────────────────────────┘
                         │ JDBC
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              PostgreSQL Database (External)                 │
│                  Service: todos-database                    │
│                     Database: todos                         │
│                     Table: todos                            │
└─────────────────────────────────────────────────────────────┘
```

### Components

**1. TodoResource** (REST Layer)
- **Package**: `org.acme.todo.resource`
- **Responsibility**: HTTP request/response handling, REST endpoint exposure
- **Technologies**: JAX-RS 3.0, RESTEasy Reactive, Jackson for JSON
- **Annotations**: `@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PathParam`, `@Produces`, `@Consumes`
- **No Business Logic**: Delegates all work to TodoService

**2. TodoService** (Business Logic Layer)
- **Package**: `org.acme.todo.service`
- **Responsibility**: Business logic orchestration, transaction management
- **Technologies**: CDI (Contexts and Dependency Injection)
- **Annotations**: `@ApplicationScoped`, `@Inject`, `@Transactional`
- **Transaction Boundaries**: All mutating operations (create, update, delete) are transactional

**3. TodoRepository** (Data Access Layer)
- **Package**: `org.acme.todo.repository`
- **Responsibility**: Database CRUD operations abstraction
- **Technologies**: Hibernate ORM with Panache
- **Pattern**: Repository pattern via `PanacheRepository<Todo>`
- **Annotations**: `@ApplicationScoped`
- **Auto-Provided Methods**: All CRUD operations inherited from Panache

**4. Todo** (Domain Model)
- **Package**: `org.acme.todo.model`
- **Responsibility**: Domain object representing a TODO item
- **Technologies**: JPA 3.0, Jakarta Bean Validation
- **Pattern**: Active Record pattern via `PanacheEntity`
- **Annotations**: `@Entity`, `@Table`, `@NotBlank`, `@Column`

**5. Static Resources** (Frontend)
- **Location**: `src/main/resources/META-INF/resources/`
- **Content**: Pre-built Angular TodoMVC application
- **Serving**: Quarkus static resource handler (no code needed)

---

## Technology Choices

### Framework/Platform

**Choice**: Quarkus 3.x (latest stable, 3.8+ recommended)

**Rationale**:
1. **Performance**: 10x faster startup (sub-second vs 3-5 seconds), 70% less memory
2. **Cloud-Native**: Built for containers from the ground up (Kubernetes, OpenShift)
3. **Standards-Based**: Jakarta EE and MicroProfile - minimal vendor lock-in
4. **Developer Experience**: Live reload, unified configuration, dev UI
5. **Red Hat Support**: Enterprise support, strong OpenShift integration
6. **Native Compilation**: GraalVM option for even better performance (optional)

**Alternatives Considered**:
- **Spring Boot**: Current framework - reject due to higher resource usage
- **Micronaut**: Similar to Quarkus but less Red Hat ecosystem integration
- **Helidon**: Oracle's framework - less mature ecosystem
- **Jakarta EE (WildFly)**: Considered but Quarkus offers better cloud-native features

### REST API: RESTEasy Reactive (JAX-RS)

**Choice**: `quarkus-resteasy-reactive-jackson`

**Rationale**:
1. **Standard**: JAX-RS 3.0 is Jakarta EE standard (portable)
2. **Reactive**: RESTEasy Reactive offers better throughput than traditional blocking I/O
3. **Compatibility**: Direct mapping from Spring MVC annotations
4. **Performance**: Non-blocking I/O for better scalability
5. **Jackson Integration**: Automatic JSON serialization/deserialization

**Alternatives Considered**:
- **RESTEasy Classic**: Blocking I/O, less performant than Reactive
- **Quarkus REST (formerly RESTEasy Reactive)**: Same choice, newer naming
- **Spring WebFlux**: Rejected as it's part of Spring ecosystem we're leaving

### Persistence: Hibernate ORM with Panache

**Choice**: `quarkus-hibernate-orm-panache`

**Rationale**:
1. **Simplified API**: Less boilerplate than traditional JPA (no need for `EntityManager` injection)
2. **Active Record Pattern**: Entity methods like `Todo.findAll()` for cleaner code
3. **Repository Pattern**: `PanacheRepository<T>` interface for repository style (chosen approach)
4. **Familiar**: Hibernate underneath, same as Spring Boot uses
5. **Automatic Methods**: CRUD operations auto-provided, no manual implementation

**Repository Pattern Choice**:
We're using `PanacheRepository<Todo>` interface (not Active Record) because:
- Maintains separation of concerns (entity vs repository)
- Easier to test (can mock repository)
- Familiar pattern for Spring Data JPA users
- Cleaner architecture for layered applications

**Alternatives Considered**:
- **Traditional JPA with EntityManager**: More boilerplate, less Quarkus-idiomatic
- **Active Record Pattern** (`PanacheEntity`): Mixes concerns, but acceptable for simple apps
- **Spring Data JPA**: Part of Spring ecosystem we're migrating away from

### Database: PostgreSQL JDBC

**Choice**: `quarkus-jdbc-postgresql` + existing PostgreSQL service

**Rationale**:
1. **No Change**: Reuse existing `todos-database` service in OpenShift
2. **Data Preservation**: Existing data remains intact, no migration needed
3. **Same Driver**: PostgreSQL JDBC driver works identically in Quarkus
4. **H2 for Testing**: Keep `quarkus-jdbc-h2` for dev/test in-memory database

**Alternatives Considered**:
- **Reactive PostgreSQL**: `quarkus-reactive-pg-client` rejected (adds complexity for simple CRUD)
- **Other Databases**: No reason to change from PostgreSQL

### Dependency Injection: CDI

**Choice**: CDI (Contexts and Dependency Injection) - built into Quarkus

**Rationale**:
1. **Standard**: Jakarta EE CDI specification (portable)
2. **Built-In**: No additional dependencies needed
3. **Powerful**: Scopes, interceptors, events, producers
4. **Direct Mapping**: `@Autowired` → `@Inject`, `@Service` → `@ApplicationScoped`

**No Alternatives**: CDI is the standard DI framework for Jakarta EE and Quarkus.

### Validation: Hibernate Validator

**Choice**: `quarkus-hibernate-validator`

**Rationale**:
1. **Same as Current**: Already using Jakarta Bean Validation (javax → jakarta migration done)
2. **No Code Changes**: `@NotBlank`, `@Column(unique=true)` work as-is
3. **Automatic Integration**: Quarkus validates request bodies automatically

### Health Checks: SmallRye Health

**Choice**: `quarkus-smallrye-health`

**Rationale**:
1. **MicroProfile Standard**: MicroProfile Health specification
2. **Kubernetes Ready**: Liveness and readiness probes out of the box
3. **Extensible**: Can add custom health checks if needed
4. **Replaces**: Spring Boot Actuator `/actuator/health` → `/q/health`

**Health Endpoints**:
- `/q/health` - Overall health
- `/q/health/live` - Liveness probe (Kubernetes)
- `/q/health/ready` - Readiness probe (Kubernetes)

---

## Design Patterns

### Repository Pattern

Using **PanacheRepository** interface for data access:

```java
@ApplicationScoped
public class TodoRepository implements PanacheRepository<Todo> {
    // All CRUD methods auto-provided by Panache:
    // - List<Todo> listAll()
    // - Optional<Todo> findByIdOptional(Long id)
    // - void persist(Todo entity)
    // - void deleteById(Long id)
    // - long count()
    // - Stream<Todo> stream()
}
```

**Benefits**:
- Clean separation: Repository vs Entity
- Easy to test (mockable)
- Familiar pattern for Spring users
- Less boilerplate than traditional JPA

### Dependency Injection Pattern

**Constructor Injection** (recommended):

```java
@Path("/api/todos")
public class TodoResource {
    
    private final TodoService todoService;
    
    @Inject
    public TodoResource(TodoService todoService) {
        this.todoService = todoService;
    }
}
```

**Why Constructor Injection**:
- Immutable dependencies (final fields)
- Explicit dependencies (visible in constructor signature)
- Easier to test (can pass mocks to constructor)
- Best practice in CDI and Spring

**Note**: Current code uses field injection. We'll migrate to constructor injection during migration.

### Transaction Management Pattern

**Service-Layer Transactions** (best practice):

```java
@ApplicationScoped
public class TodoService {
    
    @Inject
    TodoRepository todoRepository;
    
    @Transactional  // Transaction boundary at service layer
    public Todo createOrUpdateTodo(Todo todo) {
        todoRepository.persist(todo);
        return todo;
    }
}
```

**Why Service-Layer Transactions**:
- Controller is stateless (no transaction state)
- Business logic controls transaction scope
- Can combine multiple repository calls in one transaction
- Better testability

**Note**: Current code has `@Transactional` on controller methods. We'll move to service layer.

### REST Resource Pattern

**JAX-RS Resource Class**:

```java
@Path("/api/todos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TodoResource {
    
    @GET
    public List<Todo> getAllTodos() { /* delegate to service */ }
    
    @GET
    @Path("/{id}")
    public Response getTodoById(@PathParam("id") Long id) { /* delegate */ }
    
    @POST
    public Response createTodo(Todo todo) { /* delegate */ }
}
```

**Pattern Elements**:
- Class-level `@Path` for base path
- Class-level `@Produces`/`@Consumes` for content negotiation
- Method-level `@GET`, `@POST`, etc. for HTTP verbs
- Method-level `@Path` for sub-paths
- `Response` for non-200 status codes, direct return for 200 OK

---

## Migration Approach

### Phasing Strategy

**Big Bang Migration** (all components at once)

**Phase 1: Project Setup** (2-3 hours)
- Generate Quarkus project skeleton
- Configure Quarkus extensions (resteasy, panache, jdbc, health, validation)
- Set up `application.properties` with database configuration
- Configure CORS properties
- Set up dev mode and live reload

**Phase 2: Entity Layer Migration** (1 hour)
- Copy `Todo.java` to new project
- Modify to extend `PanacheEntity` (optional but recommended)
- Change visibility: private fields → public fields (Panache convention)
- Remove `@Id` and `@GeneratedValue` (inherited from PanacheEntity)
- Validate Jakarta annotations still present (`@Entity`, `@Table`, `@NotBlank`)

**Phase 3: Repository Layer Migration** (1 hour)
- Create `TodoRepository` class (was interface)
- Implement `PanacheRepository<Todo>` interface
- Add `@ApplicationScoped` annotation
- Remove all method declarations (Panache provides them)
- No custom query methods needed (none exist in current codebase)

**Phase 4: Service Layer Migration** (1 hour)
- Copy `TodoService.java` to new project
- Replace `@Service` → `@ApplicationScoped`
- Replace `@Autowired` → `@Inject` (or switch to constructor injection)
- Add `@Transactional` to mutating methods (createOrUpdateTodo, deleteTodoById)
- Update repository method calls: `findAll()` → `listAll()`, `save()` → `persist()`, etc.

**Phase 5: Controller → Resource Migration** (2-3 hours)
- Rename `TodoController.java` → `TodoResource.java`
- Move package: `controller` → `resource`
- Replace Spring annotations with JAX-RS:
  - `@RestController` → `@Path("/api/todos")`
  - Add `@Produces(MediaType.APPLICATION_JSON)` and `@Consumes(MediaType.APPLICATION_JSON)`
  - `@GetMapping` → `@GET`, `@PostMapping` → `@POST`, etc.
  - `@PathVariable` → `@PathParam`
  - Remove `@RequestBody`
  - Remove `@CrossOrigin`
- Replace `@Autowired` → `@Inject` (or constructor injection)
- Replace `ResponseEntity<T>` with JAX-RS `Response`:
  - `ResponseEntity.ok(T)` → `Response.ok(T).build()`
  - `ResponseEntity.notFound().build()` → `Response.status(404).build()`
  - `ResponseEntity.noContent().build()` → `Response.noContent().build()`
- Remove `@Transactional` (moved to service layer)

**Phase 6: Static Resources Migration** (30 min)
- Copy all files from `src/main/resources/static/` to `src/main/resources/META-INF/resources/`
- No code changes needed

**Phase 7: Configuration Migration** (1 hour)
- Migrate `application.properties` to Quarkus format:
  - `spring.datasource.*` → `quarkus.datasource.*`
  - `spring.jpa.*` → `quarkus.hibernate-orm.*`
  - Add CORS configuration
- Configure health check endpoints
- Configure logging format

### Component Migration Order

**1. Todo Entity** (bottom-up approach)
- Why first: No dependencies on other layers
- Lowest risk: Minimal changes (already Jakarta-compliant)

**2. TodoRepository**
- Why second: Depends only on Todo entity
- Low risk: Panache provides implementation

**3. TodoService**
- Why third: Depends on TodoRepository
- Medium risk: Transaction management changes

**4. TodoResource** (formerly TodoController)
- Why fourth: Depends on TodoService
- Highest risk: Most annotation changes

**5. Static Resources**
- Why last: Independent of backend code
- Zero risk: File copy only

**Rationale for Bottom-Up**:
- Each layer depends on the layer below it
- Test each layer in isolation before moving up
- Reduces integration issues

### Parallel Run Strategy

**Deployment Strategy**: Blue/Green

**During Migration**:
1. Keep existing Spring Boot deployment running (Blue)
2. Deploy new Quarkus application alongside (Green)
3. Test Quarkus deployment thoroughly in staging
4. Switch OpenShift route from Blue → Green
5. Monitor for 24-48 hours
6. Decommission Spring Boot deployment if no issues

**Route Switching**:
```bash
# Current route points to Spring Boot service
oc get route jws-app

# Create new Quarkus service
oc apply -f k8s/service.yaml

# Update route to point to Quarkus service
oc patch route jws-app -p '{"spec":{"to":{"name":"todo-app-quarkus"}}}'

# Rollback if needed (switch back to Spring Boot)
oc patch route jws-app -p '{"spec":{"to":{"name":"jws-app"}}}'
```

**Data Synchronization**: None needed (both applications use same PostgreSQL database)

---

## Integration Design

### External Systems

**PostgreSQL Database**:
- **Current**: `todos-database` Kubernetes service (OpenShift)
- **Target**: Same service (no change)
- **Connection String**: `jdbc:postgresql://todos-database:5432/todos`
- **Credentials**: Username `jws`, password `jws` (from existing ConfigMap/Secret)
- **Schema**: No changes (Hibernate ORM reads existing schema)

**No Other External Integrations**: Self-contained application

### Data Strategy

**Schema Management**:
- **Approach**: `quarkus.hibernate-orm.database.generation=update`
- **Behavior**: Hibernate updates schema if entity changes (adds columns, never drops)
- **Safety**: Non-destructive (existing data preserved)

**No Data Migration Needed**:
- Schema compatible between Spring Boot Hibernate and Quarkus Hibernate
- Same JPA annotations, same table structure
- Existing data in `todos` table remains intact

### API Compatibility

**REST API Contract**:
- **Endpoints**: Unchanged (same paths: `/api/todos`, `/api/todos/{id}`)
- **HTTP Methods**: Unchanged (GET, POST, PUT, DELETE)
- **Request Formats**: Unchanged (JSON with same field names)
- **Response Formats**: Unchanged (JSON with same field names)
- **Status Codes**: Unchanged (200, 201, 204, 404)

**Frontend Compatibility**:
- Angular frontend expects same API contract
- No frontend code changes needed
- CORS headers must be configured correctly

**Backward Compatibility**: 100% (API contract identical)

---

## Testing Strategy

### Test Approach

**Test Pyramid**:
1. **Unit Tests** (70%): Fast, isolated, mock dependencies
2. **Integration Tests** (20%): Test with real database (H2 in-memory)
3. **End-to-End Tests** (10%): Full stack with REST Assured

**Test Framework**: JUnit 5 + REST Assured + Mockito

### Test Levels

#### 1. Unit Tests

**Entity Tests**:
```java
@QuarkusTest
class TodoTest {
    
    @Test
    void testValidTodo() {
        Todo todo = new Todo();
        todo.title = "Test";
        todo.completed = false;
        
        // Validate constraints
        Set<ConstraintViolation<Todo>> violations = validator.validate(todo);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void testBlankTitle() {
        Todo todo = new Todo();
        todo.title = "";  // Blank title should violate @NotBlank
        
        Set<ConstraintViolation<Todo>> violations = validator.validate(todo);
        assertFalse(violations.isEmpty());
    }
}
```

**Service Tests** (with mocked repository):
```java
@QuarkusTest
class TodoServiceTest {
    
    @InjectMock
    TodoRepository todoRepository;
    
    @Inject
    TodoService todoService;
    
    @Test
    void testGetAllTodos() {
        when(todoRepository.listAll()).thenReturn(List.of(new Todo()));
        
        List<Todo> todos = todoService.getAllTodos();
        
        assertEquals(1, todos.size());
        verify(todoRepository).listAll();
    }
}
```

#### 2. Integration Tests

**Repository Tests** (with H2 in-memory database):
```java
@QuarkusTest
@TestTransaction
class TodoRepositoryTest {
    
    @Inject
    TodoRepository todoRepository;
    
    @Test
    void testPersistAndFind() {
        Todo todo = new Todo();
        todo.title = "Integration Test";
        todo.completed = false;
        
        todoRepository.persist(todo);
        
        Optional<Todo> found = todoRepository.findByIdOptional(todo.id);
        assertTrue(found.isPresent());
        assertEquals("Integration Test", found.get().title);
    }
}
```

**Full Stack API Tests** (with REST Assured):
```java
@QuarkusTest
class TodoResourceTest {
    
    @Test
    void testGetAllTodos() {
        given()
            .when().get("/api/todos")
            .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(0));
    }
    
    @Test
    void testCreateTodo() {
        Todo todo = new Todo();
        todo.title = "REST Test";
        todo.completed = false;
        
        given()
            .contentType(ContentType.JSON)
            .body(todo)
            .when().post("/api/todos")
            .then()
                .statusCode(201)
                .body("title", equalTo("REST Test"));
    }
    
    @Test
    void testGetNonExistentTodo() {
        given()
            .when().get("/api/todos/99999")
            .then()
                .statusCode(404);
    }
}
```

#### 3. Characterization Tests

**Purpose**: Capture current Spring Boot behavior before migration

**Approach**:
1. Run Spring Boot application locally
2. Execute all API calls and record responses
3. Create tests asserting Quarkus produces identical responses

**Test Cases**:
- GET /api/todos → Returns JSON array
- GET /api/todos/1 → Returns single TODO or 404
- POST /api/todos with valid data → Returns 201 with created entity
- POST /api/todos with invalid data → Returns 400 with validation errors
- PUT /api/todos/1 with valid data → Returns 200 with updated entity
- PUT /api/todos/99999 → Returns 404
- DELETE /api/todos/1 → Returns 204
- CORS preflight (OPTIONS) → Returns appropriate CORS headers

### Validation Criteria

**Acceptance Criteria for Each Migration Phase**:

**Phase 1 (Setup)**:
- ✅ `mvn quarkus:dev` starts successfully
- ✅ Dev UI accessible at `/q/dev`
- ✅ Health check endpoint `/q/health` returns UP

**Phase 2 (Entity)**:
- ✅ Entity unit tests pass
- ✅ Jakarta validation annotations work

**Phase 3 (Repository)**:
- ✅ Repository integration tests pass
- ✅ CRUD operations work against H2 database

**Phase 4 (Service)**:
- ✅ Service unit tests pass (with mocked repository)
- ✅ Transactions rollback on exception

**Phase 5 (Resource)**:
- ✅ All REST endpoint tests pass
- ✅ Response formats match Spring Boot

**Phase 6 (Static Resources)**:
- ✅ Frontend accessible at `/`
- ✅ Frontend can call backend API

**Phase 7 (Configuration)**:
- ✅ Database connection works
- ✅ CORS headers present

**Final Acceptance**:
- ✅ All tests passing (unit + integration + E2E)
- ✅ Code coverage >80%
- ✅ Startup time <1 second
- ✅ Memory footprint <150 MB
- ✅ No security vulnerabilities

---

## Deployment Strategy

### Containerization

**Container Strategy**: Multi-stage Docker build for JVM mode

**Dockerfile** (`src/main/docker/Dockerfile.jvm`):
```dockerfile
# Stage 1: Build
FROM registry.access.redhat.com/ubi9/openjdk-17:1.18 AS build

USER root
WORKDIR /build

# Copy Maven wrapper and pom.xml
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Build application
RUN ./mvnw package -DskipTests

# Stage 2: Runtime
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:1.18

USER 185

# Copy application JAR
COPY --from=build /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build /build/target/quarkus-app/*.jar /deployments/
COPY --from=build /build/target/quarkus-app/app/ /deployments/app/
COPY --from=build /build/target/quarkus-app/quarkus/ /deployments/quarkus/

# Quarkus configuration
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "/deployments/quarkus-run.jar" ]
```

**Image Optimization**:
- Multi-stage build reduces final image size
- Dependency download cached separately from source
- Runtime image uses UBI runtime (smaller than builder)
- Non-root user (UID 185)

**Native Image** (Optional - `src/main/docker/Dockerfile.native`):
```dockerfile
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.3

USER 1001

COPY --chown=1001 target/*-runner /application

EXPOSE 8080

ENTRYPOINT ["/application", "-Dquarkus.http.host=0.0.0.0"]
```

**When to Use Native**:
- Production deployments requiring fastest startup (<100ms)
- Serverless/FaaS deployments
- High-density containerized environments

**Build Commands**:
```bash
# JVM mode
mvn clean package
podman build -f src/main/docker/Dockerfile.jvm -t quay.io/${USER}/todo-demo-quarkus:latest .

# Native mode (requires GraalVM)
mvn package -Pnative
podman build -f src/main/docker/Dockerfile.native -t quay.io/${USER}/todo-demo-quarkus:native .

# Push to registry
podman push quay.io/${USER}/todo-demo-quarkus:latest
```

### Orchestration

**Platform**: Red Hat OpenShift 4.x

**Kubernetes Resources**:

**1. Deployment** (`k8s/deployment.yaml`):
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: todo-app-quarkus
  labels:
    app: todo-app-quarkus
spec:
  replicas: 2
  selector:
    matchLabels:
      app: todo-app-quarkus
  template:
    metadata:
      labels:
        app: todo-app-quarkus
    spec:
      containers:
      - name: quarkus
        image: quay.io/${USER}/todo-demo-quarkus:latest
        ports:
        - containerPort: 8080
          protocol: TCP
        env:
        - name: QUARKUS_DATASOURCE_JDBC_URL
          value: "jdbc:postgresql://todos-database:5432/todos"
        - name: QUARKUS_DATASOURCE_USERNAME
          value: "jws"
        - name: QUARKUS_DATASOURCE_PASSWORD
          value: "jws"
        resources:
          requests:
            cpu: "250m"
            memory: "256Mi"
          limits:
            cpu: "500m"
            memory: "512Mi"
        livenessProbe:
          httpGet:
            path: /q/health/live
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
```

**2. Service** (`k8s/service.yaml`):
```yaml
apiVersion: v1
kind: Service
metadata:
  name: todo-app-quarkus
  labels:
    app: todo-app-quarkus
spec:
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: todo-app-quarkus
  type: ClusterIP
```

**3. Route** (`k8s/route.yaml`):
```yaml
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: todo-app-quarkus
spec:
  to:
    kind: Service
    name: todo-app-quarkus
  port:
    targetPort: http
  tls:
    termination: edge
    insecureEdgeTerminationPolicy: Redirect
```

**Deployment Commands**:
```bash
# Deploy all resources
oc apply -f k8s/deployment.yaml
oc apply -f k8s/service.yaml
oc apply -f k8s/route.yaml

# Get route URL
oc get route todo-app-quarkus -o jsonpath='{.spec.host}'

# Monitor deployment
oc rollout status deployment/todo-app-quarkus

# View logs
oc logs -f deployment/todo-app-quarkus
```

### CI/CD Pipeline

**Option 1: OpenShift Pipelines (Tekton)**

**Pipeline Steps**:
1. Checkout source code from Git
2. Run Maven build (`mvn clean package`)
3. Run tests (`mvn test`)
4. Build container image (`podman build`)
5. Push to Quay.io registry
6. Update Kubernetes manifests with new image tag
7. Apply manifests to OpenShift (`oc apply`)
8. Wait for rollout completion
9. Run smoke tests against deployed application

**Option 2: Existing CI/CD** (GitHub Actions, Jenkins, GitLab CI)

Integrate Quarkus build into existing pipeline:
```yaml
# GitHub Actions example
- name: Build Quarkus Application
  run: ./mvnw package
  
- name: Build Container Image
  run: podman build -t quay.io/${{ secrets.QUAY_USER }}/todo-demo-quarkus:${{ github.sha }} .
  
- name: Push to Quay.io
  run: podman push quay.io/${{ secrets.QUAY_USER }}/todo-demo-quarkus:${{ github.sha }}
  
- name: Deploy to OpenShift
  run: |
    oc set image deployment/todo-app-quarkus quarkus=quay.io/${{ secrets.QUAY_USER }}/todo-demo-quarkus:${{ github.sha }}
```

---

## Rollback Plan

**Scenario 1: Deployment Failure (Cannot Start)**

**Detection**: Readiness probe fails, pods never become Ready

**Rollback**:
```bash
# Immediate: Delete Quarkus deployment
oc delete deployment todo-app-quarkus

# Keep Spring Boot running (already deployed)
# Route still points to Spring Boot service
```

**Scenario 2: Runtime Issues (Application Started but Broken)**

**Detection**: Errors in logs, failing health checks, user reports

**Rollback**:
```bash
# Switch route back to Spring Boot
oc patch route jws-app -p '{"spec":{"to":{"name":"jws-app"}}}'

# Verify Spring Boot receiving traffic
curl https://$(oc get route jws-app -o jsonpath='{.spec.host}')/api/todos

# Investigate Quarkus deployment (leave running for debugging)
oc logs deployment/todo-app-quarkus
```

**Scenario 3: Data Corruption (Database Issues)**

**Detection**: Incorrect data in database, failed transactions

**Rollback**:
1. Switch traffic to Spring Boot immediately (see Scenario 2)
2. Restore database from backup (if backup exists)
3. Investigate Quarkus transaction handling
4. Fix and redeploy

**Prevention**:
- `quarkus.hibernate-orm.database.generation=update` is non-destructive
- Test database operations thoroughly before production deployment
- Take database snapshot before migration

**Automated Rollback Trigger**:
```yaml
# In deployment.yaml, configure liveness probe with low failure threshold
livenessProbe:
  httpGet:
    path: /q/health/live
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
  failureThreshold: 3  # Restart pod after 3 failures

# OpenShift automatically restarts failing pods
# If continuous failures, manual rollback required
```

---

## Monitoring and Observability

### Logging

**Quarkus Logging Configuration**:
```properties
# JSON format for log aggregation
quarkus.log.console.json=true

# Log level
quarkus.log.level=INFO
quarkus.log.category."org.acme".level=DEBUG

# Include request details
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n
```

**OpenShift Log Collection**:
- Logs available via `oc logs` command
- Aggregated to OpenShift logging (EFK stack if installed)
- JSON format enables structured querying

### Metrics

**SmallRye Metrics** (optional extension):
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
</dependency>
```

**Prometheus Metrics Endpoint**: `/q/metrics`

**Key Metrics**:
- HTTP request count, duration (by endpoint)
- JVM memory usage, garbage collection
- Database connection pool stats
- Quarkus startup time

**OpenShift Integration**:
```yaml
# Add to Service for Prometheus scraping
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
    prometheus.io/path: "/q/metrics"
```

### Health Checks

**SmallRye Health Endpoints**:
- `/q/health` - Overall health status (liveness + readiness)
- `/q/health/live` - Liveness probe (is the app running?)
- `/q/health/ready` - Readiness probe (can the app serve traffic?)

**Kubernetes Probes** (see Deployment section):
```yaml
livenessProbe:
  httpGet:
    path: /q/health/live
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /q/health/ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

**Custom Health Checks** (optional):
```java
@Liveness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {
    
    @Inject
    DataSource dataSource;
    
    @Override
    public HealthCheckResponse call() {
        try (Connection conn = dataSource.getConnection()) {
            return HealthCheckResponse.up("Database");
        } catch (SQLException e) {
            return HealthCheckResponse.down("Database");
        }
    }
}
```

### Tracing (Optional)

**OpenTelemetry** (if distributed tracing needed):
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-opentelemetry</artifactId>
</dependency>
```

**Not Required for This Migration**: Single-service application, no distributed tracing needed.

---

**End of Design Document**
