package com.dteknoloji.springredisreactivecache

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

private val lock = ReentrantLock()

suspend fun waitUntilFetchData(block: suspend () -> Any?): Any {
    val condition = lock.newCondition()
    lock.lock()
    var result: Any? = null

    while (result == null) {
        condition.await(50, TimeUnit.MILLISECONDS)
        result = block()
        condition.signal()
    }
    lock.unlock()
    return result
}
