package org.rivchain.cuplink

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import org.json.JSONException
import org.rivchain.cuplink.model.Contact
import org.rivchain.cuplink.util.RlpUtils
import org.rivchain.cuplink.util.Utils

class ConnectFragmentActivity : AddContactActivity(), BarcodeCallback {
    private lateinit var publicKey: ByteArray
    private lateinit var barcodeView: DecoratedBarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        if(intent == null || intent.extras == null || intent.extras?.get("EXTRA_CONTACT_PUBLICKEY") == null){
            Toast.makeText(this, R.string.contact_public_key_invalid, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        publicKey = intent.extras?.get("EXTRA_CONTACT_PUBLICKEY") as ByteArray

        title = getString(R.string.title_scan_qr_code)

        barcodeView = findViewById(R.id.barcodeScannerView)
        barcodeView.setStatusText(null)

        findViewById<View>(R.id.fabShare).setOnClickListener {
            try {
                val contact = getContactOrOwn(publicKey)!!
                Thread {
                    val data = RlpUtils.generateLink(contact)
                    val i = Intent(Intent.ACTION_SEND)
                    i.putExtra(Intent.EXTRA_TEXT, data)
                    i.type = "text/plain"
                    startActivity(i)
                }.start()
                finish()
            } catch (e: Exception) {
                // ignore
            }
        }

        try {
            val contact = getContactOrOwn(publicKey)!!
            Thread {
                generateDeepLinkQR(contact)
            }.start()
        } catch (e: NullPointerException) {
            e.printStackTrace()
            Toast.makeText(this, "NPE", Toast.LENGTH_LONG).show()
        } catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            finish()
        }

        // manual input button
        findViewById<View>(R.id.fabManualInput).setOnClickListener { startManualInput() }

        if (!Utils.hasPermission(this, Manifest.permission.CAMERA)) {
            enabledCameraForResult.launch(Manifest.permission.CAMERA)
        }
        if (Utils.hasPermission(this, Manifest.permission.CAMERA)) {
            initCamera()
        }
    }

    private val enabledCameraForResult = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted -> if (isGranted) {
        initCamera()
    } else {
        Toast.makeText(this, R.string.missing_camera_permission, Toast.LENGTH_LONG).show()
        // no finish() in case no camera access wanted but contact data pasted
    }
    }

    override fun barcodeResult(result: BarcodeResult) {
        // no more scan until result is processed
        try {
            super.addContact(result.text)
        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(this, R.string.invalid_qr, Toast.LENGTH_LONG).show()
        }
    }

    override fun possibleResultPoints(resultPoints: List<ResultPoint>) {
        // ignore
    }

    private fun initCamera() {
        val formats = listOf(BarcodeFormat.QR_CODE)
        barcodeView.barcodeView?.decoderFactory = DefaultDecoderFactory(formats)
        barcodeView.decodeContinuous(this)
        barcodeView.resume()
    }

    override fun pause(){
        barcodeView.pause()
    }

    override fun resume(){
        barcodeView.resume()
    }

    private fun generateDeepLinkQR(contact: Contact) {

        val data = RlpUtils.generateLink(contact)
        if(data == null){
            Toast.makeText(this, R.string.contact_is_invalid, Toast.LENGTH_SHORT).show()
        }
        if (contact.addresses.isEmpty()) {
            Toast.makeText(this, R.string.contact_has_no_address_warning, Toast.LENGTH_SHORT).show()
        }
        if (contact.name.isEmpty()) {
            Toast.makeText(this, R.string.contact_name_invalid, Toast.LENGTH_SHORT).show()
        }
        if (contact.publicKey.isEmpty()) {
            Toast.makeText(this, R.string.contact_public_key_invalid, Toast.LENGTH_SHORT).show()
        }
        val multiFormatWriter = MultiFormatWriter()
        val bitMatrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, 1080, 1080)
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.createBitmap(bitMatrix)
        runOnUiThread {
            findViewById<ImageView>(R.id.QRView).setImageBitmap(bitmap)
        }
    }
}
