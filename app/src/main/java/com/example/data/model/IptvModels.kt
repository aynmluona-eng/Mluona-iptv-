package com.example.data.model

enum class AccountType {
    XTREAM,
    M3U
}

data class AccountSession(
    val id: String,
    val name: String,
    val type: AccountType,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val status: String = "Active",
    val expDate: String = "",
    val maxConnections: String = "1",
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

data class LiveCategory(
    val categoryId: String,
    val categoryName: String
)

data class LiveChannel(
    val streamId: Int,
    val num: Int? = null,
    val name: String,
    val streamIcon: String? = null,
    val categoryId: String? = null,
    val directSourceUrl: String? = null,
    val epgChannelId: String? = null
)

data class VodCategory(
    val categoryId: String,
    val categoryName: String
)

data class VodMovie(
    val streamId: Int,
    val name: String,
    val streamIcon: String? = null,
    val rating: String? = null,
    val categoryId: String? = null,
    val containerExtension: String? = "mp4",
    val directSourceUrl: String? = null
)

data class SeriesCategory(
    val categoryId: String,
    val categoryName: String
)

data class SeriesItem(
    val seriesId: Int,
    val name: String,
    val cover: String? = null,
    val rating: String? = null,
    val categoryId: String? = null,
    val plot: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null
)

data class SeriesEpisode(
    val id: String,
    val episodeNum: Int,
    val title: String,
    val containerExtension: String = "mp4",
    val info: String? = null,
    val season: Int = 1
)

data class SeriesSeason(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val episodes: List<SeriesEpisode> = emptyList()
)

data class SeriesDetail(
    val seriesId: Int,
    val name: String,
    val cover: String? = null,
    val plot: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val seasons: List<SeriesSeason> = emptyList()
)

data class VodDetail(
    val streamId: Int,
    val name: String,
    val cover: String? = null,
    val plot: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: String? = null,
    val duration: String? = null,
    val director: String? = null,
    val cast: String? = null,
    val containerExtension: String = "mp4"
)
