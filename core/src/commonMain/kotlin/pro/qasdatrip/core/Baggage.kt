package pro.qasdatrip.core

/**
 * What a fare actually carries.
 *
 * This exists because "Bagage inclus" was a lie by omission. A traveller
 * reading those two words takes them to mean a bag that goes in the hold —
 * that is what "a bag" means when you are packing for a week — and the app
 * was printing them for fares that include nothing but the case you put in
 * the overhead locker. Somebody books on that, reaches the detail page or
 * worse the airport, and finds the thing they thought they had paid for
 * costs another eight thousand dinars.
 *
 * So the question is not "is there baggage" but "is there a *checked* bag",
 * and the answer has three useful shapes plus one honest refusal.
 */
enum class Baggage {
    /** A checked bag is included. The ordinary case, and the expected one. */
    CHECKED,

    /**
     * Cabin only. The trap: a fare that looks complete and is not.
     */
    CABIN_ONLY,

    /** Neither. Some stripped low-cost fares really are this. */
    NONE,

    /**
     * Nobody said.
     *
     * Not the same as "none", and it must never be drawn as though it were.
     * A site that did not report an allowance has not told us the fare has
     * no bag; it has told us nothing, and the screen says nothing back.
     */
    UNKNOWN,
}

/**
 * The allowance, read from whatever the sites bothered to send.
 *
 * `hasLuggage` is the server's own flag and it means the hold, so it stands
 * in for a checked count when no count arrived. An explicit count always
 * wins over it — a site that says "0 checked" is more specific than a
 * boolean, and the specific answer is the true one.
 */
fun Flight.baggage(): Baggage {
    val leg = outbound ?: inbound
    val checked = leg?.checkedBags?.value ?: if (hasLuggage) 1 else null
    val cabin = leg?.cabinBags?.value

    return when {
        checked != null && checked > 0 -> Baggage.CHECKED
        // A known zero in the hold. Whether there is a cabin bag on top of
        // that is a separate question, and one we only answer if asked.
        checked != null -> if (cabin != null && cabin <= 0) Baggage.NONE else Baggage.CABIN_ONLY
        cabin != null && cabin > 0 -> Baggage.CABIN_ONLY
        cabin != null -> Baggage.NONE
        else -> Baggage.UNKNOWN
    }
}

/**
 * Whether a cabin bag is known to be included.
 *
 * Kept apart from [baggage] because the icon and the sentence answer
 * different questions: the sentence is about the hold, and the small case
 * should only be drawn when a site actually said there is one.
 */
fun Flight.hasCabinBag(): Boolean {
    val leg = outbound ?: inbound
    return (leg?.cabinBags?.value ?: 0) > 0
}
