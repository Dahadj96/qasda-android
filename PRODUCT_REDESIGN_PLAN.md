# Qasda Android Product Redesign Plan

Status: implementation-ready proposal  
Visual reference: `Codex Image Sep 16, 2026, 05_31_09 PM.png`  
Scope: Android application, with narrowly scoped API additions where required by comparison and tracking

## 1. Product direction

Qasda should feel like a fast, trustworthy flight-comparison engine—not a collection of large forms. The redesign keeps the existing off-white, charcoal, and teal identity and improves hierarchy, density, comparison, and tracking value.

The primary product journey is:

1. Enter a route quickly.
2. Choose dates and travellers without repeating choices.
3. Understand the best flights at a glance.
4. Compare sellers for the exact same itinerary.
5. Book with the selected seller, or track the route when the right offer is unavailable.
6. Return later and immediately understand what changed.

## 2. Success criteria

- The first useful result is visible without scrolling on a typical phone.
- A user can distinguish a flight from the sites selling that flight.
- Identical itineraries appear once, with all valid provider prices inside the group.
- Trip type is selected once on Home and is not requested again unless the user edits it.
- Search, date, traveller, results, details, and tracking screens use one documented token system.
- A tracking card answers: what is watched, current state, last check, remaining lifetime, and next action.
- Loading, partial-provider, empty, expired, offline, authentication, and error states are intentionally designed.
- English, French, and Arabic—including RTL—remain first-class.
- Accessibility targets meet Android guidance: 48 dp touch areas, scalable text, meaningful semantics, and sufficient contrast.

## 3. Non-goals

- No radical rebrand.
- No payment or ticket issuing inside Qasda.
- No invented prices, availability, seat counts, or fare rules.
- No merging based only on airline and departure time.
- No dependency on email notifications for the main tracking experience.
- No replacement of the existing Kotlin Multiplatform core or Jetpack Compose stack.

## 4. Design system refinement

### 4.1 Color roles

Keep the current palette and formalize its usage:

- Canvas: warm off-white `#FAF9F6`.
- Surface: white.
- Primary text/action: charcoal `#232522`.
- Brand/action accent: teal, using the current accent family.
- Selected state: deep teal with white content.
- Positive/price improvement: teal/green.
- Attention/limited seats: amber; never use it as a generic decoration.
- Error/unavailable/destructive: muted red.
- Dividers and borders: neutral gray with no colored border unless selected or highlighted.

Dark mode keeps the existing adapted palette. Every new token must be implemented for both modes.

### 4.2 Typography

Continue using Manrope and restrict screens to these roles:

- Display: product/home identity only.
- Screen title: 24–28 sp, semibold.
- Section title: 16–18 sp, semibold.
- Card title/primary time: 16–18 sp, semibold or bold.
- Body: 14–16 sp.
- Supporting metadata: 12–14 sp.
- Overline/status label: 11–12 sp, semibold; avoid excessive letter spacing and all-caps body content.
- Price: one consistent emphasized style; currency stays attached and locale-safe.

Text must respect font scaling up to at least 1.3× without clipping. Arabic text should use the same semantic hierarchy, not smaller substitute styles.

### 4.3 Geometry and spacing

- Base spacing unit: 4 dp, with primary rhythm at 8 dp.
- Screen horizontal margin: 16 dp.
- Dense list gap: 8–12 dp.
- Card internal padding: 12–16 dp.
- Standard card radius: 16 dp.
- Compact field/chip radius: 10–12 dp.
- Pill radius only for filters, segmented controls, and statuses.
- Standard border: 1 dp neutral.
- Selected/best-price border: 1.5–2 dp teal.
- Avoid shadows for ordinary cards; use border and surface separation. Reserve elevation for sticky controls, modal sheets, and active overlays.

### 4.4 Shared components

Create or consolidate:

