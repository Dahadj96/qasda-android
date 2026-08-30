package pro.qasdatrip.core

/**
 * The words, in the three languages the product speaks. They live in the
 * shared module rather than in Android resources so the iOS app says exactly
 * the same things, and so a string can never exist in one language only.
 *
 * An interface with one object per language, and not — as it was — a data
 * class with one constructor parameter per string. That version crashed the
 * app on launch, and the reason is worth writing down because it is invisible
 * until the day it happens:
 *
 *   java.lang.VerifyError: ... Rejecting invocation, expected 1 argument
 *   registers, method signature has 2 or more
 *
 * Dalvik addresses method arguments as registers, and `invoke-direct/range`
 * can span at most 255 of them. `Words` had grown to 257 parameters, so the
 * generated call to its constructor could no longer be expressed — the class
 * verifier rejected it, and every launch died before the first frame. It
 * compiled cleanly, R8 minified it cleanly, and the unit tests passed; only
 * a real dex loader can see it, which is exactly why the crash arrived on a
 * phone rather than in a build log.
 *
 * Overridden properties take no constructor arguments at all, so this shape
 * has no ceiling. The compiler still refuses to build a language that has
 * forgotten a string, which was the whole point of the data class.
 */
interface Words {
    val appName: String get() = "Qasda"
    val heroTitle: String
    val heroSub: String
    val from: String
    val to: String
    val dates: String
    val travellers: String
    val search: String
    val roundTrip: String
    val oneWay: String
    val searching: String
    val searchingSub: String
    val resultsCount: String       // {n}
    val cheapestOn: String         // {site}
    val bagIncluded: String
    val bagNone: String
    val seatsShort: String         // {n}
    val direct: String
    val stopsOne: String
    val stopsMany: String          // {n}
    val outbound: String
    val inbound: String
    val book: String
    val noResultsTitle: String     // {from} {to}
    val noResultsSub: String
    val failedTitle: String
    val failedSub: String
    val retry: String
    val priceBySite: String
    val cheapest: String
    val economy: String
    val searchCity: String
    val noAirport: String
    val chooseDates: String
    val confirm: String
    val cancel: String
    val cabinBag: String
    val checkedBag: String
    val bagUnknown: String
    val pieces: String             // {n}
    val kilos: String              // {n}
    val conditions: String
    val refundable: String
    val nonRefundable: String
    val changeable: String
    val nonChangeable: String
    val feeApplies: String         // {amount}
    val operatedBy: String         // {airline}
    val adults: String
    val adultsAge: String
    val children: String
    val childrenAge: String
    val infants: String
    val infantsAge: String
    val cabinClass: String
    val premium: String
    val business: String
    val first: String
    val apply: String
    val filters: String
    val sortBy: String
    val sortPrice: String
    val sortDeparture: String
    val sortDuration: String
    val departureTime: String
    val morning: String
    val afternoon: String
    val evening: String
    val night: String
    val maxPrice: String
    val reset: String
    val showCount: String          // {n}
    val noMatchTitle: String
    val noMatchSub: String
    val stopsLabel: String
    val navHome: String
    val navHelp: String
    val navSettings: String
    val about: String
    val language: String
    val systemLanguage: String
    val howItWorks: String
    val privacy: String
    val contactUs: String
    val openOnSite: String
    val version: String
    val recentSearches: String
    val offline: String
    val seatOne: String
    val edit: String
    val priceCalendar: String
    val calendarLoading: String
    val calendarEmpty: String
    val calendarNote: String
    val cheapestDay: String        // {date} {price}
    val noPriceThatDay: String
    val nearbyDates: String
    val comparing: String
    val offersSoFar: String        // {n}
    val aboutTitle: String
    val aboutTagline: String
    val aboutWhat: String
    val aboutNotTickets: String
    val readMore: String
    val faq: String
    val cheapestOfSites: String    // {site} {n}
    val navTracking: String
    val trackRoute: String
    val trackTitle: String
    val trackSub: String
    val trackStart: String
    val trackNoAccount: String
    val trackSent: String          // {email}
    val trackSentSub: String
    val trackFailed: String
    val myTracking: String
    val noTracking: String
    val noTrackingSub: String
    val openManageLink: String
    val pasteManageLink: String
    val linkNotValid: String
    val stopTracking: String
    val newTracking: String
    val priceHistory: String       // {n} days
    val historyLow: String
    val historyAverage: String
    val historyHigh: String
    val historyObserved: String    // {n}
    val historyThin: String
    val historyNone: String
    val seeTodayOffers: String
    val whenYouSubscribed: String  // {price}
    val targetSet: String          // {price}
    val bestPrice: String
    val seeOffer: String
    val travellerOne: String
    val travellersMany: String     // {n}
    val airlineOne: String
    val airlinesMany: String       // {n}
    val compareSites: String
    val bookOn: String             // {site}
    val stopsTwoPlus: String
    val baggage: String
    val anyBaggage: String
    val navAccount: String
    val seeDetails: String
    val sitesQuoting: String       // {n}
    val onlyOnSite: String         // {site}
    val thisDevice: String
    val thisDeviceSub: String
    val preferences: String
    val helpAndInfo: String
    val settingsAndAlerts: String
    val settingsAndAlertsSub: String
    val fromQuestion: String
    val toQuestion: String
    val whenQuestion: String
    val whoQuestion: String
    val reviewTitle: String
    val continueLabel: String
    val airportsHere: String
    val popularDestinations: String
    val swapRoute: String
    val pickReturn: String
    val stepOf: String             // {n} {total}
    val itineraryLabel: String
    val layoverAt: String          // {time} {airport}
    val layoverDeparts: String     // {time}
    val overnightStop: String
    val trackRulesLabel: String
    val trackRulePrice: String     // {price}
    val trackRulePriceBody: String
    val trackRuleSeat: String
    val trackRuleSeatBody: String
    val trackNotifications: String
    val trackNotificationsBody: String
    val trackPrivacy: String
    val trackOn: String
    val trackDone: String
    val trackDoneBody: String
    val trackSeatTitle: String
    val trackSeatBody: String
    val watchingPrice: String      // {price}
    val watchingSeat: String
    val seenAtSearch: String       // {price}
    val notificationsTitle: String
    val noNotifications: String
    val noNotificationsSub: String
    val alertDropTitle: String
    val alertDropBody: String      // {price} {saved}
    val alertSeatTitle: String
    val alertSeatBody: String      // {price}
    val notificationsFoot: String
    // --- Figma parity: the results rail, the filters page, prix par date ---
    /** Section label over the carrier rail on the results page. */
    val airlinesOnRoute: String
    /** "dès {price}" — the least a carrier costs on this route. */
    val fromPrice: String          // {price}
    /** All carriers again, the first chip in the rail. */
    val allAirlines: String
    val priceChart: String
    val priceByDate: String
    val calendarTab: String
    val bookingSites: String
    val clearAll: String
    val bagHoldIncluded: String
    val bagHoldSub: String
    val markAllRead: String
    val chartSub: String
    val chartLow: String
    val chartAvg: String
    val chartHigh: String
    val chartNote: String
    val showOffers: String         // {n}
    val cheapestOver: String
    val seeFlightsOn: String       // {date}
    val offerDetails: String
    val itinerary: String
    val cheaper: String
    val totalTime: String          // {duration}
    val sameFlightOn: String       // {n}
    val statWatches: String
    val statNotifications: String
    val statSearches: String
    val currency: String
    val currencyDzd: String
    val homeAirport: String
    val notSet: String
    val timeNow: String
    val timeHours: String          // {n}
    val timeYesterday: String
    val timeDays: String           // {n}
    val timeWeeks: String          // {n}
    val prefDrops: String
    val prefDropsSub: String
    val prefSeats: String
    val prefSeatsSub: String
    val prefEnded: String
    val prefEndedSub: String
    val prefMutedNote: String
    val display: String
    val theme: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val dataSection: String
    val clearRecent: String
    val clearRecentSub: String     // {n}
    val stopAllWatches: String
    val stopAllWatchesSub: String  // {n}
    val nothingToClear: String
    val handoverTitle: String
    val continueOn: String         // {site}
    val notSeller: String
    val labelFlight: String
    val labelRoute: String
    val labelDate: String
    val labelTravellers: String
    val labelShownPrice: String
    val priceMayChange: String
    val openSite: String           // {site}
    val backToResults: String
    val priceHistoryTitle: String
    val clearRecentOneSub: String
    val stopAllWatchesOneSub: String
    val soldOut: String
    /** "Le moins cher sur {n} sites" — the best card's footer line. */
    val cheapestOnSites: String     // {n}
    /** The tag on a tracking card: this watch is running. */
    val statusActive: String
    val seeOffers: String
    /** Shown beside the notifications switch when the phone has them off. */
    val notificationsOff: String
    /** A checked bag is included — the ordinary case. */
    val bagChecked: String
    /** Cabin only: the fare that looks complete and is not. */
    val bagCabinOnly: String
    /** Neither. */
    val bagNoneAtAll: String
    val baggageHeading: String
    val cabinBagLabel: String
    val checkedBagLabel: String
    /** "1 pièce" — never "1 pièce(s)". */
    val pieceOne: String
    val noSeats: String
    val notifyMe: String

