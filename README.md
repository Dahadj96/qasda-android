# Qasda for Android

The phone app for [qasdatrip.pro](https://qasdatrip.pro) — one search across the
Algerian booking sites, in Arabic, French and English.

Native Kotlin and Jetpack Compose. Not a browser in a costume: the list of
sixty flights has to scroll at the phone's own frame rate, and Arabic has to
mirror the layout rather than be hand-flipped.

## What is here

```
core/    Kotlin Multiplatform — everything that is not a screen
         · the API client and the search stream (Ktor)
         · the models the server already speaks
         · airport search, including the Arabic and accent folding
         · money, seats, airline names, and the three dictionaries
app/     Android — Compose screens on top of that core
```

`core` targets Android **and** iOS today. Nothing on the iOS side is built
yet, and building it needs a Mac — but the day somebody writes the SwiftUI
screens, the networking, the models and the matching are already written and
already tested. That is the whole reason the module exists.

## Running it

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or
   newer). It brings its own JDK and the Android SDK.
2. `File → Open` this folder. Let it sync — the first sync downloads Gradle
   and the dependencies and takes a few minutes.
3. Pick a device or an emulator and press Run.

The debug build talks to `dev.qasdatrip.pro`; release talks to
`qasdatrip.pro`. Both are in `app/build.gradle.kts` and nowhere else.

## Release builds

`./gradlew :app:assembleRelease` works with no setup: it shrinks with R8 and
signs with the debug key, which is installable for testing and which Play
rejects outright — so a test build cannot become a real release by accident.

A build meant for Play needs the upload keystore. Create it once, keep it
somewhere it will survive this machine, and never commit it:

```
keytool -genkeypair -v -keystore qasda-upload.jks -alias qasda \
        -keyalg RSA -keysize 2048 -validity 10000
```

Then point `local.properties` at it — that file is git-ignored:

```
QASDA_KEYSTORE=C:/Users/you/keys/qasda-upload.jks
QASDA_KEYSTORE_PASSWORD=...
QASDA_KEY_ALIAS=qasda
QASDA_KEY_PASSWORD=...
```

Losing this file is not recoverable. An app signed by a different key is a
different app to Android, and the only way back is a new listing under a new
name. Back it up somewhere that is not this computer.

## What works

- Search: origin, destination, dates, travellers — one way or return, dates
  picked from a calendar that will not offer a day that has already gone
- Airport picker over the 213 curated airports, matching the way the site
  does — أدرار and ادرار are the same query, so are Séville and seville, and
  an airport answers to the commune the databases file it under
- Live results: the list fills in as each booking site answers, sorted by the
  cheapest price any site actually quoted
- The card: bag chip, seats chip, both legs, one price and the site quoting it
- Book: opens the booking site. We do not sell tickets

## What is next, in order

1. The flight details screen — baggage, fare conditions, aircraft, and the
   price site by site
2. Recent searches, and the language switch inside the app
3. Push: a notification when a seat appears is the tracking feature the web
   cannot ship yet, because a push needs no mail domain and no inbox

## House rules, same as the website

- Booking-site names appear on the card and on the details screen, nowhere else
- Airline names stay in Latin in every language. They are brand names
- No Arabic string wraps a bare number — digits are isolated, see `Money`
- Nine seats means "at least nine" and is never printed. Below five it is a
  real count and worth knowing; null is "nobody said", which is not none
- A price shown is a price a site quoted. Nothing computed reaches a card