- `QasdaScreenHeader`
- `QasdaCard`
- `QasdaPrimaryButton`
- `QasdaSegmentedControl`
- `QasdaFilterChip`
- `QasdaStatusChip`
- `QasdaRouteSummary`
- `QasdaPrice`
- `QasdaEmptyState`
- `QasdaLoadingSkeleton`
- `QasdaInlineError`
- `QasdaBottomAction`

These components should consume tokens rather than local hard-coded dimensions.

## 5. Navigation and interaction model

Keep the four destinations: Home, Tracking, Help, Account.

- Home owns the search draft.
- Airport, date, and traveller screens edit that draft and return a result.
- Results preserve filters, sort, and scroll position when the user opens details and returns.
- Details represents one canonical itinerary and its provider quotes.
- Tracking opens either a watch detail/history view or re-runs its search.
- Account is the profile and preferences destination; Google sign-in is the first section when signed out.

Back behavior must never discard a valid draft silently. If the user changes a sub-form and presses system Back before Apply, retain the previous committed values.

## 6. Screen plans

### 6.1 Home and search hierarchy

Target: compact, decisive, and immediately searchable.

- Keep the photographic hero, Qasda wordmark, and short product promise.
- Reduce hero height enough to show search and recent-search content above the fold.
- Use one compact segmented control for One way / Round trip.
- Stack From and To as the most prominent fields with a clear swap action.
- Place Dates and Travellers/Class in a two-column row.
- Move Direct and Checked bag out of the large form body. Treat them as optional compact preference chips below the main fields or as result filters; do not let them compete with route and date.
- Use one strong teal `Search flights` action.
- Show up to two recent searches, with `See all` only when more exist.
- A recent search loads its parameters into the draft; if its dates are no longer valid, go directly to date selection with a clear explanation.
- Preserve the draft across process recreation.

Acceptance:

- Route, trip type, dates, and traveller summary are visible without scrolling on a 1080×2400 reference device.
- Search is disabled only when required values are missing, and the missing field is visually identifiable.
- No trip-type selection is shown on subsequent steps unless the user explicitly edits it.

### 6.2 Airport selection

- Search field remains pinned below the title.
- Tabs: nearby/country, international, recent. Favorites may be added only after storage behavior is defined.
- Rows become denser: code badge, city, airport name, and optional distance/favorite action.
- Prioritize current country and recent choices; do not hide the full searchable list.
- Add search-result highlighting without changing source text.
- Empty query and zero-result states must be distinct.
- Validate RTL order and airport-code isolation.

### 6.3 Date selection

- Date screen inherits trip type from Home.
- One-way shows only Departure; round-trip shows Departure and Return.
- Keep the continuous calendar but reduce vertical waste.
- Selected range uses a soft teal bridge with deep teal endpoint circles.
- Sticky bottom action states the exact committed range.
- Prevent past dates and return-before-departure.
- When changing departure makes return invalid, clear return and explain why.
- Back returns without committing partial changes; Continue commits the draft.

### 6.4 Travellers and cabin

- Compact traveller counters in one card.
- Preserve airline-valid constraints: at least one adult, infant count not above adult count, and total traveller maximum.
- Cabin is a four-row radio group.
- Sticky Apply button.
- Do not start a search automatically from this screen unless it was opened as the final required step from a recent search; default behavior is return to Home so the user can verify the complete query.

### 6.5 Results header and controls

- Compact header: route, dates, traveller/class summary, edit action.
- One horizontal row for high-value filters, horizontally scrollable if necessary.
- Show live progress while providers respond: `8 of 11 providers checked` rather than an indefinite generic loader when counts are available.
- Distinguish partial results from completed results.
- Show result count as canonical itinerary count and provider count separately.
- Sort control stays visible but visually secondary.
- Preserve list position and selected filters on return from details.

### 6.6 Redesigned search-result card

Each card represents one canonical itinerary, not one provider.

Card order:

