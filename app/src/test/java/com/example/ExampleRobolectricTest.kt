package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mluona IPTV", appName)
  }

  @Test
  fun `test session manager save and restore`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sessionManager = com.example.data.session.SessionManager(context)
    val testSession = com.example.data.model.AccountSession(
        id = "test-123",
        name = "My IPTV",
        type = com.example.data.model.AccountType.XTREAM,
        serverUrl = "http://myiptv.com:8080",
        username = "user1",
        password = "pass1"
    )
    sessionManager.saveAccount(testSession)

    val active = sessionManager.getActiveAccount()
    org.junit.Assert.assertNotNull(active)
    assertEquals("My IPTV", active?.name)
    assertEquals("http://myiptv.com:8080", active?.serverUrl)
    assertEquals("user1", active?.username)
  }
}
