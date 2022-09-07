package com.dteknoloji.reactivecacheexample

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/todos")
class TodoController(private val todoService: TodoService) {

    @GetMapping("/{id}")
    suspend fun getById(@PathVariable id: Int): Todo {
        return todoService.getById(id, cacheFirst = true)
    }

    @PostMapping
    suspend fun create(@RequestBody todo: Todo): Todo {
        return todoService.create(todo)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: Int) {
        todoService.delete(id)
    }
}
