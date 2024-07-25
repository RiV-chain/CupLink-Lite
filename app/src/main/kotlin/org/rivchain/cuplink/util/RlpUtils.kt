package org.rivchain.cuplink.util

import org.rivchain.cuplink.model.Contact
import org.tdf.rlp.RLPCodec
import org.tdf.rlp.RLPElement
import org.tdf.rlp.RLPList
import java.net.Inet4Address
import java.net.Inet6Address

internal object RlpUtils {

    private val URL_REGEX = """(ftp://)cuplink.net/c/([a-zA-Z0-9]+$)""".toRegex()

    private val BASE_URL = """ftp://cuplink.net/c/"""

    @JvmStatic
    fun parseLink(url: String): Contact? {
        val match: MatchResult = URL_REGEX.find(url) ?: return null
        val path: String = match.groups[2]?.value ?: return null

        val urlData = RLPCodec.decode(Utils.hexStringToByteArray(path), List::class.java)
        val name = (urlData[0] as RLPElement).asString()
        val publicKey = (urlData[1] as RLPElement).asBytes()
        val addresses = (urlData[2] as RLPElement).asRLPList()
        val addressList = mutableListOf<String>()
        for (address in addresses){
            val bytes = address.asBytes()
            if(bytes.size == 4){
                //ipv4
                val ipv4 = Inet4Address.getByAddress(bytes).hostAddress
                addressList.add(ipv4!!)
            }
            if(bytes.size == 16){
                //ipv6
                val ipv6 = Inet6Address.getByAddress(bytes).hostAddress
                addressList.add(ipv6!!)
            }
        }
        if (name.isEmpty() ||
            publicKey.isEmpty() ||
            addressList.isEmpty()) {
            return null
        }
        val contact = Contact(name, publicKey, addressList)
        return contact
    }

    @JvmStatic
    fun generateLink(contact: Contact):String? {
        if (contact.name.isEmpty() ||
            contact.publicKey.isEmpty() ||
            contact.addresses.isEmpty()){
            return null
        }
        val a = mutableListOf<RLPElement>()
        val socketAddress = NetworkUtils.getAllSocketAddresses(contact, false)
        for (sa in socketAddress){
            a.add(RLPElement.fromEncoded(RLPCodec.encode(sa.address.address)))
        }
        RLPList.fromElements(a)
        val list = listOf(
            RLPElement.fromEncoded(RLPCodec.encode(contact.name)),
            RLPElement.fromEncoded(RLPCodec.encode(contact.publicKey)),
            RLPElement.fromEncoded(RLPCodec.encode(a)))
        return BASE_URL + Utils.byteArrayToHexString(RLPList.fromElements(list).encoded)
    }

}