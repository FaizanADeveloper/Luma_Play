package com.lumaplay.tv.core.util

import android.os.Environment
import android.os.StatFs

/**
 * Shared storage telemetry measuring actual Android device storage capacity and availability.
 *
 * Truthful implementation using [StatFs]:
 * - Reads actual available and total bytes from [Environment.getExternalStorageDirectory].
 * - Avoids hardware-health diagnoses (which StatFs cannot verify).
 * - Reports truthful availability and capacity without fabricating connected USB drives.
 */
data class StorageTelemetry(
    val freeBytes: Long,
    val totalBytes: Long,
    val freeGb: Long,
    val totalGb: Long,
    val usedRatio: Float,
    val isAvailable: Boolean
) {
    companion object {
        /**
         * Queries the primary external storage volume via [StatFs].
         * Safely falls back to zeroed measurements if storage is inaccessible.
         */
        fun query(): StorageTelemetry {
            return runCatching {
                val stat = StatFs(Environment.getExternalStorageDirectory().path)
                val freeBytes = stat.availableBytes
                val totalBytes = stat.totalBytes
                val freeGb = freeBytes / (1024L * 1024L * 1024L)
                val totalGb = totalBytes / (1024L * 1024L * 1024L)
                val usedRatio = if (totalBytes > 0) {
                    (1f - (freeBytes.toFloat() / totalBytes.toFloat())).coerceIn(0f, 1f)
                } else {
                    0f
                }
                StorageTelemetry(
                    freeBytes = freeBytes,
                    totalBytes = totalBytes,
                    freeGb = freeGb,
                    totalGb = totalGb,
                    usedRatio = usedRatio,
                    isAvailable = totalBytes > 0
                )
            }.getOrDefault(
                StorageTelemetry(
                    freeBytes = 0L,
                    totalBytes = 0L,
                    freeGb = 0L,
                    totalGb = 0L,
                    usedRatio = 0f,
                    isAvailable = false
                )
            )
        }
    }
}