    companion object {
        fun of(lang: Lang): Words = when (lang) {
            Lang.FR -> FrWords
            Lang.AR -> ArWords
            Lang.EN -> EnWords
        }
    }
}

/** French: the language the product is designed in. */
private object FrWords : Words {
    override val pieceOne = "1 pièce"
    override val bagChecked = "Bagage en soute"
    override val bagCabinOnly = "Cabine seulement"
    override val bagNoneAtAll = "Aucun bagage"
    override val baggageHeading = "Bagages"
    override val cabinBagLabel = "Bagage cabine"
    override val checkedBagLabel = "Bagage en soute"
    override val notificationsOff = "Coupées sur ce téléphone. Touchez pour les réactiver."
    override val statusActive = "Actif"
    override val seeOffers = "Voir les offres"
    override val cheapestOnSites = "Le moins cher sur {n} sites"
    override val soldOut = "Complet"
    override val noSeats = "0 place"
    override val notifyMe = "Me prévenir"
    override val heroTitle = "Tous les sites. Une recherche."
    override val heroSub = "Qasda interroge les sites de réservation algériens en même temps."
    override val from = "Départ"
    override val to = "Destination"
    override val dates = "Dates"
    override val travellers = "Voyageurs"
    override val search = "Rechercher"
    override val roundTrip = "Aller-retour"
    override val oneWay = "Aller simple"
    override val searching = "Nous cherchons le meilleur prix"
    override val searchingSub = "Quatre sites, en même temps."
    override val resultsCount = "{n} vols"
    override val cheapestOn = "{site} · le moins cher"
    override val bagIncluded = "Bagage inclus"
    override val bagNone = "Sans bagage"
    override val seatsShort = "{n} places"
    override val direct = "Direct"
    override val stopsOne = "1 escale"
    override val stopsMany = "{n} escales"
    override val outbound = "Aller"
    override val inbound = "Retour"
    override val book = "Réserver"
    override val noResultsTitle = "Aucun vol {from} → {to}"
    override val noResultsSub = "Aucun site de réservation ne propose ce trajet à cette date."
    override val failedTitle = "La recherche n’a pas abouti"
    override val failedSub = "La connexion s’est interrompue avant que les sites répondent."
    override val retry = "Réessayer"
    override val priceBySite = "Le prix, site par site"
    override val cheapest = "le moins cher"
    override val economy = "Économique"
    override val searchCity = "Ville ou aéroport"
    override val noAirport = "Aucun aéroport ne correspond"
    override val chooseDates = "Choisir les dates"
    override val confirm = "Confirmer"
    override val cancel = "Annuler"
    override val cabinBag = "Bagage cabine"
    override val checkedBag = "Bagage en soute"
    override val bagUnknown = "Non communiqué"
    override val pieces = "{n} pièces"
    override val kilos = "{n} kg"
    override val conditions = "Conditions du billet"
    override val refundable = "Remboursable"
    override val nonRefundable = "Non remboursable"
    override val changeable = "Modifiable"
    override val nonChangeable = "Non modifiable"
    override val feeApplies = "{amount} de frais"
    override val operatedBy = "Opéré par {airline}"
    override val adults = "Adultes"
    override val adultsAge = "12 ans et plus"
    override val children = "Enfants"
    override val childrenAge = "2 – 11 ans"
    override val infants = "Bébés"
    override val infantsAge = "moins de 2 ans"
    override val cabinClass = "Classe de cabine"
    override val premium = "Économique premium"
    override val business = "Affaires"
    override val first = "Première"
    override val apply = "Appliquer"
    override val filters = "Filtres"
    override val sortBy = "Trier"
    override val sortPrice = "Prix"
    override val sortDeparture = "Départ"
    override val sortDuration = "Durée"
    override val departureTime = "Heure de départ"
    override val morning = "Matin"
    override val afternoon = "Après-midi"
    override val evening = "Soir"
    override val night = "Nuit"
    override val maxPrice = "Prix maximum"
    override val reset = "Réinitialiser"
    override val showCount = "Voir {n} vols"
    override val noMatchTitle = "Aucun vol ne correspond"
    override val noMatchSub = "Élargissez les filtres pour voir plus de résultats."
    override val stopsLabel = "Escales"
    override val navHome = "Accueil"
    override val navHelp = "Aide"
    override val navSettings = "Réglages"
    override val about = "À propos"
    override val language = "Langue"
    override val systemLanguage = "Langue du téléphone"
    override val howItWorks = "Comment ça marche"
    override val privacy = "Confidentialité"
    override val contactUs = "Nous écrire"
    override val openOnSite = "Ouvrir sur le site"
    override val version = "Version"
    override val recentSearches = "Recherches récentes"
    override val offline = "Pas de connexion internet"
    override val seatOne = "1 place"
    override val edit = "Modifier"
    override val priceCalendar = "Calendrier des prix"
    override val calendarLoading = "Nous interrogeons les sites pour chaque date. Cela prend un moment."
    override val calendarEmpty = "Aucun prix pour cette période."
    override val calendarNote = "Chaque prix est celui qu’un site a affiché pour cette date. Une date sans prix est une date qu’aucun site n’a cotée."
    override val cheapestDay = "Le moins cher : {date} · {price}"
    override val noPriceThatDay = "Aucun prix"
    override val nearbyDates = "Dates proches"
    override val comparing = "Nous comparons les offres…"
    override val offersSoFar = "{n} offres"
    override val aboutTitle = "À propos"
    override val aboutTagline = "Le comparateur de vols au départ d’Algérie"
    override val aboutWhat = "Qasda interroge en direct les agences algériennes et affiche le prix réel de chaque offre, sans commission ni majoration."
    override val aboutNotTickets = "Nous ne vendons pas de billets : la réservation se fait toujours sur le site de l’agence choisie."
    override val readMore = "En savoir plus"
    override val faq = "Questions fréquentes"
    override val cheapestOfSites = "{site} · le moins cher des {n} sites"
    override val navTracking = "Suivi"
    override val trackRoute = "Suivre ce trajet"
    override val trackTitle = "Suivre ce trajet"
    override val trackSub = "Nous surveillons le prix pour vous."
    override val trackStart = "Activer le suivi"
    override val trackNoAccount = "Sans compte. Une adresse, un e-mail de confirmation, et vous pouvez tout arrêter d’un lien."
    override val trackSent = "E-mail envoyé à {email}"
    override val trackSentSub = "Ouvrez le lien de confirmation. Tant qu’il n’est pas ouvert, nous ne surveillons rien et ne vous écrivons plus."
    override val trackFailed = "Le suivi n’a pas pu être créé. Réessayez."
    override val myTracking = "Mes suivis"
    override val noTracking = "Aucun suivi ici"
    override val noTrackingSub = "Lancez une recherche, puis touchez « Suivre ce trajet ». Nous regarderons le prix pour vous et vous préviendrons ici."
    override val openManageLink = "J’ai déjà des suivis"
    override val pasteManageLink = "Collez le lien « Gérer mes alertes » de votre e-mail"
    override val linkNotValid = "Ce lien n’est pas valide."
    override val stopTracking = "Arrêter"
    override val newTracking = "Nouveau suivi"
    override val priceHistory = "{n} derniers jours"
    override val historyLow = "Le plus bas"
    override val historyAverage = "Moyenne"
    override val historyHigh = "Le plus haut"
    override val historyObserved = "Sur {n} jours où un prix a été relevé."
    override val historyThin = "Trop peu de relevés pour tracer une courbe."
    override val historyNone = "Aucun prix relevé pour ce trajet et cette date."
    override val seeTodayOffers = "Voir les offres du jour"
    override val whenYouSubscribed = "À l’activation : {price}"
    override val targetSet = "Seuil : {price}"
    override val bestPrice = "MEILLEUR PRIX"
    override val seeOffer = "Voir l’offre"
    override val travellerOne = "1 voyageur"
    override val travellersMany = "{n} voyageurs"
    override val airlineOne = "1 compagnie"
    override val airlinesMany = "{n} compagnies"
    override val compareSites = "Comparer les sites"
    override val bookOn = "Réserver sur {site}"
    override val stopsTwoPlus = "2 escales et plus"
    override val baggage = "Bagages"
    override val anyBaggage = "Cabine seule"
    override val navAccount = "Compte"
    override val seeDetails = "Voir les détails"
    override val sitesQuoting = "sur {n} sites"
    override val onlyOnSite = "sur {site}"
    override val thisDevice = "Cet appareil"
    override val thisDeviceSub = "Pas de compte à créer. Vos suivis et vos réglages vivent ici."
    override val preferences = "Préférences"
    override val helpAndInfo = "Aide et informations"
    override val settingsAndAlerts = "Réglages et notifications"
    override val settingsAndAlertsSub = "Langue, devise, alertes, données"
    override val fromQuestion = "D’où partez-vous ?"
    override val toQuestion = "Où allez-vous ?"
    override val whenQuestion = "Quand partez-vous ?"
    override val whoQuestion = "Qui voyage ?"
    override val reviewTitle = "Vérifiez votre recherche"
    override val continueLabel = "Continuer"
    override val airportsHere = "Aéroports en Algérie"
    override val popularDestinations = "Destinations fréquentes"
    override val swapRoute = "Inverser le départ et l’arrivée"
    override val pickReturn = "Choisir le retour"
    override val stepOf = "Étape {n} sur {total}"
    override val itineraryLabel = "Itinéraire"
    override val layoverAt = "{time} d’escale à {airport}"
    override val layoverDeparts = "repart à {time}"
    override val overnightStop = "nuit sur place"
    override val trackRulesLabel = "Nous vous préviendrons quand"
    override val trackRulePrice = "Le prix descend sous {price}"
    override val trackRulePriceBody = "C’est le prix que vous avez vu en cherchant. Nous n’envoyons rien tant qu’il n’est pas battu."
    override val trackRuleSeat = "Une place se libère"
    override val trackRuleSeatBody = "Si toutes les places partent, nous continuons à regarder et vous prévenons dès qu’il en revient une."
    override val trackNotifications = "Notifications"
    override val trackNotificationsBody = "Le seul moyen de vous prévenir. Aucune adresse e-mail n’est demandée."
    override val trackPrivacy = "Pas de compte, pas d’adresse e-mail. Le suivi reste sur cet appareil et s’arrête tout seul une fois la date passée."
    override val trackOn = "Activer le suivi"
    override val trackDone = "Suivi activé"
    override val trackDoneBody = "Nous regardons ce trajet. Vous n’avez rien d’autre à faire."
    override val trackSeatTitle = "Ce trajet est complet"
    override val trackSeatBody = "Aucun site ne propose de place à cette date. Nous vous prévenons dès qu’il y en a une."
    override val watchingPrice = "Vous prévenir si le prix passe sous {price}"
    override val watchingSeat = "Vous prévenir dès qu’une place se libère"
    override val seenAtSearch = "Prix vu à la recherche : {price}"
    override val notificationsTitle = "Notifications"
    override val noNotifications = "Rien pour l’instant"
    override val noNotificationsSub = "Les notifications arrivent ici quand un trajet suivi baisse ou qu’une place se libère."
    override val alertDropTitle = "Le prix a baissé"
    override val alertDropBody = "{price}, soit {saved} de moins qu’à votre recherche."
    override val alertSeatTitle = "Une place s’est libérée"
    override val alertSeatBody = "Le vol était complet. Il y a de nouveau des places à partir de {price}."
    override val notificationsFoot = "Les notifications viennent uniquement de vos suivis. Vous pouvez les couper dans les réglages de votre téléphone."
    override val airlinesOnRoute = "Compagnies sur ce trajet"
    override val fromPrice = "dès {price}"
    override val allAirlines = "Toutes"
    override val priceChart = "Graphique"
    override val priceByDate = "Prix par date"
    override val calendarTab = "Calendrier"
    override val bookingSites = "Sites de réservation"
    override val clearAll = "Tout effacer"
    override val bagHoldIncluded = "Bagage en soute inclus"
    override val bagHoldSub = "Masquer les tarifs sans bagage"
    override val markAllRead = "Tout lire"
    override val chartSub = "prix le plus bas par jour"
    override val chartLow = "Le plus bas"
    override val chartAvg = "Moyenne"
    override val chartHigh = "Le plus haut"
    override val chartNote = "Sur les derniers relevés. Un prix passé n\u2019est pas une promesse sur le prix de demain."
    override val showOffers = "Voir les {n} offres"
    override val cheapestOver = "Le moins cher sur cette période"
    override val seeFlightsOn = "Voir les vols du {date}"
    override val offerDetails = "Détail de l\u2019offre"
    override val itinerary = "Itinéraire"
    override val cheaper = "moins cher"
    override val totalTime = "{duration} au total"
    override val sameFlightOn = "Le même vol sur {n} sites"
    override val statWatches = "suivis actifs"
    override val statNotifications = "notifications"
    override val statSearches = "recherches"
    override val currency = "Devise"
    override val currencyDzd = "Dinar algérien"
    override val homeAirport = "Aéroport de départ"
    override val notSet = "Non défini"
    override val timeNow = "à l\u2019instant"
    override val timeHours = "il y a {n} h"
    override val timeYesterday = "hier"
    override val timeDays = "il y a {n} j"
    override val timeWeeks = "il y a {n} sem"
    override val prefDrops = "Baisse de prix"
    override val prefDropsSub = "Quand un trajet suivi passe sous le prix vu"
    override val prefSeats = "Place libérée"
    override val prefSeatsSub = "Quand une place revient sur un vol complet"
    override val prefEnded = "Fin de suivi"
    override val prefEndedSub = "Quand la date d\u2019un suivi est passée"
    override val prefMutedNote = "Si vous coupez tout, les suivis continuent mais rien ne vous parvient."
    override val display = "Affichage"
    override val theme = "Thème"
    override val themeSystem = "Comme le téléphone"
    override val themeLight = "Clair"
    override val themeDark = "Sombre"
    override val dataSection = "Données"
    override val clearRecent = "Effacer les recherches récentes"
    override val clearRecentSub = "{n} recherches enregistrées sur cet appareil"
    override val stopAllWatches = "Arrêter tous les suivis"
    override val stopAllWatchesSub = "{n} suivis actifs"
    override val nothingToClear = "Rien à effacer"
    override val handoverTitle = "Réservation"
    override val continueOn = "Vous allez continuer sur {site}"
    override val notSeller = "Qasda compare les prix, il ne vend pas de billets. La réservation et le paiement se font sur le site du vendeur."
    override val labelFlight = "Vol"
    override val labelRoute = "Trajet"
    override val labelDate = "Date"
    override val labelTravellers = "Voyageurs"
    override val labelShownPrice = "Prix affiché"
    override val priceMayChange = "Le prix peut avoir changé depuis notre dernière vérification. Vérifiez-le sur la page du vendeur avant de payer."
    override val openSite = "Ouvrir {site}"
    override val backToResults = "Revenir aux résultats"
    override val priceHistoryTitle = "Historique des prix"
    override val clearRecentOneSub = "1 recherche enregistrée sur cet appareil"
    override val stopAllWatchesOneSub = "1 suivi actif"
}

