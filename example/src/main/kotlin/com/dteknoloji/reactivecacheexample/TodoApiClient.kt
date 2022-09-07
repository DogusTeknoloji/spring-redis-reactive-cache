package com.dteknoloji.reactivecacheexample

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class TodoApiClient {

    @Bean
    fun apiClient(): WebClient {
        return WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com/todos")
            .build()
    }
}
