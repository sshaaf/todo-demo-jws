# Migration Tasks: Spring Boot → Quarkus

**Project**: todo-demo-jws  
**Branch**: migrate-to-quarkus  
**Generated**: 2026-06-03  
**Total Estimated Duration**: 14-21 hours

---

## Task Group 1: Project Setup and Configuration

**Duration**: 2-3 hours  
**Dependencies**: None  
**Risk**: Low

- [ ] 1.1 Create new Quarkus project with required extensions
  - Command: `quarkus create app org.acme:todo-demo-quarkus --extension='resteasy-reactive-jackson,hibernate-orm-panache,jdbc-postgresql,jdbc-h2,hibernate-validator,smallrye-health'`
  - Verify: Project structure created with `pom.xml`, `src/main/java`, `src/main/resources`
  - Acceptance: `mvn compile` succeeds

- [ ] 1.2 Configure Maven build in `pom.xml`
  - Copy `<groupId>`, `<artifactId>`, `<version>` from Spring Boot pom.xml
  - Add Quarkus Maven plugin configuration
  - Add JaCoCo for code coverage reporting
  - Verify: All Quarkus extensions present in dependencies section
  - Acceptance: `mvn clean package` produces quarkus-app/ directory

- [ ] 1.3 Migrate `application.properties` configuration
  - Create `src/main/resources/application.properties`
  - Migrate database configuration:
    ```properties
    # Spring Boot format:
    spring.datasource.url=jdbc:postgresql://todos-database:5432/todos
    spring.datasource.username=jws
    spring.datasource.password=jws
    spring.jpa.hibernate.ddl-auto=update
    
    # Quarkus format:
    quarkus.datasource.db-kind=postgresql
    quarkus.datasource.jdbc.url=jdbc:postgresql://todos-database:5432/todos
    quarkus.datasource.username=jws
    quarkus.datasource.password=jws
    quarkus.hibernate-orm.database.generation=update
    ```
  - Add CORS configuration:
    ```properties
    quarkus.http.cors=true
    quarkus.http.cors.origins=*
    quarkus.http.cors.methods=GET,POST,PUT,DELETE,OPTIONS
    quarkus.http.cors.headers=accept,authorization,content-type
    ```
  - Add HTTP port: `quarkus.http.port=8080`
  - Add health check configuration: `quarkus.health.extensions.enabled=true`
  - Acceptance: Configuration file syntax valid, no missing properties

- [ ] 1.4 Set up H2 configuration for dev/test
  - Create `src/test/resources/application.properties` with H2 config:
    ```properties
    quarkus.datasource.db-kind=h2
    quarkus.datasource.jdbc.url=jdbc:h2:mem:testdb
    quarkus.hibernate-orm.database.generation=drop-and-create
    ```
  - Verify: H2 dependency present in pom.xml with test scope
  - Acceptance: Tests can run against in-memory H2 database

- [ ] 1.5 Verify Quarkus dev mode works
  - Run: `mvn quarkus:dev`
  - Access Dev UI: http://localhost:8080/q/dev
  - Check health endpoint: http://localhost:8080/q/health
  - Verify live reload works (change a file, see automatic recompilation)
  - Acceptance: Dev mode starts successfully, Dev UI accessible, health returns UP

- [ ] 1.6 Configure Git for new structure
  - Add Quarkus-specific patterns to `.gitignore`:
    ```
    target/
    .quarkus/
    .mvn/
    ```
  - Commit project skeleton: `git add . && git commit -m "Quarkus project setup"`
  - Acceptance: Project skeleton committed to migrate-to-quarkus branch

---

## Task Group 2: Entity Layer Migration

**Duration**: 1 hour  
**Dependencies**: Task Group 1 (project setup)  
**Risk**: Low

- [ ] 2.1 Copy Todo entity to Quarkus project
  - Copy `src/main/java/org/acme/todo/model/Todo.java` from Spring Boot project
  - Verify package: `org.acme.todo.model`
  - Verify imports: `jakarta.persistence.*`, `jakarta.validation.*` (not `javax.*`)
  - Acceptance: File exists in correct location

