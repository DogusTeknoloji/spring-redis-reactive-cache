package com.dteknoloji.springredisreactivecache.annotation

import com.dteknoloji.springredisreactivecache.config.ReactiveCachingConfiguration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(ReactiveCachingConfiguration::class)
@EnableAspectJAutoProxy
annotation class EnableReactiveCaching()