1. Optional `Best price` or meaningful status label.
2. Airline logo/name and flight number(s).
3. Outbound timeline; return timeline directly below for round trips.
4. Duration and stop count centered on the route rail.
5. High-value chips only: baggage, limited seats, self-transfer, or change risk when genuinely known.
6. Provider summary: `3 prices from 3 providers`.
7. Cheapest valid total and provider name.
8. `View details` action.

Density rules:

- Aim for roughly 190–240 dp for round-trip cards and less for one-way cards.
- Hide absent metadata instead of reserving blank rows.
- No duplicate provider cards for the same itinerary.
- No false `Best price`: calculate against all currently received canonical groups and update as streaming results arrive.
- When results are still partial, label the best-price state as provisional or wait until provider completion before applying it.

### 6.7 Canonical flight grouping and provider comparison

Grouping must be deterministic and conservative.

Create a canonical itinerary key from normalized, ordered data:

- trip type;
- departure date for each leg;
- every segment in order;
- origin and destination airport codes;
- scheduled local departure and arrival timestamps, including date rollover;
- marketing carrier and flight number when present;
- operating carrier when present;
- stop sequence;
- return segment sequence for round trips.

Do not merge when:

- segment count differs;
- an airport differs;
- timestamps differ outside a documented tolerance;
- flight numbers conflict;
- one itinerary contains an airport change or self-transfer not present in the other;
- baggage/fare data indicates genuinely different fare products that cannot be represented as provider quote attributes.

Implementation shape:

- Add a pure `canonicalItineraryKey(flight)` function in `core`.
- Add unit fixtures for direct, connection, codeshare, overnight, missing-flight-number, and round-trip cases.
- Produce `FlightGroup(itinerary, providerQuotes)` for UI consumption.
- Keep raw provider responses available for diagnostics.
- A `ProviderQuote` contains provider, total price, deep link, availability, baggage/fare annotations, freshness, and provider error state where applicable.
- Sort groups by cheapest valid quote; sort providers inside a group by valid price, then deterministic provider name.
- Never convert a provider timeout/failure into sold out.

If the current API does not return enough stable segment/provider detail, extend the server contract before enabling cross-provider grouping in production.

### 6.8 Provider comparison screen

- Summary card shows the exact itinerary once.
- `Book this flight` section lists provider rows with logo/name, total price, relevant differentiators, and chevron.
- Cheapest row receives a restrained `Cheapest` label.
- State that Qasda redirects to the provider and prices may update there.
- Selecting a row changes the sticky booking action; it does not immediately open an external site.
- External navigation occurs only from the explicit bottom button.
- Provider failure/unavailable rows appear separately or are omitted with a transparent availability note; never show a fabricated zero price.

### 6.9 Offer details

Use a compact, tabbed information model:

- Header summary: airline, selected provider, total price, trip type, traveller count.
- Status chips: direct/stops, baggage, limited seats only when verified.
- Tabs: Itinerary, Baggage, Fare details, About/booking information.
- Itinerary uses a vertical timeline, grouped by outbound and return.
- Each segment shows local date/time, airport, airline/flight number, duration, layover, terminal when available, and operating carrier when different.
- Baggage tab separates cabin and checked allowance per traveller/segment when the data supports it.
- Fare tab shows change/refund rules only when supplied; otherwise state that the provider will confirm them.
- Sticky bottom action always includes provider and total price.
- Preserve a provider switch path without requiring Back to results.
- Add Track route below booking actions where appropriate, but tracking must describe whether it watches seats or price.

### 6.10 Tracking as a serious feature

Tracking becomes a value dashboard rather than a list of saved routes.

Top-level structure:

- Title and create action.
- Segmented view: Price alerts / Seat alerts, plus an optional All state if testing shows it is needed.
- Active alerts first; ended alerts available through a secondary/history filter.

Every tracking card shows:

