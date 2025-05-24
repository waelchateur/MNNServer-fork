package io.kindbrave.mnnserver.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.util.Log
import io.kindbrave.mnnserver.service.WebServerService

class AistProvider : ContentProvider() {
    private val tag = AistProvider::class.java.simpleName
    override fun onCreate(): Boolean {
        val intent = Intent(context, WebServerService::class.java)
        context?.startService(intent)
        Log.d(tag, "AistProvider onCreate $context")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(
        uri: Uri,
        values: ContentValues?
    ): Uri? {
        return null
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String?>?
    ): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String?>?
    ): Int {
        return 0
    }
}