package com.sahedul.clipsave

import android.app.Application
import com.sahedul.clipsave.core.AppUpdater
import com.sahedul.clipsave.core.CookieStore
import com.sahedul.clipsave.core.Engine
import com.sahedul.clipsave.core.Extractor
import com.sahedul.clipsave.core.Prefs
import com.sahedul.clipsave.download.DownloadRepository
import com.sahedul.clipsave.download.DownloadScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** Hand-rolled service locator; the app is small enough that DI framework would be noise. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val prefs = Prefs(app)
    val cookies = CookieStore(app)
    val engine = Engine(app, scope, prefs)
    val extractor = Extractor(engine, cookies, File(app.cacheDir, "yt-dlp"))
    val downloads = DownloadRepository(app)
    val scheduler = DownloadScheduler(app, downloads, prefs)
    val updater = AppUpdater(app, scope)
}
