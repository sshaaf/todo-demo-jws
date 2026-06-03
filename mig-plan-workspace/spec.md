# Migration Specification: Spring Boot → Quarkus

**Project**: todo-demo-jws  
**Branch**: migrate-to-quarkus  
**Generated**: 2026-06-03  
**Migration Type**: Framework Modernization (Big Bang)

---

## Overview

This specification documents the migration of a Spring Boot 3.2.5 TODO management application to Quarkus 3.x. The migration targets improved performance, reduced resource consumption, and cloud-native optimization while maintaining 100% functional equivalence. The application's small size (6 Java files) and clean architecture make it an ideal candidate for a complete big bang migration, estimated at 12-20 hours of development effort.

---

## Current State Analysis

### Technology Stack

**Core Framework:**
- **Spring Boot**: 3.2.5
- **Java**: 17 (OpenJDK)
- **Build Tool**: Maven 3.x
- **Packaging**: WAR file for deployment to JBoss Web Server

**Dependencies** (from pom.xml analysis):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Deployment:**
- **Container**: JBoss Web Server 6 (Tomcat 10.x based)
- **Base Image**: `registry.redhat.io/jboss-webserver-6/jws60-openjdk17-openshift-rhel8:6.0.2-2`
- **Platform**: Red Hat OpenShift 4.x
- **Database**: PostgreSQL 15 (external service: `todos-database`)

### Architecture

**Pattern**: Classic 3-layer monolithic REST API

**Knowledge Graph Analysis** (Community 6):
- **Nodes**: 4 (TodoController, TodoService, Todo, TodoRepository)
- **Cohesion**: 0.10 (well-structured, appropriate for layered architecture)
- **Circular Dependencies**: None (clean one-way dependency flow)
- **Coupling**: Low inter-layer coupling

**Layer Dependencies**:
```
TodoController → TodoService → TodoRepository → Todo (Entity)
```

**Architectural Observations**:
1. **Separation of Concerns**: Clear responsibility boundaries between layers
2. **Minimal Coupling**: Each layer depends only on the layer directly below it
3. **No God Classes**: Backend nodes have reasonable complexity (TodoService is central but simple)
4. **Standard Patterns**: Uses standard Spring Boot patterns throughout

### Key Components

#### 1. **TodoController** (REST Layer)
**File**: `src/main/java/org/acme/todo/controller/TodoController.java`

**Responsibilities:**
- Expose REST API endpoints for TODO CRUD operations
- Handle HTTP request/response mapping
- Declare transaction boundaries (currently - should move to service layer)

**Current Implementation**:
```java
@RestController
@CrossOrigin
@RequestMapping("/api/todos")
public class TodoController {
    
    @Autowired
    private TodoService todoService;

    @GetMapping
    public List<Todo> getAllTodos() {
        return todoService.getAllTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> getTodoById(@PathVariable Long id) {
        Optional<Todo> todo = todoService.getTodoById(id);
        return todo.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Transactional
    @PostMapping
    public Todo createTodo(@RequestBody Todo todo) {
        return todoService.createOrUpdateTodo(todo);
    }

    @Transactional
    @PutMapping("/{id}")
    public ResponseEntity<Todo> updateTodo(@PathVariable Long id, @RequestBody Todo todoDetails) {
        Optional<Todo> todo = todoService.getTodoById(id);
        if (todo.isPresent()) {
            Todo todoToUpdate = todo.get();
            todoToUpdate.setTitle(todoDetails.getTitle());
            todoToUpdate.setCompleted(todoDetails.isCompleted());
            todoToUpdate.setOrder(todoDetails.getOrder());
            todoToUpdate.setUrl(todoDetails.getUrl());
            return ResponseEntity.ok(todoService.createOrUpdateTodo(todoToUpdate));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodoById(@PathVariable Long id) {
        todoService.deleteTodoById(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Annotations to Migrate:**
- `@RestController` → `@Path("/api/todos")` (JAX-RS)
- `@RequestMapping` → `@Path` (JAX-RS)
- `@GetMapping` → `@GET` (JAX-RS)
- `@PostMapping` → `@POST` (JAX-RS)
- `@PutMapping` → `@PUT` (JAX-RS)
- `@DeleteMapping` → `@DELETE` (JAX-RS)
- `@PathVariable` → `@PathParam` (JAX-RS)
- `@RequestBody` → Automatic in JAX-RS (no annotation needed)
- `@CrossOrigin` → Remove (configure in application.properties)
- `@Autowired` → `@Inject` (CDI)
- `@Transactional` → Move to service layer (better practice)
- `ResponseEntity<T>` → Direct return type or `Response` (JAX-RS)

#### 2. **TodoService** (Business Logic Layer)
**File**: `src/main/java/org/acme/todo/service/TodoService.java`

**Responsibilities:**
- Orchestrate business logic
- Coordinate transactions (should receive @Transactional from controller)
- Delegate data access to repository

**Current Implementation**:
```java
@Service
public class TodoService {

