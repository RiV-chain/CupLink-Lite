package org.rivchain.cuplink

import org.rivchain.cuplink.rivmesh.models.PeerInfo
import org.rivchain.cuplink.util.Utils.readInternalFile
import java.io.File
import java.net.InetAddress

class Load {
    companion object {

        lateinit var databasePath: String
        var dbEncrypted: Boolean = false
        var firstStart = false
        var databasePassword = ""

        lateinit var database: Database

        fun database(): Database {
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
    }
}