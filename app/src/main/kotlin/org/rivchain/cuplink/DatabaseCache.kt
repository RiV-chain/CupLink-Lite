package org.rivchain.cuplink

import org.rivchain.cuplink.rivmesh.models.PeerInfo
import org.rivchain.cuplink.util.Log
import org.rivchain.cuplink.util.Utils.readInternalFile
import org.rivchain.cuplink.util.Utils.writeInternalFile
import java.io.File
import java.net.InetAddress

class DatabaseCache {

    companion object {

        lateinit var databasePath: String
        var dbEncrypted: Boolean = false
        var firstStart = false
        var databasePassword = ""

        lateinit var database: Database

        fun load(): Database {
            if (File(databasePath).exists()) {
                // Open an existing database
                val db = readInternalFile(databasePath)
                database = Database.fromData(db, databasePassword)
                firstStart = false
            } else {
                // Create a new database
                database = Database()
                database.mesh.invoke()
                // Generate random port from allowed range
                val port = org.rivchain.cuplink.rivmesh.util.Utils.generateRandomPort()
                val localPeer = PeerInfo("tcp", InetAddress.getByName("0.0.0.0"), port, null, false)
                database.mesh.setListen(setOf(localPeer))
                database.mesh.multicastRegex = ".*"
                database.mesh.multicastListen = true
                database.mesh.multicastBeacon = true
                database.mesh.multicastPassword = ""
                firstStart = true
            }
            return database
        }

        fun save() {
            try {
                val db = database
                val dbData = Database.toData(db, databasePassword)
                if (dbData != null) {
                    writeInternalFile(databasePath, dbData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun isDecrypted(): Boolean{
            /**
             * Test DB access. If that fail stop service and
             * prompt user for a password in StartActivity
             */
            try {
                load()
                return true
            } catch (e: Database.WrongPasswordException) {
                // ignore and continue with initialization,
                // the password dialog comes on the next startState
                dbEncrypted = true
            } catch (e: Exception) {
                Log.e(this, "${e.message}")
            }
            return false
        }
    }
}