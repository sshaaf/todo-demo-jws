# Migration Analysis: Spring Boot → Quarkus

**Generated**: 2026-06-03
**Project**: todo-demo-jws
**Branch**: migrate-to-quarkus

---

## Executive Summary

This Spring Boot 3.2.5 application with Angular frontend is an excellent candidate for Quarkus migration. The backend uses a classic 3-layer architecture (Controller → Service → Repository) with standard REST API and JPA/Hibernate persistence. The frontend is pre-built Angular static resources.

**Migration Complexity**: **LOW**

**Estimated Effort**: 1-2 days for a skilled Java developer

**Key Insights**:
- Only 6 Java backend files to migrate
- No complex Spring features (Security, Cloud, Batch, etc.)
- Standard JPA/Hibernate - direct Quarkus compatibility
- Simple REST API maps to JAX-RS/RESTEasy
- Static resources easily served by Quarkus
- Already uses Jakarta EE annotations (validation)

---

## Codebase Structure (from Knowledge Graph)

**Analysis Summary:**
- **Total Files**: 13 (6 Java backend + frontend resources)
- **Total Dependencies**: 3522 edges
- **Communities Detected**: 72 communities (68 shown)
- **Extraction Quality**: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS
- **Backend Community**: Community 6 (4 nodes: TodoController, Todo, TodoService, build())
- **Backend Cohesion**: 0.10 (well-structured, loosely coupled)

**God Nodes** (most from Angular frontend):
1. `constructor()` - 81 edges (Angular)
2. `get()` - 54 edges (Angular)
3. `forEach()` - 45 edges (Angular)
4. `map()` - 32 edges (Angular)
5. `create()` - 31 edges (Angular)

**Backend Nodes** (Community 6):
- `TodoController` - REST endpoints
- `TodoService` - business logic
- `Todo` - JPA entity
- `build()` - application builder

**Knowledge Gaps**:
- 18 isolated nodes (mostly Angular utilities)
- TodoRepository is isolated (standard Spring Data JPA interface)

---

## Dependency Analysis

### Backend Dependencies (Spring Boot 3.2.5)

**Current Stack** (from pom.xml):
```xml
spring-boot-starter-actuator      → Quarkus SmallRye Health
spring-boot-starter-data-jpa      → Quarkus Hibernate ORM with Panache
spring-boot-starter-validation    → Quarkus Hibernate Validator
spring-boot-starter-web           → Quarkus RESTEasy (JAX-RS)
spring-boot-starter-tomcat        → Quarkus built-in Vert.x (no separate container)
postgresql                        → Quarkus JDBC PostgreSQL (same driver)
h2                                → Quarkus JDBC H2 (same driver)
```

**Migration Path**: All Spring dependencies have direct Quarkus equivalents

### Critical Dependencies (High Fan-In)

**TodoService** is the central backend node:
- Used by TodoController for all CRUD operations
- Depends on TodoRepository
- Simple pass-through service layer
- **Migration Impact**: Low - straightforward CDI injection replacement

**TodoRepository** extends JpaRepository:
- Standard Spring Data JPA interface
- No custom query methods
- **Quarkus Equivalent**: Hibernate ORM with Panache (PanacheRepository) or keep as JpaRepository

### Integration Points (High Fan-Out)

**TodoController**:
- Depends on: TodoService, Spring annotations, ResponseEntity
- Exposes 5 REST endpoints: GET all, GET by ID, POST, PUT, DELETE
- Uses `@Transactional` at controller level (should move to service layer in Quarkus)
- **Migration Impact**: Medium - annotation changes required

**Todo Entity**:
- Standard JPA entity with Jakarta persistence annotations
- **Migration Impact**: Minimal - already uses Jakarta (not javax)

### Circular Dependencies

**None detected** - Clean layered architecture with one-way dependencies:
```
TodoController → TodoService → TodoRepository → Todo
```

### Community Structure

**Backend Community (Community 6)**:
- **Cohesion**: 0.10 (appropriate for layered architecture)
- **Size**: 4 nodes
- **Coupling**: Low inter-layer coupling (good design)
- **External Dependencies**: Spring Boot, JPA, PostgreSQL

**Frontend Communities**:
- 71 other communities (Angular application)
- **Migration Impact**: Zero - frontend is pre-built static resources
- Quarkus will serve these from `src/main/resources/META-INF/resources`

