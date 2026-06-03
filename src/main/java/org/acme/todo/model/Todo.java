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
