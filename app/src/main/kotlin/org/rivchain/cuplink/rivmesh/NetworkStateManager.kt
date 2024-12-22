import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.lang.ref.WeakReference

object NetworkStateManager {
    private var callback: NetworkStateCallback? = null

    fun register(context: Context) {
        if (callback == null) {
            callback = NetworkStateCallback(context.applicationContext)
            callback?.register()
        }
    }

    fun unregister() {
        callback?.unregister()
        callback = null
    }
}

class NetworkStateCallback(context: Context) : ConnectivityManager.NetworkCallback() {
    private val contextRef = WeakReference(context.applicationContext)

    fun register() {
        val context = contextRef.get() ?: return
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        manager.registerNetworkCallback(request, this)
    }

    fun unregister() {
        val context = contextRef.get() ?: return
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            manager.unregisterNetworkCallback(this)
        } catch (e: IllegalArgumentException) {
            // Callback was not registered or already unregistered
        }
    }
}