- route and dates;
- watch type;
- specific flight identity when the watch is flight-specific, otherwise `Any matching flight`;
- current state: unavailable, checking, available, price decreased, unchanged, expired, paused, or error;
- current/last observed price where relevant;
- reference price or target threshold;
- last successful check time;
- next check estimate when the scheduler can provide it;
- 48-hour progress/expiry text;
- primary action (`Notify me`, `Notify below…`, `See offers`, or `Restart`);
- overflow actions for pause/stop/delete where appropriate.

Creation flow:

- From no-results: default to seat alert and clearly say it watches for any real bookable offer on that route/date.
- From an available result: default to price alert using the observed price, with optional target editing.
- Require Google sign-in before final creation, but preserve the pending watch draft through authentication.
- Request notification permission in context immediately before enabling the first alert, not at arbitrary app launch.
- Confirm creation with watch duration, route, date, and notification behavior.

Lifecycle and truth rules:

- Active duration is 48 hours unless product configuration changes.
- Expiry generates the `tracking ended` notification when enabled.
- Provider failures do not mean sold out or unavailable.
- Seat alert fires only when at least one real, bookable provider quote exists.
- Price alert fires only according to the configured threshold/baseline rule.
- Store and display last successful check separately from last attempted check.
- De-duplicate notification events and expose delivery state in diagnostics/admin tooling.
- Tapping a notification deep-links to the watch/result state that caused it.

Watch detail/history:

- Status timeline of checks and notifications.
- Price history visualization when enough data exists.
- Current matching offers.
- Restart/extend action after expiry.
- Notification preference and stop action.

### 6.11 Account, sign-in, settings, help

- Keep Account as the bottom destination.
- Signed-out state starts with a polished identity card and explicit `Continue with Google` action.
- Explain benefits: cross-device searches, tracking ownership, and notification recovery.
- Signed-in state shows avatar, name/email, alert/search counts, and sign-out in overflow or settings.
- Keep settings grouped: Notifications, Display, Search preferences, Privacy/Data, About.
- Android notification settings remains the source of truth for sound/vibration/channel behavior.
- Help cards should be denser and searchable if content grows; visual redesign is lower priority than core comparison screens.

## 7. State matrix

Every redesigned screen must be reviewed in these states where applicable:

- Initial/empty.
- Loading.
- Streaming partial data.
- Success.
- Empty/no flights.
- Provider partial failure.
- Total network failure.
- Offline with cached content.
- Authentication required.
- Notification permission denied.
- Expired tracking.
- RTL Arabic.
- Dark mode.
- Large font.

## 8. Architecture and code changes

### Core module

- Add canonical grouping and provider-quote models.
- Keep grouping and sorting pure and unit-testable.
- Extend tracking models with last successful check, last attempt, next check, state, and lifecycle metadata if the API exposes them.
- Add localized string keys for all new states and actions.

### App module

- Refactor `Theme.kt` into documented semantic tokens without breaking existing dark mode.
- Split large screen files into screen/state/component layers where useful.
- Replace bespoke cards/controls incrementally with shared Qasda components.
- Keep ViewModels responsible for screen state and events; Composables remain rendering-focused.
- Save search draft, filters, sort, and selected provider with `SavedStateHandle` or durable settings as appropriate.
- Add Compose previews for representative English, French, Arabic/RTL, dark, and large-font states.

### Backend/API additions, only where needed

- Stable provider quote identity and freshness.
- Check-progress metadata for streaming results.
- Tracking state, check timestamps, expiry, notification event, and price history.
- Idempotent create/restart/stop operations.
- Preserve authentication and authorization for all account-owned watches.

Version API changes rather than silently changing existing response meaning.

## 9. Analytics and operational visibility

Use privacy-conscious product events without recording passenger identity or sensitive search contents unnecessarily:

- search started/completed/empty/failed;
- result group opened;
- provider selected;
- outbound booking handoff;
- watch creation started/completed/failed;
- notification permission accepted/denied;
- notification opened;
- watch restarted/stopped/expired.

