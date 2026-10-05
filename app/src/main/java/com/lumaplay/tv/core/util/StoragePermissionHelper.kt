package com.lumaplay.tv.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Storage permission helper for Android TV.
 *
 * Implements Android version-specific media storage permissions:
 * - Android 13+ (API 33+): [Manifest.permission.READ_MEDIA_VIDEO]
 * - Android 11 & 12 (API 30-32): [Manifest.permission.READ_EXTERNAL_STORAGE]
 */
object StoragePermissionHelper {

    val requiredPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val requiredAudioPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredAudioPermission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
