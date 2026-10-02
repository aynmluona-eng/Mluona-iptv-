package com.example.data.session

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.LiveChannel
import com.example.data.model.SeriesItem
import com.example.data.model.VodMovie
import org.json.JSONArray
import org.json.JSONObject

class FavoritesHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mluona_iptv_favorites_history", Context.MODE_PRIVATE)

    private var activeAccountId: String = "default"

    companion object {
        private const val MAX_RECENT_ITEMS = 40
    }

    private fun favChannelsKey() = "key_fav_channels_$activeAccountId"
    private fun recentChannelsKey() = "key_recent_channels_$activeAccountId"
    private fun favMoviesKey() = "key_fav_movies_$activeAccountId"
    private fun recentMoviesKey() = "key_recent_movies_$activeAccountId"
    private fun favSeriesKey() = "key_fav_series_$activeAccountId"
    private fun recentSeriesKey() = "key_recent_series_$activeAccountId"
    private fun customChannelNameKey(streamId: Int) = "custom_ch_name_${activeAccountId}_$streamId"

    // In-memory high-speed cache for 0ms latency during fast TV remote navigation
    private val favChannelsCache = mutableListOf<LiveChannel>()
    private val favChannelIds = hashSetOf<Int>()
    private val recentChannelsCache = mutableListOf<LiveChannel>()

    private val favMoviesCache = mutableListOf<VodMovie>()
    private val favMovieIds = hashSetOf<Int>()
    private val recentMoviesCache = mutableListOf<VodMovie>()

    private val favSeriesCache = mutableListOf<SeriesItem>()
    private val favSeriesIds = hashSetOf<Int>()
    private val recentSeriesCache = mutableListOf<SeriesItem>()

    init {
        reloadCaches()
    }

    @Synchronized
    fun setCurrentAccount(accountId: String?) {
        val newId = if (accountId.isNullOrBlank()) "default" else accountId
        if (activeAccountId != newId) {
            activeAccountId = newId
            reloadCaches()
        }
    }

    @Synchronized
    private fun reloadCaches() {
        favChannelsCache.clear()
        favChannelIds.clear()
        recentChannelsCache.clear()
        favMoviesCache.clear()
        favMovieIds.clear()
        recentMoviesCache.clear()
        favSeriesCache.clear()
        favSeriesIds.clear()
        recentSeriesCache.clear()

        favChannelsCache.addAll(loadChannels(favChannelsKey()))
        favChannelsCache.forEach { favChannelIds.add(it.streamId) }
        recentChannelsCache.addAll(loadChannels(recentChannelsKey()))

        favMoviesCache.addAll(loadMovies(favMoviesKey()))
        favMoviesCache.forEach { favMovieIds.add(it.streamId) }
        recentMoviesCache.addAll(loadMovies(recentMoviesKey()))

        favSeriesCache.addAll(loadSeries(favSeriesKey()))
        favSeriesCache.forEach { favSeriesIds.add(it.seriesId) }
        recentSeriesCache.addAll(loadSeries(recentSeriesKey()))
    }

    // ==========================================
    // LIVE CHANNELS
    // ==========================================
    @Synchronized
    fun getFavoriteChannels(): List<LiveChannel> {
        return favChannelsCache.toList()
    }

    fun isChannelFavorite(streamId: Int): Boolean {
        return favChannelIds.contains(streamId)
    }

    @Synchronized
    fun toggleChannelFavorite(channel: LiveChannel): Boolean {
        val index = favChannelsCache.indexOfFirst { it.streamId == channel.streamId }
        val isNowFav: Boolean
        if (index >= 0) {
            favChannelsCache.removeAt(index)
            favChannelIds.remove(channel.streamId)
            isNowFav = false
        } else {
            favChannelsCache.add(0, channel)
            favChannelIds.add(channel.streamId)
            isNowFav = true
        }
        saveChannels(favChannelsKey(), favChannelsCache)
        return isNowFav
    }

    @Synchronized
    fun getRecentChannels(): List<LiveChannel> {
        return recentChannelsCache.toList()
    }

    fun getCustomChannelName(streamId: Int): String? {
        return prefs.getString(customChannelNameKey(streamId), null)
    }

    fun setCustomChannelName(streamId: Int, newName: String) {
        prefs.edit().putString(customChannelNameKey(streamId), newName).apply()
    }

    @Synchronized
    fun addChannelToRecent(channel: LiveChannel) {
        recentChannelsCache.removeAll { it.streamId == channel.streamId }
        recentChannelsCache.add(0, channel)
        if (recentChannelsCache.size > MAX_RECENT_ITEMS) {
            val trimmed = recentChannelsCache.take(MAX_RECENT_ITEMS).toMutableList()
            recentChannelsCache.clear()
            recentChannelsCache.addAll(trimmed)
        }
        saveChannels(recentChannelsKey(), recentChannelsCache)
    }

    private fun loadChannels(key: String): List<LiveChannel> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<LiveChannel>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    LiveChannel(
                        streamId = obj.getInt("streamId"),
                        num = if (obj.has("num")) obj.getInt("num") else null,
                        name = obj.getString("name"),
                        streamIcon = obj.optString("streamIcon", null),
                        categoryId = obj.optString("categoryId", null),
                        directSourceUrl = obj.optString("directSourceUrl", null),
                        epgChannelId = obj.optString("epgChannelId", null)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveChannels(key: String, list: List<LiveChannel>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("streamId", item.streamId)
                if (item.num != null) put("num", item.num)
                put("name", item.name)
                put("streamIcon", item.streamIcon ?: "")
                put("categoryId", item.categoryId ?: "")
                put("directSourceUrl", item.directSourceUrl ?: "")
                put("epgChannelId", item.epgChannelId ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    // ==========================================
    // MOVIES (VOD)
    // ==========================================
    @Synchronized
    fun getFavoriteMovies(): List<VodMovie> {
        return favMoviesCache.toList()
    }

    fun isMovieFavorite(streamId: Int): Boolean {
        return favMovieIds.contains(streamId)
    }

    @Synchronized
    fun toggleMovieFavorite(movie: VodMovie): Boolean {
        val index = favMoviesCache.indexOfFirst { it.streamId == movie.streamId }
        val isNowFav: Boolean
        if (index >= 0) {
            favMoviesCache.removeAt(index)
            favMovieIds.remove(movie.streamId)
            isNowFav = false
        } else {
            favMoviesCache.add(0, movie)
            favMovieIds.add(movie.streamId)
            isNowFav = true
        }
        saveMovies(favMoviesKey(), favMoviesCache)
        return isNowFav
    }

    @Synchronized
    fun getRecentMovies(): List<VodMovie> {
        return recentMoviesCache.toList()
    }

    @Synchronized
    fun addMovieToRecent(movie: VodMovie) {
        recentMoviesCache.removeAll { it.streamId == movie.streamId }
        recentMoviesCache.add(0, movie)
        if (recentMoviesCache.size > MAX_RECENT_ITEMS) {
            val trimmed = recentMoviesCache.take(MAX_RECENT_ITEMS).toMutableList()
            recentMoviesCache.clear()
            recentMoviesCache.addAll(trimmed)
        }
        saveMovies(recentMoviesKey(), recentMoviesCache)
    }

    private fun loadMovies(key: String): List<VodMovie> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<VodMovie>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VodMovie(
                        streamId = obj.getInt("streamId"),
                        name = obj.getString("name"),
                        streamIcon = obj.optString("streamIcon", null),
                        rating = obj.optString("rating", null),
                        categoryId = obj.optString("categoryId", null),
                        containerExtension = obj.optString("containerExtension", "mp4"),
                        directSourceUrl = obj.optString("directSourceUrl", null)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveMovies(key: String, list: List<VodMovie>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("streamId", item.streamId)
                put("name", item.name)
                put("streamIcon", item.streamIcon ?: "")
                put("rating", item.rating ?: "")
                put("categoryId", item.categoryId ?: "")
                put("containerExtension", item.containerExtension ?: "mp4")
                put("directSourceUrl", item.directSourceUrl ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    // ==========================================
    // SERIES
    // ==========================================
    @Synchronized
    fun getFavoriteSeries(): List<SeriesItem> {
        return favSeriesCache.toList()
    }

    fun isSeriesFavorite(seriesId: Int): Boolean {
        return favSeriesIds.contains(seriesId)
    }

    @Synchronized
    fun toggleSeriesFavorite(series: SeriesItem): Boolean {
        val index = favSeriesCache.indexOfFirst { it.seriesId == series.seriesId }
        val isNowFav: Boolean
        if (index >= 0) {
            favSeriesCache.removeAt(index)
            favSeriesIds.remove(series.seriesId)
            isNowFav = false
        } else {
            favSeriesCache.add(0, series)
            favSeriesIds.add(series.seriesId)
            isNowFav = true
        }
        saveSeries(favSeriesKey(), favSeriesCache)
        return isNowFav
    }

    @Synchronized
    fun getRecentSeries(): List<SeriesItem> {
        return recentSeriesCache.toList()
    }

    @Synchronized
    fun addSeriesToRecent(series: SeriesItem) {
        recentSeriesCache.removeAll { it.seriesId == series.seriesId }
        recentSeriesCache.add(0, series)
        if (recentSeriesCache.size > MAX_RECENT_ITEMS) {
            val trimmed = recentSeriesCache.take(MAX_RECENT_ITEMS).toMutableList()
            recentSeriesCache.clear()
            recentSeriesCache.addAll(trimmed)
        }
        saveSeries(recentSeriesKey(), recentSeriesCache)
    }

    private fun loadSeries(key: String): List<SeriesItem> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<SeriesItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SeriesItem(
                        seriesId = obj.getInt("seriesId"),
                        name = obj.getString("name"),
                        cover = obj.optString("cover", null),
                        rating = obj.optString("rating", null),
                        categoryId = obj.optString("categoryId", null)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveSeries(key: String, list: List<SeriesItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("seriesId", item.seriesId)
                put("name", item.name)
                put("cover", item.cover ?: "")
                put("rating", item.rating ?: "")
                put("categoryId", item.categoryId ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}