    @Autowired
    private TodoRepository todoRepository;

    public List<Todo> getAllTodos() {
        return todoRepository.findAll();
    }

    public Optional<Todo> getTodoById(Long id) {
        return todoRepository.findById(id);
    }

    public Todo createOrUpdateTodo(Todo todo) {
        return todoRepository.save(todo);
    }

    public void deleteTodoById(Long id) {
        todoRepository.deleteById(id);
    }
}
```

**Annotations to Migrate:**
- `@Service` → `@ApplicationScoped` (CDI)
- `@Autowired` → `@Inject` (CDI)

**Enhancements Needed:**
- Add `@Transactional` to mutating methods (create, update, delete)
- Consider constructor injection instead of field injection

#### 3. **TodoRepository** (Data Access Layer)
**File**: `src/main/java/org/acme/todo/repository/TodoRepository.java`

**Responsibilities:**
- Abstract database operations
- Provide CRUD operations via Spring Data JPA

**Current Implementation**:
```java
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
}
```

**Migration Options:**

**Option A - Panache Repository** (Recommended):
```java
@ApplicationScoped
public class TodoRepository implements PanacheRepository<Todo> {
    // All CRUD methods auto-provided by Panache
}
```

**Option B - Traditional JPA** (Maintain similarity):
```java
@ApplicationScoped
public class TodoRepository {
    @Inject
    EntityManager entityManager;
    
    public List<Todo> findAll() { /* manual implementation */ }
    public Optional<Todo> findById(Long id) { /* manual implementation */ }
    public Todo save(Todo todo) { /* manual implementation */ }
    public void deleteById(Long id) { /* manual implementation */ }
}
```

**Recommendation**: Use Panache for cleaner code and less boilerplate.

#### 4. **Todo Entity** (Domain Model)
**File**: `src/main/java/org/acme/todo/model/Todo.java`

**Responsibilities:**
- Represent TODO item domain object
- Define JPA entity mapping

**Current Implementation**:
```java
@Entity
@Table(name = "todos")
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String title;

    private boolean completed;

    @Column(name = "ordering")
    private int order;

    @Column(name = "url")
    private String url;

    // Getters and Setters
    // (omitted for brevity)
}
```

**Migration Notes:**
- Already uses `jakarta.persistence.*` (not `javax.persistence.*`) - Quarkus compatible!
- Already uses `jakarta.validation.*` (not `javax.validation.*`) - Quarkus compatible!
- **No changes needed** except optionally extending `PanacheEntity` for Panache pattern

**Panache Option**:
```java
@Entity
@Table(name = "todos")
public class Todo extends PanacheEntity {
    // Remove @Id and @GeneratedValue - PanacheEntity provides these
    
    @NotBlank
    @Column(unique = true)
    private String title;

    private boolean completed;

    @Column(name = "ordering")
    private int order;

    @Column(name = "url")
    private String url;

    // Getters and Setters
}
```

#### 5. **Static Frontend Resources**
**Location**: `src/main/resources/static/`

**Contents:**
- `index.html` - Angular TodoMVC entry point
- `main-*.js` - Compiled Angular application bundle
- `polyfills-*.js` - Browser polyfills
- `styles-*.css` - Application styles

**Migration:**
- Move all files from `src/main/resources/static/` to `src/main/resources/META-INF/resources/`
- Quarkus serves static content from `META-INF/resources/` by default
- No code changes needed - simple file relocation

#### 6. **Application Configuration**
**File**: `src/main/resources/application.properties`

**Current Configuration** (inferred from README and typical Spring Boot setup):
```properties
# Database
spring.datasource.url=jdbc:postgresql://todos-database:5432/todos
spring.datasource.username=jws
spring.datasource.password=jws

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Server (if any custom config)
server.port=8080
```

**Quarkus Equivalent**:
```properties
# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://todos-database:5432/todos
quarkus.datasource.username=jws
quarkus.datasource.password=jws

# Hibernate
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=false

# HTTP
quarkus.http.port=8080

