package com.dteknoloji.springredisreactivecache.service

import com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCacheEvict
import com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCacheGet
import com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCachePut
import com.dteknoloji.springredisreactivecache.dto.CacheableCustomer
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CustomerTestService {

    @RedisReactiveCachePut(keyPrefix = "CUSTOMER_", hashKey = "CUSTOMER_HASH", expireDuration = "P1D")
    suspend fun create(): CacheableCustomer {
        return CacheableCustomer(id)
    }

    @RedisReactiveCacheEvict(keyPrefix = "CUSTOMER_", hashKey = "CUSTOMER_HASH", key = "#id")
    suspend fun delete(id: UUID) {
    }

    @RedisReactiveCacheGet(keyPrefix = "CUSTOMER_", hashKey = "CUSTOMER_HASH", key = "#id")
    suspend fun getById(id: UUID, cacheFirst: Boolean = false): CacheableCustomer {
        return CacheableCustomer(id, "Jack")
    }

    companion object {
        private val id = UUID.fromString("be12698e-e2ab-42d4-96bf-d1699610a2db")
    }
}
