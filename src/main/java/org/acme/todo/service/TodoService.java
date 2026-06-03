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
