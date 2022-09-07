package com.dteknoloji.reactivecacheexample

import com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCacheEvict
import com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCacheGet
import com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCachePut
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.reactive.function.client.awaitBody

@Service
class TodoService(private val todoApiClient: WebClient) {

    @RedisReactiveCacheGet(key = "#id", keyPrefix = "TODO_", hashKey = "TODOS")
    suspend fun getById(id: Int, cacheFirst: Boolean = false): Todo {
        return todoApiClient.get()
            .uri {
                it.path("/{id}").build(id)
            }
            .retrieve()
            .awaitBody()
    }

    @RedisReactiveCachePut(keyPrefix = "TODO_", hashKey = "TODOS", expireDuration = "P1D")
    suspend fun create(todo: Todo): Todo {
        return todoApiClient.post()
            .bodyValue(todo)
            .retrieve()
            .awaitBody()
    }

    @RedisReactiveCacheEvict(keyPrefix = "TODO_", key = "#id", hashKey = "TODOS")
    suspend fun delete(id: Int) {
        todoApiClient.delete()
            .uri {
                it.path("/{id}").build(id)
            }
            .retrieve()
            .awaitBodilessEntity()
    }
}
