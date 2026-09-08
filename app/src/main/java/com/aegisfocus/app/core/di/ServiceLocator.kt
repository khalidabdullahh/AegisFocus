package com.aegisfocus.app.core.di

import android.content.Context

object ServiceLocator {
    @Volatile
    private var container: AppContainer? = null

    fun getContainer(context: Context): AppContainer {
        return container ?: synchronized(this) {
            container ?: AppContainer(context.applicationContext).also { container = it }
        }
    }
}
