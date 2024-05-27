package org.acme.todo.controller;
import org.acme.todo.model.Todo;
import org.acme.todo.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/api/todos")
public class TodoController {

    @Autowired
    private TodoService todoService;

    @CrossOrigin
    @GetMapping
    public List<Todo> getAllTodos() {
        return todoService.getAllTodos();
    }
    @CrossOrigin
    @GetMapping("/{id}")
    public ResponseEntity<Todo> getTodoById(@PathVariable Long id) {
        Optional<Todo> todo = todoService.getTodoById(id);
        return todo.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @CrossOrigin
    @PostMapping
    public Todo createTodo(@RequestBody Todo todo) {
        return todoService.createOrUpdateTodo(todo);
    }
    @CrossOrigin
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
    @CrossOrigin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodoById(@PathVariable Long id) {
        todoService.deleteTodoById(id);
        return ResponseEntity.noContent().build();
    }
}
