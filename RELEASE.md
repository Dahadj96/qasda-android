# Releasing Qasda for Android

Everything the code can do is done. What is left is the part that needs a
person: a key that must not be created by a machine, a Play account, and a
phone to prove the shrunk build still works.

Work down the list. Nothing later is safe to skip.

---

## 1. The upload keystore — do this once, and back it up

```
keytool -genkeypair -v -keystore qasda-upload.jks -alias qasda \
        -keyalg RSA -keysize 2048 -validity 10000
```

Then in `local.properties`, which is git-ignored:

```
QASDA_KEYSTORE=C:/Users/you/keys/qasda-upload.jks
QASDA_KEYSTORE_PASSWORD=...
QASDA_KEY_ALIAS=qasda
QASDA_KEY_PASSWORD=...
```

**This file cannot be replaced.** An app signed by a different key is a
different app to Android; there is no recovery, only a new listing under a new
name. Put a copy somewhere that is not this computer before going further.

Opting into Play App Signing at upload time softens this — Google then holds
the signing key and this one only authorises uploads — and it is worth doing.

---

## 2. Prove the release build works — the untested risk

R8 is enabled. `assembleRelease` succeeds and R8 reports no missing rules,
but **nothing has ever run the minified build**. Ktor and
kotlinx-serialization under R8 is the classic failure that works in debug and
dies on the first API call.

```
gradlew :app:assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

On the phone, in order:

- [ ] a search returns results (this is the one that catches R8)
- [ ] results keep arriving as sites answer — the SSE stream, not one payload
- [ ] a flight opens, prices per site are listed
- [ ] **Book** opens the booking site in a browser
- [ ] Arabic mirrors the layout and prices stay left-to-right
- [ ] aeroplane mode shows the offline line rather than a 25-second wait

If a search fails only in release, it is a keep rule. `mapping.txt` under
`app/build/outputs/mapping/release/` turns the stack trace back into names.

---

## 3. Deep links

`AndroidManifest.xml` declares `autoVerify="true"` for `qasdatrip.pro`. Until
the site serves the matching file, verification fails silently and shared
links open the browser instead of the app.

Get the signing certificate's SHA-256 — from Play Console under *App signing*
once uploaded, or locally:

```
keytool -list -v -keystore qasda-upload.jks -alias qasda
```

Then serve this at `https://qasdatrip.pro/.well-known/assetlinks.json` from
the web repo:

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "pro.qasdatrip.app",
    "sha256_cert_fingerprints": ["THE:SHA:256:FROM:ABOVE"]
  }
}]
```

Use the **Play App Signing** fingerprint if you opted in — the upload key's
fingerprint is not the one installed apps are signed with.

---

## 4. Data safety, answered honestly

The form Google asks for. These answers match what the code actually does; if
that changes, this changes with it.

| Question | Answer |
|---|---|
| Does the app collect or share user data? | No |
| Accounts | None. The app has no sign-in |
| Location | Not collected. Airports are typed, never sensed |
| Personal identifiers | A random UUID per install, used only for rate-limit bucketing. Not tied to the device, the phone number or a person, and gone when the app is uninstalled |
| Analytics / crash reporting | None wired |
| Advertising | None |
| Data encrypted in transit | Yes — HTTPS throughout |
| Data deletion | Nothing to delete: uninstalling removes the only stored things, which are the chosen language and the last five searches, and both stay on the phone |

The privacy policy URL is `https://qasdatrip.pro/fr/confidentialite` (and the
`ar` / `en` equivalents).

---

## 5. Store listing

**App name:** Qasda

**Short description (80 chars max):**

- FR — `Comparez les vols au départ d'Algérie. Tous les sites, une recherche.`
- AR — `قارن رحلات الطيران من الجزائر. كل المواقع، بحث واحد.`
- EN — `Compare flights from Algeria. Every site, one search.`

**Full description** — the site's own words, so the two say the same thing:

> Qasda interroge en même temps les sites de réservation algériens et affiche
> le prix réel de chaque offre, sans commission ni majoration.
>
> • Une recherche, quatre sites — les résultats arrivent au fur et à mesure
> • Le prix, site par site, sur chaque vol
> • Aéroports algériens en arabe, en français et en anglais
> • Gratuit, sans compte
>
> Qasda ne vend pas de billets. La réservation et le paiement se font sur le
> site de l'agence que vous choisissez.

**Category:** Travel & Local · **Content rating:** Everyone · **Free**

**Graphics still needed** — the launcher icon exists in the repo, the rest do
not: a 512×512 icon PNG, a 1024×500 feature graphic, and at least two phone
screenshots. The Figma file's `🤖 Android` page has all twenty screens laid
out at 360×800 and is the place to export them from.

---

## 6. Ship

```
gradlew :app:bundleRelease
```

Upload `app/build/outputs/bundle/release/app-release.aab` to a **closed
test** track first. Not production: section 2 is the first time this build
runs on hardware that is not yours, and a bad release is easier to replace
than to recall.

Bump `versionCode` in `app/build.gradle.kts` for every upload — Play refuses a
repeat. `versionName` is for people and changes when there is something to
tell them.
