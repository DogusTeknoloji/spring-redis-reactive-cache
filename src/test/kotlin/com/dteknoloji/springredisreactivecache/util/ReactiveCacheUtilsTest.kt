package com.dteknoloji.springredisreactivecache.util

import com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCacheGet
import com.dteknoloji.springredisreactivecache.dto.CacheableCustomer
import com.dteknoloji.springredisreactivecache.dto.DummyGetRequest
import io.mockk.every
import io.mockk.mockk
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.javaMethod

class ReactiveCacheUtilsTest {

    @Test
    fun testResolveParameterValue() {
        // Given
        val annotationKey = "#request.customerId"
        val method = ReactiveCacheUtilsTest::class.declaredFunctions.find { it -> it.name == "get" }!!.javaMethod
        val methodSignature = mockk<MethodSignature>() { every { this@mockk.method } returns method }

        val joinPoint = mockk<JoinPoint> {
            every { args } returns arrayOf(DummyGetRequest(15L))
            every { signature } returns methodSignature
        }

        // When
        val actual = resolveParameterValue(joinPoint, annotationKey)

        // Then
        assertEquals(15L, actual)
    }

    @Test
    fun `when resolveParameterValue for primitive object it should successfully resolve param value`() {
        // Given
        val annotationKey = "#customerId"
        val method = mockk<MethodSignature> {
            every { method } returns mockk() {
                every { parameters } returns arrayOf(
                    mockk {
                        every { name } returns "customerId"
                    }
                )
            }
        }
        val joinPoint = mockk<JoinPoint> {
            every { args } returns arrayOf(16L)
            every { signature } returns method
        }

        // When
        val actual = resolveParameterValue(joinPoint, annotationKey)

        // Then
        assertEquals(16L, actual)
    }

    @Test
    fun `when isSuspending is called it should check if method is suspending`() {
        // Given
        val method = ReactiveCacheUtilsTest::class.declaredFunctions.find { it -> it.name == "get" }!!.javaMethod!!

        // When
        val actual = isSuspending(method)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `when resolveUniqueIdentifierValue is called it should resolve id value`() {
        // Given
        val customerId = UUID.randomUUID()
        val entity = CacheableCustomer(id = customerId)

        // When
        val actual = entity.resolveUniqueIdentifierValue()

        // Then
        assertEquals(customerId, actual)
    }

    @RedisReactiveCacheGet(keyPrefix = "CUSTOMER_", hashKey = "CUSTOMER_HASH", key = "#request.customerId")
    private suspend fun get(request: DummyGetRequest, cacheFirst: Boolean = false): CacheableCustomer {
        return CacheableCustomer(UUID.randomUUID(), "Jack")
    }
}
