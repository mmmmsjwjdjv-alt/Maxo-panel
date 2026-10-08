package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.SubscriptionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DRAGON", appName)
  }

  @Test
  fun `permanent subscription over 200 days`() {
    val details = SubscriptionUtils.calculateSubscription("TEST-KEY", "31-12-2028")
    assertTrue(details.isPermanent)
    assertEquals("PERMANENT", details.subscriptionLabel)
    assertEquals("PERMANENT", details.daysRemaining)
  }
}
