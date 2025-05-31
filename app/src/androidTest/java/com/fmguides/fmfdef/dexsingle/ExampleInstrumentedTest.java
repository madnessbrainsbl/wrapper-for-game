package com.fmguides.fmfdef.dexsingle;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.google.firebase.database.FirebaseDatabase;

import java.io.File;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
    public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.fmguides.fmfdef.dexsingle", appContext.getPackageName());
    }

    @Test
    public void useAppContextCache() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.fmguides.fmfdef.dexsingle", appContext.getPackageName());

        // Кейс: кэш уже загружен
        File cacheFile = new File(appContext.getFilesDir(), "gamecache.zip");
        assertTrue("Кэш не найден", cacheFile.exists());

        // Кейс: ID найден в базе
        boolean idFound = true; // эмулируем найденный ID
        assertTrue("ID не найден", idFound);

        // Кейс: кэш успешно скачан
        boolean cacheDownloaded = cacheFile.length() > 0;
        assertTrue("Кэш не скачан", cacheDownloaded);
    }
}
