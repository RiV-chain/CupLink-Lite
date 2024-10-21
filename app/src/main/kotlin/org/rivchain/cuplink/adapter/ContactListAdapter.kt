package org.rivchain.cuplink.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.nikhiljain.blockiesgenerator.BlockiesIconGenerator
import org.json.JSONObject
import org.libsodium.jni.Sodium
import org.rivchain.cuplink.BaseActivity
import org.rivchain.cuplink.Crypto
import org.rivchain.cuplink.DatabaseCache
import org.rivchain.cuplink.R
import org.rivchain.cuplink.call.PacketReader
import org.rivchain.cuplink.call.PacketWriter
import org.rivchain.cuplink.model.Contact
import org.rivchain.cuplink.util.Log
import org.rivchain.cuplink.util.NetworkUtils
import org.rivchain.cuplink.util.Utils
import java.lang.Integer.max
import java.lang.Integer.min
import java.net.ConnectException
import java.net.Socket

internal class ContactListAdapter(
    context: Context,
    resource: Int,
    private val contacts: MutableList<Contact>  // Changed to MutableList
) : ArrayAdapter<Contact?>(
    context, resource, contacts as List<Contact?>
) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false)
        val contact = contacts[position]
        val userIcon = itemView.findViewById<ImageView>(R.id.contact_state)
        val statusCircle = itemView.findViewById<ImageView>(R.id.status_circle)
        itemView.findViewById<TextView>(R.id.contact_name).text = contact.name

        // Generate the blockies icon from the user's public key
        val blockiesBitmap = generateBlockies(contact.publicKey)

        // Create a circular bitmap from the blockies image
        val circularBitmap = getRoundedCroppedBitmap(blockiesBitmap)
        userIcon.setImageBitmap(circularBitmap)

        val p = Paint()
        p.color = when (contact.state) {
            Contact.State.CONTACT_ONLINE -> Color.parseColor("#00ff0a") // green
            Contact.State.CONTACT_OFFLINE -> Color.parseColor("#808080") // grey
            Contact.State.NETWORK_UNREACHABLE -> Color.parseColor("#f25400") // light orange
            Contact.State.APP_NOT_RUNNING -> Color.parseColor("#ff7000") // orange
            Contact.State.AUTHENTICATION_FAILED -> Color.parseColor("#612c00") // brown
            Contact.State.COMMUNICATION_FAILED -> Color.parseColor("#808080") // grey
            Contact.State.PENDING -> Color.parseColor("#00000000") // transparent
        }
        statusCircle.setColorFilter(p.color)


        return itemView
    }

    private fun generateBlockies(publicKey: ByteArray): Bitmap {
        // Prepare a buffer to hold the sha256 hash output
        val sha256 = ByteArray(Sodium.crypto_hash_sha256_bytes())
        // Compute sha256 hash
        Sodium.crypto_hash_sha256(sha256, publicKey, publicKey.size)

        val hash = Utils.byteArrayToHexString(sha256)

        val iconGenerator = BlockiesIconGenerator(
            seed = hash,
            size = 10,
            scale = 10,
        )

        return iconGenerator.generateIconBitmap()
    }

    private fun getRoundedCroppedBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val color = 0xff424242.toInt()
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }


    fun updateContact(contact: Contact) {
        val index = contacts.indexOfFirst { it.publicKey.contentEquals(contact.publicKey) }
        if (index >= 0) {
            contacts[index] = contact
            notifyDataSetChanged()
        }
    }

    fun startPingingContacts() {
        val pinger = Pinger(context as BaseActivity, this, contacts)
        val thread = Thread(pinger)
        thread.start()
    }

    /*
     * Checks if a contact is online.
    */
    class Pinger(val context: BaseActivity, val adapter: ContactListAdapter, val contacts: MutableList<Contact>) : Runnable {
        private fun pingContact(contact: Contact) : Contact.State {
            Log.d(this, "pingContact() contact: ${contact.name}")

            val otherPublicKey = ByteArray(Sodium.crypto_sign_publickeybytes())
            val settings = DatabaseCache.database.settings
            val useNeighborTable = settings.useNeighborTable
            val connectTimeout = settings.connectTimeout
            val ownPublicKey = settings.publicKey
            val ownSecretKey = settings.secretKey
            val connectRetries = settings.connectRetries
            var connectedSocket: Socket? = null
            var networkNotReachable = false
            var appNotRunning = false

            try {
                val allGeneratedAddresses = NetworkUtils.getAllSocketAddresses(contact, useNeighborTable)
                Log.d(this, "pingContact() connectTimeout: ${connectTimeout}, contact.addresses: ${contact.addresses}, allGeneratedAddresses: $allGeneratedAddresses")

                // try to connect
                for (iteration in 0..max(0, min(connectRetries, 4))) {
                    Log.d(this, "pingContact() loop number $iteration")
                    for (address in allGeneratedAddresses) {
                        val socket = Socket()
                        try {
                            socket.connect(address, connectTimeout)
                            connectedSocket = socket
                            break
                        } catch (e: ConnectException) {
                            Log.d(this, "pingContact() $e, ${e.message}")
                            if (" ENETUNREACH " in e.toString()) {
                                networkNotReachable = true
                            } else {
                                appNotRunning = true
                            }
                        } catch (e: Exception) {
                            // ignore
                            Log.d(this, "pingContact() $e, ${e.message}")
                        }

                        closeSocket(socket)
                    }

                    // TCP/IP connection successfully
                    if (connectedSocket != null) {
                        break
                    }
                }

                if (connectedSocket == null) {
                    return if (appNotRunning) {
                        Contact.State.APP_NOT_RUNNING
                    } else if (networkNotReachable) {
                        Contact.State.NETWORK_UNREACHABLE
                    } else {
                        Contact.State.CONTACT_OFFLINE
                    }
                }

                connectedSocket.soTimeout = 12000

                val pw = PacketWriter(connectedSocket)
                val pr = PacketReader(connectedSocket)

                Log.d(this, "pingContact() send ping to ${contact.name}")
                val encrypted = Crypto.encryptMessage(
                    "{\"action\":\"ping\"}",
                    contact.publicKey,
                    ownPublicKey,
                    ownSecretKey
                ) ?: return Contact.State.COMMUNICATION_FAILED

                pw.writeMessage(encrypted)
                val request = pr.readMessage() ?: return Contact.State.COMMUNICATION_FAILED
                val decrypted = Crypto.decryptMessage(
                    request,
                    otherPublicKey,
                    ownPublicKey,
                    ownSecretKey
                ) ?: return Contact.State.AUTHENTICATION_FAILED

                if (!otherPublicKey.contentEquals(contact.publicKey)) {
                    return Contact.State.AUTHENTICATION_FAILED
                }

                val obj = JSONObject(decrypted)
                val action = obj.optString("action", "")
                if (action == "pong") {
                    Log.d(this, "pingContact() got pong")
                    return Contact.State.CONTACT_ONLINE
                } else {
                    return Contact.State.COMMUNICATION_FAILED
                }
            } catch (e: Exception) {
                return Contact.State.COMMUNICATION_FAILED
            } finally {
                // make sure to close the socket
                closeSocket(connectedSocket)
            }
        }

        private fun closeSocket(socket: Socket?) {
            try {
                socket?.close()
            } catch (_: Exception) {
                // ignore
            }
        }

        override fun run() {
            // set all states to unknown
            for (contact in contacts) {
                DatabaseCache.database.contacts
                    .getContactByPublicKey(contact.publicKey)
                    ?.state = Contact.State.PENDING
            }

            // ping contacts
            for (contact in contacts) {
                val state = pingContact(contact)
                Log.d(this, "contact state is $state")

                // set contact state
                DatabaseCache.database.contacts
                    .getContactByPublicKey(contact.publicKey)
                    ?.state = state

                // Notify adapter to update the UI
                contact.state = state
                context.runOnUiThread {
                    adapter.updateContact(contact)
                }
            }
        }
    }
}