### External Dependencies

| Spring Dependency | Quarkus Equivalent | Migration Difficulty |
|-------------------|-------------------|---------------------|
| spring-boot-starter-web | quarkus-resteasy-reactive-jackson | Low |
| spring-boot-starter-data-jpa | quarkus-hibernate-orm-panache | Low |
| spring-boot-starter-validation | quarkus-hibernate-validator | Trivial |
| spring-boot-starter-actuator | quarkus-smallrye-health | Low |
| postgresql driver | Same (quarkus-jdbc-postgresql) | Trivial |
| h2 driver | Same (quarkus-jdbc-h2) | Trivial |

---

## Migration Complexity Assessment

**Overall**: **LOW**

**Reasoning** (based on graph metrics):

1. **Small Codebase**: Only 6 Java files in backend (13,718 words total)
   - Low complexity = less migration work
   
2. **Clean Architecture**: Community 6 (backend) has cohesion 0.10
   - Well-structured layered architecture
   - No god classes in backend
   - Clear separation of concerns
   
3. **No Circular Dependencies**: One-way dependency flow
   - No refactoring needed before migration
   - Can migrate layer by layer
   
4. **High Extraction Confidence**: 98% EXTRACTED edges
   - Graph analysis is highly accurate
   - Few ambiguous relationships to verify
   
5. **Standard Patterns**: Classic Spring Boot REST API
   - Well-documented migration paths
   - Red Hat provides migration tooling (Windup, MTA)
   
6. **Minimal Spring Features**:
   - No Spring Security, Cloud, Batch, Integration
   - No custom Spring configurations
   - Standard annotations only
   
7. **Jakarta EE Already**: Uses `jakarta.persistence` and `jakarta.validation`
   - Already migrated from javax → jakarta
   - Quarkus native compatibility

**Risk Factors** (Low Impact):
- `@Transactional` at controller level (should be in service layer)
- Field injection with `@Autowired` (should use constructor injection)
- `ResponseEntity` usage (JAX-RS uses different approach)

---

## Recommended Migration Strategy

### Approach: **Big Bang Migration**

**Rationale**:
- Small codebase (6 files) - low risk of partial migration
- No complex features requiring phased approach
- Clean architecture supports full migration
- Development can complete in 1-2 days

### Migration Order (based on graph structure):

#### Phase 1: Project Setup & Configuration
**Duration**: 2-3 hours

1. Create new Quarkus project with required extensions:
   ```bash
   quarkus create app org.acme:todo-demo-quarkus \
     --extension='resteasy-reactive-jackson,hibernate-orm-panache,jdbc-postgresql,hibernate-validator,smallrye-health'
   ```

2. Copy frontend static resources to `src/main/resources/META-INF/resources/`

3. Migrate `application.properties`:
   - Database configuration (datasource properties)
   - Hibernate settings
   - Server port and context path

**Files Changed**: pom.xml, application.properties

#### Phase 2: Entity Layer (Leaf Nodes)
**Duration**: 1 hour

1. Copy `Todo.java` entity to new project
2. Optionally extend `PanacheEntity` for simplified repository pattern
3. Validate JPA annotations (already Jakarta-compatible)

**Files Changed**: 1 file (Todo.java)
**Migration Impact**: Minimal - already uses Jakarta annotations

#### Phase 3: Repository Layer
**Duration**: 1 hour

**Option A** (Panache Repository Pattern - Recommended):
```java
@ApplicationScoped
public class TodoRepository implements PanacheRepository<Todo> {
    // Automatic CRUD methods from Panache
}
```

**Option B** (Keep Spring Data JPA style):
```java
@ApplicationScoped
public class TodoRepository {
    @Inject
    EntityManager em;
    
    public List<Todo> findAll() { ... }
    public Optional<Todo> findById(Long id) { ... }
    public void persist(Todo todo) { ... }
    public void deleteById(Long id) { ... }
}
```

**Files Changed**: 1 file (TodoRepository.java)

#### Phase 4: Service Layer
**Duration**: 1 hour

1. Replace `@Service` → `@ApplicationScoped`
2. Replace `@Autowired` → `@Inject`
3. Keep business logic unchanged
4. Add `@Transactional` to mutating methods (currently in controller)

