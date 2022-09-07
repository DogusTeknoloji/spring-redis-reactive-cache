package com.dteknoloji.springredisreactivecache.util

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.reflect.full.memberProperties

private val logger = LoggerFactory.getLogger("ReactiveCacheUtils")

fun resolveParameterValue(joinPoint: JoinPoint, annotationKey: String): Any? {
    val method: Method = (joinPoint.signature as MethodSignature).method

    if (!annotationKey.startsWith('#')) throw IllegalArgumentException("Annotation key should start with # character")

    val resolvedValue = if (annotationKey.contains('.')) {
        val paramNameRegex = "(?<=#).*?(?=\\.)".toRegex() // matches between first '#' and '.'

        method.parameters.withIndex().find { parameter -> parameter.value.name == paramNameRegex.find(annotationKey)?.value }
            ?.let { parameterIndexedValue ->
                val param = joinPoint.args[parameterIndexedValue.index]
                param::class.memberProperties.find { it.name == annotationKey.substringAfter('.') }!!.getter.call(param)!!
            }
    } else {
        method.parameters.withIndex().find { parameter -> parameter.value.name == annotationKey.substringAfter('#') }?.let {
            joinPoint.args[it.index]
        }
    }

    if (resolvedValue == null) {
        logger.warn("Couldn't find expected parameter in ${method.name}. Expected $annotationKey but got null. Did you forget to add?")
    }

    return resolvedValue
}

fun isSuspending(method: Method): Boolean = method.parameters.lastOrNull()?.type?.isAssignableFrom(Continuation::class.java) == true

fun assertSuspending(method: Method) {
    if (!isSuspending(method)) throw UnsupportedOperationException("Only suspending methods allowed")
}

fun Any.resolveUniqueIdentifierValue(propertyName: String = "id"): Any {
    return this::class.memberProperties.find { kProperty1 -> kProperty1.name == propertyName }!!.getter.call(this)!!
}