# CORS
quarkus.http.cors=true
quarkus.http.cors.origins=*
```

### Integration Points

**External Systems:**
1. **PostgreSQL Database**
   - Service: `todos-database` (Kubernetes service)
   - Port: 5432
   - Database: `todos`
   - Schema: Single table (`todos`)
   - Connection: JDBC driver (org.postgresql.Driver)

**No Other External Dependencies:**
- No message queues
- No caching layers
- No external APIs
- No authentication services
- Self-contained application

### Code Examples

**Example 1: Current REST Endpoint Pattern**
```java
@GetMapping("/{id}")
public ResponseEntity<Todo> getTodoById(@PathVariable Long id) {
    Optional<Todo> todo = todoService.getTodoById(id);
    return todo.map(ResponseEntity::ok)
               .orElseGet(() -> ResponseEntity.notFound().build());
}
```

**Example 2: Current Dependency Injection Pattern**
```java
@Autowired
private TodoService todoService;
```

**Example 3: Current Transaction Management**
```java
@Transactional
@PostMapping
public Todo createTodo(@RequestBody Todo todo) {
    return todoService.createOrUpdateTodo(todo);
}
```

---

## Target State

### Technology Stack

**Core Framework:**
- **Quarkus**: 3.x (latest stable - 3.8+ recommended)
- **Java**: 17 (OpenJDK - maintain current)
- **Build Tool**: Maven with Quarkus Maven Plugin
- **Packaging**: JAR file (JVM mode) or native binary (GraalVM mode)

**Required Quarkus Extensions:**
```xml
<!-- Core -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-reactive-jackson</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-orm-panache</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-h2</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-validator</artifactId>
</dependency>

<!-- Monitoring -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-health</artifactId>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit5</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

**Deployment:**
- **Container**: Red Hat UBI 9 with OpenJDK 17
- **Base Image**: `registry.access.redhat.com/ubi9/openjdk-17` or `ubi9/openjdk-17-runtime` (multi-stage)
- **Platform**: Red Hat OpenShift 4.x (same namespace)
- **Database**: PostgreSQL 15 (reuse existing `todos-database` service)

### Architecture Changes

**Pattern**: Remains 3-layer monolithic REST API (no architectural changes)

**Framework Changes Only:**
- Spring Boot → Quarkus (framework swap)
- Spring MVC → JAX-RS (REST API standard)
- Spring Data JPA → Hibernate ORM with Panache (JPA implementation with sugar)
- Spring DI → CDI (Dependency Injection standard)

**Key Architectural Improvements:**
1. **Transaction Boundaries**: Move `@Transactional` from controller to service layer (best practice)
2. **Dependency Injection**: Switch from field injection to constructor injection (recommended)
3. **Resource Efficiency**: Quarkus optimizations reduce startup time and memory footprint
4. **Cloud-Native**: Better Kubernetes integration, health checks, metrics out-of-the-box

**No Changes To:**
- Database schema
- API contracts (same endpoints, same request/response formats)
- Frontend code
- Business logic
- Domain model

### Target Component Structure

#### 1. **TodoResource** (REST Layer)
**File**: `src/main/java/org/acme/todo/resource/TodoResource.java`

**Quarkus Implementation** (target code):
```java
package org.acme.todo.resource;

import org.acme.todo.model.Todo;
import org.acme.todo.service.TodoService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/todos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TodoResource {

    @Inject
    TodoService todoService;

    @GET
    public List<Todo> getAllTodos() {
        return todoService.getAllTodos();
    }

    @GET
    @Path("/{id}")
    public Response getTodoById(@PathParam("id") Long id) {
        return todoService.getTodoById(id)
                .map(todo -> Response.ok(todo).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response createTodo(Todo todo) {
        Todo created = todoService.createOrUpdateTodo(todo);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateTodo(@PathParam("id") Long id, Todo todoDetails) {
        return todoService.getTodoById(id)
                .map(existing -> {
                    existing.setTitle(todoDetails.getTitle());
                    existing.setCompleted(todoDetails.isCompleted());
                    existing.setOrder(todoDetails.getOrder());
                    existing.setUrl(todoDetails.getUrl());
                    Todo updated = todoService.createOrUpdateTodo(existing);
                    return Response.ok(updated).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTodoById(@PathParam("id") Long id) {
        todoService.deleteTodoById(id);
        return Response.noContent().build();
    }
}
```

**Key Changes from Spring:**
- `@RestController` → `@Path`, `@Produces`, `@Consumes`
- `@RequestMapping` → `@Path`
- `@GetMapping` → `@GET`
- `@PostMapping` → `@POST`
- `@PutMapping` → `@PUT`
- `@DeleteMapping` → `@DELETE`
- `@PathVariable` → `@PathParam`
- `@RequestBody` → Removed (automatic in JAX-RS)
- `@Autowired` → `@Inject`
- `ResponseEntity<T>` → `Response` with builder pattern
- `@Transactional` → Removed (moved to service layer)

