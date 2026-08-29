package pro.qasdatrip.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The one place that knows the server exists.
 *
 * `apiKey` is the app-level identifier the web bundle also carries — it says
 * "this is the Qasda client", nothing about a person. `deviceId` is a random
 * per-install string used for rate-limit bucketing, so a mobile carrier NAT
 * does not make every subscriber on it look like one very busy client.
 */
class QasdaApi(
    private val baseUrl: String,
    private val apiKey: String,
    private val deviceId: String,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client: HttpClient = httpClient().config {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = WHOLE_SEARCH_MS
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = SILENCE_MS
        }
    }

    /**
     * A search, as it arrives.
     *
     * The server sends a merged list after each booking site answers, so the
     * screen fills in rather than waiting for the slowest of four. The socket
     * timeout is the watchdog: a stream that goes quiet is a failure, not a
     * result, and answering "no flights" then would tell somebody something
     * false about the route.
     */
    fun search(q: SearchQuery): Flow<SearchEvent> = flow {
        var sawAnything = false
        try {
            client.prepareGet("$baseUrl/api/v1/flights/stream") {
                identify()
                searchParams(q)
            }.execute { response ->
                val channel = response.bodyAsChannel()
                while (true) {
                    val line = channel.readUTF8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty()) continue

                    val obj = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonObject ?: continue
                    when (obj["type"]?.jsonPrimitive?.content) {
                        "update" -> {
                            val array = obj["data"] as? JsonArray ?: JsonArray(emptyList())
                            val flights = runCatching {
                                json.decodeFromJsonElement(ListSerializer(Flight.serializer()), array)
                            }.getOrElse { emptyList() }
                            sawAnything = true
                            emit(SearchEvent.Results(flights))
                        }
                        "done" -> {
                            emit(SearchEvent.Done)
                            return@execute
                        }
                        "error" -> {
                            emit(SearchEvent.Failed(SearchEvent.Reason.SERVER))
                            return@execute
                        }
                    }
                }
                // The stream ended without ever saying it was done.
                emit(if (sawAnything) SearchEvent.Done else SearchEvent.Failed(SearchEvent.Reason.CONNECTION))
            }
        } catch (t: Throwable) {
            emit(SearchEvent.Failed(SearchEvent.Reason.CONNECTION))
        }
    }

    /**
     * What the route costs on the days around this one.
     *
     * Slow on purpose - the server is asking four sites about a week, not a
     * day - so the screen that calls it has to say it is working. Failure
     * returns null rather than throwing: a calendar is a helpful extra, and
     * losing it should never take a search down with it.
     */
    suspend fun calendar(q: SearchQuery): FlightCalendar? = runCatching {
        val envelope: Envelope<FlightCalendar> = client.get("$baseUrl/api/v1/flights/calendar") {
            identify()
            searchParams(q)
        }.body()
        envelope.data
    }.getOrNull()

    /**
     * Where to send somebody who chose a site. We do not sell tickets — the
     * booking and the payment happen there, and this is the door.
     */
    suspend fun bookingUrl(q: SearchQuery, provider: String): String? = runCatching {
        val envelope: Envelope<BookingLink> = client.get("$baseUrl/api/v1/flights/book") {
            identify()
            searchParams(q)
            parameter("provider", provider)
        }.body()
        envelope.data?.redirectUrl
    }.getOrNull()

    private fun HttpRequestBuilder.identify() {
        header("X-Api-Key", apiKey)
        header("X-Device-Id", deviceId)
        // EventSource cannot set headers, so the web client passes these in the
        // query string and the server accepts both. We send both too: the
        // headers for the ordinary requests, the query for the stream.
        parameter("api_key", apiKey)
        parameter("device_id", deviceId)
    }

    private fun HttpRequestBuilder.searchParams(q: SearchQuery) {
        parameter("from", q.from)
        parameter("to", q.to)
        parameter("departDate", q.departDate)
        parameter("returnDate", q.returnDate ?: "")
        parameter("adults", q.adults)
        parameter("children", q.children)
        parameter("infants", q.infants)
        parameter("cabinClass", q.cabin.wire)
    }

    companion object {
        /** No frame for this long means the connection is gone, whatever it claims. */
        const val SILENCE_MS = 25_000L
        const val WHOLE_SEARCH_MS = 75_000L
    }
}

@Serializable
data class BookingLink(val redirectUrl: String? = null)

/** Each platform brings its own engine: OkHttp on Android, NSURLSession on iOS. */
expect fun httpClient(): HttpClient
