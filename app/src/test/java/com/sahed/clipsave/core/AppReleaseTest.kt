package com.sahed.clipsave.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppReleaseTest {

    private fun release(vararg variants: String) = GithubRelease(
        tag = "v1.2.0",
        assets = variants.map { GithubRelease.Asset("ClipSave-1.2.0-$it.apk", 1, "https://example.com/$it") } +
            GithubRelease.Asset("SHA256SUMS.txt", 1, "https://example.com/sums"),
    )

    @Test fun `newer versions are detected`() {
        assertTrue(AppRelease.isNewer("1.0.1", "1.0.0"))
        assertTrue(AppRelease.isNewer("v1.1.0", "1.0.9"))
        assertTrue(AppRelease.isNewer("2.0.0", "1.10.0"))
        assertTrue(AppRelease.isNewer("1.0.10", "1.0.9"))
        assertTrue(AppRelease.isNewer("1.1", "1.0.5"))
    }

    @Test fun `same or older versions are not updates`() {
        assertFalse(AppRelease.isNewer("1.0.0", "1.0.0"))
        assertFalse(AppRelease.isNewer("v1.0.0", "1.0.0"))
        assertFalse(AppRelease.isNewer("1.0.0", "1.0.1"))
        assertFalse(AppRelease.isNewer("1.0", "1.0.0"))
    }

    @Test fun `a release is newer than its pre-release`() {
        assertTrue(AppRelease.isNewer("1.1.0", "1.1.0-beta"))
        assertFalse(AppRelease.isNewer("1.1.0-beta", "1.1.0"))
        assertTrue(AppRelease.isNewer("1.1.0-beta.2", "1.1.0-beta.1"))
    }

    @Test fun `variant comes from the APK's native libs`() {
        assertEquals("arm64-v8a", AppRelease.variantFor(setOf("arm64-v8a")))
        assertEquals("x86_64", AppRelease.variantFor(setOf("x86_64")))
        assertEquals("universal", AppRelease.variantFor(setOf("arm64-v8a", "armeabi-v7a", "x86_64")))
        assertEquals("universal", AppRelease.variantFor(emptySet()))
        assertEquals("universal", AppRelease.variantFor(setOf("mips")))
    }

    @Test fun `abis are read from zip entry names`() {
        val entries = sequenceOf("AndroidManifest.xml", "lib/arm64-v8a/libpython.so", "lib/arm64-v8a/libffmpeg.so", "classes.dex")
        assertEquals(setOf("arm64-v8a"), AppRelease.abisIn(entries))
        assertEquals(setOf("arm64-v8a", "x86_64"), AppRelease.abisIn(sequenceOf("lib/arm64-v8a/a.so", "lib/x86_64/a.so")))
    }

    @Test fun `picks the matching asset`() {
        val r = release("arm64-v8a", "armeabi-v7a", "x86_64", "universal")
        assertEquals("ClipSave-1.2.0-armeabi-v7a.apk", AppRelease.assetFor(r, "armeabi-v7a")?.name)
        assertEquals("ClipSave-1.2.0-universal.apk", AppRelease.assetFor(r, "universal")?.name)
    }

    @Test fun `falls back to universal, then nothing`() {
        assertEquals("ClipSave-1.2.0-universal.apk", AppRelease.assetFor(release("universal"), "x86_64")?.name)
        assertNull(AppRelease.assetFor(release("arm64-v8a"), "x86_64"))
    }
}
