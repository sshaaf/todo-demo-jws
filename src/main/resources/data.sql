CREATE TABLE IF NOT EXISTS todos (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     title VARCHAR(255) NOT NULL UNIQUE,
    completed BOOLEAN NOT NULL,
    ordering INT NOT NULL,
    url VARCHAR(255)
    );
INSERT INTO todos (title, completed, ordering, url) VALUES
                                                        ('Todo 1', false, 1, 'http://example.com/todo1'),
                                                        ('Todo 2', true, 2, 'http://example.com/todo2'),
                                                        ('Todo 3', false, 3, 'http://example.com/todo3'),
                                                        ('Todo 4', true, 4, 'http://example.com/todo4'),
                                                        ('Todo 5', false, 5, 'http://example.com/todo5');