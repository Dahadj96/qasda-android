package pro.qasdatrip.app.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import pro.qasdatrip.app.BuildConfig

class GoogleAccount(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("qasda-account", Context.MODE_PRIVATE)
    private val diagnostics = context.getSharedPreferences("qasda-diagnostics", Context.MODE_PRIVATE)
    private val current = MutableStateFlow(auth.currentUser)
    val user = current.asStateFlow()
    // Publish a newly signed-in user only after server profile/link validation.
    init { auth.addAuthStateListener { if (it.currentUser == null) current.value = null } }
    val serverUserId: String? get() = if (prefs.getString("uid", null) == auth.currentUser?.uid) prefs.getString("server_id", null) else null
    fun markReady(serverId: String) {
        prefs.edit().putString("uid", auth.currentUser?.uid).putString("server_id", serverId).apply()
        current.value = auth.currentUser
    }
    val signedIn: Boolean get() = auth.currentUser != null
    suspend fun token(): String? = auth.currentUser?.getIdToken(false)?.await()?.token
    private suspend fun credential(context: Context): AuthCredential {
        check(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) { "Google sign-in is not configured" }
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val result = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
        return GoogleAuthProvider.getCredential(credential.idToken, null)
    }
    suspend fun signIn(context: Context): FirebaseUser = requireNotNull(auth.signInWithCredential(credential(context)).await().user)
    suspend fun reauthenticate(context: Context) {
        requireNotNull(auth.currentUser).reauthenticate(credential(context)).await()
        requireNotNull(auth.currentUser).getIdToken(true).await()
    }
    fun signOut() { prefs.edit().clear().apply(); diagnostics.edit().clear().apply(); auth.signOut() }
}
