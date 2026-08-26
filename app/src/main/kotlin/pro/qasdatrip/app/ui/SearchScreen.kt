package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Airport
import pro.qasdatrip.core.Airports
import pro.qasdatrip.core.Cabin
import pro.qasdatrip.core.SearchQuery

/**
 * The search, in the order somebody fills it in: where from, where to, when,
 * and how many. Dates are typed as YYYY-MM-DD for now — the calendar is the
 * next screen to build, and a wrong date is worse than a plain field.
 */
@Composable
fun SearchScreen(onSearch: (SearchQuery) -> Unit) {
    val words = LocalWords.current
    val lang = LocalLang.current

    var from by remember { mutableStateOf("ALG") }
    var to by remember { mutableStateOf("CDG") }
    var depart by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var adults by remember { mutableStateOf(1) }
    var picking by remember { mutableStateOf<String?>(null) }

    if (picking != null) {
        AirportPicker(
            originSide = picking == "from",
            onPick = { airport ->
                if (picking == "from") from = airport.iata else to = airport.iata
                picking = null
            },
            onDismiss = { picking = null },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.canvas)
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        Text(words.heroTitle, style = MaterialTheme.typography.displaySmall)
        Text(words.heroSub, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)

        Field(label = words.from, value = "${cityName(from, lang)} ($from)") { picking = "from" }
        Field(label = words.to, value = "${cityName(to, lang)} ($to)") { picking = "to" }

        OutlinedTextField(
            value = depart,
            onValueChange = { depart = it },
            label = { Text(words.dates) },
            placeholder = { Text("2026-09-20") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = back,
            onValueChange = { back = it },
            label = { Text(words.inbound) },
            placeholder = { Text("2026-09-27") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
            Text(words.travellers, style = MaterialTheme.typography.bodyLarge)
            Stepper(value = adults, onChange = { adults = it.coerceIn(1, 9) })
        }

        Button(
            onClick = {
                onSearch(
                    SearchQuery(
                        from = from, to = to,
                        departDate = depart.trim(),
                        returnDate = back.trim().ifBlank { null },
                        adults = adults,
                        cabin = Cabin.ECONOMY,
                    ),
                )
            },
            enabled = depart.isNotBlank() && from != to,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radius.sm),
            colors = ButtonDefaults.buttonColors(containerColor = Ink.ink, contentColor = Ink.inverse),
        ) { Text(words.search, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun Field(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick)
            .padding(Space.s4),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Stepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
        Button(
            onClick = { onChange(value - 1) },
            shape = RoundedCornerShape(Radius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = Ink.surfaceSoft, contentColor = Ink.ink),
        ) { Text("−") }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = { onChange(value + 1) },
            shape = RoundedCornerShape(Radius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = Ink.surfaceSoft, contentColor = Ink.ink),
        ) { Text("+") }
    }
}

/**
 * The picker, with the matching from the shared module: أدرار and ادرار are
 * the same query, so are Séville and seville, and an airport answers to the
 * commune the databases file it under as well as to its own name.
 */
@Composable
fun AirportPicker(originSide: Boolean, onPick: (Airport) -> Unit, onDismiss: () -> Unit) {
    val words = LocalWords.current
    val lang = LocalLang.current
    var query by remember { mutableStateOf("") }
    val results = remember(query) {
        if (query.isBlank()) Airports.suggestions(originSide) else Airports.search(query)
    }

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas).padding(Space.s4)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(words.searchCity) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (results.isEmpty()) {
            Text(
                words.noAirport,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.muted,
                modifier = Modifier.padding(Space.s4),
            )
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(results, key = { it.iata }) { airport ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(airport) }
                        .padding(vertical = Space.s3),
                    horizontalArrangement = Arrangement.spacedBy(Space.s3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        airport.iata,
                        style = MaterialTheme.typography.labelSmall,
                        color = Ink.inkSoft,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(Ink.surfaceSoft)
                            .padding(horizontal = Space.s2, vertical = Space.s1),
                    )
                    Column {
                        Text(airport.cityIn(lang), style = MaterialTheme.typography.titleMedium)
                        if (airport.name.isNotEmpty()) {
                            Text(airport.name, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
                        }
                    }
                }
            }
        }
    }
}
