package pro.qasdatrip.core

/**
 * Airline names are brand names: they stay in Latin in every language, the
 * same rule the site follows. A carrier we have no name for is shown by the
 * name the booking site sent with it rather than by its code — learned as
 * results arrive, exactly as the web app does it.
 */
object Airlines {
    private val known = mutableMapOf(
        "AH" to "Air Algérie", "AF" to "Air France", "TK" to "Turkish Airlines",
        "EK" to "Emirates", "QR" to "Qatar Airways", "LH" to "Lufthansa",
        "TU" to "Tunisair", "MS" to "EgyptAir", "IB" to "Iberia",
        "VY" to "Vueling", "PC" to "Pegasus", "AZ" to "ITA Airways",
        "BJ" to "Nouvelair", "SF" to "Tassili Airlines", "HV" to "Transavia",
        "AT" to "Royal Air Maroc", "G9" to "Air Arabia", "3O" to "Air Arabia Maroc",
        "FZ" to "flydubai", "SV" to "Saudia", "XQ" to "SunExpress",
        "TO" to "Transavia France", "FR" to "Ryanair", "U2" to "easyJet",
        "W6" to "Wizz Air", "BA" to "British Airways", "KL" to "KLM",
        "LX" to "Swiss", "SN" to "Brussels Airlines", "TP" to "TAP Air Portugal",
        "AC" to "Air Canada", "TS" to "Air Transat", "8U" to "Afriqiyah Airways",
    )

    fun name(code: String?): String {
        val c = code?.uppercase().orEmpty()
        return known[c] ?: c
    }

    /** The logo the site already uses, so the two look like one product. */
    fun logoUrl(code: String?): String =
        "https://www.gstatic.com/flights/airline_logos/70px/dark/${code?.uppercase().orEmpty()}.png"

    /** A name a booking site sent for a code we did not know. Ours always wins. */
    fun learn(flights: List<Flight>) {
        for (f in flights) {
            for (leg in listOfNotNull(f.outbound, f.inbound)) {
                for (s in leg.segments) {
                    val code = (s.operatingAirline ?: s.marketingAirline)?.uppercase().orEmpty()
                    val name = s.operatingAirlineName?.trim().orEmpty()
                    if (code.isNotEmpty() && name.isNotEmpty() && !known.containsKey(code)) known[code] = name
                }
            }
        }
    }
}

/** The four booking sites, named the way they are on the site. */
object Sites {
    private val names = mapOf(
        "volz" to "Volz",
        "mondial" to "Mondial",
        "h24voyages" to "H24 Voyages",
        "dunevoyages" to "Dune Voyages",
    )

    fun name(key: String): String = names[key] ?: key
}