#### 2. **TodoService** (Business Logic Layer)
**File**: `src/main/java/org/acme/todo/service/TodoService.java`

**Quarkus Implementation** (target code):
```java
package org.acme.todo.service;

import org.acme.todo.model.Todo;
import org.acme.todo.repository.TodoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TodoService {

    @Inject
    TodoRepository todoRepository;

    public List<Todo> getAllTodos() {
        return todoRepository.listAll();
    }

    public Optional<Todo> getTodoById(Long id) {
        return todoRepository.findByIdOptional(id);
    }

    @Transactional
    public Todo createOrUpdateTodo(Todo todo) {
        todoRepository.persist(todo);
        return todo;
    }

    @Transactional
    public void deleteTodoById(Long id) {
        todoRepository.deleteById(id);
    }
}
```

**Key Changes from Spring:**
- `@Service` → `@ApplicationScoped`
- `@Autowired` → `@Inject`
- Added `@Transactional` to mutating methods (createOrUpdateTodo, deleteTodoById)
- Repository method names change: `findAll()` → `listAll()`, `findById()` → `findByIdOptional()`, `save()` → `persist()`

#### 3. **TodoRepository** (Data Access Layer)
**File**: `src/main/java/org/acme/todo/repository/TodoRepository.java`

**Quarkus Implementation with Panache** (target code):
```java
package org.acme.todo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.acme.todo.model.Todo;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TodoRepository implements PanacheRepository<Todo> {
    // All CRUD methods inherited from PanacheRepository:
    // - listAll()
    // - findByIdOptional(Long id)
    // - persist(Todo entity)
    // - deleteById(Long id)
    // - count()
    // - findAll()
    // - stream()
    // ... and many more
}
```

**Key Changes from Spring:**
- Interface extends `JpaRepository<Todo, Long>` → Class implements `PanacheRepository<Todo>`
- `@Repository` → `@ApplicationScoped`
- All CRUD methods now inherited from Panache (no need to declare)

#### 4. **Todo Entity** (Domain Model)
**File**: `src/main/java/org/acme/todo/model/Todo.java`

**Option A - Keep as-is** (minimal change):
```java
// Same as current - already Jakarta-compliant, works with Quarkus
```

**Option B - Use Panache Entity** (recommended):
```java
package org.acme.todo.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "todos")
public class Todo extends PanacheEntity {
    // id field inherited from PanacheEntity

    @NotBlank
    @Column(unique = true)
    public String title;

    public boolean completed;

    @Column(name = "ordering")
    public int order;

    @Column(name = "url")
    public String url;
}
```

**Key Changes if using Panache:**
- Extends `PanacheEntity` (provides `id` field automatically)
- Remove `@Id` and `@GeneratedValue` annotations
- Public fields instead of private fields + getters/setters (Panache convention)
- Getters/setters auto-generated at build time

**Recommendation**: Use Panache entity pattern for consistency and less boilerplate.

---

## Migration Scenario

### Migration Type

**Classification**: **Replatform** (framework swap, no architectural changes)

**Not:**
- Refactor (architecture remains the same)
- Rearchitect (not moving to microservices)
- Rebuild (not rewriting from scratch)

### Migration Strategy

**Approach**: **Big Bang Migration**

**Justification:**
1. **Small Codebase**: Only 6 Java files
2. **Low Complexity**: Simple CRUD operations, no complex features
3. **Clean Architecture**: Zero circular dependencies
4. **Direct Equivalents**: All Spring features have Quarkus counterparts
5. **Fast Timeline**: 1-2 days development time achievable
6. **Low Risk**: Migration complexity = LOW (high confidence from graph analysis)

**Migration Phases:**
1. **Setup** (2-3 hours): Bootstrap Quarkus project, configure extensions
2. **Code Migration** (6-8 hours): Migrate all layers sequentially (entity → repository → service → controller)
3. **Testing** (2-4 hours): Write and execute tests
4. **Containerization** (2-3 hours): Build and test container images
5. **Deployment** (2-3 hours): Deploy to OpenShift, validate

**Total Duration**: 14-21 hours (1.75-2.6 days)

### Risk Assessment

**Overall Risk Level**: **LOW**

