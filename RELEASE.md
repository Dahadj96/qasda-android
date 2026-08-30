# Releasing Qasda for Android

Everything the code can do is done. What is left is the part that needs a
person: a key that must not be created by a machine, a Play account, and a
phone to prove the shrunk build still works.

Work down the list. Nothing later is safe to skip.

---

## 1. The upload keystore ÔÇö do this once, and back it up

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

Opting into Play App Signing at upload time softens this ÔÇö Google then holds
the signing key and this one only authorises uploads ÔÇö and it is worth doing.

---

## 2. Prove the shrunk build works

R8 is enabled, and a minified build has now been run on hardware: a Redmi
Note 13 Pro on 29 August returned sixteen flights from a real search with no
`ClassNotFound`, no missing serializer, nothing. **Ktor and
kotlinx-serialization survive R8 with the keep rules in `proguard-rules.pro`.**

Re-run it whenever those rules, the dependencies or the models change ÔÇö it is
the check that catches the classic failure that works in debug and dies on the
first API call in release:

```
gradlew :app:assembleStaging
adb install -r app\build\outputs\apk\staging\app-staging.apk
```

`staging` is `release` in every respect R8 can see, pointed at
`dev.qasdatrip.pro` so it can be exercised against a server that is not
production. Use it rather than `release` for this: a release build aimed at a
domain that is down proves nothing.

Note that Xiaomi devices refuse Gradle's installer with
`INSTALL_FAILED_USER_RESTRICTED` unless *Install via USB* is on in Developer
options; plain `adb install -r` works either way.

On the phone, in order:

- [ ] a search returns results (this is the one that catches R8)
- [ ] results keep arriving as sites answer ÔÇö the SSE stream, not one payload
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

Get the signing certificate's SHA-256 ÔÇö from Play Console under *App signing*
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

Use the **Play App Signing** fingerprint if you opted in ÔÇö the upload key's
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
| Data encrypted in transit | Yes ÔÇö HTTPS throughout |
| Data deletion | Nothing to delete: uninstalling removes the only stored things, which are the chosen language and the last five searches, and both stay on the phone |

The privacy policy URL is `https://qasdatrip.pro/fr/confidentialite` (and the
`ar` / `en` equivalents).

---

## 5. Store listing

**App name:** Qasda

**Short description (80 chars max):**

- FR ÔÇö `Comparez les vols au d├®part d'Alg├®rie. Tous les sites, une recherche.`
- AR ÔÇö `┘éÏºÏ▒┘å Ï▒Ï¡┘äÏºÏ¬ Ïº┘äÏÀ┘èÏ▒Ïº┘å ┘à┘å Ïº┘äÏ¼Ï▓ÏºÏªÏ▒. ┘â┘ä Ïº┘ä┘à┘êÏº┘éÏ╣Ïî Ï¿Ï¡Ï½ ┘êÏºÏ¡Ï».`
- EN ÔÇö `Compare flights from Algeria. Every site, one search.`

**Full description** ÔÇö the site's own words, so the two say the same thing:

> Qasda interroge en m├¬me temps les sites de r├®servation alg├®riens et affiche
> le prix r├®el de chaque offre, sans commission ni majoration.
>
> ÔÇó Une recherche, quatre sites ÔÇö les r├®sultats arrivent au fur et ├á mesure
> ÔÇó Le prix, site par site, sur chaque vol
> ÔÇó A├®roports alg├®riens en arabe, en fran├ºais et en anglais
> ÔÇó Gratuit, sans compte
>
> Qasda ne vend pas de billets. La r├®servation et le paiement se font sur le
> site de l'agence que vous choisissez.

**Category:** Travel & Local ┬À **Content rating:** Everyone ┬À **Free**

**Graphics still needed** ÔÇö the launcher icon exists in the repo, the rest do
not: a 512├ù512 icon PNG, a 1024├ù500 feature graphic, and at least two phone
screenshots. The Figma file's `­ƒñû Android` page has all twenty screens laid
out at 360├ù800 and is the place to export them from.

