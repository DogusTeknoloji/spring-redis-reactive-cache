package com.dteknoloji.springredisreactivecache.aspect

import com.dteknoloji.springredisreactivecache.dto.CacheableCustomer
import com.dteknoloji.springredisreactivecache.service.CustomerTestService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory
import org.springframework.data.redis.core.ReactiveHashOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.removeAndAwait
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ReactiveRedisCacheAspectTest {

    private var testScope: TestScope = TestScope()
    private val proxyFactory = AspectJProxyFactory(CustomerTestService())
    private val objectMapper = mockk<ObjectMapper>()
    private val opsForHash = mockk<ReactiveHashOperations<String, String, Any>> {
        every { put(ofType(), ofType(), ofType()) } returns Mono.just(true)
        every { get(ofType(), ofType()) } returns Mono.empty()
    }
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any> = mockk {
        every { opsForHash<String, Any>() } returns opsForHash
        every { expire(ofType(), ofType()) } returns Mono.just(true)
    }

    @BeforeEach
    fun setup() {
        testScope = TestScope()
        proxyFactory.addAspect(ReactiveRedisCacheAspect(reactiveRedisTemplate, objectMapper, testScope))
    }

    @Test
    fun `when RedisReactiveCachePut annotated method executed by proxy it should put entity to redis cache`() = testScope.runTest {
        // Given
        val proxy: CustomerTestService = proxyFactory.getProxy()
        val id = UUID.fromString("be12698e-e2ab-42d4-96bf-d1699610a2db")
        val dummyCacheableCustomer = CacheableCustomer(id)
        val key = "CUSTOMER_$id"

        // When
        proxy.create()
        advanceUntilIdle()

        // Then
        verify(Ordering.ORDERED) {
            opsForHash.put(key, "CUSTOMER_HASH", dummyCacheableCustomer)
            reactiveRedisTemplate.expire(key, Duration.parse("P1D"))
        }
    }

    @Test
    fun `when RedisReactiveCacheEvict annotated method executed by proxy it should remove entity from redis cache`() = testScope.runTest {
        // Given
        mockkStatic(opsForHash::removeAndAwait)
        coEvery { opsForHash.removeAndAwait(ofType(), ofType()) } returns 1
        val proxy: CustomerTestService = proxyFactory.getProxy()
        val id = UUID.fromString("be12698e-e2ab-42d4-96bf-d1699610a2db")
        val key = "CUSTOMER_$id"

        // When
        proxy.delete(id)
        advanceUntilIdle()

        // Then
        coVerify {
            opsForHash.removeAndAwait(key, "CUSTOMER_HASH")
        }
        unmockkStatic(opsForHash::removeAndAwait)
    }

    @Test
    fun `when RedisReactiveCacheGet annotated method executed by proxy it should get entity from redis cache`() = testScope.runTest {
        // Given
        val proxy: CustomerTestService = proxyFactory.getProxy()
        val id = UUID.fromString("be12698e-e2ab-42d4-96bf-d1699610a2db")
        val key = "CUSTOMER_$id"
        every { objectMapper.convertValue(ofType(), CacheableCustomer::class.java) } answers { firstArg() }

        // When
        proxy.getById(id, cacheFirst = true)
        advanceUntilIdle()

        // Then
        verify(Ordering.ORDERED) {
            opsForHash.get(key, "CUSTOMER_HASH")
        }
    }

    @Test
    fun `when RedisReactiveCacheGet annotated method executed by proxy it should get entity from real method and put to redis cache`() = testScope.runTest {
        // Given
        val proxy: CustomerTestService = proxyFactory.getProxy()
        val id = UUID.fromString("be12698e-e2ab-42d4-96bf-d1699610a2db")
        val key = "CUSTOMER_$id"
        every { objectMapper.convertValue(ofType(), CacheableCustomer::class.java) } answers { firstArg() }

        // When
        proxy.getById(id, cacheFirst = false)
        advanceUntilIdle()

        // Then
        verify(Ordering.ORDERED) {
            opsForHash.put(key, "CUSTOMER_HASH", CacheableCustomer(id))
            reactiveRedisTemplate.expire(key, Duration.parse("P1D"))
        }
    }
}