**Files Changed**: 1 file (TodoService.java)

#### Phase 5: Controller Layer (REST API)
**Duration**: 2-3 hours

1. Replace `@RestController` → `@Path("/api/todos")`
2. Replace `@RequestMapping` → `@Path`, `@GET`, `@POST`, etc.
3. Replace `@RequestBody` → JAX-RS automatic deserialization
4. Replace `@PathVariable` → `@PathParam`
5. Replace `ResponseEntity` → JAX-RS `Response` or direct return
6. Remove `@CrossOrigin` → configure in application.properties
7. Move `@Transactional` to service layer

**Files Changed**: 1 file (TodoController.java)

#### Phase 6: Testing & Validation
**Duration**: 2-4 hours

1. Add Quarkus tests:
   - `@QuarkusTest` for integration tests
   - REST Assured for API testing
   - Test database (H2 or PostgreSQL testcontainer)

2. Validate endpoints:
   - GET /api/todos
   - GET /api/todos/{id}
   - POST /api/todos
   - PUT /api/todos/{id}
   - DELETE /api/todos/{id}

3. Test health endpoints: `/q/health`

4. Test static frontend resources

**Files Changed**: New test files

#### Phase 7: Containerization & OpenShift Deployment
**Duration**: 2-3 hours

1. Build Quarkus container image (JVM or native):
   ```bash
   quarkus build --native  # Optional: native compilation
   # or
   mvn clean package
   podman build -f src/main/docker/Dockerfile.jvm -t quay.io/${USER}/todo-demo-quarkus:latest .
   ```

2. Deploy to OpenShift:
   - Database deployment (PostgreSQL)
   - Application deployment (Quarkus)
   - Service and Route configuration
   - ConfigMap for application properties

3. Validate deployment in current namespace

**Integration Skills**:
- `mig-containerize` - Generate Dockerfile and container configuration
- `mig-deploy` - Deploy to OpenShift with dependencies

---

## Risk Areas

### High Priority (Must Address)

**1. Transaction Management**
- **Issue**: `@Transactional` on controller methods (lines 32, 38, 54 in TodoController)
- **Risk**: Incorrect transaction boundaries in Quarkus
- **Mitigation**: Move `@Transactional` to service layer methods
- **Graph Evidence**: TodoController has high fan-out, affects all CRUD operations

**2. Response Handling**
- **Issue**: Uses Spring `ResponseEntity` pattern extensively
- **Risk**: JAX-RS uses different response model (`Response` class or direct returns)
- **Mitigation**: 
  - Return entities directly for success cases
  - Use `Response.status(404).build()` for not found
  - Use `Response.noContent().build()` for deletes
- **Graph Evidence**: 5 REST methods in TodoController (GET, POST, PUT, DELETE)

**3. CORS Configuration**
- **Issue**: Uses `@CrossOrigin` annotation on controller
- **Risk**: Quarkus CORS configured in application.properties, not annotations
- **Mitigation**: Add to application.properties:
  ```properties
  quarkus.http.cors=true
  quarkus.http.cors.origins=*
  ```

### Medium Priority (Should Address)

**4. Dependency Injection Pattern**
- **Issue**: Field injection with `@Autowired`
- **Risk**: Not Quarkus best practice (prefer constructor injection)
- **Mitigation**: Use constructor injection:
  ```java
  @Inject
  public TodoController(TodoService todoService) {
      this.todoService = todoService;
  }
  ```

**5. Database Configuration**
- **Issue**: Spring Boot auto-configuration vs Quarkus explicit configuration
- **Risk**: Missing datasource configuration
- **Mitigation**: Ensure application.properties has:
  ```properties
  quarkus.datasource.db-kind=postgresql
  quarkus.datasource.jdbc.url=jdbc:postgresql://todos-database:5432/todos
  quarkus.datasource.username=jws
  quarkus.datasource.password=jws
  quarkus.hibernate-orm.database.generation=update
  ```

### Low Priority (Nice to Have)

**6. Repository Modernization**
- **Issue**: Empty JpaRepository interface (no custom methods)
- **Risk**: None - works as-is
- **Mitigation**: Consider Panache for cleaner syntax:
  ```java
  Todo.findById(id);
  Todo.listAll();
  ```