---

## 6. Ship

```
gradlew :app:bundleRelease
```

Upload `app/build/outputs/bundle/release/app-release.aab` to a **closed
test** track first. Not production: section 2 is the first time this build
runs on hardware that is not yours, and a bad release is easier to replace
than to recall.

Bump `versionCode` in `app/build.gradle.kts` for every upload ÔÇö Play refuses a
repeat. `versionName` is for people and changes when there is something to
tell them.

## Running it on an emulator

An AVD is cheaper than a phone on a cable, and it is the only way to see
Arabic without changing the phone's own language. Four of the settings
below are not defaults, and the emulator is unusable without them.

    sdkmanager "emulator" "system-images;android-35;google_apis;x86_64"
    avdmanager create avd -n qasda -k "system-images;android-35;google_apis;x86_64" -d pixel_6

    emulator -avd qasda -no-snapshot-load -no-boot-anim \
      -gpu swiftshader_indirect \
      -dns-server 8.8.8.8,1.1.1.1 \
      -feature -Wifi

    adb -s emulator-5554 shell svc data enable

  - `-gpu swiftshader_indirect` because the mini PC is headless.
  - `-dns-server` because the emulator inherits the host resolver, and on
    that machine it could not resolve anything at all.
  - `-feature -Wifi` because the virtio-wifi device never comes up there: the
    guest boots with only `lo` and `dummy0`, and turning Wi-Fi off restores
    the classic emulated radio, which does.
  - `svc data enable` because that radio comes up with data off, so the guest
    has no route and the app's offline banner is telling the truth. It does
    not survive a cold boot, so it belongs in the launch script rather than
    in the AVD.

If `sdkmanager` stalls part-way through the system image - it did, twice, at
a kilobyte a second - fetch the zip directly and unpack it into
`system-images/android-35/google_apis/x86_64/`:

    curl -L -C - --speed-limit 51200 --speed-time 30 --retry 20 \
      -o x86_64-35_r09.zip \
      https://dl.google.com/android/repository/sys-img/google_apis/x86_64-35_r09.zip

`--speed-limit`/`--speed-time` are the point: they make curl give up on a
connection that has gone bad so the next attempt resumes at full speed,
instead of crawling to the end of the week.

### Firing a manage link by hand

The "manage my alerts" link in a confirmation email opens the tracking
screen. To try it without waiting for mail:

    adb shell am start -a android.intent.action.VIEW \
      -d "'https://dev.qasdatrip.pro/alerts?w=1&s=<signature>'" \
      -n pro.qasdatrip.app.debug/pro.qasdatrip.app.MainActivity

The signature is what `lib/alerts.js signWatcher(id)` returns. On a running
server:

    docker exec qasda-app-1 node -e "console.log(require('/app/lib/alerts').signWatcher(1))"

Until `assetlinks.json` is served (see section 3), Android hands these links
to the browser rather than the app, and the tracking screen's "paste the
link" fallback is how somebody gets in.
## Never ship a build you have not launched

A green build is not a working app. On 30 August a staging APK compiled,
minified, passed every unit test, and then died on every launch with:

    java.lang.VerifyError: Verifier rejected class ...
    Rejecting invocation, expected 1 argument registers,
    method signature has 2 or more

`Words` had grown to 257 constructor parameters. Dalvik addresses method
arguments as registers and `invoke-direct/range` reaches at most 255 of
them, so the call to that constructor could not be expressed and the class
verifier rejected it before the first frame. Nothing in the toolchain could
see it: javac and Kotlin have no such limit, R8 was happy, and the unit
tests run on a JVM, which is also happy. Only a dex loader disagrees.

`Words` is an interface with one object per language now, which has no
ceiling — but the class of bug does not go away, and the guard is cheap:

    powershell -File qasda-crash.ps1

It uninstalls, installs the current staging APK, clears the log, launches
the app, waits, and prints the crash buffer. An empty `==== CRASH ====`
section is the pass. Run it before sending an APK to anybody, every time,
including for a one-line change — this one was two strings.
