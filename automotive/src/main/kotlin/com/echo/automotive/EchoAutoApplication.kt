package com.echo.automotive

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Echo Android Auto.
 * Uses Hilt for dependency injection, sharing modules with the mobile app.
 */
@HiltAndroidApp
class EchoAutoApplication : Application()
