package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.SecureConnectionForm
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ConnectionFormScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun connection_form_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SecureConnectionForm(
          profileName = "Production Replica",
          onProfileNameChange = {},
          host = "postgres.myorg.internal",
          onHostChange = {},
          port = "5432",
          onPortChange = {},
          database = "analytics_db",
          onDatabaseChange = {},
          username = "admin_readonly",
          onUsernameChange = {},
          password = "supersecretpassword",
          onPasswordChange = {},
          sslMode = "require",
          onSslModeChange = {},
          onSaveProfile = {},
          onConnect = {},
          isConnecting = false
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/connection_form.png")
  }
}