- [ ] 2.2 Modify Todo entity for Panache pattern
  - Add import: `import io.quarkus.hibernate.orm.panache.PanacheEntity;`
  - Change class declaration: `public class Todo extends PanacheEntity`
  - Remove `@Id` and `@GeneratedValue` annotations (inherited from PanacheEntity)
  - Remove `private Long id;` field (inherited from PanacheEntity)
  - Change field visibility: `private` → `public` (Panache convention)
  - Remove all getter/setter methods (Panache auto-generates via byte code)
  - Keep validation annotations: `@NotBlank`, `@Column(unique = true)`, etc.
  - **Before**:
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
        
        // getters and setters
    }
    ```
  - **After**:
    ```java
    @Entity
    @Table(name = "todos")
    public class Todo extends PanacheEntity {
        // id inherited from PanacheEntity
        
        @NotBlank
        @Column(unique = true)
        public String title;
        
        public boolean completed;
        
        @Column(name = "ordering")
        public int order;
        
        @Column(name = "url")
        public String url;
        
        // No getters/setters needed - Panache provides them
    }
    ```
  - Acceptance: Entity compiles successfully

- [ ] 2.3 Create entity unit tests
  - Create `src/test/java/org/acme/todo/model/TodoTest.java`
  - Test valid Todo creation and field access
  - Test Jakarta validation (`@NotBlank` constraint)
  - Test inherited PanacheEntity methods (getId())
  - Example test:
    ```java
    @QuarkusTest
    class TodoTest {
        
        @Inject
        Validator validator;
        
        @Test
        void testValidTodo() {
            Todo todo = new Todo();
            todo.title = "Test Todo";
            todo.completed = false;
            todo.order = 1;
            
            Set<ConstraintViolation<Todo>> violations = validator.validate(todo);
            assertTrue(violations.isEmpty());
        }
        
        @Test
        void testBlankTitle() {
            Todo todo = new Todo();
            todo.title = "";  // Violates @NotBlank
            
            Set<ConstraintViolation<Todo>> violations = validator.validate(todo);
            assertFalse(violations.isEmpty());
            assertEquals("must not be blank", violations.iterator().next().getMessage());
        }
    }
    ```
  - Acceptance: `mvn test` passes all entity tests

- [ ] 2.4 **Test Generation**: Use mig-test-gen for comprehensive entity tests
  - Skill invocation: `/skill mig-test-gen` targeting entity layer
  - Context: Generate tests for Todo entity validation, field constraints, edge cases
  - Expected output:
    - Validation tests for all constraints (`@NotBlank`, `@Column(unique=true)`)
    - Edge case tests (null values, empty strings, special characters)
    - Panache entity method tests
  - Acceptance: All generated tests pass

- [ ] 2.5 **Documentation**: Document entity migration decisions
  - Update `README.md` with Panache entity pattern explanation
  - Document why public fields are used (Panache convention)
  - Note that getters/setters are auto-generated at build time
  - Add section on Jakarta vs javax annotations
  - Acceptance: Documentation clear and complete

---

## Task Group 3: Repository Layer Migration

**Duration**: 1 hour  
**Dependencies**: Task Group 2 (entity layer)  
**Risk**: Low

- [ ] 3.1 Create TodoRepository with Panache
  - Create `src/main/java/org/acme/todo/repository/TodoRepository.java`
  - Implement `PanacheRepository<Todo>` interface
  - Add `@ApplicationScoped` annotation
  - Import: `import io.quarkus.hibernate.orm.panache.PanacheRepository;`
  - Import: `import jakarta.enterprise.context.ApplicationScoped;`
  - Implementation:
    ```java
    package org.acme.todo.repository;
    
    import io.quarkus.hibernate.orm.panache.PanacheRepository;
    import org.acme.todo.model.Todo;
    import jakarta.enterprise.context.ApplicationScoped;
    
    @ApplicationScoped
    public class TodoRepository implements PanacheRepository<Todo> {
        // All CRUD methods auto-provided by PanacheRepository:
        // - List<Todo> listAll()
        // - Optional<Todo> findByIdOptional(Long id)
        // - void persist(Todo entity)
        // - void deleteById(Long id)
        // - long count()
        // - Stream<Todo> stream()
        // - PanacheQuery<Todo> findAll()
    }
    ```
  - Acceptance: Repository compiles successfully

- [ ] 3.2 Create repository integration tests
  - Create `src/test/java/org/acme/todo/repository/TodoRepositoryTest.java`
  - Use `@QuarkusTest` for integration test context
  - Use `@TestTransaction` for automatic transaction rollback
  - Test all CRUD operations against H2 database:
    - `persist()` - Create new Todo
    - `listAll()` - Retrieve all Todos
    - `findByIdOptional()` - Retrieve by ID
    - `deleteById()` - Delete Todo
    - `count()` - Count Todos
  - Example test:
    ```java
    @QuarkusTest
    @TestTransaction
    class TodoRepositoryTest {
        
        @Inject
        TodoRepository todoRepository;
        
        @Test
        void testPersistAndFind() {
            Todo todo = new Todo();
            todo.title = "Repository Test";
            todo.completed = false;
            todo.order = 1;
            
            todoRepository.persist(todo);
            assertNotNull(todo.id);  // ID assigned after persist
            
            Optional<Todo> found = todoRepository.findByIdOptional(todo.id);
            assertTrue(found.isPresent());
            assertEquals("Repository Test", found.get().title);
        }
        
        @Test
        void testFindAll() {
            long countBefore = todoRepository.count();
            
            Todo todo1 = new Todo();
            todo1.title = "Test 1";
            todoRepository.persist(todo1);
            
            Todo todo2 = new Todo();
            todo2.title = "Test 2";
            todoRepository.persist(todo2);
            
            List<Todo> all = todoRepository.listAll();
            assertEquals(countBefore + 2, all.size());
        }
        
        @Test
        void testDeleteById() {
            Todo todo = new Todo();
            todo.title = "To Delete";
            todoRepository.persist(todo);
            Long id = todo.id;
            
            todoRepository.deleteById(id);
            
            Optional<Todo> found = todoRepository.findByIdOptional(id);
            assertFalse(found.isPresent());
        }
    }
    ```
  - Acceptance: `mvn test` passes all repository tests

- [ ] 3.3 Test PostgreSQL connection (local or OpenShift)
  - Start PostgreSQL locally or connect to OpenShift service:
    ```bash
    podman run --name todos-database -p 5432:5432 \
      -e POSTGRES_USER=jws \
      -e POSTGRES_PASSWORD=jws \
      -e POSTGRES_DB=todos \
      -d postgres:15-alpine
    ```
  - Update `application.properties` with local PostgreSQL URL
  - Run Quarkus dev mode: `mvn quarkus:dev`
  - Verify Hibernate creates `todos` table
  - Test CRUD operations via Dev UI or curl
  - Acceptance: Can create, read, update, delete todos in PostgreSQL

- [ ] 3.4 **Test Generation**: Use mig-test-gen for repository tests
  - Skill invocation: `/skill mig-test-gen` targeting repository layer
  - Context: Generate tests for all Panache repository methods, edge cases, concurrency
  - Expected output:
    - Tests for all CRUD operations
    - Transaction rollback tests
    - Concurrent access tests
    - Edge cases (null IDs, non-existent records)
  - Acceptance: All generated tests pass

- [ ] 3.5 **Documentation**: Document repository pattern
  - Update `README.md` with Panache repository pattern explanation
  - Document available Panache methods and when to use them
  - Add examples of custom query methods (if needed in future)
  - Note difference from Spring Data JPA repositories
  - Acceptance: Documentation clear and complete

---

## Task Group 4: Service Layer Migration

**Duration**: 1 hour  
**Dependencies**: Task Group 3 (repository layer)  
**Risk**: Low

- [ ] 4.1 Copy TodoService to Quarkus project
  - Copy `src/main/java/org/acme/todo/service/TodoService.java` from Spring Boot project
  - Verify package: `org.acme.todo.service`
  - Acceptance: File exists in correct location

- [ ] 4.2 Migrate TodoService annotations and dependencies
  - Replace `@Service` → `@ApplicationScoped`
  - Replace `@Autowired` → `@Inject` (or use constructor injection)
  - Add `@Transactional` to mutating methods: `createOrUpdateTodo()`, `deleteTodoById()`
  - Update repository method calls:
    - `findAll()` → `listAll()`
    - `findById(Long)` → `findByIdOptional(Long)`
    - `save(Todo)` → `persist(Todo)` (returns void, modifies entity in-place)
    - `deleteById(Long)` → `deleteById(Long)` (same name, but returns void)
  - **Before** (Spring Boot):
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
  - **After** (Quarkus):
    ```java
    import jakarta.enterprise.context.ApplicationScoped;
    import jakarta.inject.Inject;
    import jakarta.transaction.Transactional;
    import org.acme.todo.model.Todo;
    import org.acme.todo.repository.TodoRepository;
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
            return todo;  // persist() modifies todo in-place, returns void
        }
        
        @Transactional
        public void deleteTodoById(Long id) {
            todoRepository.deleteById(id);
        }
    }
    ```
  - Acceptance: Service compiles successfully

- [ ] 4.3 Create service unit tests (with mocked repository)
  - Create `src/test/java/org/acme/todo/service/TodoServiceTest.java`
  - Use `@QuarkusTest` and `@InjectMock` for mocking repository
  - Test all service methods with mocked repository responses
  - Example test:
    ```java
    @QuarkusTest
    class TodoServiceTest {
        
        @InjectMock
        TodoRepository todoRepository;
        
        @Inject
        TodoService todoService;
        
        @Test
        void testGetAllTodos() {
            Todo todo1 = new Todo();
            todo1.id = 1L;
            todo1.title = "Test 1";
            
            Todo todo2 = new Todo();
            todo2.id = 2L;
            todo2.title = "Test 2";
            
            when(todoRepository.listAll()).thenReturn(List.of(todo1, todo2));
            
            List<Todo> result = todoService.getAllTodos();
            
            assertEquals(2, result.size());
            verify(todoRepository).listAll();
        }
        
        @Test
        void testGetTodoById() {
            Todo todo = new Todo();
            todo.id = 1L;
            todo.title = "Test";
            
            when(todoRepository.findByIdOptional(1L)).thenReturn(Optional.of(todo));
            
            Optional<Todo> result = todoService.getTodoById(1L);
            
            assertTrue(result.isPresent());
            assertEquals("Test", result.get().title);
            verify(todoRepository).findByIdOptional(1L);
        }
        
        @Test
        void testCreateTodo() {
            Todo todo = new Todo();
            todo.title = "New Todo";
            
            doNothing().when(todoRepository).persist(any(Todo.class));
            
            Todo result = todoService.createOrUpdateTodo(todo);
            
            assertEquals("New Todo", result.title);
            verify(todoRepository).persist(todo);
        }
    }
    ```
  - Acceptance: `mvn test` passes all service tests

- [ ] 4.4 Test transaction behavior (rollback on exception)
  - Create test that triggers exception in transactional method
  - Verify transaction rolls back (no data persisted)
  - Example test:
    ```java
    @Test
    void testTransactionRollback() {
        Todo todo = new Todo();
        todo.title = "Test";
        
        doThrow(new RuntimeException("DB Error")).when(todoRepository).persist(any());
        
        assertThrows(RuntimeException.class, () -> {
            todoService.createOrUpdateTodo(todo);
        });
        
        // Verify rollback: count should not increase
        verify(todoRepository).persist(todo);
    }
    ```
  - Acceptance: Transaction rollback test passes

- [ ] 4.5 **Test Generation**: Use mig-test-gen for service tests
  - Skill invocation: `/skill mig-test-gen` targeting service layer
  - Context: Generate tests for service methods, transaction boundaries, error handling
  - Expected output:
    - Tests for all service methods with various scenarios
    - Transaction rollback tests
    - Error handling tests
    - Mock configuration examples
  - Acceptance: All generated tests pass

- [ ] 4.6 **Documentation**: Document service layer patterns
  - Update `README.md` with transaction management explanation
  - Document when to use `@Transactional` (mutating operations)
  - Note difference between Panache `persist()` and Spring Data JPA `save()`
  - Add section on constructor injection vs field injection
  - Acceptance: Documentation clear and complete

---

## Task Group 5: REST API Layer Migration (Controller → Resource)

**Duration**: 2-3 hours  
**Dependencies**: Task Group 4 (service layer)  
**Risk**: Medium (most annotation changes)

- [ ] 5.1 Create TodoResource (JAX-RS Resource class)
  - Create `src/main/java/org/acme/todo/resource/TodoResource.java`
  - Note: Package name changes from `controller` to `resource` (JAX-RS convention)
  - Add class-level annotations:
    - `@Path("/api/todos")` (base path for all endpoints)
    - `@Produces(MediaType.APPLICATION_JSON)` (all methods return JSON)
    - `@Consumes(MediaType.APPLICATION_JSON)` (all methods accept JSON)
  - Inject TodoService:
    ```java
    @Inject
    TodoService todoService;
    ```
  - Acceptance: Resource class skeleton compiles

- [ ] 5.2 Migrate GET /api/todos endpoint (getAllTodos)
  - **Spring Boot version**:
    ```java
    @GetMapping
    public List<Todo> getAllTodos() {
        return todoService.getAllTodos();
    }
    ```
  - **Quarkus version**:
    ```java
    @GET
    public List<Todo> getAllTodos() {
        return todoService.getAllTodos();
    }
    ```
  - Changes: `@GetMapping` → `@GET`
  - Return type: Same (direct return of List<Todo>, JAX-RS auto-converts to JSON)
  - Acceptance: Method compiles successfully

- [ ] 5.3 Migrate GET /api/todos/{id} endpoint (getTodoById)
  - **Spring Boot version**:
    ```java
    @GetMapping("/{id}")
    public ResponseEntity<Todo> getTodoById(@PathVariable Long id) {
        Optional<Todo> todo = todoService.getTodoById(id);
        return todo.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }
    ```
  - **Quarkus version**:
    ```java
    @GET
    @Path("/{id}")
    public Response getTodoById(@PathParam("id") Long id) {
        Optional<Todo> todo = todoService.getTodoById(id);
        return todo.map(t -> Response.ok(t).build())
                   .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    ```
  - Changes:
    - `@GetMapping("/{id}")` → `@GET @Path("/{id}")`
    - `@PathVariable` → `@PathParam`
    - `ResponseEntity<Todo>` → `Response`
    - `ResponseEntity.ok()` → `Response.ok().build()`
    - `ResponseEntity.notFound()` → `Response.status(404)`
  - Acceptance: Method compiles, returns 200 for existing, 404 for non-existent

- [ ] 5.4 Migrate POST /api/todos endpoint (createTodo)
  - **Spring Boot version**:
    ```java
    @Transactional
    @PostMapping
    public Todo createTodo(@RequestBody Todo todo) {
        return todoService.createOrUpdateTodo(todo);
    }
    ```
  - **Quarkus version**:
    ```java
    @POST
    public Response createTodo(Todo todo) {
        Todo created = todoService.createOrUpdateTodo(todo);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }
    ```
  - Changes:
    - `@PostMapping` → `@POST`
    - `@RequestBody` → Remove (automatic in JAX-RS)
    - `@Transactional` → Remove (moved to service layer)
    - Return `Response` with 201 status instead of direct entity
  - Acceptance: Method compiles, returns 201 with created entity in body

- [ ] 5.5 Migrate PUT /api/todos/{id} endpoint (updateTodo)
  - **Spring Boot version**:
    ```java
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
    ```
  - **Quarkus version** (with Panache entity public fields):
    ```java
    @PUT
    @Path("/{id}")
    public Response updateTodo(@PathParam("id") Long id, Todo todoDetails) {
        Optional<Todo> todo = todoService.getTodoById(id);
        return todo.map(existing -> {
            existing.title = todoDetails.title;
            existing.completed = todoDetails.completed;
            existing.order = todoDetails.order;
            existing.url = todoDetails.url;
            Todo updated = todoService.createOrUpdateTodo(existing);
            return Response.ok(updated).build();
        }).orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    ```
  - Changes:
    - `@PutMapping("/{id}")` → `@PUT @Path("/{id}")`
    - `@PathVariable` → `@PathParam`
    - `@RequestBody` → Remove
    - `@Transactional` → Remove (moved to service layer)
    - `setters` → Direct field access (Panache entity public fields)
    - `ResponseEntity` → `Response`
  - Acceptance: Method compiles, returns 200 with updated entity or 404

- [ ] 5.6 Migrate DELETE /api/todos/{id} endpoint (deleteTodoById)
  - **Spring Boot version**:
    ```java
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodoById(@PathVariable Long id) {
        todoService.deleteTodoById(id);
        return ResponseEntity.noContent().build();
    }
    ```
  - **Quarkus version**:
    ```java
    @DELETE
    @Path("/{id}")
    public Response deleteTodoById(@PathParam("id") Long id) {
        todoService.deleteTodoById(id);
        return Response.noContent().build();
    }
    ```
  - Changes:
    - `@DeleteMapping("/{id}")` → `@DELETE @Path("/{id}")`
    - `@PathVariable` → `@PathParam`
    - `@Transactional` → Remove (moved to service layer)
    - `ResponseEntity<Void>` → `Response`
    - `ResponseEntity.noContent()` → `Response.noContent()`
  - Acceptance: Method compiles, returns 204 No Content

- [ ] 5.7 Remove @CrossOrigin annotation (CORS now in application.properties)
  - Spring Boot version had `@CrossOrigin` on controller class
  - Quarkus: CORS configured in `application.properties` (already done in Task 1.3)
  - Verify no `@CrossOrigin` annotation on TodoResource class
  - Acceptance: No @CrossOrigin annotation, CORS still works via config

- [ ] 5.8 Create REST API integration tests
  - Create `src/test/java/org/acme/todo/resource/TodoResourceTest.java`
  - Use `@QuarkusTest` for full stack testing
  - Use REST Assured for HTTP testing
  - Test all endpoints:
    ```java
    @QuarkusTest
    class TodoResourceTest {
        
        @Test
        @TestTransaction
        void testGetAllTodos() {
            given()
                .when().get("/api/todos")
                .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("$.size()", greaterThanOrEqualTo(0));
        }
        
        @Test
        @TestTransaction
        void testCreateTodo() {
            Todo todo = new Todo();
            todo.title = "REST Test Todo";
            todo.completed = false;
            todo.order = 1;
            
            given()
                .contentType(ContentType.JSON)
                .body(todo)
                .when().post("/api/todos")
                .then()
                    .statusCode(201)
                    .body("title", equalTo("REST Test Todo"))
                    .body("completed", equalTo(false))
                    .body("id", notNullValue());
        }
        
        @Test
        @TestTransaction
        void testGetTodoById() {
            // Create a todo first
            Todo todo = new Todo();
            todo.title = "Get By ID Test";
            Todo.persist(todo);
            
            given()
                .when().get("/api/todos/" + todo.id)
                .then()
                    .statusCode(200)
                    .body("title", equalTo("Get By ID Test"));
        }
        
        @Test
        void testGetNonExistentTodo() {
            given()
                .when().get("/api/todos/99999")
                .then()
                    .statusCode(404);
        }
        
        @Test
        @TestTransaction
        void testUpdateTodo() {
            Todo todo = new Todo();
            todo.title = "Original Title";
            todo.completed = false;
            Todo.persist(todo);
            
            Todo update = new Todo();
            update.title = "Updated Title";
            update.completed = true;
            
            given()
                .contentType(ContentType.JSON)
                .body(update)
                .when().put("/api/todos/" + todo.id)
                .then()
                    .statusCode(200)
                    .body("title", equalTo("Updated Title"))
                    .body("completed", equalTo(true));
        }
        
        @Test
        @TestTransaction
        void testDeleteTodo() {
            Todo todo = new Todo();
            todo.title = "To Delete";
            Todo.persist(todo);
            
            given()
                .when().delete("/api/todos/" + todo.id)
                .then()
                    .statusCode(204);
            
            // Verify deleted
            given()
                .when().get("/api/todos/" + todo.id)
                .then()
                    .statusCode(404);
        }
    }
    ```
  - Acceptance: All REST API tests pass

- [ ] 5.9 Test CORS headers
  - Send OPTIONS preflight request to `/api/todos`
  - Verify response headers include:
    - `Access-Control-Allow-Origin: *`
    - `Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS`
    - `Access-Control-Allow-Headers: accept,authorization,content-type`
  - Example test:
    ```java
    @Test
    void testCorsHeaders() {
        given()
            .header("Origin", "http://example.com")
            .header("Access-Control-Request-Method", "POST")
            .when().options("/api/todos")
            .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", containsString("POST"));
    }
    ```
  - Acceptance: CORS test passes

- [ ] 5.10 **Test Generation**: Use mig-test-gen for REST API tests
  - Skill invocation: `/skill mig-test-gen` targeting REST API layer
  - Context: Generate comprehensive REST API tests, edge cases, error scenarios
  - Expected output:
    - Tests for all HTTP methods and endpoints
    - Validation error tests (400 Bad Request)
    - Not found tests (404)
    - CORS tests
    - Edge cases (invalid JSON, large payloads, special characters)
  - Acceptance: All generated tests pass

- [ ] 5.11 **Documentation**: Document REST API migration
  - Update `README.md` with JAX-RS vs Spring MVC comparison table
  - Document REST API endpoints (paths, methods, request/response formats)
  - Add examples of calling API with curl
  - Note CORS configuration location
  - Acceptance: Documentation clear and complete

---

## Task Group 6: Static Resources Migration

**Duration**: 30 minutes  
**Dependencies**: Task Group 5 (REST API layer)  
**Risk**: Very Low

- [ ] 6.1 Create META-INF/resources directory
  - Create directory: `src/main/resources/META-INF/resources/`
  - This is where Quarkus serves static content from (equivalent to Spring Boot's `static/`)
  - Acceptance: Directory exists

- [ ] 6.2 Copy Angular frontend files
  - Copy all files from `src/main/resources/static/` to `src/main/resources/META-INF/resources/`
  - Files to copy:
    - `index.html`
    - `main-*.js`
    - `polyfills-*.js`
    - `styles-*.css`
    - Any other frontend assets
  - Acceptance: All frontend files present in new location

- [ ] 6.3 Test static resource serving in dev mode
  - Start Quarkus dev mode: `mvn quarkus:dev`
  - Access root: http://localhost:8080/
  - Verify Angular app loads correctly
  - Verify CSS and JavaScript files load
  - Acceptance: Frontend accessible and functional

- [ ] 6.4 Test frontend-backend integration
  - Open Angular app in browser: http://localhost:8080/
  - Test CRUD operations via UI:
    - Create a new TODO
    - Mark TODO as complete
    - Edit TODO title
    - Delete TODO
  - Open browser DevTools Network tab, verify:
    - XHR requests to `/api/todos` succeed (200/201/204 status codes)
    - CORS headers present
    - JSON responses well-formed
  - Acceptance: Frontend fully functional against Quarkus backend

- [ ] 6.5 **Documentation**: Document static resource serving
  - Update `README.md` with explanation of Quarkus static resource serving
  - Document path change: `static/` → `META-INF/resources/`
  - Add note that no code changes are needed in frontend
  - Acceptance: Documentation complete

---

## Task Group 7: Testing and Quality Assurance

**Duration**: 2-4 hours  
**Dependencies**: Task Groups 1-6 (all migration complete)  
**Risk**: Medium

- [ ] 7.1 Run full test suite
  - Execute: `mvn clean test`
  - Verify all tests pass:
    - Entity tests (Task 2)
    - Repository tests (Task 3)
    - Service tests (Task 4)
    - REST API tests (Task 5)
    - All generated tests from mig-test-gen
  - Acceptance: 100% tests passing, no failures

- [ ] 7.2 Measure test coverage
  - Execute: `mvn clean verify` (includes JaCoCo coverage)
  - Open coverage report: `target/site/jacoco/index.html`
  - Verify coverage metrics:
    - Line coverage: >80%
    - Branch coverage: >70%
    - Method coverage: >90%
  - Identify untested code and add tests if needed
  - Acceptance: Coverage targets met

- [ ] 7.3 Create characterization tests (compare Spring Boot vs Quarkus)
  - Run Spring Boot application locally
  - Record baseline behavior for all endpoints:
    - Request: GET /api/todos → Response: JSON array, status 200
    - Request: GET /api/todos/1 → Response: JSON object or 404
    - Request: POST /api/todos → Response: 201 with created object
    - Request: PUT /api/todos/1 → Response: 200 with updated object or 404
    - Request: DELETE /api/todos/1 → Response: 204
    - Request: OPTIONS /api/todos → Response: CORS headers present
  - Run Quarkus application
  - Execute same requests, verify responses identical
  - Document any differences (should be none)
  - Acceptance: Quarkus responses match Spring Boot baseline exactly

- [ ] 7.4 Performance baseline testing
  - **Spring Boot baseline**:
    - Start Spring Boot app, measure startup time
    - Measure memory footprint (RSS) after startup
    - Run load test: `ab -n 1000 -c 10 http://localhost:8080/api/todos`
    - Record throughput (requests/sec) and latency (P50, P95, P99)
  - **Quarkus comparison**:
    - Start Quarkus app (JVM mode), measure startup time
    - Measure memory footprint (RSS) after startup
    - Run same load test: `ab -n 1000 -c 10 http://localhost:8080/api/todos`
    - Record throughput and latency
  - **Compare results**:
    - Startup time: Quarkus should be <1 second (Spring Boot ~3-5 seconds)
    - Memory: Quarkus should be <150 MB RSS (Spring Boot ~200-300 MB)
    - Throughput: Quarkus should be equal or better
    - Latency: Quarkus should be equal or better
  - Acceptance: Quarkus meets or exceeds Spring Boot performance

- [ ] 7.5 Native compilation test (optional)
  - Install GraalVM (if not already installed)
  - Build native executable: `mvn package -Pnative`
  - Test native binary: `./target/todo-demo-quarkus-1.0.0-SNAPSHOT-runner`
  - Measure native performance:
    - Startup time: <100ms
    - Memory footprint: <50 MB RSS
    - Throughput: Similar to JVM mode
  - Acceptance: Native binary runs successfully (optional step)

- [ ] 7.6 **Documentation**: Document testing approach
  - Update `README.md` with testing section
  - Explain test structure (unit, integration, E2E)
  - Document how to run tests: `mvn test`, `mvn verify`
  - Document coverage requirements and how to generate reports
  - Add performance comparison table (Spring Boot vs Quarkus)
  - Acceptance: Testing documentation complete

---

## Task Group 8: Containerization

**Duration**: 2-3 hours  
**Dependencies**: Task Group 7 (testing complete)  
**Risk**: Medium

- [ ] 8.1 **Containerization**: Use mig-containerize for Dockerfile generation
  - Skill invocation: `/skill mig-containerize`
  - Context: Quarkus application, JVM mode (primary) and native mode (optional)
  - Configuration needs:
    - Database connection: Environment variable `QUARKUS_DATASOURCE_JDBC_URL`
    - Database credentials: Environment variables `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD`
    - CORS config: Environment variable `QUARKUS_HTTP_CORS_ORIGINS` (override default * if needed)
  - Expected output:
    - `src/main/docker/Dockerfile.jvm` - Multi-stage JVM build
    - `src/main/docker/Dockerfile.native` - Native binary build (optional)
    - `.dockerignore` - Optimize build context
    - Container build and test instructions
  - Acceptance: Dockerfiles generated and documented

- [ ] 8.2 Build JVM container image
  - Build image:
    ```bash
    mvn clean package
    podman build -f src/main/docker/Dockerfile.jvm -t quay.io/${USER}/todo-demo-quarkus:latest .
    ```
  - Verify image created: `podman images | grep todo-demo-quarkus`
  - Check image size: Should be <500 MB for JVM mode
  - Acceptance: Image builds successfully

- [ ] 8.3 Test container image locally
  - Run PostgreSQL container (if not already running):
    ```bash
    podman run --name todos-database -p 5432:5432 \
      -e POSTGRES_USER=jws \
      -e POSTGRES_PASSWORD=jws \
      -e POSTGRES_DB=todos \
      -d postgres:15-alpine
    ```
  - Run Quarkus container:
    ```bash
    podman run --rm -p 8080:8080 \
      -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.containers.internal:5432/todos \
      -e QUARKUS_DATASOURCE_USERNAME=jws \
      -e QUARKUS_DATASOURCE_PASSWORD=jws \
      quay.io/${USER}/todo-demo-quarkus:latest
    ```
  - Test endpoints:
    - Health: `curl http://localhost:8080/q/health`
    - API: `curl http://localhost:8080/api/todos`
    - Frontend: Open browser to http://localhost:8080/
  - Acceptance: Container runs successfully, all endpoints work

- [ ] 8.4 Security scan container image
  - Scan with Trivy (or OpenShift built-in scanner):
    ```bash
    trivy image quay.io/${USER}/todo-demo-quarkus:latest
    ```
  - Review vulnerabilities report
  - Fix critical vulnerabilities if any (update base image, dependencies)
  - Acceptance: No critical vulnerabilities

- [ ] 8.5 Push image to container registry
  - Log in to Quay.io: `podman login quay.io`
  - Push image:
    ```bash
    podman push quay.io/${USER}/todo-demo-quarkus:latest
    ```
  - Verify image in registry: Visit https://quay.io/repository/${USER}/todo-demo-quarkus
  - Tag with semantic version:
    ```bash
    podman tag quay.io/${USER}/todo-demo-quarkus:latest quay.io/${USER}/todo-demo-quarkus:1.0.0
    podman push quay.io/${USER}/todo-demo-quarkus:1.0.0
    ```
  - Acceptance: Image accessible in Quay.io

- [ ] 8.6 **Documentation**: Document containerization
  - Update `README.md` with container build instructions
  - Document environment variables needed for container
  - Add examples of running container locally
  - Document image registry URL
  - Acceptance: Containerization documentation complete

---

## Task Group 9: OpenShift Deployment

**Duration**: 2-3 hours  
**Dependencies**: Task Group 8 (containerization complete)  
**Risk**: Medium

- [ ] 9.1 Determine current OpenShift namespace
  - Ask user for target namespace or use current context:
    ```bash
    oc project
    ```
  - Verify namespace exists and is accessible
  - Verify `todos-database` service exists:
    ```bash
    oc get svc todos-database
    ```
  - Acceptance: Namespace confirmed, database service accessible

- [ ] 9.2 **Deployment**: Use mig-deploy for Kubernetes manifest generation
  - Skill invocation: `/skill mig-deploy`
  - Context: Quarkus application deployment to OpenShift
  - Dependencies:
    - Database: `todos-database` service (existing, reuse)
    - Port: 8080
  - Network:
    - Expose HTTP port 8080
    - Connect to PostgreSQL at `todos-database:5432`
  - Resources:
    - CPU request: 250m, limit: 500m
    - Memory request: 256Mi, limit: 512Mi
  - Health probes:
    - Liveness: `/q/health/live`
    - Readiness: `/q/health/ready`
  - Replicas: 2 (high availability)
  - Expected output:
    - `k8s/deployment.yaml` - Deployment manifest
    - `k8s/service.yaml` - Service manifest
    - `k8s/route.yaml` - OpenShift Route manifest
    - `k8s/configmap.yaml` - Application configuration (optional)
    - Deployment instructions
  - Acceptance: Manifests generated and documented

- [ ] 9.3 Create ConfigMap for application configuration (optional)
  - Create `k8s/configmap.yaml`:
    ```yaml
    apiVersion: v1
    kind: ConfigMap
    metadata:
      name: todo-app-config
    data:
      database.url: "jdbc:postgresql://todos-database:5432/todos"
      database.username: "jws"
      cors.origins: "*"
    ```
  - Apply to OpenShift: `oc apply -f k8s/configmap.yaml`
  - Update deployment.yaml to mount ConfigMap as environment variables
  - Acceptance: ConfigMap created and referenced in deployment

- [ ] 9.4 Deploy to OpenShift
  - Apply deployment manifest:
    ```bash
    oc apply -f k8s/deployment.yaml
    ```
  - Verify deployment started:
    ```bash
    oc rollout status deployment/todo-app-quarkus
    ```
  - Check pods running:
    ```bash
    oc get pods -l app=todo-app-quarkus
    ```
  - View logs:
    ```bash
    oc logs -f deployment/todo-app-quarkus
    ```
  - Verify pods are Ready (2/2 replicas)
  - Acceptance: Deployment successful, pods running and ready

- [ ] 9.5 Create and expose Service
  - Apply service manifest:
    ```bash
    oc apply -f k8s/service.yaml
    ```
  - Verify service created:
    ```bash
    oc get svc todo-app-quarkus
    ```
  - Test service from within cluster:
    ```bash
    oc run test-pod --rm -it --image=curlimages/curl -- curl http://todo-app-quarkus:8080/q/health
    ```
  - Acceptance: Service accessible from within cluster

- [ ] 9.6 Create OpenShift Route for external access
  - Apply route manifest:
    ```bash
    oc apply -f k8s/route.yaml
    ```
  - Get route URL:
    ```bash
    oc get route todo-app-quarkus -o jsonpath='{.spec.host}'
    ```
  - Test external access:
    ```bash
    ROUTE_URL=$(oc get route todo-app-quarkus -o jsonpath='{.spec.host}')
    curl https://$ROUTE_URL/q/health
    curl https://$ROUTE_URL/api/todos
    ```
  - Open in browser: `https://$ROUTE_URL/`
  - Acceptance: Application accessible via HTTPS route

- [ ] 9.7 Validate health checks
  - Check liveness probe:
    ```bash
    curl https://$ROUTE_URL/q/health/live
    ```
  - Check readiness probe:
    ```bash
    curl https://$ROUTE_URL/q/health/ready
    ```
  - Verify probes return status UP and 200 status code
  - Check OpenShift console: Pods should show healthy status
  - Acceptance: Health checks passing

- [ ] 9.8 Validate database connectivity
  - Create a TODO via API:
    ```bash
    curl -X POST https://$ROUTE_URL/api/todos \
      -H "Content-Type: application/json" \
      -d '{"title":"OpenShift Test","completed":false,"order":1}'
    ```
  - Retrieve TODOs:
    ```bash
    curl https://$ROUTE_URL/api/todos
    ```
  - Verify created TODO appears in response
  - Acceptance: Database operations working in OpenShift

- [ ] 9.9 Test high availability (2 replicas)
  - Verify 2 pods running:
    ```bash
    oc get pods -l app=todo-app-quarkus
    ```
  - Delete one pod (simulate failure):
    ```bash
    POD_NAME=$(oc get pods -l app=todo-app-quarkus -o jsonpath='{.items[0].metadata.name}')
    oc delete pod $POD_NAME
    ```
  - Verify OpenShift automatically creates replacement pod
  - Verify application remains accessible during pod restart
  - Acceptance: High availability confirmed

- [ ] 9.10 **Documentation**: Document OpenShift deployment
  - Update `README.md` with OpenShift deployment section
  - Document deployment commands
  - Document how to get route URL
  - Add troubleshooting section (common issues)
  - Document rollback procedure (see design.md)
  - Acceptance: Deployment documentation complete

---

## Task Group 10: Migration Validation and Cutover

**Duration**: 1-2 hours  
**Dependencies**: Task Group 9 (OpenShift deployment complete)  
**Risk**: Low

- [ ] 10.1 Compare Spring Boot and Quarkus deployments
  - Both should be running in OpenShift at this point
  - Get route URLs for both:
    ```bash
    SPRING_ROUTE=$(oc get route jws-app -o jsonpath='{.spec.host}')
    QUARKUS_ROUTE=$(oc get route todo-app-quarkus -o jsonpath='{.spec.host}')
    ```
  - Test same API calls against both, compare responses:
    ```bash
    curl https://$SPRING_ROUTE/api/todos > spring-response.json
    curl https://$QUARKUS_ROUTE/api/todos > quarkus-response.json
    diff spring-response.json quarkus-response.json
    ```
  - Verify responses identical (except for timestamps if any)
  - Acceptance: Functional equivalence confirmed

- [ ] 10.2 Performance comparison in OpenShift
  - Test Spring Boot performance:
    ```bash
    ab -n 1000 -c 10 https://$SPRING_ROUTE/api/todos
    ```
  - Test Quarkus performance:
    ```bash
    ab -n 1000 -c 10 https://$QUARKUS_ROUTE/api/todos
    ```
  - Compare metrics:
    - Throughput (requests/sec)
    - Latency (mean, P50, P95, P99)
  - Check pod resource usage:
    ```bash
    oc adm top pods -l app=jws-app
    oc adm top pods -l app=todo-app-quarkus
    ```
  - Verify Quarkus uses less CPU and memory
  - Acceptance: Quarkus performance equal or better

- [ ] 10.3 Execute end-to-end smoke tests
  - Test critical user journeys via frontend UI:
    1. Open app: `https://$QUARKUS_ROUTE/`
    2. Create 3 new TODOs
    3. Mark 1 TODO as complete
    4. Edit 1 TODO title
    5. Delete 1 TODO
    6. Verify remaining TODOs are correct
  - Repeat test 5 times to ensure consistency
  - Test from multiple browsers (Chrome, Firefox, Safari)
  - Acceptance: All user journeys work flawlessly

- [ ] 10.4 Soak test (extended load)
  - Run extended load test against Quarkus deployment:
    ```bash
    ab -n 10000 -c 20 -t 300 https://$QUARKUS_ROUTE/api/todos
    ```
  - Monitor pod metrics during test:
    ```bash
    watch oc adm top pods -l app=todo-app-quarkus
    ```
  - Check for memory leaks (memory usage should stabilize)
  - Check logs for errors:
    ```bash
    oc logs -f deployment/todo-app-quarkus
    ```
  - Acceptance: No errors, no memory leaks, stable performance

- [ ] 10.5 Plan cutover to Quarkus
  - Document cutover procedure:
    1. Verify Quarkus deployment healthy (health checks passing)
    2. Notify users of upcoming change (if applicable)
    3. Update route to point to Quarkus service:
       ```bash
       oc patch route jws-app -p '{"spec":{"to":{"name":"todo-app-quarkus"}}}'
       ```
    4. Monitor for errors
    5. If issues, rollback:
       ```bash
       oc patch route jws-app -p '{"spec":{"to":{"name":"jws-app"}}}'
       ```
  - Document rollback procedure (see design.md for details)
  - Acceptance: Cutover procedure documented and approved

- [ ] 10.6 Execute cutover (switch route)
  - Verify Quarkus deployment healthy
  - Switch route to Quarkus:
    ```bash
    oc patch route jws-app -p '{"spec":{"to":{"name":"todo-app-quarkus"}}}'
    ```
  - Verify route now points to Quarkus:
    ```bash
    oc describe route jws-app | grep "Service:"
    ```
  - Test application via original route URL:
    ```bash
    curl https://$SPRING_ROUTE/api/todos  # Now serves from Quarkus backend
    ```
  - Acceptance: Traffic switched to Quarkus successfully

- [ ] 10.7 Monitor post-cutover (24-48 hours)
  - Monitor for errors in logs:
    ```bash
    oc logs -f deployment/todo-app-quarkus
    ```
  - Monitor health checks in OpenShift console
  - Monitor user reports (if applicable)
  - Check database for any data issues
  - Measure performance metrics (should be stable)
  - Acceptance: 24-48 hours of stable operation

- [ ] 10.8 Decommission Spring Boot deployment (after monitoring period)
  - After successful monitoring period (24-48 hours), decommission Spring Boot:
    ```bash
    oc delete deployment jws-app
    oc delete svc jws-app
    ```
  - Keep Spring Boot container image in registry for 30 days (rollback option)
  - Archive Spring Boot source code:
    ```bash
    git tag spring-boot-final
    git push origin spring-boot-final
    ```
  - Acceptance: Spring Boot deployment removed, rollback option preserved

---

## Task Group 11: Documentation and Knowledge Transfer

**Duration**: 1-2 hours  
**Dependencies**: Task Group 10 (cutover complete)  
**Risk**: Low

- [ ] 11.1 Update README.md with final documentation
  - Ensure all sections complete:
    - Project overview
    - Technology stack (Quarkus)
    - Build instructions (`mvn clean package`)
    - Run instructions (dev mode, production)
    - Testing instructions (`mvn test`, `mvn verify`)
    - Containerization (build, run, push)
    - OpenShift deployment (manifests, commands)
    - API documentation (endpoints, examples)
    - Performance metrics (startup time, memory, throughput)
    - Troubleshooting guide
  - Remove Spring Boot-specific sections
  - Add migration notes (Spring Boot → Quarkus)
  - Acceptance: README comprehensive and up-to-date

- [ ] 11.2 Generate API documentation (OpenAPI)
  - Add Quarkus OpenAPI extension:
    ```xml
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-smallrye-openapi</artifactId>
    </dependency>
    ```
  - Access Swagger UI: http://localhost:8080/q/swagger-ui
  - Download OpenAPI spec: http://localhost:8080/q/openapi
  - Save OpenAPI spec to `docs/openapi.json`
  - Acceptance: API documentation generated and accessible

- [ ] 11.3 Create migration retrospective document
  - Create `MIGRATION.md` with:
    - Migration timeline (actual vs estimated)
    - Challenges encountered and solutions
    - Performance improvements achieved
    - Lessons learned
    - Recommendations for future migrations
  - Acceptance: Retrospective document complete

- [ ] 11.4 Archive migration artifacts
  - Collect all migration-related files:
    - Migration plan (this file)
    - Migration specification (spec.md)
    - Migration design (design.md)
    - User stories (UserStory.md)
    - Performance comparison data
    - Test coverage reports
  - Create `docs/migration/` directory
  - Move all artifacts to `docs/migration/`
  - Acceptance: Migration artifacts organized and archived

- [ ] 11.5 Knowledge transfer (if team-based)
  - Schedule walkthrough meeting with team
  - Present migration changes:
    - Technology stack changes (Spring Boot → Quarkus)
    - Code structure changes (controller → resource, etc.)
    - Deployment changes (JBoss Web Server → Quarkus container)
    - Testing approach
    - Performance improvements
  - Answer questions
  - Share documentation
  - Acceptance: Team familiar with Quarkus codebase (if applicable)

- [ ] 11.6 Update project dependencies
  - Verify all dependencies are latest stable versions
  - Run: `mvn versions:display-dependency-updates`
  - Update any outdated dependencies
  - Re-run tests after updates
  - Acceptance: Dependencies up-to-date

- [ ] 11.7 Final code cleanup
  - Remove unused imports
  - Remove commented-out code
  - Format code consistently: `mvn formatter:format`
  - Run linter/checkstyle if configured
  - Acceptance: Code clean and well-formatted

- [ ] 11.8 Create git tag for final migration
  - Commit all final changes:
    ```bash
    git add .
    git commit -m "Complete Spring Boot to Quarkus migration"
    ```
  - Create tag:
    ```bash
    git tag -a quarkus-v1.0.0 -m "Quarkus migration complete"
    git push origin quarkus-v1.0.0
    ```
  - Merge to main (if branch strategy requires it):
    ```bash
    git checkout main
    git merge migrate-to-quarkus
    git push origin main
    ```
  - Acceptance: Migration tagged and merged

---

## Summary

**Total Tasks**: 100+ subtasks across 11 task groups  
**Total Duration**: 14-21 hours (1.75-2.6 days)  
**Critical Path**:
1. Project Setup (Task Group 1)
2. Entity → Repository → Service → Resource migration (Task Groups 2-5)
3. Testing (Task Group 7)
4. Containerization (Task Group 8)
5. OpenShift Deployment (Task Group 9)
6. Cutover (Task Group 10)

**Integration Points**:
- **mig-test-gen**: Task Groups 2, 3, 4, 5 (comprehensive test generation)
- **mig-containerize**: Task Group 8 (Dockerfile generation)
- **mig-deploy**: Task Group 9 (Kubernetes manifest generation)

**Risk Mitigation**:
- Bottom-up migration (entity → repository → service → resource) minimizes integration risk
- Comprehensive testing at each layer before moving up
- Characterization tests ensure functional equivalence
- Blue/green deployment enables easy rollback
- 24-48 hour monitoring period before decommissioning Spring Boot

---

**End of Task Breakdown**
