package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.SeriesItem
import com.example.data.model.VodMovie
import com.example.ui.screens.LiveTvScreen
import com.example.ui.screens.M3uLoadScreen
import com.example.ui.screens.MovieDetailScreen
import com.example.ui.screens.MoviesScreen
import com.example.ui.screens.SavedAccountsScreen
import com.example.ui.screens.SeriesDetailScreen
import com.example.ui.screens.SeriesScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TvDashboardScreen
import com.example.ui.screens.TvPlayerScreen
import com.example.ui.screens.TvPortalScreen
import com.example.ui.screens.TvSettingsScreen
import com.example.ui.screens.XtreamLoginScreen
import com.example.ui.theme.MluonaTheme
import com.example.ui.theme.TvBackground
import com.example.ui.viewmodel.IptvViewModel

enum class TvScreen {
  SPLASH,
  PORTAL,
  XTREAM_LOGIN,
  M3U_LOAD,
  SAVED_ACCOUNTS,
  DASHBOARD,
  LIVE_TV,
  MOVIES,
  MOVIE_DETAIL,
  SERIES,
  SERIES_DETAIL,
  PLAYER,
  SETTINGS
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Keep screen on to prevent the TV from sleeping, daydreaming, or closing the app on inactivity
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    enableEdgeToEdge()
    setContent {
      MluonaTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = TvBackground
        ) {
          MluonaTvApp()
        }
      }
    }
  }
}

