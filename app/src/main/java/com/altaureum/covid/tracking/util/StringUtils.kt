package com.altaureum.covid.tracking.util

import android.util.Log
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import kotlin.experimental.and

/**
 * This class is meant to be a replacement for TextUtils to allow unit testing
 * of files that may want to use common TextUtils methods.
 */
object StringUtils {
    private val TAG = StringUtils::class.java.simpleName
    private fun byteToHex(b: Byte): String {
        val char1 = Character.forDigit((b and 0xF0.toByte()).toInt() shr 4, 16)
        val char2 = Character.forDigit((b and 0x0F).toInt(), 16)
        return String.format("0x%1\$s%2\$s", char1, char2)
    }

    fun byteArrayInHexFormat(byteArray: ByteArray?): String? {
        if (byteArray == null) {
            return null
        }
        val stringBuilder = StringBuilder()
        stringBuilder.append("{ ")
        for (i in byteArray.indices) {
            if (i > 0) {
                stringBuilder.append(", ")
            }
            val hexString = byteToHex(byteArray[i])
            stringBuilder.append(hexString)
        }
        stringBuilder.append(" }")
        return stringBuilder.toString()
    }

    fun bytesFromString(string: String): ByteArray {
        var stringBytes = ByteArray(0)
        try {
            stringBytes = string.toByteArray(charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "Failed to convert message string to byte array")
        }
        return stringBytes
    }

    fun stringFromBytes(bytes: ByteArray?): String? {
        var byteString: String? = null
        try {
            byteString = String(bytes!!, Charset.forName("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "Unable to convert message bytes to string")
        }
        return byteString
    }
}