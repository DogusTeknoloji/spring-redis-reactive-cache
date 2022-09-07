package com.dteknoloji.springredisreactivecache.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RedisReactiveCachePut(

    /**
     * Cache key prefix.
     *
     * Ex: CUSTOMER_
     */
    val keyPrefix: String,

    /**
     * Redis hash key
     *
     * Ex: CUSTOMER_HASH
     */
    val hashKey: String,

    /**
     * Cache expire duration. Must be a valid Java Duration string
     *
     * Ex: P1D
     */
    val expireDuration: String,

    val idPropertyName: String = "id"
)