**7. ServletInitializer**
- **Issue**: Spring Boot WAR deployment class
- **Risk**: Not needed in Quarkus (JAR-based)
- **Mitigation**: Delete ServletInitializer.java

---

## Next Steps

### Immediate Actions (Phase 1)

1. **Create Quarkus Project**:
   ```bash
   quarkus create app org.acme:todo-demo-quarkus \
     --extension='resteasy-reactive-jackson,hibernate-orm-panache,jdbc-postgresql,jdbc-h2,hibernate-validator,smallrye-health'
   ```

2. **Verify Required Extensions** (add if missing):
   ```bash
   quarkus extension add resteasy-reactive-jackson
   quarkus extension add hibernate-orm-panache
   quarkus extension add jdbc-postgresql
   quarkus extension add hibernate-validator
   quarkus extension add smallrye-health
   ```

3. **Copy Frontend Resources**:
   ```bash
   mkdir -p src/main/resources/META-INF/resources
   cp -r src/main/resources/static/* src/main/resources/META-INF/resources/
   ```

4. **Initialize application.properties** with database configuration

### Validation Checkpoints

After each phase, validate:

✅ **Phase 1**: Project builds successfully (`quarkus build`)
✅ **Phase 2**: Entity tests pass, database schema created
✅ **Phase 3**: Repository CRUD operations work
✅ **Phase 4**: Service layer transactions work correctly
✅ **Phase 5**: All REST endpoints return expected responses
✅ **Phase 6**: Full integration test suite passes
✅ **Phase 7**: Application runs in OpenShift, health checks pass

### Testing Strategy

**Unit Tests**:
- Service layer business logic
- Entity validation rules

**Integration Tests** (`@QuarkusTest`):
- REST API endpoints with REST Assured
- Database operations with test transactions
- Health endpoint validation

**E2E Tests**:
- Frontend application against Quarkus backend
- OpenShift deployment validation

### Migration Tools

**Red Hat Migration Toolkit for Applications (MTA)**:
- Automated code analysis
- Spring Boot → Quarkus migration rules
- Identifies required changes

**Quarkus CLI**:
- Project scaffolding
- Extension management
- Development mode (`quarkus dev`)

**OpenShift CLI** (`oc`):
- Deployment to current namespace
- Database provisioning
- Service/Route configuration

---

## Appendix: Graph Analysis Details

### Community 6 (Backend) Structure

**Nodes**: 4
- TodoController (REST layer)
- TodoService (business layer)
- Todo (entity layer)
- build() (application builder)

**Cohesion**: 0.10 (appropriate for layered architecture)

**Edges** (dependencies):
- TodoController → TodoService (REST → business)
- TodoService → TodoRepository (business → data)
- TodoRepository → Todo (data → entity)
- TodoController → Todo (REST → entity, for request/response binding)

**External Connections**:
- Community 6 → Community 0 via `build()` (high betweenness: 0.037)
- Indicates `build()` is a bridge between backend logic and application context

### Isolated Nodes

- **TodoRepository**: Isolated in graph (interface with no explicit method calls detected)
  - This is expected for Spring Data JPA repositories
  - Runtime proxy provides implementations
  - No migration risk

### Frontend Structure

**Communities 0-5, 7-71**: Angular application
- 1153 nodes (99% of total)
- Complex inter-community connections
- **Migration Impact**: None (pre-built static resources)
- **Deployment**: Copy to `src/main/resources/META-INF/resources/`

---

## Summary

This is a **textbook-simple Spring Boot → Quarkus migration**:

✅ Small codebase (6 Java files)
✅ Clean architecture (one-way dependencies)
✅ Standard patterns (REST, JPA, validation)
✅ No complex Spring features
✅ Direct Quarkus equivalents for all dependencies
✅ Already uses Jakarta EE annotations

**Recommended Approach**: Big bang migration in 1-2 days

**Critical Path**: 
1. Setup Quarkus project
2. Migrate entities (trivial)
3. Migrate repository (choose Panache or classic)
4. Migrate service layer (CDI injection)
5. Migrate controller (JAX-RS annotations)
6. Test and validate
7. Containerize and deploy to OpenShift

**Success Metrics**:
- All REST endpoints functional
- Database operations working
- Frontend served correctly
- Health checks passing
- OpenShift deployment successful

