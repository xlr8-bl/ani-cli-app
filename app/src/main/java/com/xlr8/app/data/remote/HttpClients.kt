package com.xlr8.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Shared JSON config: tolerant of AniList/AllAnime adding fields we don't model. */
val XLR8Json: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

/** Builds the single shared Ktor client used by all remote data sources. */
fun buildHttpClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
    install(ContentNegotiation) { json(XLR8Json) }
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 20_000
    }
}
