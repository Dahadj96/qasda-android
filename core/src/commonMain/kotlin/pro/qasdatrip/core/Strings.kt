package pro.qasdatrip.core

/**
 * The words, in the three languages the product speaks. They live in the
 * shared module rather than in Android resources so the iOS app says exactly
 * the same things, and so a string can never exist in one language only.
 */
data class Words(
    val appName: String = "Qasda",
    val heroTitle: String,
    val heroSub: String,
    val from: String,
    val to: String,
    val dates: String,
    val travellers: String,
    val search: String,
    val roundTrip: String,
    val oneWay: String,
    val searching: String,
    val searchingSub: String,
    val resultsCount: String,       // {n}
    val cheapestOn: String,         // {site}
    val bagIncluded: String,
    val bagNone: String,
    val seatsShort: String,         // {n}
    val direct: String,
    val stopsOne: String,
    val stopsMany: String,          // {n}
    val outbound: String,
    val inbound: String,
    val book: String,
    val noResultsTitle: String,     // {from} {to}
    val noResultsSub: String,
    val failedTitle: String,
    val failedSub: String,
    val retry: String,
    val priceBySite: String,
    val cheapest: String,
    val economy: String,
    val searchCity: String,
    val noAirport: String,
) {
    companion object {
        fun of(lang: Lang): Words = when (lang) {
            Lang.FR -> Words(
                heroTitle = "Tous les sites. Une recherche.",
                heroSub = "Qasda interroge les sites de réservation algériens en même temps.",
                from = "Départ", to = "Destination", dates = "Dates", travellers = "Voyageurs",
                search = "Rechercher", roundTrip = "Aller-retour", oneWay = "Aller simple",
                searching = "Nous cherchons le meilleur prix", searchingSub = "Quatre sites, en même temps.",
                resultsCount = "{n} vols", cheapestOn = "{site} · le moins cher",
                bagIncluded = "Bagage inclus", bagNone = "Sans bagage", seatsShort = "{n} places",
                direct = "Direct", stopsOne = "1 escale", stopsMany = "{n} escales",
                outbound = "Aller", inbound = "Retour", book = "Réserver",
                noResultsTitle = "Aucun vol {from} → {to}",
                noResultsSub = "Aucun site de réservation ne propose ce trajet à cette date.",
                failedTitle = "La recherche n’a pas abouti",
                failedSub = "La connexion s’est interrompue avant que les sites répondent.",
                retry = "Réessayer", priceBySite = "Le prix, site par site", cheapest = "le moins cher",
                economy = "Économique", searchCity = "Ville ou aéroport", noAirport = "Aucun aéroport ne correspond",
            )
            Lang.AR -> Words(
                heroTitle = "كل المواقع. بحث واحد.",
                heroSub = "يبحث قصدة في مواقع الحجز الجزائرية في الوقت نفسه.",
                from = "المغادرة", to = "الوجهة", dates = "التواريخ", travellers = "المسافرون",
                search = "ابحث", roundTrip = "ذهاب وإياب", oneWay = "ذهاب فقط",
                searching = "نبحث عن أفضل سعر", searchingSub = "أربعة مواقع، في الوقت نفسه.",
                resultsCount = "{n} رحلات", cheapestOn = "{site} · الأرخص",
                bagIncluded = "مع حقيبة", bagNone = "بدون حقيبة", seatsShort = "مقاعد: {n}",
                direct = "مباشر", stopsOne = "توقف واحد", stopsMany = "{n} توقفات",
                outbound = "ذهاب", inbound = "إياب", book = "احجز",
                noResultsTitle = "لا توجد رحلات {from} ← {to}",
                noResultsSub = "لا يعرض أي موقع حجز هذا الخط في هذا التاريخ.",
                failedTitle = "لم يكتمل البحث",
                failedSub = "انقطع الاتصال قبل أن تجيب المواقع.",
                retry = "أعد المحاولة", priceBySite = "السعر، موقعًا بموقع", cheapest = "الأرخص",
                economy = "اقتصادية", searchCity = "مدينة أو مطار", noAirport = "لا يوجد مطار مطابق",
            )
            Lang.EN -> Words(
                heroTitle = "Every site. One search.",
                heroSub = "Qasda queries the Algerian booking sites at the same time.",
                from = "From", to = "To", dates = "Dates", travellers = "Travellers",
                search = "Search", roundTrip = "Round trip", oneWay = "One way",
                searching = "Finding the best price", searchingSub = "Four sites, at once.",
                resultsCount = "{n} flights", cheapestOn = "{site} · cheapest",
                bagIncluded = "Bag included", bagNone = "No checked bag", seatsShort = "{n} seats",
                direct = "Direct", stopsOne = "1 stop", stopsMany = "{n} stops",
                outbound = "Outbound", inbound = "Return", book = "Book",
                noResultsTitle = "No flights {from} → {to}",
                noResultsSub = "No booking site offers this route on this date.",
                failedTitle = "The search did not complete",
                failedSub = "The connection dropped before the sites answered.",
                retry = "Try again", priceBySite = "The price, site by site", cheapest = "cheapest",
                economy = "Economy", searchCity = "City or airport", noAirport = "No matching airport",
            )
        }
    }
}