@Composable
fun MluonaTvApp(
  viewModel: IptvViewModel = viewModel()
) {
  var currentScreen by remember { mutableStateOf(TvScreen.SPLASH) }
  var playerTitle by remember { mutableStateOf("البث المباشر") }
  var playerUrl by remember { mutableStateOf("") }
  var isPlayerLive by remember { mutableStateOf(false) }
  var playerChannelNumber by remember { mutableStateOf<Int?>(null) }
  var playerEpgInfo by remember { mutableStateOf<String?>(null) }
  var playerFrequencyInfo by remember { mutableStateOf<String?>(null) }

  var previousScreenBeforePlayer by remember { mutableStateOf(TvScreen.DASHBOARD) }

  // Media Detail Selection States
  var selectedMovieForDetail by remember { mutableStateOf<VodMovie?>(null) }
  var selectedSeriesForDetail by remember { mutableStateOf<SeriesItem?>(null) }

  // Direct instant screen rendering (no overlapping transitions to keep TV RAM and CPU completely free)
  when (currentScreen) {
      TvScreen.SPLASH -> {
        SplashScreen(
          onSplashComplete = {
            if (viewModel.activeAccount.value != null) {
              currentScreen = TvScreen.DASHBOARD
            } else {
              currentScreen = TvScreen.PORTAL
            }
          }
        )
      }
      TvScreen.PORTAL -> {
        TvPortalScreen(
          viewModel = viewModel,
          onNavigateToDashboard = {
            currentScreen = TvScreen.DASHBOARD
          },
          onNavigateToXtreamLogin = {
            currentScreen = TvScreen.XTREAM_LOGIN
          },
          onNavigateToM3u = {
            currentScreen = TvScreen.M3U_LOAD
          },
          onNavigateToUsers = {
            currentScreen = TvScreen.SAVED_ACCOUNTS
          },
          onBackToSplash = {
            currentScreen = TvScreen.SPLASH
          }
        )
      }
      TvScreen.XTREAM_LOGIN -> {
        XtreamLoginScreen(
          viewModel = viewModel,
          onSuccess = {
            currentScreen = TvScreen.DASHBOARD
          },
          onBack = {
            currentScreen = TvScreen.PORTAL
          }
        )
      }
      TvScreen.M3U_LOAD -> {
        M3uLoadScreen(
          viewModel = viewModel,
          onSuccess = {
            currentScreen = TvScreen.DASHBOARD
          },
          onBack = {
            currentScreen = TvScreen.PORTAL
          }
        )
      }
      TvScreen.SAVED_ACCOUNTS -> {
        SavedAccountsScreen(
          viewModel = viewModel,
          onSelectAccount = {
            currentScreen = TvScreen.DASHBOARD
          },
          onAddNew = {
            currentScreen = TvScreen.PORTAL
          },
          onBack = {
            if (viewModel.activeAccount.value != null) {
              currentScreen = TvScreen.DASHBOARD
            } else {
              currentScreen = TvScreen.PORTAL
            }
          }
        )
      }
      TvScreen.DASHBOARD -> {
        TvDashboardScreen(
          viewModel = viewModel,
          onNavigateToLiveTv = {
            currentScreen = TvScreen.LIVE_TV
          },
          onNavigateToMovies = {
            currentScreen = TvScreen.MOVIES
          },
          onNavigateToSeries = {
            currentScreen = TvScreen.SERIES
          },
          onNavigateToUsers = {
            currentScreen = TvScreen.SAVED_ACCOUNTS
          },
          onNavigateToSettings = {
            currentScreen = TvScreen.SETTINGS
          },
          onBack = {
            currentScreen = TvScreen.PORTAL
          }
        )
      }
      TvScreen.LIVE_TV -> {
        LiveTvScreen(
          viewModel = viewModel,
          onPlayChannel = { channel ->
            val streamUrl = viewModel.getLiveStreamUrl(channel)
            if (!streamUrl.isNullOrBlank()) {
              playerTitle = channel.name
              playerUrl = streamUrl
              isPlayerLive = true
              playerChannelNumber = channel.num
              playerEpgInfo = channel.epgChannelId ?: "البث الحي المباشر"
              playerFrequencyInfo = "FHD • 1080p • 50fps"
              previousScreenBeforePlayer = TvScreen.LIVE_TV
              currentScreen = TvScreen.PLAYER
            }
          },
          onBack = {
            currentScreen = TvScreen.DASHBOARD
          }
        )
      }
      TvScreen.MOVIES -> {
        MoviesScreen(
          viewModel = viewModel,
          onSelectMovie = { movie ->
            selectedMovieForDetail = movie
            currentScreen = TvScreen.MOVIE_DETAIL
          },
          onBack = {
            currentScreen = TvScreen.DASHBOARD
          }
        )
      }
      TvScreen.MOVIE_DETAIL -> {
        val movie = selectedMovieForDetail
        if (movie != null) {
          MovieDetailScreen(
            movie = movie,
            viewModel = viewModel,
            onPlayMovie = { vodDetail ->
              val streamUrl = viewModel.getVodStreamUrlFromId(vodDetail.streamId, vodDetail.containerExtension)
                ?: viewModel.getVodStreamUrl(movie)
              if (!streamUrl.isNullOrBlank()) {
                playerTitle = vodDetail.name
                playerUrl = streamUrl
                isPlayerLive = false
                playerChannelNumber = null
                playerEpgInfo = vodDetail.genre ?: "فيلم سينمائي"
                playerFrequencyInfo = "${vodDetail.containerExtension.uppercase()} • 1080p"
                previousScreenBeforePlayer = TvScreen.MOVIE_DETAIL
                currentScreen = TvScreen.PLAYER
              }
            },
            onBack = {
              currentScreen = TvScreen.MOVIES
            }
          )
        } else {
          currentScreen = TvScreen.MOVIES
        }
      }
      TvScreen.SERIES -> {
        SeriesScreen(
          viewModel = viewModel,
          onSelectSeries = { seriesItem ->
            selectedSeriesForDetail = seriesItem
            currentScreen = TvScreen.SERIES_DETAIL
          },
          onBack = {
            currentScreen = TvScreen.DASHBOARD
          }
        )
      }
      TvScreen.SERIES_DETAIL -> {
        val series = selectedSeriesForDetail
        if (series != null) {
          SeriesDetailScreen(
            seriesItem = series,
            viewModel = viewModel,
            onPlayEpisode = { episode, seriesTitle ->
              val streamUrl = viewModel.getEpisodeStreamUrl(episode.id, episode.containerExtension)
                ?: viewModel.getSeriesStreamUrl(series)
              if (!streamUrl.isNullOrBlank()) {
                playerTitle = "$seriesTitle - ${episode.title}"
                playerUrl = streamUrl
                isPlayerLive = false
                playerChannelNumber = null
                playerEpgInfo = "الموسم ${episode.season} • الحلقة ${episode.episodeNum}"
                playerFrequencyInfo = "${episode.containerExtension.uppercase()} • FHD"
                previousScreenBeforePlayer = TvScreen.SERIES_DETAIL
                currentScreen = TvScreen.PLAYER
              }
            },
            onBack = {
              currentScreen = TvScreen.SERIES
            }
          )
        } else {
          currentScreen = TvScreen.SERIES
        }
      }
      TvScreen.PLAYER -> {
        TvPlayerScreen(
          title = playerTitle,
          streamUrl = playerUrl,
          isLive = isPlayerLive,
          channelNumber = playerChannelNumber,
          epgInfo = playerEpgInfo,
          frequencyInfo = playerFrequencyInfo,
          viewModel = viewModel,
          onBack = {
            currentScreen = previousScreenBeforePlayer
          }
        )
      }
      TvScreen.SETTINGS -> {
        TvSettingsScreen(
          viewModel = viewModel,
          onBack = {
            currentScreen = TvScreen.DASHBOARD
          }
        )
      }
    }
}
