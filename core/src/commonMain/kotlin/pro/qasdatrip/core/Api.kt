package pro.qasdatrip.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

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

    /**
     * Ask to be told when this route and date gets cheaper.
     *
     * The address is not stored on the phone and no account is created. The
     * server sends a confirmation mail and does nothing at all until it is
     * answered, which is why the reply says `needsConfirmation` rather than
     * "done": telling somebody they are being watched when the mail is still
     * unopened is the one lie this feature cannot afford.
     */
    suspend fun createWatch(
        q: SearchQuery,
        contact: String,
        locale: String,
        targetPrice: Double? = null,
    ): WatchCreated? = runCatching {
        val envelope: Envelope<WatchCreated> = client.post("$baseUrl/api/v1/alerts") {
            identify()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("contact", JsonPrimitive(contact))
                    put("locale", JsonPrimitive(locale))
                    put("from", JsonPrimitive(q.from))
                    put("to", JsonPrimitive(q.to))
                    put("departDate", JsonPrimitive(q.departDate))
                    q.returnDate?.takeIf { it.isNotBlank() }?.let { put("returnDate", JsonPrimitive(it)) }
                    put("adults", JsonPrimitive(q.adults))
                    put("children", JsonPrimitive(q.children))
                    put("infants", JsonPrimitive(q.infants))
                    put("cabinClass", JsonPrimitive(q.cabin.wire))
                    targetPrice?.let { put("targetPrice", JsonPrimitive(it)) }
                }
            )
        }.body()
        envelope.data
    }.getOrNull()

    /**
     * Tell the server this install exists, and hand it a push token if we
     * have one.
     *
     * Called on first launch and again whenever the token changes — which is
     * often, because tokens rotate on reinstall and on restore to a new
     * handset. Passing back the key we already hold keeps the watches
     * attached to it; the server mints a fresh one only when it does not
     * recognise what we sent, which is a lost install rather than a merge.
     *
     * The token is optional on purpose. Registering before the permission
     * dialog means a watch can be created the moment somebody asks for one,
     * instead of a system prompt standing between them and the thing they
     * were trying to do.
     */
    suspend fun registerDevice(
        existing: DeviceKey? = null,
        pushToken: String? = null,
        platform: String = "android",
        locale: String,
    ): DeviceKey? = runCatching {
        val envelope: Envelope<DeviceKey> = client.post("$baseUrl/api/v1/devices") {
            identify()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    existing?.let {
                        put("deviceId", JsonPrimitive(it.deviceId))
                        put("signature", JsonPrimitive(it.signature))
                    }
                    pushToken?.takeIf { it.isNotBlank() }?.let { put("pushToken", JsonPrimitive(it)) }
                    put("platform", JsonPrimitive(platform))
                    put("locale", JsonPrimitive(locale))
                }
            )
        }.body()
        envelope.data
    }.getOrNull()

    /**
     * Watch a route and a date from the app.
     *
     * There is no address and no target price. `seenPrice` is the number that
     * was on screen when somebody decided to watch, and it is what "cheaper"
     * will be measured against — a promise that can actually be kept, unlike
     * a figure typed into a box that the fare may never reach.
     *
     * `seenPrice` is null only for a seat watch, which exists precisely
     * because there was no price to see.
     */
    suspend fun trackRoute(
        key: DeviceKey,
        q: SearchQuery,
        seenPrice: Double?,
        locale: String,
    ): WatchCreated? = runCatching {
        val envelope: Envelope<WatchCreated> = client.post("$baseUrl/api/v1/alerts/device") {
            identify()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("deviceId", JsonPrimitive(key.deviceId))
                    put("signature", JsonPrimitive(key.signature))
                    put("locale", JsonPrimitive(locale))
                    put("from", JsonPrimitive(q.from))
                    put("to", JsonPrimitive(q.to))
                    put("departDate", JsonPrimitive(q.departDate))
                    q.returnDate?.takeIf { it.isNotBlank() }?.let { put("returnDate", JsonPrimitive(it)) }
                    put("adults", JsonPrimitive(q.adults))
                    put("children", JsonPrimitive(q.children))
                    put("infants", JsonPrimitive(q.infants))
                    put("cabinClass", JsonPrimitive(q.cabin.wire))
                    // No price to see means the date came back empty, which is
                    // the other thing worth watching.
                    put("kind", JsonPrimitive(if (seenPrice == null) "seat" else "price"))
                    seenPrice?.let { put("seenPrice", JsonPrimitive(it)) }
                }
            )
        }.body()
        envelope.data
    }.getOrNull()

    /** Everything that mailbox is watching. Null means the link is no good. */
    suspend fun watches(key: ManageKey): List<Watch>? = runCatching {
        val envelope: Envelope<List<Watch>> = client.get("$baseUrl/api/v1/alerts") {
            identify()
            parameter("w", key.watcherId)
            parameter("s", key.signature)
        }.body()
        envelope.data
    }.getOrNull()

    /**
     * What we have already told this device, newest first.
     *
     * Signed with the same pair as the listing, because it is the same claim.
     * Null is the server refusing the link; an empty list is a device that
     * has been told nothing yet, and the two must not look the same on
     * screen.
     */
    suspend fun alerts(key: ManageKey, limit: Int = 50): List<Alert>? = runCatching {
        val envelope: Envelope<List<Alert>> = client.get("$baseUrl/api/v1/alerts/notifications") {
            identify()
            parameter("w", key.watcherId)
            parameter("s", key.signature)
            parameter("limit", limit)
        }.body()
        envelope.data
    }.getOrNull()

    /** Stop one watch. True only when the server confirms it. */
    suspend fun cancelWatch(key: ManageKey, watchId: Long): Boolean = runCatching {
        val envelope: Envelope<CancelResult> = client.post("$baseUrl/api/v1/alerts/cancel") {
            identify()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("w", JsonPrimitive(key.watcherId))
                    put("s", JsonPrimitive(key.signature))
                    put("id", JsonPrimitive(watchId))
                }
            )
        }.body()
        envelope.data?.cancelled == true
    }.getOrElse { false }

    /**
     * What this route and date has cost, day by day, as we observed it.
     *
     * Null on any failure, including a server that does not have this endpoint
     * yet — the screen draws without a chart rather than refusing to open.
     */
    suspend fun priceHistory(q: SearchQuery, days: Int = 30): PriceTrend? = runCatching {
        val envelope: Envelope<PriceTrend> = client.get("$baseUrl/api/v1/price-history") {
            identify()
            searchParams(q)
            parameter("days", days)
        }.body()
        envelope.data
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

@Serializable
data class CancelResult(val cancelled: Boolean = false)

/** Each platform brings its own engine: OkHttp on Android, NSURLSession on iOS. */
expect fun httpClient(): HttpClient
