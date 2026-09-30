package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountSession
import com.example.data.model.AccountType
import com.example.data.model.LiveCategory
import com.example.data.model.LiveChannel
import com.example.data.model.SeriesCategory
import com.example.data.model.SeriesDetail
import com.example.data.model.SeriesItem
import com.example.data.model.VodCategory
import com.example.data.model.VodDetail
import com.example.data.model.VodMovie
import com.example.data.repository.IptvRepository
import com.example.data.repository.IptvResult
import com.example.data.session.SessionManager
import com.example.ui.i18n.AppText
import com.example.ui.i18n.LocalizedStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IptvViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    val favHistoryManager = com.example.data.session.FavoritesHistoryManager(application)
    private val repository = IptvRepository()

    companion object {
        const val ID_FAVORITES = "CAT_FAVORITES"
        const val ID_RECENTS = "CAT_RECENTS"

        val LIVE_FAV_CATEGORY = LiveCategory(categoryId = ID_FAVORITES, categoryName = "المفضلة")
        val LIVE_RECENT_CATEGORY = LiveCategory(categoryId = ID_RECENTS, categoryName = "آخر المشاهدات")

        val VOD_FAV_CATEGORY = VodCategory(categoryId = ID_FAVORITES, categoryName = "المفضلة")
        val VOD_RECENT_CATEGORY = VodCategory(categoryId = ID_RECENTS, categoryName = "آخر المشاهدات")

        val SERIES_FAV_CATEGORY = SeriesCategory(categoryId = ID_FAVORITES, categoryName = "المفضلة")
        val SERIES_RECENT_CATEGORY = SeriesCategory(categoryId = ID_RECENTS, categoryName = "آخر المشاهدات")
    }

    private val _activeAccount = MutableStateFlow<AccountSession?>(null)
    val activeAccount: StateFlow<AccountSession?> = _activeAccount.asStateFlow()

    private val _savedAccounts = MutableStateFlow<List<AccountSession>>(emptyList())
    val savedAccounts: StateFlow<List<AccountSession>> = _savedAccounts.asStateFlow()

    // Settings & Localization
    private val _currentLanguage = MutableStateFlow(sessionManager.getLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _appText = MutableStateFlow(LocalizedStrings.get(sessionManager.getLanguage()))
    val appText: StateFlow<AppText> = _appText.asStateFlow()

    private val _streamFormat = MutableStateFlow(sessionManager.getStreamFormat())
    val streamFormat: StateFlow<String> = _streamFormat.asStateFlow()

    private val _audioType = MutableStateFlow(sessionManager.getAudioType())
    val audioType: StateFlow<String> = _audioType.asStateFlow()

    fun setLanguage(lang: String) {
        sessionManager.setLanguage(lang)
        _currentLanguage.value = lang
        _appText.value = LocalizedStrings.get(lang)
    }

    fun setStreamFormat(format: String) {
        sessionManager.setStreamFormat(format)
        _streamFormat.value = format
    }

    fun setAudioType(type: String) {
        sessionManager.setAudioType(type)
        _audioType.value = type
    }

    // Live TV State
    private val _liveCategories = MutableStateFlow<List<LiveCategory>>(emptyList())
    val liveCategories: StateFlow<List<LiveCategory>> = _liveCategories.asStateFlow()

    private val _selectedLiveCategory = MutableStateFlow<LiveCategory?>(null)
    val selectedLiveCategory: StateFlow<LiveCategory?> = _selectedLiveCategory.asStateFlow()

    private val _liveChannels = MutableStateFlow<List<LiveChannel>>(emptyList())
    val liveChannels: StateFlow<List<LiveChannel>> = _liveChannels.asStateFlow()

    private val _selectedLiveChannel = MutableStateFlow<LiveChannel?>(null)
    val selectedLiveChannel: StateFlow<LiveChannel?> = _selectedLiveChannel.asStateFlow()

    // VOD & Series State
    private val _vodCategories = MutableStateFlow<List<VodCategory>>(emptyList())
    val vodCategories: StateFlow<List<VodCategory>> = _vodCategories.asStateFlow()

    private val _selectedVodCategory = MutableStateFlow<VodCategory?>(null)
    val selectedVodCategory: StateFlow<VodCategory?> = _selectedVodCategory.asStateFlow()

    private val _vodMovies = MutableStateFlow<List<VodMovie>>(emptyList())
    val vodMovies: StateFlow<List<VodMovie>> = _vodMovies.asStateFlow()

    private val _seriesCategories = MutableStateFlow<List<SeriesCategory>>(emptyList())
    val seriesCategories: StateFlow<List<SeriesCategory>> = _seriesCategories.asStateFlow()

    private val _selectedSeriesCategory = MutableStateFlow<SeriesCategory?>(null)
    val selectedSeriesCategory: StateFlow<SeriesCategory?> = _selectedSeriesCategory.asStateFlow()

    private val _seriesList = MutableStateFlow<List<SeriesItem>>(emptyList())
    val seriesList: StateFlow<List<SeriesItem>> = _seriesList.asStateFlow()

    // Per-category in-memory cache to ensure instant switching and prevent re-downloading
    private val liveCategoryCache = mutableMapOf<String, List<LiveChannel>>()
    private val vodCategoryCache = mutableMapOf<String, List<VodMovie>>()
    private val seriesCategoryCache = mutableMapOf<String, List<SeriesItem>>()

    // Counts
    private val _liveCount = MutableStateFlow(0)
    val liveCount: StateFlow<Int> = _liveCount.asStateFlow()

    private val _vodCount = MutableStateFlow(0)
    val vodCount: StateFlow<Int> = _vodCount.asStateFlow()

    private val _seriesCount = MutableStateFlow(0)
    val seriesCount: StateFlow<Int> = _seriesCount.asStateFlow()

    // UI Loading & Status
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLiveChannelsLoading = MutableStateFlow(false)
    val isLiveChannelsLoading: StateFlow<Boolean> = _isLiveChannelsLoading.asStateFlow()

    private val _isVodLoading = MutableStateFlow(false)
    val isVodLoading: StateFlow<Boolean> = _isVodLoading.asStateFlow()

    private val _isSeriesLoading = MutableStateFlow(false)
    val isSeriesLoading: StateFlow<Boolean> = _isSeriesLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _loginInProgress = MutableStateFlow(false)
    val loginInProgress: StateFlow<Boolean> = _loginInProgress.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Cache for M3U channels
    private var cachedM3uChannels: List<LiveChannel> = emptyList()

    init {
        refreshAccounts()
        val currentActive = sessionManager.getActiveAccount()
        _activeAccount.value = currentActive
        if (currentActive != null) {
            loadAccountContent(currentActive)
        }
    }

    fun refreshAccounts() {
        _savedAccounts.value = sessionManager.getAllAccounts()
        _activeAccount.value = sessionManager.getActiveAccount()
    }

    fun loginXtream(
        serverUrl: String,
        username: String,
        password: String,
        accountName: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _loginInProgress.value = true
            _loginError.value = null
            _statusMessage.value = "جارٍ الاتصال بالخادم والتحقق من البيانات..."

            when (val result = repository.authenticateXtream(serverUrl, username, password, accountName)) {
                is IptvResult.Success -> {
                    val session = result.data
                    sessionManager.saveAccount(session)
                    _activeAccount.value = session
                    refreshAccounts()
                    _loginInProgress.value = false
                    _statusMessage.value = null
                    loadAccountContent(session)
                    onSuccess()
                }
                is IptvResult.Error -> {
                    _loginInProgress.value = false
                    _statusMessage.value = null
                    _loginError.value = result.message
                }
            }
        }
    }

    fun loadM3u(
        url: String,
        name: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _loginInProgress.value = true
            _loginError.value = null
            _statusMessage.value = "جارٍ تحميل قائمة القنوات وقراءتها..."

            when (val result = repository.loadM3uPlaylist(url, name)) {
                is IptvResult.Success -> {
                    val (session, channels) = result.data
                    cachedM3uChannels = channels
                    sessionManager.saveAccount(session)
                    _activeAccount.value = session
                    refreshAccounts()
                    _loginInProgress.value = false
                    _statusMessage.value = null

                    // Set up categories from M3U
                    val groups = channels.mapNotNull { it.categoryId }.distinct()
                    val catList = groups.mapIndexed { idx, grp -> LiveCategory(categoryId = grp, categoryName = grp) }
                    val allCats = listOf(LIVE_FAV_CATEGORY, LIVE_RECENT_CATEGORY) + catList
                    _liveCategories.value = allCats
                    _liveChannels.value = channels
                    _liveCount.value = channels.size
                    _vodCount.value = 0
                    _seriesCount.value = 0
                    _selectedLiveCategory.value = catList.firstOrNull() ?: allCats.firstOrNull()
                    _selectedLiveChannel.value = channels.firstOrNull()

                    onSuccess()
                }
                is IptvResult.Error -> {
                    _loginInProgress.value = false
                    _statusMessage.value = null
                    _loginError.value = result.message
                }
            }
        }
    }

    fun switchAccount(account: AccountSession) {
        sessionManager.setActiveAccountId(account.id)
        _activeAccount.value = account
        refreshAccounts()
        loadAccountContent(account)
    }

    fun deleteAccount(accountId: String) {
        sessionManager.deleteAccount(accountId)
        refreshAccounts()
        val current = _activeAccount.value
        if (current?.id == accountId) {
            val next = sessionManager.getActiveAccount()
            _activeAccount.value = next
            if (next != null) {
                loadAccountContent(next)
            } else {
                clearContent()
            }
        }
    }

    fun logout() {
        sessionManager.clearActiveSession()
        _activeAccount.value = null
        clearContent()
    }

    private fun clearContent() {
        _liveCategories.value = emptyList()
        _liveChannels.value = emptyList()
        _selectedLiveCategory.value = null
        _selectedLiveChannel.value = null
        _vodCategories.value = emptyList()
        _selectedVodCategory.value = null
        _vodMovies.value = emptyList()
        _seriesCategories.value = emptyList()
        _selectedSeriesCategory.value = null
        _seriesList.value = emptyList()
        liveCategoryCache.clear()
        vodCategoryCache.clear()
        seriesCategoryCache.clear()
        _liveCount.value = 0
        _vodCount.value = 0
        _seriesCount.value = 0
        _errorMessage.value = null
    }

    fun loadAccountContent(session: AccountSession) {
        if (session.type == AccountType.M3U) {
            if (cachedM3uChannels.isNotEmpty()) {
                val groups = cachedM3uChannels.mapNotNull { it.categoryId }.distinct()
                val catList = groups.map { LiveCategory(it, it) }
                _liveCategories.value = catList
                _liveChannels.value = cachedM3uChannels
                _liveCount.value = cachedM3uChannels.size
                _selectedLiveCategory.value = catList.firstOrNull()
                _selectedLiveChannel.value = cachedM3uChannels.firstOrNull()
            } else {
                loadM3u(session.m3uUrl, session.name) {}
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Fetch live categories
            when (val catRes = repository.getLiveCategories(session)) {
                is IptvResult.Success -> {
                    val fullList = listOf(LIVE_FAV_CATEGORY, LIVE_RECENT_CATEGORY) + catRes.data
                    _liveCategories.value = fullList
                    val firstCat = catRes.data.firstOrNull() ?: fullList.firstOrNull()
                    _selectedLiveCategory.value = firstCat
                    if (firstCat != null) {
                        selectLiveCategory(firstCat)
                    }
                }
                is IptvResult.Error -> {
                    _errorMessage.value = catRes.message
                }
            }

            // Fetch Live Streams overall to populate total count and pre-cache all categories
            when (val liveRes = repository.getLiveStreams(session)) {
                is IptvResult.Success -> {
                    _liveCount.value = liveRes.data.size
                    // Pre-index all channels by category for 0ms instantaneous switching!
                    val grouped = liveRes.data.groupBy { it.categoryId ?: "" }
                    grouped.forEach { (catId, chList) ->
                        liveCategoryCache[catId] = chList
                    }
                    val activeCat = _selectedLiveCategory.value
                    if (activeCat != null && _liveChannels.value.isEmpty()) {
                        val channelsForCat = liveCategoryCache[activeCat.categoryId] ?: emptyList()
                        _liveChannels.value = channelsForCat
                        _selectedLiveChannel.value = channelsForCat.firstOrNull()
                    }
                }
                is IptvResult.Error -> {}
            }

            _isLoading.value = false

            // Preload VOD and Series in background so remote navigation from Dashboard is instantaneous
            launch {
                loadVodContentIfNeeded(forceRefresh = false)
            }
            launch {
                loadSeriesContentIfNeeded(forceRefresh = false)
            }
        }
    }

    /**
     * Load VOD categories and only the first category's movies initially for maximum speed.
     */
    fun loadVodContentIfNeeded(forceRefresh: Boolean = false) {
        val session = _activeAccount.value ?: return
        if (session.type == AccountType.M3U) return
        if (!forceRefresh && _vodCategories.value.isNotEmpty() && _vodMovies.value.isNotEmpty()) return

        viewModelScope.launch {
            _isVodLoading.value = true
            when (val vodCatRes = repository.getVodCategories(session)) {
                is IptvResult.Success -> {
                    val cats = vodCatRes.data
                    val fullList = listOf(VOD_FAV_CATEGORY, VOD_RECENT_CATEGORY) + cats
                    _vodCategories.value = fullList
                    val firstCat = cats.firstOrNull() ?: fullList.firstOrNull()
                    _selectedVodCategory.value = firstCat
                    if (firstCat != null) {
                        selectVodCategory(firstCat)
                    } else {
                        _isVodLoading.value = false
                    }
                }
                is IptvResult.Error -> {
                    _isVodLoading.value = false
                }
            }
        }
    }

    /**
     * Fast category selection for Movies: fetches only this category with caching
     */
    fun selectVodCategory(category: VodCategory) {
        _selectedVodCategory.value = category
        if (category.categoryId == ID_FAVORITES) {
            _vodMovies.value = favHistoryManager.getFavoriteMovies()
            _isVodLoading.value = false
            return
        }
        if (category.categoryId == ID_RECENTS) {
            _vodMovies.value = favHistoryManager.getRecentMovies()
            _isVodLoading.value = false
            return
        }

        val cached = vodCategoryCache[category.categoryId]
        if (cached != null) {
            _vodMovies.value = cached
            return
        }

        val session = _activeAccount.value ?: return
        viewModelScope.launch {
            _isVodLoading.value = true
            when (val vodRes = repository.getVodStreams(session, category.categoryId)) {
                is IptvResult.Success -> {
                    vodCategoryCache[category.categoryId] = vodRes.data
                    _vodMovies.value = vodRes.data
                    if (_vodCount.value == 0) {
                        _vodCount.value = vodRes.data.size
                    }
                }
                is IptvResult.Error -> {
                    _vodMovies.value = emptyList()
                }
            }
            _isVodLoading.value = false
        }
    }

    /**
     * Load Series categories and only the first category's series initially for maximum speed.
     */
    fun loadSeriesContentIfNeeded(forceRefresh: Boolean = false) {
        val session = _activeAccount.value ?: return
        if (session.type == AccountType.M3U) return
        if (!forceRefresh && _seriesCategories.value.isNotEmpty() && _seriesList.value.isNotEmpty()) return

        viewModelScope.launch {
            _isSeriesLoading.value = true
            when (val serCatRes = repository.getSeriesCategories(session)) {
                is IptvResult.Success -> {
                    val cats = serCatRes.data
                    val fullList = listOf(SERIES_FAV_CATEGORY, SERIES_RECENT_CATEGORY) + cats
                    _seriesCategories.value = fullList
                    val firstCat = cats.firstOrNull() ?: fullList.firstOrNull()
                    _selectedSeriesCategory.value = firstCat
                    if (firstCat != null) {
                        selectSeriesCategory(firstCat)
                    } else {
                        _isSeriesLoading.value = false
                    }
                }
                is IptvResult.Error -> {
                    _isSeriesLoading.value = false
                }
            }
        }
    }

    /**
     * Fast category selection for Series: fetches only this category with caching
     */
    fun selectSeriesCategory(category: SeriesCategory) {
        _selectedSeriesCategory.value = category
        if (category.categoryId == ID_FAVORITES) {
            _seriesList.value = favHistoryManager.getFavoriteSeries()
            _isSeriesLoading.value = false
            return
        }
        if (category.categoryId == ID_RECENTS) {
            _seriesList.value = favHistoryManager.getRecentSeries()
            _isSeriesLoading.value = false
            return
        }

        val cached = seriesCategoryCache[category.categoryId]
        if (cached != null) {
            _seriesList.value = cached
            return
        }

        val session = _activeAccount.value ?: return
        viewModelScope.launch {
            _isSeriesLoading.value = true
            when (val serRes = repository.getSeries(session, category.categoryId)) {
                is IptvResult.Success -> {
                    seriesCategoryCache[category.categoryId] = serRes.data
                    _seriesList.value = serRes.data
                    if (_seriesCount.value == 0) {
                        _seriesCount.value = serRes.data.size
                    }
                }
                is IptvResult.Error -> {
                    _seriesList.value = emptyList()
                }
            }
            _isSeriesLoading.value = false
        }
    }

    fun selectLiveCategory(category: LiveCategory) {
        _selectedLiveCategory.value = category
        if (category.categoryId == ID_FAVORITES) {
            val favs = favHistoryManager.getFavoriteChannels()
            _liveChannels.value = favs
            _selectedLiveChannel.value = favs.firstOrNull()
            return
        }
        if (category.categoryId == ID_RECENTS) {
            val recs = favHistoryManager.getRecentChannels()
            _liveChannels.value = recs
            _selectedLiveChannel.value = recs.firstOrNull()
            return
        }

        val session = _activeAccount.value ?: return
        if (session.type == AccountType.M3U) {
            val filtered = cachedM3uChannels.filter { it.categoryId == category.categoryId }
            _liveChannels.value = filtered
            _selectedLiveChannel.value = filtered.firstOrNull()
            return
        }

        // Instant in-memory cache lookup: 0ms latency
        val cached = liveCategoryCache[category.categoryId]
        if (cached != null) {
            _liveChannels.value = cached
            _selectedLiveChannel.value = cached.firstOrNull()
            return
        }

        loadLiveChannelsForCategory(session, category.categoryId)
    }

    fun selectLiveChannel(channel: LiveChannel) {
        _selectedLiveChannel.value = channel
        favHistoryManager.addChannelToRecent(channel)
    }

    fun markMovieWatched(movie: VodMovie) {
        favHistoryManager.addMovieToRecent(movie)
    }

    fun markSeriesWatched(series: SeriesItem) {
        favHistoryManager.addSeriesToRecent(series)
    }

    fun toggleChannelFavorite(channel: LiveChannel): Boolean {
        val isFav = favHistoryManager.toggleChannelFavorite(channel)
        if (_selectedLiveCategory.value?.categoryId == ID_FAVORITES) {
            _liveChannels.value = favHistoryManager.getFavoriteChannels()
        }
        return isFav
    }

    fun getChannelDisplayName(channel: LiveChannel): String {
        return favHistoryManager.getCustomChannelName(channel.streamId) ?: channel.name
    }

    fun renameLiveChannel(channel: LiveChannel, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        favHistoryManager.setCustomChannelName(channel.streamId, trimmed)
        _liveChannels.value = _liveChannels.value.map {
            if (it.streamId == channel.streamId) it.copy(name = trimmed) else it
        }
        if (_selectedLiveChannel.value?.streamId == channel.streamId) {
            _selectedLiveChannel.value = _selectedLiveChannel.value?.copy(name = trimmed)
        }
    }

    fun isChannelFavorite(channelId: Int): Boolean = favHistoryManager.isChannelFavorite(channelId)

    fun toggleMovieFavorite(movie: VodMovie): Boolean {
        val isFav = favHistoryManager.toggleMovieFavorite(movie)
        if (_selectedVodCategory.value?.categoryId == ID_FAVORITES) {
            _vodMovies.value = favHistoryManager.getFavoriteMovies()
        }
        return isFav
    }

    fun isMovieFavorite(movieId: Int): Boolean = favHistoryManager.isMovieFavorite(movieId)

    fun toggleSeriesFavorite(series: SeriesItem): Boolean {
        val isFav = favHistoryManager.toggleSeriesFavorite(series)
        if (_selectedSeriesCategory.value?.categoryId == ID_FAVORITES) {
            _seriesList.value = favHistoryManager.getFavoriteSeries()
        }
        return isFav
    }

    fun isSeriesFavorite(seriesId: Int): Boolean = favHistoryManager.isSeriesFavorite(seriesId)

    fun playNextLiveChannel(): LiveChannel? {
        val list = _liveChannels.value
        if (list.isEmpty()) return null
        val current = _selectedLiveChannel.value
        val currentIndex = if (current != null) list.indexOfFirst { it.streamId == current.streamId } else -1
        val nextIndex = if (currentIndex in 0 until list.size - 1) currentIndex + 1 else 0
        val nextChannel = list[nextIndex]
        _selectedLiveChannel.value = nextChannel
        return nextChannel
    }

    fun playPreviousLiveChannel(): LiveChannel? {
        val list = _liveChannels.value
        if (list.isEmpty()) return null
        val current = _selectedLiveChannel.value
        val currentIndex = if (current != null) list.indexOfFirst { it.streamId == current.streamId } else -1
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        val prevChannel = list[prevIndex]
        _selectedLiveChannel.value = prevChannel
        return prevChannel
    }

    fun playChannelByNumber(number: Int): LiveChannel? {
        val list = _liveChannels.value
        if (list.isEmpty()) return null
        val matched = list.firstOrNull { it.num == number }
            ?: list.getOrNull(number - 1)
            ?: list.firstOrNull { it.streamId == number }
        if (matched != null) {
            _selectedLiveChannel.value = matched
            return matched
        }
        return null
    }

    private fun loadLiveChannelsForCategory(session: AccountSession, categoryId: String?) {
        viewModelScope.launch {
            _isLiveChannelsLoading.value = true
            when (val res = repository.getLiveStreams(session, categoryId)) {
                is IptvResult.Success -> {
                    if (categoryId != null) {
                        liveCategoryCache[categoryId] = res.data
                    }
                    if (_selectedLiveCategory.value?.categoryId == categoryId) {
                        _liveChannels.value = res.data
                        _selectedLiveChannel.value = res.data.firstOrNull()
                    }
                }
                is IptvResult.Error -> {
                    if (_selectedLiveCategory.value?.categoryId == categoryId) {
                        _errorMessage.value = res.message
                    }
                }
            }
            _isLiveChannelsLoading.value = false
        }
    }

    fun getLiveStreamUrl(channel: LiveChannel): String? {
        if (!channel.directSourceUrl.isNullOrBlank()) {
            return channel.directSourceUrl
        }
        val session = _activeAccount.value ?: return null
        if (session.type == AccountType.XTREAM) {
            val server = session.serverUrl.trimEnd('/')
            val ext = if (_streamFormat.value == "ts") "ts" else "m3u8"
            return "$server/live/${session.username}/${session.password}/${channel.streamId}.$ext"
        }
        return null
    }

    fun getVodStreamUrl(movie: VodMovie): String? {
        val session = _activeAccount.value ?: return null
        if (session.type == AccountType.XTREAM) {
            val server = session.serverUrl.trimEnd('/')
            val ext = movie.containerExtension ?: "mp4"
            return "$server/movie/${session.username}/${session.password}/${movie.streamId}.$ext"
        }
        return null
    }

    fun getVodStreamUrlFromId(streamId: Int, extension: String = "mp4"): String? {
        val session = _activeAccount.value ?: return null
        if (session.type == AccountType.XTREAM) {
            val server = session.serverUrl.trimEnd('/')
            return "$server/movie/${session.username}/${session.password}/$streamId.$extension"
        }
        return null
    }

    fun getSeriesStreamUrl(seriesItem: SeriesItem): String? {
        val session = _activeAccount.value ?: return null
        if (session.type == AccountType.XTREAM) {
            val server = session.serverUrl.trimEnd('/')
            return "$server/series/${session.username}/${session.password}/${seriesItem.seriesId}.mp4"
        }
        return null
    }

    fun getEpisodeStreamUrl(episodeId: String, extension: String = "mp4"): String? {
        val session = _activeAccount.value ?: return null
        if (session.type == AccountType.XTREAM) {
            val server = session.serverUrl.trimEnd('/')
            return "$server/series/${session.username}/${session.password}/$episodeId.$extension"
        }
        return null
    }

    suspend fun fetchSeriesDetails(seriesId: Int): IptvResult<SeriesDetail> {
        val session = _activeAccount.value ?: return IptvResult.Error("لا يوجد حساب نشط")
        return repository.getSeriesInfo(session, seriesId)
    }

    suspend fun fetchVodDetails(vodId: Int): IptvResult<VodDetail> {
        val session = _activeAccount.value ?: return IptvResult.Error("لا يوجد حساب نشط")
        return repository.getVodInfo(session, vodId)
    }

    fun clearErrors() {
        _errorMessage.value = null
        _loginError.value = null
    }
}