/** Arabic: right to left, and the reason every layout is mirrored rather than hand-written twice. */
private object ArWords : Words {
    override val pieceOne = "قطعة واحدة"
    override val bagChecked = "أمتعة مسجلة"
    override val bagCabinOnly = "حقيبة يد فقط"
    override val bagNoneAtAll = "بدون أمتعة"
    override val baggageHeading = "الأمتعة"
    override val cabinBagLabel = "حقيبة يد"
    override val checkedBagLabel = "أمتعة مسجلة"
    override val notificationsOff = "موقوفة على هذا الهاتف. اضغط لإعادة تفعيلها."
    override val statusActive = "نشط"
    override val seeOffers = "عرض العروض"
    override val cheapestOnSites = "الأرخص على {n} مواقع"
    override val soldOut = "مكتملة"
    override val noSeats = "لا مقاعد"
    override val notifyMe = "أعلمني"
    override val heroTitle = "كل المواقع. بحث واحد."
    override val heroSub = "يبحث قصدة في مواقع الحجز الجزائرية في الوقت نفسه."
    override val from = "المغادرة"
    override val to = "الوجهة"
    override val dates = "التواريخ"
    override val travellers = "المسافرون"
    override val search = "ابحث"
    override val roundTrip = "ذهاب وإياب"
    override val oneWay = "ذهاب فقط"
    override val searching = "نبحث عن أفضل سعر"
    override val searchingSub = "أربعة مواقع، في الوقت نفسه."
    override val resultsCount = "{n} رحلات"
    override val cheapestOn = "{site} · الأرخص"
    override val bagIncluded = "مع حقيبة"
    override val bagNone = "بدون حقيبة"
    override val seatsShort = "مقاعد: {n}"
    override val direct = "مباشر"
    override val stopsOne = "توقف واحد"
    override val stopsMany = "{n} توقفات"
    override val outbound = "ذهاب"
    override val inbound = "إياب"
    override val book = "احجز"
    override val noResultsTitle = "لا توجد رحلات {from} ← {to}"
    override val noResultsSub = "لا يعرض أي موقع حجز هذا الخط في هذا التاريخ."
    override val failedTitle = "لم يكتمل البحث"
    override val failedSub = "انقطع الاتصال قبل أن تجيب المواقع."
    override val retry = "أعد المحاولة"
    override val priceBySite = "السعر، موقعًا بموقع"
    override val cheapest = "الأرخص"
    override val economy = "اقتصادية"
    override val searchCity = "مدينة أو مطار"
    override val noAirport = "لا يوجد مطار مطابق"
    override val chooseDates = "اختر التواريخ"
    override val confirm = "تأكيد"
    override val cancel = "إلغاء"
    override val cabinBag = "حقيبة اليد"
    override val checkedBag = "حقيبة مسجلة"
    override val bagUnknown = "غير محدد"
    override val pieces = "{n} قطعة"
    override val kilos = "{n} كغ"
    override val conditions = "شروط التذكرة"
    override val refundable = "قابلة للاسترداد"
    override val nonRefundable = "غير قابلة للاسترداد"
    override val changeable = "قابلة للتغيير"
    override val nonChangeable = "غير قابلة للتغيير"
    override val feeApplies = "رسوم {amount}"
    override val operatedBy = "تشغيل {airline}"
    override val adults = "البالغون"
    override val adultsAge = "12 سنة فما فوق"
    override val children = "الأطفال"
    override val childrenAge = "2 – 11 سنة"
    override val infants = "الرضّع"
    override val infantsAge = "أقل من سنتين"
    override val cabinClass = "درجة السفر"
    override val premium = "اقتصادية بريميوم"
    override val business = "رجال الأعمال"
    override val first = "الأولى"
    override val apply = "تطبيق"
    override val filters = "عوامل التصفية"
    override val sortBy = "ترتيب"
    override val sortPrice = "السعر"
    override val sortDeparture = "المغادرة"
    override val sortDuration = "المدة"
    override val departureTime = "وقت المغادرة"
    override val morning = "صباحًا"
    override val afternoon = "بعد الظهر"
    override val evening = "مساءً"
    override val night = "ليلًا"
    override val maxPrice = "السعر الأقصى"
    override val reset = "إعادة الضبط"
    override val showCount = "عرض {n} رحلات"
    override val noMatchTitle = "لا توجد رحلة مطابقة"
    override val noMatchSub = "وسّع عوامل التصفية لرؤية نتائج أكثر."
    override val stopsLabel = "التوقفات"
    override val navHome = "الرئيسية"
    override val navHelp = "مساعدة"
    override val navSettings = "الإعدادات"
    override val about = "من نحن"
    override val language = "اللغة"
    override val systemLanguage = "لغة الهاتف"
    override val howItWorks = "كيف تعمل"
    override val privacy = "الخصوصية"
    override val contactUs = "اكتب لنا"
    override val openOnSite = "افتح على الموقع"
    override val version = "الإصدار"
    override val recentSearches = "عمليات بحث أخيرة"
    override val offline = "لا يوجد اتصال بالإنترنت"
    override val seatOne = "مقعد واحد"
    override val edit = "تعديل"
    override val priceCalendar = "تقويم الأسعار"
    override val calendarLoading = "نسأل المواقع عن كل تاريخ. يستغرق هذا لحظة."
    override val calendarEmpty = "لا توجد أسعار لهذه الفترة."
    override val calendarNote = "كل سعر هنا عرضه موقع لذلك التاريخ. التاريخ بلا سعر هو تاريخ لم يسعّره أي موقع."
    override val cheapestDay = "الأرخص: {date} · {price}"
    override val noPriceThatDay = "لا سعر"
    override val nearbyDates = "تواريخ قريبة"
    override val comparing = "نقارن العروض…"
    override val offersSoFar = "{n} عروض"
    override val aboutTitle = "من نحن"
    override val aboutTagline = "مقارنة أسعار الرحلات انطلاقًا من الجزائر"
    override val aboutWhat = "يسأل قصدة الوكالات الجزائرية مباشرة ويعرض السعر الحقيقي لكل عرض، دون عمولة ولا زيادة."
    override val aboutNotTickets = "نحن لا نبيع التذاكر: الحجز يتم دائمًا على موقع الوكالة التي تختارها."
    override val readMore = "اقرأ المزيد"
    override val faq = "أسئلة شائعة"
    override val cheapestOfSites = "{site} · الأرخص من بين {n} مواقع"
    override val navTracking = "المتابعة"
    override val trackRoute = "تابع هذه الرحلة"
    override val trackTitle = "تابع هذه الرحلة"
    override val trackSub = "نراقب السعر نيابة عنك."
    override val trackStart = "فعّل المتابعة"
    override val trackNoAccount = "بدون حساب. عنوان بريد، رسالة تأكيد، ويمكنك إيقاف كل شيء برابط واحد."
    override val trackSent = "أُرسلت رسالة إلى {email}"
    override val trackSentSub = "افتح رابط التأكيد. ما لم تفتحه، لا نراقب شيئًا ولا نراسلك مرة أخرى."
    override val trackFailed = "تعذّر إنشاء المتابعة. أعد المحاولة."
    override val myTracking = "متابعاتي"
    override val noTracking = "لا توجد متابعات هنا"
    override val noTrackingSub = "ابحث عن رحلة ثم المس «تابع هذه الرحلة». سنراقب السعر عنك وننبهك هنا."
    override val openManageLink = "لدي متابعات بالفعل"
    override val pasteManageLink = "الصق رابط «إدارة التنبيهات» من بريدك"
    override val linkNotValid = "هذا الرابط غير صالح."
    override val stopTracking = "أوقف"
    override val newTracking = "متابعة جديدة"
    override val priceHistory = "آخر {n} يومًا"
    override val historyLow = "الأدنى"
    override val historyAverage = "المتوسط"
    override val historyHigh = "الأعلى"
    override val historyObserved = "على {n} يومًا سُجّل فيها سعر."
    override val historyThin = "السجلات أقل من أن تُرسم منحنى."
    override val historyNone = "لا توجد أسعار مسجّلة لهذه الرحلة وهذا التاريخ."
    override val seeTodayOffers = "شاهد عروض اليوم"
    override val whenYouSubscribed = "عند التفعيل: {price}"
    override val targetSet = "الحد: {price}"
    override val bestPrice = "أفضل سعر"
    override val seeOffer = "عرض التفاصيل"
    override val travellerOne = "مسافر واحد"
    override val travellersMany = "{n} مسافرين"
    override val airlineOne = "شركة واحدة"
    override val airlinesMany = "{n} شركات"
    override val compareSites = "قارن المواقع"
    override val bookOn = "احجز على {site}"
    override val stopsTwoPlus = "توقفان أو أكثر"
    override val baggage = "الأمتعة"
    override val anyBaggage = "حقيبة يد فقط"
    override val navAccount = "الحساب"
    override val seeDetails = "عرض التفاصيل"
    override val sitesQuoting = "على {n} مواقع"
    override val onlyOnSite = "على {site}"
    override val thisDevice = "هذا الجهاز"
    override val thisDeviceSub = "لا حاجة لإنشاء حساب. متابعاتك وإعداداتك محفوظة هنا."
    override val preferences = "التفضيلات"
    override val helpAndInfo = "المساعدة والمعلومات"
    override val settingsAndAlerts = "الإعدادات والإشعارات"
    override val settingsAndAlertsSub = "اللغة والعملة والتنبيهات والبيانات"
    override val fromQuestion = "من أين تسافر؟"
    override val toQuestion = "إلى أين تريد الذهاب؟"
    override val whenQuestion = "متى تسافر؟"
    override val whoQuestion = "من يسافر؟"
    override val reviewTitle = "تحقق من بحثك"
    override val continueLabel = "متابعة"
    override val airportsHere = "مطارات في الجزائر"
    override val popularDestinations = "وجهات متكررة"
    override val swapRoute = "عكس المغادرة والوجهة"
    override val pickReturn = "اختر تاريخ العودة"
    override val stepOf = "الخطوة {n} من {total}"
    override val itineraryLabel = "خط الرحلة"
    override val layoverAt = "{time} توقف في {airport}"
    override val layoverDeparts = "المغادرة {time}"
    override val overnightStop = "مبيت في المطار"
    override val trackRulesLabel = "سننبهك عندما"
    override val trackRulePrice = "ينزل السعر تحت {price}"
    override val trackRulePriceBody = "هذا هو السعر الذي رأيته عند البحث. لا نرسل شيئًا قبل أن يُتجاوز."
    override val trackRuleSeat = "يتوفر مقعد"
    override val trackRuleSeatBody = "إذا نفدت كل المقاعد، نواصل المراقبة وننبهك فور عودة مقعد."
    override val trackNotifications = "الإشعارات"
    override val trackNotificationsBody = "الوسيلة الوحيدة لتنبيهك. لا نطلب أي بريد إلكتروني."
    override val trackPrivacy = "لا حساب ولا بريد إلكتروني. تبقى المتابعة على هذا الجهاز وتتوقف وحدها بعد مرور التاريخ."
    override val trackOn = "تفعيل المتابعة"
    override val trackDone = "تم تفعيل المتابعة"
    override val trackDoneBody = "نراقب هذا المسار. لا شيء آخر عليك فعله."
    override val trackSeatTitle = "هذه الرحلة كاملة"
    override val trackSeatBody = "لا يعرض أي موقع مقعدًا في هذا التاريخ. سننبهك فور توفر مقعد."
    override val watchingPrice = "ننبهك إذا نزل السعر تحت {price}"
    override val watchingSeat = "ننبهك فور توفر مقعد"
    override val seenAtSearch = "السعر عند البحث · {price}"
    override val notificationsTitle = "الإشعارات"
    override val noNotifications = "لا شيء بعد"
    override val noNotificationsSub = "تصلك الإشعارات هنا عند انخفاض سعر مسار متابَع أو عند توفر مقعد."
    override val alertDropTitle = "انخفض السعر"
    override val alertDropBody = "{price} · أقل بـ {saved} مما رأيته عند البحث"
    override val alertSeatTitle = "توفرت مقاعد"
    override val alertSeatBody = "كانت الرحلة كاملة. المقاعد متوفرة الآن ابتداءً من {price}"
    override val notificationsFoot = "لا تأتي الإشعارات إلا من متابعاتك. يمكنك إيقافها من إعدادات هاتفك."
    override val airlinesOnRoute = "شركات الطيران على هذا المسار"
    override val fromPrice = "ابتداءً من {price}"
    override val allAirlines = "الكل"
    override val priceChart = "رسم بياني"
    override val priceByDate = "السعر حسب التاريخ"
    override val calendarTab = "تقويم"
    override val bookingSites = "مواقع الحجز"
    override val clearAll = "مسح الكل"
    override val bagHoldIncluded = "أمتعة مسجلة مشمولة"
    override val bagHoldSub = "إخفاء الأسعار بدون أمتعة"
    override val markAllRead = "تعليم الكل كمقروء"
    override val chartSub = "أدنى سعر في اليوم"
    override val chartLow = "الأدنى"
    override val chartAvg = "المتوسط"
    override val chartHigh = "الأعلى"
    override val chartNote = "بناءً على آخر عمليات الرصد. السعر السابق ليس وعدًا بسعر الغد."
    override val showOffers = "عرض {n} عرضًا"
    override val cheapestOver = "الأرخص في هذه الفترة"
    override val seeFlightsOn = "عرض رحلات {date}"
    override val offerDetails = "تفاصيل العرض"
    override val itinerary = "خط الرحلة"
    override val cheaper = "الأرخص"
    override val totalTime = "{duration} إجمالاً"
    override val sameFlightOn = "الرحلة نفسها على {n} مواقع"
    override val statWatches = "متابعات نشطة"
    override val statNotifications = "إشعارات"
    override val statSearches = "عمليات بحث"
    override val currency = "العملة"
    override val currencyDzd = "دينار جزائري"
    override val homeAirport = "مطار المغادرة"
    override val notSet = "غير محدد"
    override val timeNow = "الآن"
    override val timeHours = "قبل {n} ساعة"
    override val timeYesterday = "أمس"
    override val timeDays = "قبل {n} يوم"
    override val timeWeeks = "قبل {n} أسبوع"
    override val prefDrops = "انخفاض السعر"
    override val prefDropsSub = "عندما ينزل سعر رحلة متابَعة تحت السعر الذي رأيته"
    override val prefSeats = "توفر مقعد"
    override val prefSeatsSub = "عندما يعود مقعد إلى رحلة مكتملة"
    override val prefEnded = "انتهاء المتابعة"
    override val prefEndedSub = "عندما يمر تاريخ المتابعة"
    override val prefMutedNote = "إذا أوقفت كل شيء، تستمر المتابعة لكن لن يصلك أي إشعار."
    override val display = "العرض"
    override val theme = "المظهر"
    override val themeSystem = "حسب الهاتف"
    override val themeLight = "فاتح"
    override val themeDark = "داكن"
    override val dataSection = "البيانات"
    override val clearRecent = "مسح عمليات البحث الأخيرة"
    override val clearRecentSub = "{n} عمليات بحث محفوظة على هذا الجهاز"
    override val stopAllWatches = "إيقاف كل المتابعات"
    override val stopAllWatchesSub = "{n} متابعات نشطة"
    override val nothingToClear = "لا شيء لمسحه"
    override val handoverTitle = "الحجز"
    override val continueOn = "ستتابع على {site}"
    override val notSeller = "قصدة يقارن الأسعار ولا يبيع التذاكر. يتم الحجز والدفع على موقع البائع."
    override val labelFlight = "الرحلة"
    override val labelRoute = "المسار"
    override val labelDate = "التاريخ"
    override val labelTravellers = "المسافرون"
    override val labelShownPrice = "السعر المعروض"
    override val priceMayChange = "قد يكون السعر تغيّر منذ آخر تحقق. تأكد منه على صفحة البائع قبل الدفع."
    override val openSite = "فتح {site}"
    override val backToResults = "العودة إلى النتائج"
    override val priceHistoryTitle = "سجل الأسعار"
    override val clearRecentOneSub = "عملية بحث واحدة محفوظة على هذا الجهاز"
    override val stopAllWatchesOneSub = "متابعة واحدة نشطة"
}

