package org.rivchain.cuplink

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.rivchain.cuplink.model.Contact
import org.rivchain.cuplink.util.RlpUtils

class ShareContactFragment : Fragment() {

    private lateinit var activity: BaseActivity
    private lateinit var publicKey: ByteArray

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_share_contact, container, false)
        activity = requireActivity() as BaseActivity
        // Inflate the layout for this fragment
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments == null || arguments?.get("EXTRA_CONTACT_PUBLICKEY") == null) {
            Toast.makeText(requireContext(), R.string.contact_public_key_invalid, Toast.LENGTH_LONG).show()
            activity.finish()
            return
        }

        publicKey = arguments?.get("EXTRA_CONTACT_PUBLICKEY") as ByteArray

        // Set the title for the fragment (if needed)
        activity.title = getString(R.string.title_show_qr_code)

        view.findViewById<View>(R.id.fabPresenter).setOnClickListener {
            startActivity(Intent(requireContext(), QRScanActivity::class.java))
            activity.finish() // Since you may be navigating to a new activity, you may want to finish this one
        }

        view.findViewById<View>(R.id.fabShare).setOnClickListener {
            try {
                val contact = activity.getContactOrOwn(publicKey)!!
                Thread {
                    val data = RlpUtils.generateLink(contact)
                    val i = Intent(Intent.ACTION_SEND)
                    i.putExtra(Intent.EXTRA_TEXT, data)
                    i.type = "text/plain"
                    startActivity(i)
                }.start()
                activity.finish()
            } catch (e: Exception) {
                // Ignore errors
            }
        }

        try {
            val contact = activity.getContactOrOwn(publicKey)!!
            Thread {
                generateDeepLinkQR(contact)
            }.start()
        } catch (e: NullPointerException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "NPE", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            activity.finish()
        }
    }

    private fun generateQR(contact: Contact) {
        view?.findViewById<TextView>(R.id.contact_name_tv)
            ?.text = contact.name

        val data = Contact.toJSON(contact, false).toString()
        if (contact.addresses.isEmpty()) {
            Toast.makeText(requireContext(), R.string.contact_has_no_address_warning, Toast.LENGTH_SHORT).show()
        }
        if (contact.name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.contact_name_invalid, Toast.LENGTH_SHORT).show()
        }
        if (contact.publicKey.isEmpty()) {
            Toast.makeText(requireContext(), R.string.contact_public_key_invalid, Toast.LENGTH_SHORT).show()
        }

        val multiFormatWriter = MultiFormatWriter()
        val bitMatrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, 1080, 1080)
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.createBitmap(bitMatrix)
        view?.findViewById<ImageView>(R.id.QRView)?.setImageBitmap(bitmap)
    }

    private fun generateDeepLinkQR(contact: Contact) {
        activity.runOnUiThread {
            view?.findViewById<TextView>(R.id.contact_name_tv)
                ?.text = contact.name
        }
        val data = RlpUtils.generateLink(contact)
        if (data == null) {
            Toast.makeText(requireContext(), R.string.contact_is_invalid, Toast.LENGTH_SHORT).show()
        }
        if (contact.addresses.isEmpty()) {
            Toast.makeText(requireContext(), R.string.contact_has_no_address_warning, Toast.LENGTH_SHORT).show()
        }
        if (contact.name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.contact_name_invalid, Toast.LENGTH_SHORT).show()
        }
        if (contact.publicKey.isEmpty()) {
            Toast.makeText(requireContext(), R.string.contact_public_key_invalid, Toast.LENGTH_SHORT).show()
        }

        val multiFormatWriter = MultiFormatWriter()
        val bitMatrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, 1080, 1080)
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.createBitmap(bitMatrix)
        activity.runOnUiThread {
            view?.findViewById<ImageView>(R.id.QRView)?.setImageBitmap(bitmap)
        }
    }
}
