package com.dteknoloji.reactivecacheexample

import com.dteknoloji.springredisreactivecache.annotation.EnableReactiveCaching
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableReactiveCaching
class ReactiveCacheExampleApplication

fun main(args: Array<String>) {
    runApplication<ReactiveCacheExampleApplication>(*args)
}
