package com.lumaplay.tv.player.seek

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class SeekPreferencesTest {

    @Test
    fun testDefaultFallbackWhenNoPreferenceStored() {
        val fakePrefs = FakeSharedPreferences()
        val seekPrefs = SeekPreferences(fakePrefs)

        assertEquals(SeekInterval.DEFAULT, seekPrefs.getSeekInterval())
        assertEquals(SeekInterval.TEN, seekPrefs.getSeekInterval())
    }

    @Test
    fun testPersistAndRetrieveAllIntervals() {
        val fakePrefs = FakeSharedPreferences()
        val seekPrefs = SeekPreferences(fakePrefs)

        for (interval in SeekInterval.entries) {
            seekPrefs.setSeekInterval(interval)
            assertEquals(interval, seekPrefs.getSeekInterval())
        }
    }

    @Test
    fun testUnrecognizedValueFallsBackToDefault() {
        val fakePrefs = FakeSharedPreferences()
        fakePrefs.putInt(SeekPreferences.KEY_SEEK_INTERVAL_SECONDS, 999)
        val seekPrefs = SeekPreferences(fakePrefs)

        assertEquals(SeekInterval.DEFAULT, seekPrefs.getSeekInterval())
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any>()

        fun putInt(key: String, value: Int) {
            data[key] = value
        }

        override fun getInt(key: String?, defValue: Int): Int {
            return (data[key] as? Int) ?: defValue
        }

        override fun getAll(): MutableMap<String, *> = data

        override fun getString(key: String?, defValue: String?): String? = (data[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
        override fun getLong(key: String?, defValue: Long): Long = (data[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (data[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (data[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun edit(): SharedPreferences.Editor = FakeEditor(data)

        private class FakeEditor(private val data: MutableMap<String, Any>) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any>()

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }

            override fun apply() {
                data.putAll(temp)
            }

            override fun commit(): Boolean {
                data.putAll(temp)
                return true
            }

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = this
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String?): SharedPreferences.Editor = this
            override fun clear(): SharedPreferences.Editor = this
        }
    }
}
