package com.echo.tv

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Echo TV.
 * Uses Hilt for dependency injection, sharing modules with the mobile app.
 */
@HiltAndroidApp
class EchoTvApplication : Application()
