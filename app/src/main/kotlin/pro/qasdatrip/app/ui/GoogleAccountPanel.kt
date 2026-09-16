package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import pro.qasdatrip.app.data.GoogleAccount
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.core.DeviceKey
import pro.qasdatrip.core.Cabin
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.QasdaApi
import pro.qasdatrip.core.SearchQuery
import pro.qasdatrip.core.Watch

@Composable
fun GoogleAccountPanel(account: GoogleAccount, api: QasdaApi, lang: Lang,
    device: () -> DeviceKey?, recent: List<SearchQuery> = emptyList(),
    history: List<Watch> = emptyList(), onRestart: (Long) -> Unit = {}, onChanged: () -> Unit = {},
    onSearch: (SearchQuery) -> Unit = {}, signInOnly: Boolean = false,
    historyHasMore: Boolean = false, onMoreHistory: () -> Unit = {}) {
    val context = LocalContext.current
    val user by account.user.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var phone by remember(user?.uid) { mutableStateOf("") }
    var consent by remember(user?.uid) { mutableStateOf(false) }
    var searches by remember(user?.uid) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var deleting by remember { mutableStateOf(false) }
    fun copy(fr: String, en: String, ar: String) = when (lang) { Lang.AR -> ar; Lang.EN -> en; else -> fr }
    suspend fun refresh() {
        val profile = api.accountGet("/me") ?: kotlin.error("Account service unavailable")
        phone = profile["phone"]?.jsonPrimitive?.content?.takeUnless { it == "null" }.orEmpty()
        consent = (profile["emailMarketing"] as? JsonObject)?.get("granted")?.jsonPrimitive?.content == "true"
        searches = (api.accountGet("/saved-searches")?.get("items") as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()
    }
    LaunchedEffect(user?.uid) { if (user != null && !signInOnly) runCatching { refresh() }.onFailure { error = it.message } }
    Column(Modifier.fillMaxWidth().background(Ink.surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(copy("Compte Google", "Google account", "حساب Google"), style = MaterialTheme.typography.titleLarge)
        if (user == null) {
            Text(copy("Connectez-vous pour activer le suivi et retrouver vos recherches sur vos appareils.", "Sign in to track flights and restore your searches across devices.", "سجّل الدخول لتتبع الرحلات واستعادة البحث عبر أجهزتك."))
            Button(enabled = !busy, onClick = { scope.launch {
                busy = true; error = null
                try { account.signIn(context); val profile = api.accountGet("/me") ?: kotlin.error("Account service unavailable")
                    val key = device() ?: kotlin.error("Device registration is not ready")
                    check(api.linkAccountDevice(key)) { "Unable to link this installation" }; account.markReady(profile["id"]!!.jsonPrimitive.content); onChanged()
                } catch (failure: Exception) { account.signOut(); error = failure.message }
                finally { busy = false }
            } }) { Text(copy("Continuer avec Google", "Continue with Google", "المتابعة باستخدام Google")) }
        } else if (!signInOnly) {
            Text(user?.displayName.orEmpty()); Text(user?.email.orEmpty(), color = Ink.muted)
            OutlinedTextField(value = phone, onValueChange = { phone = it.take(30) }, label = { Text(copy("Téléphone facultatif · non vérifié", "Optional phone · unverified", "الهاتف اختياري · غير متحقق منه")) }, modifier = Modifier.fillMaxWidth())
            Row { Checkbox(checked = consent, onCheckedChange = { consent = it }); Text(copy("J’accepte les actualités et offres par email (facultatif).", "I opt into news and offers by email (optional).", "أوافق على الأخبار والعروض بالبريد الإلكتروني (اختياري).")) }
            Text(copy("Indépendant des alertes de vols. Retirez votre accord à tout moment.", "Separate from flight alerts. Withdraw consent at any time.", "مستقل عن تنبيهات الرحلات. يمكن سحب الموافقة في أي وقت."), color = Ink.muted)
            Button(enabled = !busy, onClick = { scope.launch { busy = true; error = null
                try { check(api.accountPatch("/profile", buildJsonObject { put("locale", lang.tag); if (phone.isNotBlank()) put("phone", phone) })) { "Unable to save profile" }
                    check(api.accountPost("/consent", buildJsonObject { put("granted", consent); put("source", "android") }) != null) { "Unable to save consent" }
                } catch (failure: Exception) { error = failure.message } finally { busy = false }
            } }) { Text(copy("Enregistrer", "Save", "حفظ")) }
            Text(copy("Recherches enregistrées", "Saved searches", "البحث المحفوظ"), style = MaterialTheme.typography.titleMedium)
            recent.firstOrNull()?.let { query -> TextButton(enabled = !busy, onClick = { scope.launch {
                if (api.accountPost("/saved-searches", buildJsonObject { put("name", "${query.from} → ${query.to}"); put("search", api.savedSearchBody(query)) }) == null) error = "Unable to save search" else refresh()
            } }) { Text(copy("Enregistrer la dernière recherche", "Save latest search", "حفظ آخر بحث")) } }
            for (item in searches) { val saved = item["search"] as? JsonObject ?: continue
                val label = "${saved["origin"]?.jsonPrimitive?.content} → ${saved["destination"]?.jsonPrimitive?.content} · ${saved["departDate"]?.jsonPrimitive?.content}"
                Text(label)
                var name by remember(item["id"]?.jsonPrimitive?.content) { mutableStateOf(item["name"]?.jsonPrimitive?.content.orEmpty()) }
                OutlinedTextField(value = name, onValueChange = { name = it.take(100) }, label = { Text(copy("Nom", "Name", "الاسم")) })
                TextButton(onClick = { scope.launch { if (api.accountPatch("/saved-searches/${item["id"]?.jsonPrimitive?.content}", buildJsonObject { put("name", name) })) refresh() else error = "Unable to rename search" } }) { Text(copy("Renommer", "Rename", "إعادة تسمية")) }
                TextButton(onClick = {
                    val q = SearchQuery(from = saved["origin"]!!.jsonPrimitive.content, to = saved["destination"]!!.jsonPrimitive.content,
                        departDate = saved["departDate"]!!.jsonPrimitive.content, returnDate = saved["returnDate"]?.jsonPrimitive?.content?.takeUnless { it == "null" },
                        adults = saved["adults"]!!.jsonPrimitive.content.toInt(), children = saved["children"]!!.jsonPrimitive.content.toInt(), infants = saved["infants"]!!.jsonPrimitive.content.toInt(),
                        cabin = Cabin.entries.firstOrNull { it.wire == saved["cabinClass"]?.jsonPrimitive?.content } ?: Cabin.ECONOMY)
                    onSearch(q)
                }) { Text(copy("Rechercher", "Search", "بحث")) }
                TextButton(onClick = { scope.launch { if (api.accountDelete("/saved-searches/${item["id"]?.jsonPrimitive?.content}")) refresh() else error = "Unable to delete search" } }) { Text(copy("Supprimer", "Delete", "حذف")) }
            }
            if (history.isNotEmpty()) Text(copy("Historique des suivis", "Tracking history", "سجل التتبع"), style = MaterialTheme.typography.titleMedium)
            for (watch in history) {
                Text("${watch.origin} → ${watch.destination} · ${watch.departDate} · ${watch.trackerState ?: "inactive"}")
                Row { TextButton(onClick = { onSearch(watch.asQuery()) }) { Text(copy("Rechercher", "Search", "بحث")) }
                    TextButton(onClick = { onRestart(watch.id) }) { Text(copy("Relancer", "Restart", "إعادة التتبع")) } }
            }
            if (historyHasMore) TextButton(onClick = onMoreHistory) { Text(copy("Afficher plus", "Load more", "عرض المزيد")) }
            TextButton(enabled = !busy, onClick = { scope.launch { busy = true; error = null
                try { val key = device(); if (key != null) check(api.unlinkAccountDevice(key)) { "Unable to detach phone; try again online" }
                    account.signOut(); onChanged()
                } catch (failure: Exception) { error = failure.message } finally { busy = false }
            } }) { Text(copy("Déconnexion", "Sign out", "تسجيل الخروج")) }
            TextButton(enabled = !busy, onClick = { deleting = true }) { Text(copy("Supprimer mon compte", "Delete my account", "حذف حسابي"), color = Ink.alert) }
        }
        error?.let { Text(it, color = Ink.alert) }
    }
    if (deleting) AlertDialog(onDismissRequest = { if (!busy) deleting = false },
        title = { Text(copy("Supprimer définitivement le compte ?", "Permanently delete account?", "حذف الحساب نهائيًا؟")) },
        text = { Text(copy("Les suivis seront arrêtés et vos données supprimées. Vous devrez confirmer votre identité Google.", "Trackers will stop and your account data will be removed. Confirm your Google identity to continue.", "سيتوقف التتبع وتُحذف بيانات الحساب. أكّد هويتك باستخدام Google للمتابعة.")) },
        confirmButton = { TextButton(enabled = !busy, onClick = { scope.launch { busy = true; error = null
            try { account.reauthenticate(context); check(api.accountPost("/delete", buildJsonObject { put("confirm", "DELETE") }) != null) { "Unable to request deletion" }; account.signOut(); deleting = false; onChanged() }
            catch (failure: Exception) { error = failure.message; deleting = false } finally { busy = false }
        } }) { Text(copy("Supprimer", "Delete", "حذف"), color = Ink.alert) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { deleting = false }) { Text(copy("Annuler", "Cancel", "إلغاء")) } })
}
