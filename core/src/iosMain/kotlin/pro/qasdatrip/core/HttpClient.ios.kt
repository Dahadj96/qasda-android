package pro.qasdatrip.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun httpClient(): HttpClient = HttpClient(Darwin)