/** English. */
private object EnWords : Words {
    override val pieceOne = "1 piece"
    override val bagChecked = "Checked bag"
    override val bagCabinOnly = "Cabin only"
    override val bagNoneAtAll = "No bag"
    override val baggageHeading = "Baggage"
    override val cabinBagLabel = "Cabin bag"
    override val checkedBagLabel = "Checked bag"
    override val notificationsOff = "Turned off on this phone. Tap to switch them back on."
    override val statusActive = "Active"
    override val seeOffers = "See offers"
    override val cheapestOnSites = "Cheapest on {n} sites"
    override val soldOut = "Sold out"
    override val noSeats = "0 seats"
    override val notifyMe = "Notify me"
    override val heroTitle = "Every site. One search."
    override val heroSub = "Qasda queries the Algerian booking sites at the same time."
    override val from = "From"
    override val to = "To"
    override val dates = "Dates"
    override val travellers = "Travellers"
    override val search = "Search"
    override val roundTrip = "Round trip"
    override val oneWay = "One way"
    override val searching = "Finding the best price"
    override val searchingSub = "Four sites, at once."
    override val resultsCount = "{n} flights"
    override val cheapestOn = "{site} · cheapest"
    override val bagIncluded = "Bag included"
    override val bagNone = "No checked bag"
    override val seatsShort = "{n} seats"
    override val direct = "Direct"
    override val stopsOne = "1 stop"
    override val stopsMany = "{n} stops"
    override val outbound = "Outbound"
    override val inbound = "Return"
    override val book = "Book"
    override val noResultsTitle = "No flights {from} → {to}"
    override val noResultsSub = "No booking site offers this route on this date."
    override val failedTitle = "The search did not complete"
    override val failedSub = "The connection dropped before the sites answered."
    override val retry = "Try again"
    override val priceBySite = "The price, site by site"
    override val cheapest = "cheapest"
    override val economy = "Economy"
    override val searchCity = "City or airport"
    override val noAirport = "No matching airport"
    override val chooseDates = "Choose dates"
    override val confirm = "Confirm"
    override val cancel = "Cancel"
    override val cabinBag = "Cabin bag"
    override val checkedBag = "Checked bag"
    override val bagUnknown = "Not stated"
    override val pieces = "{n} pieces"
    override val kilos = "{n} kg"
    override val conditions = "Ticket conditions"
    override val refundable = "Refundable"
    override val nonRefundable = "Non-refundable"
    override val changeable = "Changeable"
    override val nonChangeable = "Not changeable"
    override val feeApplies = "{amount} fee"
    override val operatedBy = "Operated by {airline}"
    override val adults = "Adults"
    override val adultsAge = "12 and over"
    override val children = "Children"
    override val childrenAge = "2 – 11"
    override val infants = "Infants"
    override val infantsAge = "under 2"
    override val cabinClass = "Cabin class"
    override val premium = "Premium economy"
    override val business = "Business"
    override val first = "First"
    override val apply = "Apply"
    override val filters = "Filters"
    override val sortBy = "Sort"
    override val sortPrice = "Price"
    override val sortDeparture = "Departure"
    override val sortDuration = "Duration"
    override val departureTime = "Departure time"
    override val morning = "Morning"
    override val afternoon = "Afternoon"
    override val evening = "Evening"
    override val night = "Night"
    override val maxPrice = "Maximum price"
    override val reset = "Reset"
    override val showCount = "Show {n} flights"
    override val noMatchTitle = "No flight matches"
    override val noMatchSub = "Widen the filters to see more results."
    override val stopsLabel = "Stops"
    override val navHome = "Home"
    override val navHelp = "Help"
    override val navSettings = "Settings"
    override val about = "About"
    override val language = "Language"
    override val systemLanguage = "Phone language"
    override val howItWorks = "How it works"
    override val privacy = "Privacy"
    override val contactUs = "Write to us"
    override val openOnSite = "Open on the site"
    override val version = "Version"
    override val recentSearches = "Recent searches"
    override val offline = "No internet connection"
    override val seatOne = "1 seat"
    override val edit = "Edit"
    override val priceCalendar = "Price calendar"
    override val calendarLoading = "Asking the sites about each date. This takes a moment."
    override val calendarEmpty = "No prices for these dates."
    override val calendarNote = "Every price here is one a site showed for that date. A date with no price is a date no site quoted."
    override val cheapestDay = "Cheapest: {date} · {price}"
    override val noPriceThatDay = "No price"
    override val nearbyDates = "Nearby dates"
    override val comparing = "Comparing the offers…"
    override val offersSoFar = "{n} offers"
    override val aboutTitle = "About"
    override val aboutTagline = "Flight prices from Algeria, compared"
    override val aboutWhat = "Qasda queries the Algerian agencies live and shows the real price of every offer, with no commission and no markup."
    override val aboutNotTickets = "We do not sell tickets: the booking always happens on the site of the agency you choose."
    override val readMore = "Read more"
    override val faq = "Frequently asked questions"
    override val cheapestOfSites = "{site} · cheapest of {n} sites"
    override val navTracking = "Tracking"
    override val trackRoute = "Track this route"
    override val trackTitle = "Track this route"
    override val trackSub = "We watch the price for you."
    override val trackStart = "Start tracking"
    override val trackNoAccount = "No account. One address, one confirmation email, and one link that stops everything."
    override val trackSent = "Email sent to {email}"
    override val trackSentSub = "Open the confirmation link. Until you do we watch nothing and write to you no further."
    override val trackFailed = "The tracking could not be created. Try again."
    override val myTracking = "My tracking"
    override val noTracking = "Nothing tracked here"
    override val noTrackingSub = "Run a search, then tap “Track this route”. We will watch the price for you and tell you here."
    override val openManageLink = "I already track routes"
    override val pasteManageLink = "Paste the “Manage my alerts” link from your email"
    override val linkNotValid = "That link is not valid."
    override val stopTracking = "Stop"
    override val newTracking = "New tracking"
    override val priceHistory = "Last {n} days"
    override val historyLow = "Lowest"
    override val historyAverage = "Average"
    override val historyHigh = "Highest"
    override val historyObserved = "Over {n} days on which a price was recorded."
    override val historyThin = "Too few readings to draw a line."
    override val historyNone = "No price recorded for this route and date."
    override val seeTodayOffers = "See today’s offers"
    override val whenYouSubscribed = "When you started: {price}"
    override val targetSet = "Threshold: {price}"
    override val bestPrice = "BEST PRICE"
    override val seeOffer = "See the offer"
    override val travellerOne = "1 traveller"
    override val travellersMany = "{n} travellers"
    override val airlineOne = "1 airline"
    override val airlinesMany = "{n} airlines"
    override val compareSites = "Compare the sites"
    override val bookOn = "Book on {site}"
    override val stopsTwoPlus = "2 stops or more"
    override val baggage = "Baggage"
    override val anyBaggage = "Cabin bag only"
    override val navAccount = "Account"
    override val seeDetails = "See details"
    override val sitesQuoting = "on {n} sites"
    override val onlyOnSite = "on {site}"
    override val thisDevice = "This device"
    override val thisDeviceSub = "No account to create. Your tracking and settings live here."
    override val preferences = "Preferences"
    override val helpAndInfo = "Help and information"
    override val settingsAndAlerts = "Settings and notifications"
    override val settingsAndAlertsSub = "Language, currency, alerts, data"
    override val fromQuestion = "Where are you flying from?"
    override val toQuestion = "Where are you going?"
    override val whenQuestion = "When are you going?"
    override val whoQuestion = "Who is travelling?"
    override val reviewTitle = "Check your search"
    override val continueLabel = "Continue"
    override val airportsHere = "Airports in Algeria"
    override val popularDestinations = "Frequent destinations"
    override val swapRoute = "Swap departure and destination"
    override val pickReturn = "Choose the return"
    override val stepOf = "Step {n} of {total}"
    override val itineraryLabel = "Itinerary"
    override val layoverAt = "{time} layover in {airport}"
    override val layoverDeparts = "departs {time}"
    override val overnightStop = "overnight on the ground"
    override val trackRulesLabel = "We will tell you when"
    override val trackRulePrice = "The price drops below {price}"
    override val trackRulePriceBody = "That is the price you were shown when you searched. Nothing is sent until it is beaten."
    override val trackRuleSeat = "A seat opens up"
    override val trackRuleSeatBody = "If every seat goes, we keep looking and tell you the moment one comes back."
    override val trackNotifications = "Notifications"
    override val trackNotificationsBody = "The only way we can reach you. No email address is asked for."
    override val trackPrivacy = "No account, no email address. Tracking stays on this device and stops on its own once the date has passed."
    override val trackOn = "Start tracking"
    override val trackDone = "Tracking is on"
    override val trackDoneBody = "We are watching this route. There is nothing else for you to do."
    override val trackSeatTitle = "This route is fully booked"
    override val trackSeatBody = "No site has a seat on this date. We will tell you as soon as one appears."
    override val watchingPrice = "Tell you if the price drops below {price}"
    override val watchingSeat = "Tell you as soon as a seat opens up"
    override val seenAtSearch = "Price when you searched: {price}"
    override val notificationsTitle = "Notifications"
    override val noNotifications = "Nothing yet"
    override val noNotificationsSub = "Notifications land here when a route you follow gets cheaper, or when a seat opens up."
    override val alertDropTitle = "The price has dropped"
    override val alertDropBody = "{price}, {saved} less than when you searched."
    override val alertSeatTitle = "A seat has opened up"
    override val alertSeatBody = "The flight was full. Seats are back from {price}."
    override val notificationsFoot = "Notifications only ever come from routes you follow. You can turn them off in your phone's settings."
    override val airlinesOnRoute = "Airlines on this route"
    override val fromPrice = "from {price}"
    override val allAirlines = "All"
    override val priceChart = "Chart"
    override val priceByDate = "Price by date"
    override val calendarTab = "Calendar"
    override val bookingSites = "Booking sites"
    override val clearAll = "Clear all"
    override val bagHoldIncluded = "Checked bag included"
    override val bagHoldSub = "Hide fares without a bag"
    override val markAllRead = "Mark all read"
    override val chartSub = "lowest price per day"
    override val chartLow = "Lowest"
    override val chartAvg = "Average"
    override val chartHigh = "Highest"
    override val chartNote = "From the readings we have. A past price is not a promise about tomorrow\u2019s."
    override val showOffers = "Show the {n} offers"
    override val cheapestOver = "Cheapest over this period"
    override val seeFlightsOn = "See flights on {date}"
    override val offerDetails = "Offer details"
    override val itinerary = "Itinerary"
    override val cheaper = "cheaper"
    override val totalTime = "{duration} in total"
    override val sameFlightOn = "The same flight on {n} sites"
    override val statWatches = "active alerts"
    override val statNotifications = "notifications"
    override val statSearches = "searches"
    override val currency = "Currency"
    override val currencyDzd = "Algerian dinar"
    override val homeAirport = "Home airport"
    override val notSet = "Not set"
    override val timeNow = "just now"
    override val timeHours = "{n} h ago"
    override val timeYesterday = "yesterday"
    override val timeDays = "{n} d ago"
    override val timeWeeks = "{n} w ago"
    override val prefDrops = "Price drop"
    override val prefDropsSub = "When a tracked route goes below the price you saw"
    override val prefSeats = "Seat freed"
    override val prefSeatsSub = "When a seat comes back on a full flight"
    override val prefEnded = "Tracking ended"
    override val prefEndedSub = "When a tracked date has passed"
    override val prefMutedNote = "Turn everything off and the routes stay tracked, but nothing reaches you."
    override val display = "Display"
    override val theme = "Theme"
    override val themeSystem = "Match phone"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val dataSection = "Data"
    override val clearRecent = "Clear recent searches"
    override val clearRecentSub = "{n} searches kept on this device"
    override val stopAllWatches = "Stop all tracking"
    override val stopAllWatchesSub = "{n} active"
    override val nothingToClear = "Nothing to clear"
    override val handoverTitle = "Booking"
    override val continueOn = "You are continuing on {site}"
    override val notSeller = "Qasda compares prices, it does not sell tickets. Booking and payment happen on the seller\u2019s own site."
    override val labelFlight = "Flight"
    override val labelRoute = "Route"
    override val labelDate = "Date"
    override val labelTravellers = "Travellers"
    override val labelShownPrice = "Price shown"
    override val priceMayChange = "The price may have changed since we last checked. Confirm it on the seller\u2019s page before you pay."
    override val openSite = "Open {site}"
    override val backToResults = "Back to results"
    override val priceHistoryTitle = "Price history"
    override val clearRecentOneSub = "1 search kept on this device"
    override val stopAllWatchesOneSub = "1 active"
}

