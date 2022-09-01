package com.dteknoloji.springredisreactivecache.dto

import java.util.UUID

data class CacheableCustomer(val id: UUID, var name: String = "Jack", val surname: String = "Yago")
