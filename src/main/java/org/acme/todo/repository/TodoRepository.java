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
}
