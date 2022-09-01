package com.dteknoloji.springredisreactivecache.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RedisReactiveCacheEvict(
    /**
     * Cache key prefix.
     *
     * Ex: CUSTOMER_
     */
    val keyPrefix: String,

    /**
     * Parameter name of the unique identifier. Must start with #.
     */
    val key: String,

    /**
     * Redis hash key
     *
     * Ex: CUSTOMER_HASH
     */
    val hashKey: String,
)
