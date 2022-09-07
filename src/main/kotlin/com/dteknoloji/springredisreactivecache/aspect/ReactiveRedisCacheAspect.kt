package com.dteknoloji.springredisreactivecache.aspect

import com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCacheEvict
import com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCacheGet
import com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCachePut
import com.dteknoloji.springredisreactivecache.util.assertSuspending
import com.dteknoloji.springredisreactivecache.util.proceedCoroutine
import com.dteknoloji.springredisreactivecache.util.resolveParameterValue
import com.dteknoloji.springredisreactivecache.util.resolveUniqueIdentifierValue
import com.dteknoloji.springredisreactivecache.util.runCoroutine
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.removeAndAwait
import java.lang.reflect.Method
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction

@Aspect
class ReactiveRedisCacheAspect(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val cacheScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val logger = LoggerFactory.getLogger(ReactiveRedisCacheAspect::class.java)
    private val hashOps = reactiveRedisTemplate.opsForHash<String, Any>()

    @Around("execution(public * *(..)) && @annotation(com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCachePut)")
    fun redisReactiveCachePut(joinPoint: ProceedingJoinPoint): Any? {
        val method: Method = (joinPoint.signature as MethodSignature).method
        assertSuspending(method)
        val annotation: RedisReactiveCachePut = method.getAnnotation(RedisReactiveCachePut::class.java)

        return joinPoint.runCoroutine {
            joinPoint.proceedCoroutine()?.let {
                val id = it.resolveUniqueIdentifierValue(annotation.idPropertyName)
                putToCache("${annotation.keyPrefix}$id", annotation.hashKey, annotation.expireDuration, it)
                return@let it
            } ?: joinPoint.proceedCoroutine()
        }
    }

    @Around("execution(public * *(..)) && @annotation(com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCacheGet)")
    fun redisReactiveCacheGet(joinPoint: ProceedingJoinPoint): Any? {
        val method: Method = (joinPoint.signature as MethodSignature).method
        assertSuspending(method)
        val returnType = (method.kotlinFunction!!.returnType.classifier as KClass<*>).javaObjectType
        val annotation: RedisReactiveCacheGet = method.getAnnotation(RedisReactiveCacheGet::class.java)

        val id = resolveParameterValue(joinPoint, annotation.key)
        val cacheKey = "${annotation.keyPrefix}$id"
        val cacheFirst = resolveParameterValue(joinPoint, annotation.cacheFirstParam) as Boolean? ?: false

        return joinPoint.runCoroutine {
            if (cacheFirst) {
                getFromCacheAsync(cacheKey, annotation.hashKey, returnType).await() ?: getFromRealCall(joinPoint, cacheKey, annotation.hashKey, returnType)
            } else {
                logger.debug("Got cacheFirst false. Will ignore cache. Calling the real service. key: $cacheKey")
                getFromRealCall(joinPoint, cacheKey, annotation.hashKey, returnType)
            }
        }
    }

    @Around("execution(public * *(..)) && @annotation(com.dteknoloji.springredisreactivecache.annotation.RedisReactiveCacheEvict)")
    fun redisReactiveCacheEvict(joinPoint: ProceedingJoinPoint): Any? {
        val method: Method = (joinPoint.signature as MethodSignature).method
        assertSuspending(method)
        val annotation: RedisReactiveCacheEvict = method.getAnnotation(RedisReactiveCacheEvict::class.java)

        val id = resolveParameterValue(joinPoint, annotation.key)

        return joinPoint.runCoroutine {
            removeFromCache("${annotation.keyPrefix}$id", annotation.hashKey)
        }
    }

    private fun getFromCacheAsync(key: String, hashKey: String, type: Class<out Any>): Deferred<Any?> {
        return cacheScope.async {
            val cachedValue: Any? = try {
                hashOps.get(key, hashKey).awaitFirstOrNull()
            } catch (ex: Exception) {
                logger.error("Redis threw exception while trying to get from cache", ex)
                null
            }

            if (cachedValue != null) {
                logger.debug("Returning from cache. key: $key")
                return@async objectMapper.convertValue(cachedValue, type)
            }

            logger.debug("Record doesn't exists on cache: $key")
            return@async null
        }
    }

    private suspend fun getFromRealCall(joinPoint: ProceedingJoinPoint, key: String, hashKey: String, type: Class<out Any>): Any? {
        logger.debug("Getting from real call")
        return joinPoint.proceedCoroutine()?.let {
            putToCache(key, hashKey, "P1D", it)
            objectMapper.convertValue(it, type)
        } ?: objectMapper.convertValue(joinPoint.proceedCoroutine(), type)
    }

    private fun putToCache(key: String, hashKey: String, expireDuration: String, entity: Any) {
        cacheScope.launch {
            try {
                logger.debug("Putting to cache: key: $key")
                hashOps.put(key, hashKey, entity).awaitSingle()
                reactiveRedisTemplate.expire(key, Duration.parse(expireDuration)).awaitSingle()
            } catch (ex: Exception) {
                logger.error("Redis threw exception while trying to put to cache", ex)
            }
        }
    }

    private fun removeFromCache(key: String, hashKey: String) {
        cacheScope.launch {
            try {
                logger.debug("Removing from cache: key: $key")
                hashOps.removeAndAwait(key, hashKey)
            } catch (ex: Exception) {
                logger.error("Redis threw exception while trying to remove from cache", ex)
            }
        }
    }
}
