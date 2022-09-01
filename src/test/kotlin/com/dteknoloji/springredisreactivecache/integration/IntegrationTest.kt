package com.dteknoloji.springredisreactivecache.integration

import com.dteknoloji.springredisreactivecache.annotation.EnableReactiveCaching
import com.dteknoloji.springredisreactivecache.aspect.ReactiveRedisCacheAspect
import com.dteknoloji.springredisreactivecache.config.ReactiveCachingConfiguration
import com.dteknoloji.springredisreactivecache.dto.CacheableCustomer
import com.dteknoloji.springredisreactivecache.service.CustomerTestService
import com.dteknoloji.springredisreactivecache.waitUntilFetchData
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.redis.testcontainers.RedisContainer
import com.redis.testcontainers.RedisContainer.DEFAULT_IMAGE_NAME
import com.redis.testcontainers.RedisContainer.DEFAULT_TAG
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.getAndAwait
import org.springframework.data.redis.core.putAndAwait
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@ContextConfiguration(
    classes = [
        IntegrationTest.TestConfig::class,
        ReactiveCachingConfiguration::class,
        ReactiveRedisCacheAspect::class,
        RedisReactiveAutoConfiguration::class,
        RedisAutoConfiguration::class,
        CustomerTestService::class
    ]
)
@Testcontainers
class IntegrationTest {

    @Autowired
    private lateinit var reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>

    @Autowired
    private lateinit var customerTestService: CustomerTestService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `when a method invoked that annotated with RedisReactiveCachePut it should put the return value to redis`(): Unit = runBlocking {
        // Given
        val customerId = UUID.fromString("be12698e-e2ab-42d4-96bf-d1699610a2db")
        val expected = CacheableCustomer(id = customerId)

        // When
        val actual = customerTestService.create()

        // Then
        val cache = waitUntilFetchData {
            reactiveRedisTemplate.opsForHash<String, Any>().getAndAwait("CUSTOMER_$customerId", "CUSTOMER_HASH")
        }

        assertEquals(expected, actual)
        assertEquals(expected, objectMapper.convertValue(cache, CacheableCustomer::class.java))
    }

    @Test
    fun `when a method invoked that annotated with RedisReactiveCacheGet it should get from redis`(): Unit = runBlocking {
        // Given
        val customerId = UUID.fromString("be12698e-e2ab-42d4-96bf-d1699610a2db")
        val expected = CacheableCustomer(id = customerId)
        reactiveRedisTemplate.opsForHash<String, Any>().putAndAwait("CUSTOMER_$customerId", "CUSTOMER_HASH", expected)

        // When
        val actual = customerTestService.getById(customerId, true)

        // Then
        assertEquals(expected, actual)
    }

    @TestConfiguration
    @EnableReactiveCaching
    class TestConfig {

        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper()

        @Container
        private val redisContainer = RedisContainer(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG))

        init {
            redisContainer.start()
        }

        @Bean
        fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
            return LettuceConnectionFactory("localhost", redisContainer.firstMappedPort)
        }
    }
}
