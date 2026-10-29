package com.sahedul.clipsave.core

import android.content.Context
import android.webkit.CookieManager
import java.io.File

/**
 * Instagram and (private) Facebook posts usually need a logged-in session. [LoginActivity]
 * lets the user sign in inside a WebView; we copy that session's cookies into a Netscape
 * cookie file, which is what yt-dlp's `--cookies` expects.
 */
class CookieStore(context: Context) {
    private val dir = File(context.filesDir, "cookies").apply { mkdirs() }
    private val merged = File(dir, "merged.txt")

    enum class Site(val key: String, val loginUrl: String, val cookieUrls: List<String>, val domain: String) {
        INSTAGRAM("instagram", "https://www.instagram.com/accounts/login/", listOf("https://www.instagram.com"), ".instagram.com"),
        FACEBOOK("facebook", "https://m.facebook.com/login/", listOf("https://www.facebook.com", "https://m.facebook.com"), ".facebook.com"),
    }

    fun isLoggedIn(site: Site) = file(site).let { it.exists() && it.length() > 0 }

    /** Snapshot the WebView's cookies for [site]. Returns false when nothing useful was found. */
    fun captureFromWebView(site: Site): Boolean {
        val manager = CookieManager.getInstance().apply { flush() }
        val pairs = site.cookieUrls
            .mapNotNull { manager.getCookie(it) }
            .flatMap { it.split(";") }
            .mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null else part.substring(0, i).trim() to part.substring(i + 1).trim()
            }
            .toMap()
        if (pairs.isEmpty()) return false

        val expiry = System.currentTimeMillis() / 1000 + 365L * 24 * 3600
        val lines = buildList {
            add("# Netscape HTTP Cookie File")
            pairs.forEach { (name, value) ->
                add("${site.domain}\tTRUE\t/\tTRUE\t$expiry\t$name\t$value")
            }
        }
        file(site).writeText(lines.joinToString("\n") + "\n")
        return true
    }

    fun clear(site: Site) {
        file(site).delete()
        val manager = CookieManager.getInstance()
        site.cookieUrls.forEach { url ->
            manager.getCookie(url)?.split(";")?.forEach { c ->
                manager.setCookie(url, "${c.substringBefore('=').trim()}=; Max-Age=0")
            }
        }
        manager.flush()
    }

    /** Path of a cookie file combining every saved site, or null when the user never logged in. */
    fun mergedFileOrNull(): File? {
        val files = Site.entries.map(::file).filter { it.exists() && it.length() > 0 }
        if (files.isEmpty()) return null
        val text = buildString {
            append("# Netscape HTTP Cookie File\n")
            files.forEach { f -> f.readLines().filter { !it.startsWith("#") && it.isNotBlank() }.forEach { append(it).append('\n') } }
        }
        merged.writeText(text)
        return merged
    }

    private fun file(site: Site) = File(dir, "${site.key}.txt")
}
