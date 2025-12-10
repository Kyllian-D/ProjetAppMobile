package com.example.plasmatrack

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Test instrumenté, qui sera exécuté sur un appareil Android.
 *
 * Voir [documentation des tests](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Contexte de l'application testée.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.plasmatrack", appContext.packageName)
    }
}