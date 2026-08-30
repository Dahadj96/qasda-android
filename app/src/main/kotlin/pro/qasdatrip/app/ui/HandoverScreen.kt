package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Airlines
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.SearchQuery
import pro.qasdatrip.core.Sites
import pro.qasdatrip.core.routeArrow

/**
 * The moment the app hands somebody over to a booking site.
 *
 * This screen did not exist. Tapping "Réserver" swapped the app for a browser
 * mid-tap, which is the single most disorienting thing the product did: you
 * are looking at Qasda, then you are looking at a site you have never heard
 * of asking for a card number, with nothing in between to explain the jump.
 * People close that. Worse, some of them assume they bought a ticket from us,
 * which is the one belief this product cannot afford anybody holding.
 *
 * So there is a page in between, and it does three jobs.
 *
 * It says where you are going and draws it, so the site's name is read once
 * before it appears in a browser bar. It says plainly that we compare prices
 * and do not sell tickets — the sentence that matters if a booking later goes
 * wrong and somebody is deciding who to call. And it restates the offer being
 * carried across, with a warning that the number came from our last check
 * rather than from the seller's till, so nobody arrives believing a price
 * they were shown here is a price they are owed.
 *
 * Nothing here is a dark pattern in reverse: "Ouvrir" is the obvious, primary,
 * full-width action, and getting back is one tap. The screen costs a second
 * and buys the difference between a redirect and a handover.
 */
@Composable
fun HandoverScreen(
    flight: Flight,
    site: String,
    query: SearchQuery?,
    opening: Boolean,
    onOpen: () -> Unit,
    onBack: () -> Unit,
) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val leg = flight.outboundOrSelf
    val airline = leg?.operatingAirline ?: flight.airline
    val price = flight.prices[site]?.takeIf { it > 0 } ?: flight.cheapest?.second

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        QasdaAppBar(title = words.handoverTitle, onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(
                start = Space.s4, end = Space.s4, bottom = Space.s4,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            item { Crossing(Sites.name(site)) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                    Text(
                        words.continueOn.replace("{site}", Sites.name(site)),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Ink.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        words.notSeller,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.md))
                        .background(Ink.surface)
                        .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
                        .padding(Space.s4),
                    verticalArrangement = Arrangement.spacedBy(Space.s3),
                ) {
                    Fact(
                        words.labelFlight,
                        listOfNotNull(
                            Airlines.name(airline).takeIf { it.isNotBlank() },
                            leg?.flightNo?.takeIf { it.isNotBlank() },
                        ).joinToString(" "),
                    )
                    query?.let { q ->
                        Fact(
                            words.labelRoute,
                            "${cityName(q.from, lang)} (${q.from}) ${routeArrow(lang)} " +
                                "${cityName(q.to, lang)} (${q.to})",
                        )
                        Fact(
                            words.labelDate,
                            listOfNotNull(
                                formatDateLong(q.departDate, lang),
                                q.returnDate?.let { formatDate(it, lang) },
                            ).joinToString(" · "),
                        )
                        Fact(
                            words.labelTravellers,
                            "${travellersLabel(q.travellers, words)} · ${cabinLabel(q.cabin, words)}",
                        )
                    }
                    price?.let { Fact(words.labelShownPrice, Money.format(it, lang), accent = true) }
                }
            }

            // The price came from our last look at that site, not from their
            // till. Saying so here is the difference between a comparison and
            // a quote, and only one of those we are able to make.
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.md))
                        .background(Ink.noticeSoft)
                        .padding(Space.s3),
                    horizontalArrangement = Arrangement.spacedBy(Space.s2),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_warning),
                        contentDescription = null,
                        tint = Ink.notice,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        words.priceMayChange,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink.notice,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Space.s4, end = Space.s4, top = Space.s2, bottom = Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onOpen,
                enabled = !opening,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.ink,
                    contentColor = Ink.inverse,
                    disabledContainerColor = Ink.surfaceSoft,
                    disabledContentColor = Ink.muted,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        words.openSite.replace("{site}", Sites.name(site)),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_external),
                        contentDescription = null,
                        tint = if (opening) Ink.muted else Ink.inverse,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                words.backToResults,
                style = MaterialTheme.typography.labelLarge,
                color = Ink.inkSoft,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.sm))
                    .clickable(onClick = onBack)
                    .padding(horizontal = Space.s3, vertical = Space.s2),
            )
        }
    }
}

/** Us, a dotted flight path, and them. */
@Composable
private fun Crossing(siteName: String) {
    val words = LocalWords.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Space.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        Badge(words.appName, dark = true, modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier.size(4.dp).clip(CircleShape).background(Ink.lineStrong),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_plane_right),
                contentDescription = null,
                tint = Ink.accentDeep,
                modifier = Modifier.size(18.dp),
            )
            repeat(3) {
                Box(
                    modifier = Modifier.size(4.dp).clip(CircleShape).background(Ink.lineStrong),
                )
            }
        }
        Badge(siteName, dark = false, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Badge(text: String, dark: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(if (dark) Ink.ink else Ink.surface)
            .border(
                1.dp,
                if (dark) Ink.ink else Ink.lineStrong,
                RoundedCornerShape(Radius.md),
            )
            .padding(horizontal = Space.s2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (dark) Ink.inverse else Ink.ink,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Fact(label: String, value: String, accent: Boolean = false) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink.muted,
        )
        Text(
            Money.isolate(value),
            style = if (accent) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (accent) Ink.accentDeep else Ink.ink,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
