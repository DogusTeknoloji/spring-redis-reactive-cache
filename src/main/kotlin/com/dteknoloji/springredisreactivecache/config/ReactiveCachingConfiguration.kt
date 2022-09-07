package com.dteknoloji.springredisreactivecache.config

import com.dteknoloji.springredisreactivecache.aspect.ReactiveRedisCacheAspect
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@ConditionalOnClass(ReactiveRedisConnectionFactory::class)
class ReactiveCachingConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun objectMapper() = jacksonObjectMapper()

    @Bean
    @Primary
    fun reactiveRedisTemplate(
        reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory,
        objectMapper: ObjectMapper,
    ): ReactiveRedisTemplate<String, Any> {
        val serializer = Jackson2JsonRedisSerializer(Any::class.java)
        serializer.setObjectMapper(objectMapper)

        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, Any>()
            .key(StringRedisSerializer())
            .value(serializer)
            .hashKey(StringRedisSerializer())
            .hashValue(serializer)
            .build()

        return ReactiveRedisTemplate(reactiveRedisConnectionFactory, serializationContext)
    }

    @Bean
    @ConditionalOnMissingBean
    fun reactiveRedisCacheAspect(reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>, objectMapper: ObjectMapper) =
        ReactiveRedisCacheAspect(reactiveRedisTemplate, objectMapper)
}
