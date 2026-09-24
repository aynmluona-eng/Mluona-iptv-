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

    companion object {
        private const val KEY_FAV_CHANNELS = "key_fav_channels"
        private const val KEY_RECENT_CHANNELS = "key_recent_channels"

        private const val KEY_FAV_MOVIES = "key_fav_movies"
        private const val KEY_RECENT_MOVIES = "key_recent_movies"

        private const val KEY_FAV_SERIES = "key_fav_series"
        private const val KEY_RECENT_SERIES = "key_recent_series"

        private const val MAX_RECENT_ITEMS = 40
    }

    // ==========================================
    // LIVE CHANNELS
    // ==========================================
    fun getFavoriteChannels(): List<LiveChannel> {
        return loadChannels(KEY_FAV_CHANNELS)
    }

    fun isChannelFavorite(streamId: Int): Boolean {
        return getFavoriteChannels().any { it.streamId == streamId }
    }

    fun toggleChannelFavorite(channel: LiveChannel): Boolean {
        val list = getFavoriteChannels().toMutableList()
        val index = list.indexOfFirst { it.streamId == channel.streamId }
        val isNowFav: Boolean
        if (index >= 0) {
            list.removeAt(index)
            isNowFav = false
        } else {
            list.add(0, channel)
            isNowFav = true
        }
        saveChannels(KEY_FAV_CHANNELS, list)
        return isNowFav
    }

    fun getRecentChannels(): List<LiveChannel> {
        return loadChannels(KEY_RECENT_CHANNELS)
    }

    fun getCustomChannelName(streamId: Int): String? {
        return prefs.getString("custom_ch_name_$streamId", null)
    }

    fun setCustomChannelName(streamId: Int, newName: String) {
        prefs.edit().putString("custom_ch_name_$streamId", newName).apply()
    }

    fun addChannelToRecent(channel: LiveChannel) {
        val list = getRecentChannels().toMutableList()
        list.removeAll { it.streamId == channel.streamId }
        list.add(0, channel)
        if (list.size > MAX_RECENT_ITEMS) {
            saveChannels(KEY_RECENT_CHANNELS, list.take(MAX_RECENT_ITEMS))
        } else {
            saveChannels(KEY_RECENT_CHANNELS, list)
        }
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
    fun getFavoriteMovies(): List<VodMovie> {
        return loadMovies(KEY_FAV_MOVIES)
    }

    fun isMovieFavorite(streamId: Int): Boolean {
        return getFavoriteMovies().any { it.streamId == streamId }
    }

    fun toggleMovieFavorite(movie: VodMovie): Boolean {
        val list = getFavoriteMovies().toMutableList()
        val index = list.indexOfFirst { it.streamId == movie.streamId }
        val isNowFav: Boolean
        if (index >= 0) {
            list.removeAt(index)
            isNowFav = false
        } else {
            list.add(0, movie)
            isNowFav = true
        }
        saveMovies(KEY_FAV_MOVIES, list)
        return isNowFav
    }

    fun getRecentMovies(): List<VodMovie> {
        return loadMovies(KEY_RECENT_MOVIES)
    }

    fun addMovieToRecent(movie: VodMovie) {
        val list = getRecentMovies().toMutableList()
        list.removeAll { it.streamId == movie.streamId }
        list.add(0, movie)
        if (list.size > MAX_RECENT_ITEMS) {
            saveMovies(KEY_RECENT_MOVIES, list.take(MAX_RECENT_ITEMS))
        } else {
            saveMovies(KEY_RECENT_MOVIES, list)
        }
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
    fun getFavoriteSeries(): List<SeriesItem> {
        return loadSeries(KEY_FAV_SERIES)
    }

    fun isSeriesFavorite(seriesId: Int): Boolean {
        return getFavoriteSeries().any { it.seriesId == seriesId }
    }

    fun toggleSeriesFavorite(series: SeriesItem): Boolean {
        val list = getFavoriteSeries().toMutableList()
        val index = list.indexOfFirst { it.seriesId == series.seriesId }
        val isNowFav: Boolean
        if (index >= 0) {
            list.removeAt(index)
            isNowFav = false
        } else {
            list.add(0, series)
            isNowFav = true
        }
        saveSeries(KEY_FAV_SERIES, list)
        return isNowFav
    }

    fun getRecentSeries(): List<SeriesItem> {
        return loadSeries(KEY_RECENT_SERIES)
    }

    fun addSeriesToRecent(series: SeriesItem) {
        val list = getRecentSeries().toMutableList()
        list.removeAll { it.seriesId == series.seriesId }
        list.add(0, series)
        if (list.size > MAX_RECENT_ITEMS) {
            saveSeries(KEY_RECENT_SERIES, list.take(MAX_RECENT_ITEMS))
        } else {
            saveSeries(KEY_RECENT_SERIES, list)
        }
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
                        categoryId = obj.optString("categoryId", null),
                        plot = obj.optString("plot", null),
                        genre = obj.optString("genre", null),
                        releaseDate = obj.optString("releaseDate", null)
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
                put("plot", item.plot ?: "")
                put("genre", item.genre ?: "")
                put("releaseDate", item.releaseDate ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}
