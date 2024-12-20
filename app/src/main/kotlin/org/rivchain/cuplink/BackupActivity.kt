package org.rivchain.cuplink

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter.formatFileSize
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import org.acra.util.UriUtils.getFileNameFromUri
import org.rivchain.cuplink.util.Log
import org.rivchain.cuplink.util.Utils.exportDatabase
import org.rivchain.cuplink.util.Utils.readExternalFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : BaseActivity() {

    private lateinit var exportButton: Button
    private lateinit var importButton: Button

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        val toolbar = findViewById<Toolbar>(R.id.backup_toolbar)
        toolbar.apply {
            setNavigationOnClickListener {
                finish()
            }
        }
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
        initViews()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun initViews() {
        importButton = findViewById(R.id.ImportButton)
        exportButton = findViewById(R.id.ExportButton)
        //passwordEditText = findViewById(R.id.PasswordEditText)
        importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/json"
            importFileLauncher.launch(intent)
        }

        exportButton.setOnClickListener {
            showBackupPasswordDialog()
        }
        findViewById<TextView>(R.id.splashText).text = "CupLink v${BuildConfig.VERSION_NAME}"

        // Load preferences
        val lastBackupDateTime = preferences.getLong("lastBackupDateTime", 0L) // Default: 0 if not set
        val dbSize = preferences.getInt("dbSize", 0) // Default: 0 if not set
        val backupStatus = preferences.getString("backupStatus", "No backup available") //
        // Update UI or variables based on loaded values
        val lastBackupTextView = findViewById<TextView>(R.id.lastBackupDate)
        if(lastBackupDateTime > 0L){
            val formattedDate = SimpleDateFormat("dd MMM yyyy h:mma", Locale.US)
                .format(Date(lastBackupDateTime))
            lastBackupTextView.text = formattedDate
        }

        val dbSizeTextView = findViewById<TextView>(R.id.backupSize) // Example TextView for db size
        if(dbSize == 0){
            dbSizeTextView.text = ""
        } else {
            dbSizeTextView.text = formatFileSize(this, dbSize.toLong())
        }
        val backupStatusText = findViewById<TextView>(R.id.backupResult)
        if(backupStatus.equals("No backup available")){
            backupStatusText.setTextColor(ContextCompat.getColor(this, R.color.yellowStatus))
        }
        if(backupStatus.equals("Error")){
            backupStatusText.setTextColor(ContextCompat.getColor(this, R.color.redStatus))
        }
        backupStatusText.text = backupStatus

        findViewById<SwitchMaterial>(R.id.autoBackupDescriptionSwitch).apply {
            isChecked = DatabaseCache.database.settings.enableAutoBackup
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                if(isChecked){
                    showBackupPasswordDialog()
                } else{
                    val database = DatabaseCache.database
                    database.settings.enableAutoBackup = false
                    DatabaseCache.save()
                }
            }
        }
    }

    private fun showBackupPasswordDialog() {

        val view: View = LayoutInflater.from(this).inflate(R.layout.dialog_enter_backup_password, null)
        val dialog = createBlurredPPTCDialog(view)
        val backupFilename = "cuplink-backup-${BuildConfig.VERSION_NAME}.json"
        val passwordEditText = view.findViewById<TextInputEditText>(R.id.passwordEditTextView)
        val backupFilenameText = view.findViewById<TextView>(R.id.backupFilename)
        val okButton = view.findViewById<Button>(R.id.okButton)
        backupFilenameText.text = backupFilename
        okButton.setOnClickListener {
            val password = passwordEditText.text.toString()
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_TITLE, backupFilename)
            intent.type = "application/json"
            exportFileLauncher.launch(intent)
            saveTemporalBackupPassword(password)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showRestorePasswordDialog(uri: Uri) {
        val fileName = getFileNameFromUri(this, uri)
        val view: View = LayoutInflater.from(this).inflate(R.layout.dialog_enter_backup_password, null)
        val dialog = createBlurredPPTCDialog(view)
        dialog.setCanceledOnTouchOutside(false)
        val passwordEditText = view.findViewById<TextInputEditText>(R.id.passwordEditTextView)
        val title = view.findViewById<TextView>(R.id.enterPasswordTitle)
        title.text = getString(R.string.enter_password)
        val backupFilenameText = view.findViewById<TextView>(R.id.backupFilename)
        val okButton = view.findViewById<Button>(R.id.okButton)
        backupFilenameText.text = fileName
        okButton.setOnClickListener {
            val password = passwordEditText.text.toString()
            importDatabase(uri, password)
            dialog.dismiss()
        }
        dialog.show()
    }

    private var importFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data ?: return@registerForActivityResult
            val uri = intent.data ?: return@registerForActivityResult
            showRestorePasswordDialog(uri)
        }
    }

    private var exportFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data ?: return@registerForActivityResult
            val uri: Uri = intent.data ?: return@registerForActivityResult
            val title = result.data?.getStringExtra(Intent.EXTRA_TITLE)
            // Retrieve the ByteArray from the intent extra
            val database = DatabaseCache.database
            val password = getTemporalBackupPassword()
            val dbData = Database.toData(database, password)
            try {
                // Persist URI permissions (useful if reopening later)
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                exportDatabase(this, uri, dbData)
                // Set backup status in Activity
                val lastBackupDate = Date()
                val lastBackupDateTime = lastBackupDate.time
                val dbSize = dbData!!.size
                val backupStatus = getString(R.string.success)
                saveBackupDetails(uri, lastBackupDateTime, dbSize, backupStatus)
                // Create a formatter for the desired date format
                val dateFormat = SimpleDateFormat("dd MMM yyyy h:mma", Locale.getDefault())

                // Format the current date
                val formattedDate = dateFormat.format(lastBackupDate)

                // Set the formatted date to the TextView
                findViewById<TextView>(R.id.lastBackupDate).text = formattedDate
                findViewById<TextView>(R.id.backupSize).text = formatFileSize(this, dbSize.toLong())
                findViewById<TextView>(R.id.backupResult).text = backupStatus
                findViewById<TextView>(R.id.backupResult).setTextColor(ContextCompat.getColor(this, R.color.selectedColor))
                val enableAutoBackup = findViewById<SwitchMaterial>(R.id.autoBackupDescriptionSwitch).isChecked
                if(enableAutoBackup){
                    database.settings.enableAutoBackup = true
                    DatabaseCache.save()
                }
                val fileName = getFileNameFromUri(this, uri)
                showMessage("$fileName\n" + getString(R.string.export_success))
            } catch (e: SecurityException) {
                Log.e(this, "Failed to take URI permission: ${e.message}")
                showMessage(getString(R.string.error)+"\n"+"Failed to take URI permission: ${e.message}")
            } catch (e: Exception){
                Log.e(this, "Error during export: ${e.message}")
                showMessage(getString(R.string.error)+"\n"+(e.message ?: "unknown"))
                findViewById<TextView>(R.id.backupResult).setTextColor(ContextCompat.getColor(this, R.color.redStatus))
                saveBackupDetails(uri,0, 0, getString(R.string.error))
            }
        }
    }

    private fun importDatabase(uri: Uri, password: String) {
        val newDatabase: Database
        val db: ByteArray
        try {
            db = readExternalFile(this, uri)
            newDatabase = Database.fromData(db, password)
        } catch (e: Database.WrongPasswordException) {
            showMessage(getString(R.string.wrong_password))
            return
        } catch (e: Exception) {
            showMessage(getString(R.string.error)+"\n"+(e.message ?: "unknown"))
            return
        }

        val contactCount = newDatabase.contacts.contactList.size
        val eventCount = newDatabase.events.eventList.size
        val peersCount = newDatabase.mesh.getPeers().length()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_import_backup, null)
        val titleTextView = dialogView.findViewById<TextView>(R.id.checkboxDialogTitle)
        val contactsCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.contacts)
        val callsCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.calls)
        val settingsCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.settings)
        val okButton = dialogView.findViewById<MaterialButton>(R.id.OkButton)

        titleTextView.text = getString(R.string.dialog_title_import_backup)
        contactsCheckbox.text = getString(R.string.title_contacts) + "($contactCount)"
        callsCheckbox.text = getString(R.string.title_calls) + "($eventCount)"

        // Define the color state list for the checkbox text color
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked), // checked state
                intArrayOf(-android.R.attr.state_checked) // unchecked state
            ),
            intArrayOf(
                ContextCompat.getColor(this, R.color.light_light_grey), // color for checked state
                ContextCompat.getColor(this, R.color.light_grey) // color for unchecked state
            )
        )

        // Apply the color state list to the checkboxes
        contactsCheckbox.setTextColor(colorStateList)
        callsCheckbox.setTextColor(colorStateList)
        settingsCheckbox.setTextColor(colorStateList)

        val dialog = createBlurredPPTCDialog(dialogView)

        okButton.setOnClickListener {
            if (!contactsCheckbox.isChecked &&
                !callsCheckbox.isChecked &&
                !settingsCheckbox.isChecked){
                Toast.makeText(this, "Please select Contacts, Calls, Peers or Settings", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (contactsCheckbox.isChecked) {
                // Handle contacts import
                importContacts(newDatabase)
            }
            if (callsCheckbox.isChecked) {
                // Handle calls import
                importCalls(newDatabase)
            }
            if (settingsCheckbox.isChecked) {
                // Handle peers import
                importSettings(newDatabase)
            }
            DatabaseCache.save()
            dialog.dismiss()
            // Restart service
            restartService()
        }

        dialog.show()
    }

}