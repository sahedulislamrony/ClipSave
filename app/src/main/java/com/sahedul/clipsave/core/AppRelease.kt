package com.sahedul.clipsave.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The bits of GitHub's `releases/latest` response the updater needs. */
@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tag: String,
    val assets: List<Asset> = emptyList(),
) {
    @Serializable
    data class Asset(
        val name: String,
        val size: Long,
        @SerialName("browser_download_url") val url: String,
    )

    val version get() = tag.removePrefix("v")
}

/** Pure release logic, kept apart from [AppUpdater] so it can be unit-tested. */
object AppRelease {
    /** build.sh names each APK `ClipSave-<version>-<variant>.apk`. */
    val VARIANTS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "universal")
    const val UNIVERSAL = "universal"

    /** An ABI-split APK carries one `lib/<abi>/` folder; the universal one carries all of them. */
    fun variantFor(abis: Set<String>): String = abis.singleOrNull()?.takeIf { it in VARIANTS } ?: UNIVERSAL

    /** Native lib ABIs present in an APK, from its zip entry names. */
    fun abisIn(entries: Sequence<String>): Set<String> =
        entries.filter { it.startsWith("lib/") }.mapNotNull { it.split('/').getOrNull(1)?.ifEmpty { null } }.toSet()

    /** The asset for the installed [variant], falling back to universal when that split is missing. */
    fun assetFor(release: GithubRelease, variant: String): GithubRelease.Asset? {
        fun find(v: String) = release.assets.firstOrNull { it.name.endsWith("-$v.apk") }
        return find(variant) ?: find(UNIVERSAL)
    }

    /** True when [latest] is a newer version than [current] (semver; a pre-release sorts before its release). */
    fun isNewer(latest: String, current: String): Boolean = compare(latest, current) > 0

    /**
     * Whether a check that found [latest] should show the update screen. A manual check always does;
     * the start-up check skips the one version the user put off with "Update later" ([dismissed]).
     */
    fun shouldPrompt(latest: String, dismissed: String?, manual: Boolean): Boolean =
        manual || dismissed == null || compare(latest, dismissed) != 0

    fun compare(a: String, b: String): Int {
        val (coreA, preA) = split(a)
        val (coreB, preB) = split(b)
        for (i in 0 until maxOf(coreA.size, coreB.size)) {
            val d = coreA.getOrElse(i) { 0 }.compareTo(coreB.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return when {
            preA == preB -> 0
            preA == null -> 1
            preB == null -> -1
            else -> preA.compareTo(preB)
        }
    }

    private fun split(v: String): Pair<List<Int>, String?> {
        val clean = v.trim().removePrefix("v").substringBefore('+')
        val core = clean.substringBefore('-')
        val pre = clean.substringAfter('-', "").ifEmpty { null }
        return core.split('.').map { it.toIntOrNull() ?: 0 } to pre
    }
}
