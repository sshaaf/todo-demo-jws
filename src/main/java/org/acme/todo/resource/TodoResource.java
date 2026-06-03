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
                    existing.title = todoDetails.title;
                    existing.completed = todoDetails.completed;
                    existing.order = todoDetails.order;
                    existing.url = todoDetails.url;
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
