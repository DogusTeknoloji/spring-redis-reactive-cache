## spring-redis-reactive-cache
Adds annotation support to Spring for reactive cache operations

>Note : Works only with Kotlin.

### Usage

First add Jitpack to repositories:
>`maven { url 'https://jitpack.io' }`

Add dependency:
>`implementation("com.github.DogusTeknoloji:spring-redis-reactive-cache:1.0.3")`

```kotlin
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
```

[Full example](https://github.com/DogusTeknoloji/spring-redis-reactive-cache/tree/main/example)