**Technical Risks:**

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Database connection issues | Low | High | Test early, validate config in dev mode |
| CORS misconfiguration | Medium | Medium | Capture current behavior in tests |
| Transaction boundary issues | Low | Medium | Move @Transactional to service layer |
| Performance regression | Very Low | Medium | Baseline before migration, monitor after |
| OpenShift deployment failure | Low | High | Test container locally first, keep Spring Boot running |

**Non-Technical Risks:**

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Timeline slip | Low | Medium | Conservative estimates, buffer time |
| Learning curve | Very Low | Low | Team experienced with Quarkus |

### Success Criteria

**Functional Success:**
- ✅ All 5 REST endpoints functional (GET all, GET by ID, POST, PUT, DELETE)
- ✅ Database operations working (CRUD against PostgreSQL)
- ✅ Angular frontend fully operational
- ✅ CORS working correctly
- ✅ Validation working (Jakarta Bean Validation)

**Non-Functional Success:**
- ✅ Startup time < 1 second (JVM mode) or < 100ms (native mode)
- ✅ Memory footprint < 150 MB RSS (JVM) or < 50 MB RSS (native)
- ✅ Response times equal to or better than Spring Boot baseline
- ✅ Zero downtime during deployment cutover

**Quality Success:**
- ✅ All tests passing (>80% code coverage)
- ✅ No security vulnerabilities in container image
- ✅ Health checks passing
- ✅ Logs and metrics accessible in OpenShift

---

## Observations and Challenges

### Technical Challenges

**Challenge 1: Transaction Management Anti-Pattern**
- **Observation**: Current code has `@Transactional` on controller methods
- **Issue**: Controller should not manage transactions (separation of concerns)
- **Solution**: Move `@Transactional` to service layer methods
- **Impact**: Better testability, clearer responsibility boundaries

**Challenge 2: Field Injection**
- **Observation**: Current code uses `@Autowired` field injection
- **Issue**: Field injection harder to test, hides dependencies
- **Solution**: Migrate to constructor injection with `@Inject`
- **Impact**: More testable code, explicit dependencies

**Challenge 3: ResponseEntity Pattern**
- **Observation**: Spring uses `ResponseEntity<T>` for flexible responses
- **Issue**: JAX-RS uses different pattern (`Response` builder or direct returns)
- **Solution**: Use JAX-RS `Response` with builder pattern for non-200 cases
- **Impact**: Different code style, same functionality

**Challenge 4: CORS Configuration**
- **Observation**: Current code uses `@CrossOrigin` annotation
- **Issue**: Quarkus configures CORS in `application.properties`, not annotations
- **Solution**: Remove annotation, add Quarkus CORS properties
- **Impact**: Configuration change only, no code impact

### Dependencies

**Critical Dependencies:**
1. **PostgreSQL Database**: Must be accessible at `todos-database:5432`
   - Mitigation: Reuse existing OpenShift service, no changes needed
2. **OpenShift Namespace**: Deployment namespace must exist
   - Mitigation: Deploy to current namespace specified by user

**Library Dependencies:**
All Spring dependencies have direct Quarkus equivalents (see Target State section).

### Data Migration

**Database Schema:**
- **No schema changes required**
- Current schema created by Spring Boot JPA works with Quarkus Hibernate
- `quarkus.hibernate-orm.database.generation=update` maintains compatibility
- Existing data in `todos` table remains untouched

**Configuration Migration:**
- Spring Boot `application.properties` → Quarkus `application.properties`
- Property name changes only (values remain the same)
- See Target State section for property mapping

---

## Appendix: Gap Analysis Summary

| Aspect | Current (Spring Boot) | Target (Quarkus) | Gap |
|--------|----------------------|------------------|-----|
| **Framework** | Spring Boot 3.2.5 | Quarkus 3.x | Complete swap |
| **REST** | Spring MVC | JAX-RS (RESTEasy Reactive) | Annotation changes |
| **DI** | Spring DI (@Autowired) | CDI (@Inject) | Annotation changes |
| **Persistence** | Spring Data JPA | Hibernate ORM with Panache | API changes |
| **Validation** | Jakarta Validation | Jakarta Validation | No change |
| **Database** | PostgreSQL JDBC | PostgreSQL JDBC | No change |
| **Packaging** | WAR | JAR | Build config change |
| **Container** | JBoss Web Server image | UBI OpenJDK image | Dockerfile change |
| **Health** | Spring Boot Actuator | SmallRye Health | Endpoint changes |
| **Config** | application.properties | application.properties | Property name changes |

**Lines of Code to Change**: ~200-300 LOC (out of ~500 total backend LOC)  
**Files to Change**: 6 Java files, 1 pom.xml, 1 application.properties, 1 Dockerfile

---

**End of Specification**