Operational dashboards should track provider success rate, median search latency, grouping anomalies, active watches, successful checks, notification send failures, and notification opens.

## 10. Testing strategy

### Automated

- Unit tests for canonical grouping and non-grouping edge cases.
- Unit tests for price/provider sorting, provisional best-price logic, and missing values.
- Tracking lifecycle and notification de-duplication tests.
- ViewModel tests for state restoration and authentication continuation.
- Compose UI tests for primary flows and accessibility semantics.
- Screenshot/golden tests for key screens in three languages, RTL, light/dark, and font scaling.
- API contract tests for provider quote and tracking payloads.

### Manual device matrix

- Small phone, reference phone, and large phone.
- Android 8 minimum, a mid-range supported version, and current target version.
- Real device notification tests in foreground, background, force-stopped, battery saver, and after reboot where Android permits delivery.
- Slow network, offline transition, partial provider failure, and expired authentication.
- External provider handoff and safe return to Qasda.

## 11. Delivery phases

### Phase 0 — Baseline and specification

- Freeze current screenshots and performance measurements.
- Finalize tokens and canonical grouping rules.
- Confirm API fields needed for grouping and tracking state.
- Create component/state inventory and translation inventory.

Exit: signed-off visual tokens, data contract, and test fixtures.

### Phase 1 — Foundations and compact search

- Shared components and tokens.
- Home hierarchy.
- Airport, dates, and travellers screens.
- Remove repeated trip-type selection.
- State restoration and accessibility baseline.

Exit: complete search draft flow matches the new hierarchy in all languages.

### Phase 2 — Grouped results and provider comparison

- Canonical grouping in core.
- Compact result header/filter controls.
- Redesigned result cards.
- Provider comparison screen.
- Streaming/provisional result handling.

Exit: grouping fixtures pass and no known different itineraries merge.

### Phase 3 — Optimized offer details

- Tabbed detail screen and vertical itinerary.
- Provider switching and sticky booking action.
- Baggage/fare truth states.
- Track-route entry points.

Exit: one-way, round-trip, connection, codeshare, and missing-data cases verified.

### Phase 4 — Tracking product upgrade

- Price/seat tracking dashboard.
- Creation, authentication continuation, contextual permission request.
- Lifecycle metadata, expiry, restart, history, and deep links.
- Notification and delivery diagnostics.

Exit: a real watch can be created, checked, notified, opened, expired, and restarted end-to-end.

### Phase 5 — Polish and release

- Dark/RTL/large-font QA.
- Animation and haptic restraint.
- Performance profiling and list scroll verification.
- Full regression, production monitoring, staged rollout, and rollback plan.

Exit: release candidate meets quality gates and production telemetry is ready.

## 12. Recommended implementation order inside each phase

1. Models and state contract.
2. Pure business logic and unit tests.
3. Reusable components.
4. Screen implementation.
5. Loading/empty/error/accessibility states.
6. Localization and RTL.
7. Screenshot and integration tests.
8. Emulator and real-device validation.

## 13. Definition of done

A phase is done only when:

- production code, tests, and localized strings are complete;
- all relevant state-matrix cases are reviewed;
- no price or availability claim is inferred from missing data;
- RTL, dark mode, and large text have been checked;
- navigation and state restoration work after process recreation;
- screenshots are captured and compared with the approved direction;
- backend changes are deployed compatibly before dependent app behavior ships;
- monitoring can distinguish app bugs, provider failures, and notification-delivery failures.

## 14. Product decisions to retain during implementation

- The visual reference is direction, not permission to invent unavailable data.
- Comparison is itinerary-first and provider-second.
- Tracking watches a durable route/date or explicit flight identity—not a temporary provider result ID.
- The app remains useful before sign-in; authentication is required only when ownership/synchronization demands it.
- Notification settings belong both in Qasda for category preference and in Android for channel sound/vibration behavior.
- Compactness must not reduce touch targets, readability, translation resilience, or truthfulness.
