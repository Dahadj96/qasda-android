package pro.qasdatrip.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Standard Output Format the server already speaks. These names are the
 * server's, not ours — every field here exists in what /api/v1 returns, and
 * anything the server does not send is nullable rather than defaulted, so a
 * missing value never becomes a claim.
 */
@Serializable
data class Flight(
    val id: String? = null,
    val airline: String = "",
    val isRoundTrip: Boolean = false,
    val stops: Int? = null,
    val duration: String? = null,
    val hasLuggage: Boolean = false,
    val fareClass: String? = null,
    val price: Double = 0.0,
    val provider: String = "",
    val seatsAvailable: Int? = null,
    val prices: Map<String, Double?> = emptyMap(),
    val seats: Map<String, Int?> = emptyMap(),
    val quoted: Map<String, Boolean> = emptyMap(),
    val fareRules: FareRules? = null,
    val outbound: Leg? = null,
    @SerialName("returnLeg") val inbound: Leg? = null,
) {
    /** The cheapest price any site actually quoted, or null if none did. */
    val cheapest: Pair<String, Double>?
        get() = prices.entries
            .mapNotNull { (site, value) -> value?.takeIf { it > 0 }?.let { site to it } }
            .minByOrNull { it.second }

    /** How many sites put a real price on this flight. */
    val quotingSites: Int
        get() = prices.values.count { it != null && it > 0 }

    val outboundOrSelf: Leg?
        get() = outbound ?: Leg(
            flightNo = null, operatingAirline = airline, departure = null, arrival = null,
            departureDate = null, origin = null, destination = null, stops = stops,
            duration = duration, segments = emptyList(), aircraft = emptyList(),
        )
}

@Serializable
data class Leg(
    val flightNo: String? = null,
    val operatingAirline: String? = null,
    val operatingAirlines: List<String> = emptyList(),
    val departure: String? = null,
    val arrival: String? = null,
    val departureDate: String? = null,
    val arrivalDate: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val stops: Int? = null,
    val duration: String? = null,
    val segments: List<Segment> = emptyList(),
    val aircraft: List<Aircraft> = emptyList(),
    val cabinBags: BagAllowance? = null,
    val checkedBags: BagAllowance? = null,
)

@Serializable
data class Segment(
    val flightNo: String? = null,
    val marketingAirline: String? = null,
    val operatingAirline: String? = null,
    val operatingAirlineName: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val departure: String? = null,
    val arrival: String? = null,
    val aircraft: String? = null,
    val aircraftName: String? = null,
)

@Serializable
data class Aircraft(val code: String = "", val name: String = "")

/** `PIECE` and `KG` are different promises; the unit travels with the number. */
@Serializable
data class BagAllowance(val value: Int, val unit: String)

@Serializable
data class FareRules(
    val refundable: Boolean? = null,
    val changeable: Boolean? = null,
    val refundFee: Double? = null,
    val changeFee: Double? = null,
)

@Serializable
data class Envelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: ApiError? = null,
)

@Serializable
data class ApiError(val code: String = "", val message: String = "")
