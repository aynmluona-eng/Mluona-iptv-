package com.example.data.repository

import com.example.data.model.AccountSession
import com.example.data.model.AccountType
import com.example.data.model.LiveCategory
import com.example.data.model.LiveChannel
import com.example.data.model.SeriesCategory
import com.example.data.model.SeriesDetail
import com.example.data.model.SeriesEpisode
import com.example.data.model.SeriesItem
import com.example.data.model.SeriesSeason
import com.example.data.model.VodCategory
import com.example.data.model.VodDetail
import com.example.data.model.VodMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class IptvResult<out T> {
    data class Success<out T>(val data: T) : IptvResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : IptvResult<Nothing>()
}

class IptvRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    private fun cleanServerUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "http://$url"
        }
        return url.trimEnd('/')
    }

    private fun formatTimestamp(timestamp: String?): String {
        if (timestamp.isNullOrBlank() || timestamp == "null") return "Unlimited"
        return try {
            val millis = timestamp.toLong() * 1000L
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(millis))
        } catch (_: Exception) {
            timestamp
        }
    }

    /**
     * Authenticate with Xtream Codes API
     */
    suspend fun authenticateXtream(
        rawServerUrl: String,
        username: String,
        password: String,
        accountName: String? = null
    ): IptvResult<AccountSession> = withContext(Dispatchers.IO) {
        val serverUrl = cleanServerUrl(rawServerUrl)
        if (username.isBlank() || password.isBlank()) {
            return@withContext IptvResult.Error("يرجى إدخال اسم المستخدم وكلمة المرور")
        }

        val requestUrl = "$serverUrl/player_api.php?username=$username&password=$password"
        val request = Request.Builder()
            .url(requestUrl)
            .header("User-Agent", "IPTVSmarters/1.0.0 (Linux; Android TV)")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext IptvResult.Error("خطأ في الخادم: رمز الاستجابة ${response.code}")
            }

            val bodyString = response.body?.string() ?: ""
            if (bodyString.isBlank()) {
                return@withContext IptvResult.Error("رد فارغ من خادم IPTV")
            }

            val rootObj = JSONObject(bodyString)
            val userInfo = rootObj.optJSONObject("user_info")
            if (userInfo == null) {
                return@withContext IptvResult.Error("بيانات الاعتماد غير صالحة أو تعذر قراءة معلومات المستخدم")
            }

            val authStatus = userInfo.optInt("auth", -1)
            val status = userInfo.optString("status", "Unknown")

            if (authStatus == 0 || status.equals("Disabled", ignoreCase = true) || status.equals("Banned", ignoreCase = true)) {
                val msg = if (authStatus == 0) "اسم المستخدم أو كلمة المرور غير صحيحة" else "الحساب غير نشط ($status)"
                return@withContext IptvResult.Error(msg)
            }

            val rawExpDate = userInfo.optString("exp_date", "")
            val expDate = formatTimestamp(rawExpDate)
            val maxConnections = userInfo.optString("max_connections", "1")

            val name = if (!accountName.isNullOrBlank()) {
                accountName
            } else {
                try {
                    java.net.URI(serverUrl).host ?: "Xtream IPTV"
                } catch (_: Exception) {
                    "Xtream IPTV"
                }
            }

            val session = AccountSession(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                type = AccountType.XTREAM,
                serverUrl = serverUrl,
                username = username,
                password = password,
                status = status,
                expDate = expDate,
                maxConnections = maxConnections,
                lastActiveAt = System.currentTimeMillis()
            )

            IptvResult.Success(session)
        } catch (e: UnknownHostException) {
            IptvResult.Error("تعذر العثور على عنوان الخادم، تحقق من الرابط والاتصال بالإنترنت", e)
        } catch (e: SocketTimeoutException) {
            IptvResult.Error("انتهت مهلة الاتصال بالخادم، يرجى المحاولة لاحقاً", e)
        } catch (e: ConnectException) {
            IptvResult.Error("فشل الاتصال بالخادم، تأكد من صحة المنفذ والرابط", e)
        } catch (e: Exception) {
            IptvResult.Error("حدث خطأ أثناء تسجيل الدخول: ${e.localizedMessage ?: e.message}", e)
        }
    }

    /**
     * Fetch Live Categories from Xtream server
     */
    suspend fun getLiveCategories(session: AccountSession): IptvResult<List<LiveCategory>> = withContext(Dispatchers.IO) {
        val url = "${session.serverUrl}/player_api.php?username=${session.username}&password=${session.password}&action=get_live_categories"
        fetchCategories(url)
    }

    /**
     * Fetch Live Streams from Xtream server
     */
    suspend fun getLiveStreams(
        session: AccountSession,
        categoryId: String? = null
    ): IptvResult<List<LiveChannel>> = withContext(Dispatchers.IO) {
        val catParam = if (!categoryId.isNullOrBlank() && categoryId != "all") "&category_id=$categoryId" else ""
        val url = "${session.serverUrl}/player_api.php?username=${session.username}&password=${session.password}&action=get_live_streams$catParam"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "IPTVSmarters/1.0.0 (Linux; Android TV)")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext IptvResult.Error("خطأ في جلب القنوات (${response.code})")
            }
            val body = response.body?.string() ?: "[]"
            val array = JSONArray(body)
            val channels = mutableListOf<LiveChannel>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val streamId = obj.optInt("stream_id", -1)
                if (streamId <= 0) continue
                channels.add(
                    LiveChannel(
                        streamId = streamId,
                        num = obj.optInt("num", i + 1),
                        name = obj.optString("name", "Channel $streamId"),
                        streamIcon = obj.optString("stream_icon", null).takeIf { !it.isNullOrBlank() },
                        categoryId = obj.optString("category_id", null),
                        epgChannelId = obj.optString("epg_channel_id", null)
                    )
                )
            }
            IptvResult.Success(channels)
        } catch (e: Exception) {
            IptvResult.Error("فشل تحميل قنوات البث المباشر: ${e.localizedMessage}", e)
        }
    }

    /**
     * Fetch VOD (Movies) Categories from Xtream server
     */
    suspend fun getVodCategories(session: AccountSession): IptvResult<List<VodCategory>> = withContext(Dispatchers.IO) {
        val url = "${session.serverUrl}/player_api.php?username=${session.username}&password=${session.password}&action=get_vod_categories"
        val res = fetchCategories(url)
        when (res) {
            is IptvResult.Success -> IptvResult.Success(res.data.map { VodCategory(it.categoryId, it.categoryName) })
            is IptvResult.Error -> IptvResult.Error(res.message, res.cause)
        }
    }

    /**
     * Fetch VOD (Movies) Streams from Xtream server
     */
    suspend fun getVodStreams(
        session: AccountSession,
        categoryId: String? = null
    ): IptvResult<List<VodMovie>> = withContext(Dispatchers.IO) {
        val catParam = if (!categoryId.isNullOrBlank() && categoryId != "all") "&category_id=$categoryId" else ""
        val url = "${session.serverUrl}/player_api.php?username=${session.username}&password=${session.password}&action=get_vod_streams$catParam"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "IPTVSmarters/1.0.0 (Linux; Android TV)")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext IptvResult.Error("خطأ في جلب الأفلام (${response.code})")
            }
            val body = response.body?.string() ?: "[]"
            val array = JSONArray(body)
            val movies = mutableListOf<VodMovie>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val streamId = obj.optInt("stream_id", -1)
                if (streamId <= 0) continue
                movies.add(
                    VodMovie(
                        streamId = streamId,
                        name = obj.optString("name", "Movie $streamId"),
                        streamIcon = obj.optString("stream_icon", null).takeIf { !it.isNullOrBlank() },
                        rating = obj.optString("rating", null).takeIf { !it.isNullOrBlank() },
                        categoryId = obj.optString("category_id", null),
                        containerExtension = obj.optString("container_extension", "mp4")
                    )
                )
            }
            IptvResult.Success(movies)
        } catch (e: Exception) {
            IptvResult.Error("فشل تحميل الأفلام: ${e.localizedMessage}", e)
        }
    }

    /**
     * Fetch Series Categories from Xtream server
     */
    suspend fun getSeriesCategories(session: AccountSession): IptvResult<List<SeriesCategory>> = withContext(Dispatchers.IO) {
        val url = "${session.serverUrl}/player_api.php?username=${session.username}&password=${session.password}&action=get_series_categories"
        val res = fetchCategories(url)
        when (res) {
            is IptvResult.Success -> IptvResult.Success(res.data.map { SeriesCategory(it.categoryId, it.categoryName) })
            is IptvResult.Error -> IptvResult.Error(res.message, res.cause)
        }
    }

    /**
     * Fetch Series list from Xtream server
     */
    suspend fun getSeries(
        session: AccountSession,
        categoryId: String? = null
    ): IptvResult<List<SeriesItem>> = withContext(Dispatchers.IO) {
        val catParam = if (!categoryId.isNullOrBlank() && categoryId != "all") "&category_id=$categoryId" else ""
        val url = "${session.serverUrl}/player_api.php?username=${session.username}&password=${session.password}&action=get_series$catParam"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "IPTVSmarters/1.0.0 (Linux; Android TV)")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext IptvResult.Error("خطأ في جلب المسلسلات (${response.code})")
            }
            val body = response.body?.string() ?: "[]"
            val array = JSONArray(body)
            val seriesList = mutableListOf<SeriesItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val seriesId = obj.optInt("series_id", -1)
                if (seriesId <= 0) continue
                seriesList.add(
                    SeriesItem(
                        seriesId = seriesId,
                        name = obj.optString("name", "Series $seriesId"),
                        cover = obj.optString("cover", null).takeIf { !it.isNullOrBlank() },
                        rating = obj.optString("rating", null).takeIf { !it.isNullOrBlank() },
                        categoryId = obj.optString("category_id", null),
                        plot = obj.optString("plot", null).takeIf { !it.isNullOrBlank() },
                        genre = obj.optString("genre", null).takeIf { !it.isNullOrBlank() },
                        releaseDate = obj.optString("releaseDate", null).takeIf { !it.isNullOrBlank() }
                    )
                )
            }
            IptvResult.Success(seriesList)
        } catch (e: Exception) {
            IptvResult.Error("فشل تحميل المسلسلات: ${e.localizedMessage}", e)
        }
    }

    /**
     * Fetch Series Info (Seasons, Episodes, Plot, Cast) from Xtream server
     */
    suspend fun getSeriesInfo(
        session: AccountSession,
        seriesId: Int
    ): IptvResult<SeriesDetail> = withContext(Dispatchers.IO) {
        val url = "${session.serverUrl}/player_api.php?username=${session.username}&password=${session.password}&action=get_series_info&series_id=$seriesId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "IPTVSmarters/1.0.0 (Linux; Android TV)")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext IptvResult.Error("خطأ في جلب تفاصيل المسلسل (${response.code})")
            }
            val body = response.body?.string() ?: "{}"
            val root = JSONObject(body)
            val info = root.optJSONObject("info") ?: JSONObject()
            val episodesObj = root.optJSONObject("episodes") ?: JSONObject()

            val seasonsList = mutableListOf<SeriesSeason>()
            val keys = episodesObj.keys()
            while (keys.hasNext()) {
                val sKey = keys.next()
                val sNum = sKey.toIntOrNull() ?: 1
                val epArray = episodesObj.optJSONArray(sKey) ?: JSONArray()
                val episodes = mutableListOf<SeriesEpisode>()
                for (j in 0 until epArray.length()) {
                    val epObj = epArray.optJSONObject(j) ?: continue
                    val epId = epObj.optString("id", "${seriesId}_${sNum}_$j")
                    val epNum = epObj.optInt("episode_num", j + 1)
                    val title = epObj.optString("title", "Episode $epNum")
                    val ext = epObj.optString("container_extension", "mp4")
                    val plot = epObj.optJSONObject("info")?.optString("plot", null)
                    episodes.add(
                        SeriesEpisode(
                            id = epId,
                            episodeNum = epNum,
                            title = title,
                            containerExtension = ext,
                            info = plot,
                            season = sNum
                        )
                    )
                }
                seasonsList.add(
                    SeriesSeason(
                        seasonNumber = sNum,
                        name = "Season $sNum",
                        episodeCount = episodes.size,
                        episodes = episodes.sortedBy { it.episodeNum }
                    )
                )
            }

            // If no episodes in episodesObj, check seasons array
            if (seasonsList.isEmpty()) {
                val seasonsArr = root.optJSONArray("seasons")
                if (seasonsArr != null) {
                    for (k in 0 until seasonsArr.length()) {
                        val sObj = seasonsArr.optJSONObject(k) ?: continue
                        val sNum = sObj.optInt("season_number", k + 1)
                        val name = sObj.optString("name", "Season $sNum")
                        val epCount = sObj.optInt("episode_count", 0)
                        seasonsList.add(
                            SeriesSeason(
                                seasonNumber = sNum,
                                name = name,
                                episodeCount = epCount
                            )
                        )
                    }
                }
            }

            val detail = SeriesDetail(
                seriesId = seriesId,
                name = info.optString("name", "Series $seriesId"),
                cover = info.optString("cover", null).takeIf { !it.isNullOrBlank() },
                plot = info.optString("plot", null).takeIf { !it.isNullOrBlank() },
                genre = info.optString("genre", null).takeIf { !it.isNullOrBlank() },
                releaseDate = info.optString("releaseDate", null).takeIf { !it.isNullOrBlank() },
                rating = info.optString("rating", null).takeIf { !it.isNullOrBlank() },
                cast = info.optString("cast", null).takeIf { !it.isNullOrBlank() },
                director = info.optString("director", null).takeIf { !it.isNullOrBlank() },
                seasons = seasonsList.sortedBy { it.seasonNumber }
            )
            IptvResult.Success(detail)
        } catch (e: Exception) {
            IptvResult.Error("فشل تحميل تفاصيل المسلسل: ${e.localizedMessage}", e)
        }
    }

    /**
     * Fetch VOD Info (plot, cast, duration) from Xtream server
     */
    suspend fun getVodInfo(
        session: AccountSession,
        vodId: Int
    ): IptvResult<VodDetail> = withContext(Dispatchers.IO) {
        val url = "${session.serverUrl}/player_api.php?username=${session.username}&password=${session.password}&action=get_vod_info&vod_id=$vodId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "IPTVSmarters/1.0.0 (Linux; Android TV)")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext IptvResult.Error("خطأ في جلب تفاصيل الفيلم (${response.code})")
            }
            val body = response.body?.string() ?: "{}"
            val root = JSONObject(body)
            val info = root.optJSONObject("info") ?: JSONObject()
            val movieData = root.optJSONObject("movie_data") ?: JSONObject()

            val detail = VodDetail(
                streamId = vodId,
                name = info.optString("name", movieData.optString("name", "Movie $vodId")),
                cover = info.optString("cover_big", info.optString("movie_image", null)).takeIf { !it.isNullOrBlank() },
                plot = info.optString("plot", info.optString("description", null)).takeIf { !it.isNullOrBlank() },
                genre = info.optString("genre", null).takeIf { !it.isNullOrBlank() },
                releaseDate = info.optString("releasedate", info.optString("release_date", null)).takeIf { !it.isNullOrBlank() },
                rating = info.optString("rating", null).takeIf { !it.isNullOrBlank() },
                duration = info.optString("duration", info.optString("duration_secs", null)).takeIf { !it.isNullOrBlank() },
                director = info.optString("director", null).takeIf { !it.isNullOrBlank() },
                cast = info.optString("cast", info.optString("actors", null)).takeIf { !it.isNullOrBlank() },
                containerExtension = movieData.optString("container_extension", "mp4")
            )
            IptvResult.Success(detail)
        } catch (e: Exception) {
            IptvResult.Error("فشل تحميل تفاصيل الفيلم: ${e.localizedMessage}", e)
        }
    }

    /**
     * Parse and fetch real M3U / M3U8 playlist
     */
    suspend fun loadM3uPlaylist(rawUrl: String, playlistName: String? = null): IptvResult<Pair<AccountSession, List<LiveChannel>>> = withContext(Dispatchers.IO) {
        val url = cleanServerUrl(rawUrl)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "IPTVSmarters/1.0.0 (Linux; Android TV)")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext IptvResult.Error("تعذر تنزيل قائمة M3U (${response.code})")
            }

            val body = response.body ?: return@withContext IptvResult.Error("الملف المستلم فارغ")
            val reader = BufferedReader(InputStreamReader(body.byteStream()))

            val channels = mutableListOf<LiveChannel>()
            var currentLine: String?
            var currentName = ""
            var currentLogo: String? = null
            var currentGroup: String? = null
            var streamCounter = 1

            while (reader.readLine().also { currentLine = it } != null) {
                val line = currentLine?.trim() ?: continue
                if (line.isEmpty()) continue

                if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    // Extract channel name after comma
                    val commaIndex = line.lastIndexOf(',')
                    currentName = if (commaIndex != -1 && commaIndex < line.length - 1) {
                        line.substring(commaIndex + 1).trim()
                    } else {
                        "Channel $streamCounter"
                    }

                    // Extract tvg-logo
                    val logoRegex = Regex("""tvg-logo="([^"]+)"""", RegexOption.IGNORE_CASE)
                    currentLogo = logoRegex.find(line)?.groupValues?.getOrNull(1)

                    // Extract group-title
                    val groupRegex = Regex("""group-title="([^"]+)"""", RegexOption.IGNORE_CASE)
                    currentGroup = groupRegex.find(line)?.groupValues?.getOrNull(1)
                } else if (!line.startsWith("#")) {
                    // This is a direct stream URL
                    if (currentName.isNotEmpty()) {
                        channels.add(
                            LiveChannel(
                                streamId = streamCounter,
                                num = streamCounter,
                                name = currentName,
                                streamIcon = currentLogo,
                                categoryId = currentGroup,
                                directSourceUrl = line
                            )
                        )
                        streamCounter++
                        currentName = ""
                        currentLogo = null
                        currentGroup = null
                    }
                }
            }

            if (channels.isEmpty()) {
                return@withContext IptvResult.Error("لم يتم العثور على أي قنوات صالحة في ملف M3U")
            }

            val session = AccountSession(
                id = java.util.UUID.randomUUID().toString(),
                name = playlistName?.takeIf { it.isNotBlank() } ?: "M3U Playlist",
                type = AccountType.M3U,
                m3uUrl = url,
                status = "Active",
                expDate = "Unlimited",
                lastActiveAt = System.currentTimeMillis()
            )

            IptvResult.Success(Pair(session, channels))
        } catch (e: Exception) {
            IptvResult.Error("فشل قراءة رابط M3U: ${e.localizedMessage ?: e.message}", e)
        }
    }

    private fun fetchCategories(url: String): IptvResult<List<LiveCategory>> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "IPTVSmarters/1.0.0 (Linux; Android TV)")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return IptvResult.Error("خطأ في جلب التصنيفات (${response.code})")
            }
            val body = response.body?.string() ?: "[]"
            val array = JSONArray(body)
            val categories = mutableListOf<LiveCategory>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val catId = obj.optString("category_id", "")
                val catName = obj.optString("category_name", "")
                if (catId.isNotBlank() && catName.isNotBlank()) {
                    categories.add(LiveCategory(catId, catName))
                }
            }
            IptvResult.Success(categories)
        } catch (e: Exception) {
            IptvResult.Error("فشل تحميل التصنيفات: ${e.localizedMessage}", e)
        }
    }
}
