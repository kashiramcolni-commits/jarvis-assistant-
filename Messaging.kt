package com.jarvis.assistant.control

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

object Contacts {
    fun findNumber(ctx: Context, name: String): String? {
        val c = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"), null
        ) ?: return null
        return c.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    fun isKnown(ctx: Context, number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return try {
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
                ?.use { it.count > 0 } ?: false
        } catch (e: SecurityException) { false }
    }
}

object Messaging {
    /** Opens a prefilled WhatsApp chat, then presses Send via the accessibility service. */
    suspend fun whatsApp(ctx: Context, contact: String, message: String): String {
        val number = Contacts.findNumber(ctx, contact) ?: return "I couldn't find $contact in your contacts."
        // wa.me needs international format; store contacts as +<country code><number>.
        val digits = number.filter { it.isDigit() }
        val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val a11y = JarvisAccessibilityService.instance
            ?: return "I opened the chat, but I need accessibility access to press send."
        kotlinx.coroutines.delay(1800)
        val sent = a11y.awaitClick { a11y.clickById("com.whatsapp:id/send") || a11y.clickByText("Send") }
        return if (sent) "Message sent to $contact." else "I opened the chat but couldn't press send."
    }

    fun call(ctx: Context, contact: String): String {
        val number = Contacts.findNumber(ctx, contact) ?: return "I couldn't find $contact in your contacts."
        ctx.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Calling $contact."
    }
}