/**
 * Every string this table holds, by name.
 *
 * Written out rather than reflected because `core` is shared with iOS, where
 * there is no reflection to do it with. It exists for the tests — one that
 * checks no language has left a string blank, and one that checks a
 * translation has not dropped a `{n}` placeholder and taken the number with
 * it — so being one entry out of date costs coverage, never correctness.
 */
fun Words.everyString(): List<Pair<String, String>> = listOf(
    "appName" to appName,
    "heroTitle" to heroTitle,
    "heroSub" to heroSub,
    "from" to from,
    "to" to to,
    "dates" to dates,
    "travellers" to travellers,
    "search" to search,
    "roundTrip" to roundTrip,
    "oneWay" to oneWay,
    "searching" to searching,
    "searchingSub" to searchingSub,
    "resultsCount" to resultsCount,
    "cheapestOn" to cheapestOn,
    "bagIncluded" to bagIncluded,
    "bagNone" to bagNone,
    "seatsShort" to seatsShort,
    "direct" to direct,
    "stopsOne" to stopsOne,
    "stopsMany" to stopsMany,
    "outbound" to outbound,
    "inbound" to inbound,
    "book" to book,
    "noResultsTitle" to noResultsTitle,
    "noResultsSub" to noResultsSub,
    "failedTitle" to failedTitle,
    "failedSub" to failedSub,
    "retry" to retry,
    "priceBySite" to priceBySite,
    "cheapest" to cheapest,
    "economy" to economy,
    "searchCity" to searchCity,
    "noAirport" to noAirport,
    "chooseDates" to chooseDates,
    "confirm" to confirm,
    "cancel" to cancel,
    "cabinBag" to cabinBag,
    "checkedBag" to checkedBag,
    "bagUnknown" to bagUnknown,
    "pieces" to pieces,
    "kilos" to kilos,
    "conditions" to conditions,
    "refundable" to refundable,
    "nonRefundable" to nonRefundable,
    "changeable" to changeable,
    "nonChangeable" to nonChangeable,
    "feeApplies" to feeApplies,
    "operatedBy" to operatedBy,
    "adults" to adults,
    "adultsAge" to adultsAge,
    "children" to children,
    "childrenAge" to childrenAge,
    "infants" to infants,
    "infantsAge" to infantsAge,
    "cabinClass" to cabinClass,
    "premium" to premium,
    "business" to business,
    "first" to first,
    "apply" to apply,
    "filters" to filters,
    "sortBy" to sortBy,
    "sortPrice" to sortPrice,
    "sortDeparture" to sortDeparture,
    "sortDuration" to sortDuration,
    "departureTime" to departureTime,
    "morning" to morning,
    "afternoon" to afternoon,
    "evening" to evening,
    "night" to night,
    "maxPrice" to maxPrice,
    "reset" to reset,
    "showCount" to showCount,
    "noMatchTitle" to noMatchTitle,
    "noMatchSub" to noMatchSub,
    "stopsLabel" to stopsLabel,
    "navHome" to navHome,
    "navHelp" to navHelp,
    "navSettings" to navSettings,
    "about" to about,
    "language" to language,
    "systemLanguage" to systemLanguage,
    "howItWorks" to howItWorks,
    "privacy" to privacy,
    "contactUs" to contactUs,
    "openOnSite" to openOnSite,
    "version" to version,
    "recentSearches" to recentSearches,
    "offline" to offline,
    "seatOne" to seatOne,
    "edit" to edit,
    "priceCalendar" to priceCalendar,
    "calendarLoading" to calendarLoading,
    "calendarEmpty" to calendarEmpty,
    "calendarNote" to calendarNote,
    "cheapestDay" to cheapestDay,
    "noPriceThatDay" to noPriceThatDay,
    "nearbyDates" to nearbyDates,
    "comparing" to comparing,
    "offersSoFar" to offersSoFar,
    "aboutTitle" to aboutTitle,
    "aboutTagline" to aboutTagline,
    "aboutWhat" to aboutWhat,
    "aboutNotTickets" to aboutNotTickets,
    "readMore" to readMore,
    "faq" to faq,
    "cheapestOfSites" to cheapestOfSites,
    "navTracking" to navTracking,
    "trackRoute" to trackRoute,
    "trackTitle" to trackTitle,
    "trackSub" to trackSub,
    "trackStart" to trackStart,
    "trackNoAccount" to trackNoAccount,
    "trackSent" to trackSent,
    "trackSentSub" to trackSentSub,
    "trackFailed" to trackFailed,
    "myTracking" to myTracking,
    "noTracking" to noTracking,
    "noTrackingSub" to noTrackingSub,
    "openManageLink" to openManageLink,
    "pasteManageLink" to pasteManageLink,
    "linkNotValid" to linkNotValid,
    "stopTracking" to stopTracking,
    "newTracking" to newTracking,
    "priceHistory" to priceHistory,
    "historyLow" to historyLow,
    "historyAverage" to historyAverage,
    "historyHigh" to historyHigh,
    "historyObserved" to historyObserved,
    "historyThin" to historyThin,
    "historyNone" to historyNone,
    "seeTodayOffers" to seeTodayOffers,
    "whenYouSubscribed" to whenYouSubscribed,
    "targetSet" to targetSet,
    "bestPrice" to bestPrice,
    "seeOffer" to seeOffer,
    "travellerOne" to travellerOne,
    "travellersMany" to travellersMany,
    "airlineOne" to airlineOne,
    "airlinesMany" to airlinesMany,
    "compareSites" to compareSites,
    "bookOn" to bookOn,
    "stopsTwoPlus" to stopsTwoPlus,
    "baggage" to baggage,
    "anyBaggage" to anyBaggage,
    "navAccount" to navAccount,
    "seeDetails" to seeDetails,
    "sitesQuoting" to sitesQuoting,
    "onlyOnSite" to onlyOnSite,
    "thisDevice" to thisDevice,
    "thisDeviceSub" to thisDeviceSub,
    "preferences" to preferences,
    "helpAndInfo" to helpAndInfo,
    "settingsAndAlerts" to settingsAndAlerts,
    "settingsAndAlertsSub" to settingsAndAlertsSub,
    "fromQuestion" to fromQuestion,
    "toQuestion" to toQuestion,
    "whenQuestion" to whenQuestion,
    "whoQuestion" to whoQuestion,
    "reviewTitle" to reviewTitle,
    "continueLabel" to continueLabel,
    "airportsHere" to airportsHere,
    "popularDestinations" to popularDestinations,
    "swapRoute" to swapRoute,
    "pickReturn" to pickReturn,
    "stepOf" to stepOf,
    "itineraryLabel" to itineraryLabel,
    "layoverAt" to layoverAt,
    "layoverDeparts" to layoverDeparts,
    "overnightStop" to overnightStop,
    "trackRulesLabel" to trackRulesLabel,
    "trackRulePrice" to trackRulePrice,
    "trackRulePriceBody" to trackRulePriceBody,
    "trackRuleSeat" to trackRuleSeat,
    "trackRuleSeatBody" to trackRuleSeatBody,
    "trackNotifications" to trackNotifications,
    "trackNotificationsBody" to trackNotificationsBody,
    "trackPrivacy" to trackPrivacy,
    "trackOn" to trackOn,
    "trackDone" to trackDone,
    "trackDoneBody" to trackDoneBody,
    "trackSeatTitle" to trackSeatTitle,
    "trackSeatBody" to trackSeatBody,
    "watchingPrice" to watchingPrice,
    "watchingSeat" to watchingSeat,
    "seenAtSearch" to seenAtSearch,
    "notificationsTitle" to notificationsTitle,
    "noNotifications" to noNotifications,
    "noNotificationsSub" to noNotificationsSub,
    "alertDropTitle" to alertDropTitle,
    "alertDropBody" to alertDropBody,
    "alertSeatTitle" to alertSeatTitle,
    "alertSeatBody" to alertSeatBody,
    "notificationsFoot" to notificationsFoot,
    "airlinesOnRoute" to airlinesOnRoute,
    "fromPrice" to fromPrice,
    "allAirlines" to allAirlines,
    "priceChart" to priceChart,
    "priceByDate" to priceByDate,
    "calendarTab" to calendarTab,
    "bookingSites" to bookingSites,
    "clearAll" to clearAll,
    "bagHoldIncluded" to bagHoldIncluded,
    "bagHoldSub" to bagHoldSub,
    "markAllRead" to markAllRead,
    "chartSub" to chartSub,
    "chartLow" to chartLow,
    "chartAvg" to chartAvg,
    "chartHigh" to chartHigh,
    "chartNote" to chartNote,
    "showOffers" to showOffers,
    "cheapestOver" to cheapestOver,
    "seeFlightsOn" to seeFlightsOn,
    "offerDetails" to offerDetails,
    "itinerary" to itinerary,
    "cheaper" to cheaper,
    "totalTime" to totalTime,
    "sameFlightOn" to sameFlightOn,
    "statWatches" to statWatches,
    "statNotifications" to statNotifications,
    "statSearches" to statSearches,
    "currency" to currency,
    "currencyDzd" to currencyDzd,
    "homeAirport" to homeAirport,
    "notSet" to notSet,
    "timeNow" to timeNow,
    "timeHours" to timeHours,
    "timeYesterday" to timeYesterday,
    "timeDays" to timeDays,
    "timeWeeks" to timeWeeks,
    "prefDrops" to prefDrops,
    "prefDropsSub" to prefDropsSub,
    "prefSeats" to prefSeats,
    "prefSeatsSub" to prefSeatsSub,
    "prefEnded" to prefEnded,
    "prefEndedSub" to prefEndedSub,
    "prefMutedNote" to prefMutedNote,
    "display" to display,
    "theme" to theme,
    "themeSystem" to themeSystem,
    "themeLight" to themeLight,
    "themeDark" to themeDark,
    "dataSection" to dataSection,
    "clearRecent" to clearRecent,
    "clearRecentSub" to clearRecentSub,
    "stopAllWatches" to stopAllWatches,
    "stopAllWatchesSub" to stopAllWatchesSub,
    "nothingToClear" to nothingToClear,
    "handoverTitle" to handoverTitle,
    "continueOn" to continueOn,
    "notSeller" to notSeller,
    "labelFlight" to labelFlight,
    "labelRoute" to labelRoute,
    "labelDate" to labelDate,
    "labelTravellers" to labelTravellers,
    "labelShownPrice" to labelShownPrice,
    "priceMayChange" to priceMayChange,
    "openSite" to openSite,
    "backToResults" to backToResults,
    "priceHistoryTitle" to priceHistoryTitle,
    "clearRecentOneSub" to clearRecentOneSub,
    "stopAllWatchesOneSub" to stopAllWatchesOneSub,
    "soldOut" to soldOut,
    "cheapestOnSites" to cheapestOnSites,
    "statusActive" to statusActive,
    "seeOffers" to seeOffers,
    "notificationsOff" to notificationsOff,
    "bagChecked" to bagChecked,
    "bagCabinOnly" to bagCabinOnly,
    "bagNoneAtAll" to bagNoneAtAll,
    "baggageHeading" to baggageHeading,
    "cabinBagLabel" to cabinBagLabel,
    "checkedBagLabel" to checkedBagLabel,
    "pieceOne" to pieceOne,
    "noSeats" to noSeats,
    "notifyMe" to notifyMe,
)
