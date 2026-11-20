package com.example.data.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object OfflineModeManager {
    /**
     * Enables unlimited persistent disk caching on Firestore so all customers, bills,
     * payments, expenses, and chat records remain fully readable & writable offline.
     */
    fun configurePersistentCache(db: FirebaseFirestore) {
        runCatching {
            val cacheSettings = PersistentCacheSettings.newBuilder()
                .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()
            db.firestoreSettings = settings
        }
    }

    fun isCurrentlyOnline(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun observeNetworkOnline(context: Context): Flow<Boolean> = callbackFlow {
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(true)
            close()
            return@callbackFlow
        }

        trySend(isCurrentlyOnline(appContext))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isCurrentlyOnline(appContext))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet)
            }
        }

        runCatching {
            cm.registerDefaultNetworkCallback(callback)
        }.onFailure {
            trySend(true)
        }

        awaitClose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged()
}
