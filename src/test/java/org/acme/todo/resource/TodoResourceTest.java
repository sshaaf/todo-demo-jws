package org.acme.todo.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.acme.todo.model.Todo;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class TodoResourceTest {
    
    @Test
    void testGetAllTodos() {
        given()
            .when().get("/api/todos")
            .then()
                .statusCode(200)
                .contentType("application/json")
                .body("$.size()", greaterThanOrEqualTo(0));
    }
    
    @Test
    void testCreateTodo() {
        given()
            .contentType("application/json")
            .body("{\"title\":\"Test Todo\",\"completed\":false,\"order\":1}")
            .when().post("/api/todos")
            .then()
                .statusCode(201)
                .body("title", equalTo("Test Todo"))
                .body("completed", equalTo(false))
                .body("id", notNullValue());
    }
    
    @Test
    void testGetNonExistentTodo() {
        given()
            .when().get("/api/todos/99999")
            .then()
                .statusCode(404);
    }
    
    @Test
    void testDeleteTodo() {
        // Create a todo first
        Integer id = given()
            .contentType("application/json")
            .body("{\"title\":\"To Delete\",\"completed\":false,\"order\":1}")
            .when().post("/api/todos")
            .then()
                .statusCode(201)
                .extract().path("id");
        
        // Delete it
        given()
            .when().delete("/api/todos/" + id)
            .then()
                .statusCode(204);
        
        // Verify deleted
        given()
            .when().get("/api/todos/" + id)
            .then()
                .statusCode(404);
    }
}
