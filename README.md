# spring-redis-reactive-cache
Adds annotation support to Spring for reactive cache operations

### Usage
```kotlin
@RedisReactiveCachePut(keyPrefix = "CUSTOMER_", hashKey = "CUSTOMER_HASH", expireDuration = "P1D")
suspend fun create(): CacheableCustomer {
    return CacheableCustomer(id)
}

@RedisReactiveCacheEvict(keyPrefix = "CUSTOMER_", hashKey = "CUSTOMER_HASH", key = "#id")
suspend fun delete(id: UUID) {}

@RedisReactiveCacheGet(keyPrefix = "CUSTOMER_", hashKey = "CUSTOMER_HASH", key = "#id")
suspend fun getById(id: UUID, cacheFirst: Boolean = false): CacheableCustomer {
    return CacheableCustomer(id, "Jack")
}
```

TODO
