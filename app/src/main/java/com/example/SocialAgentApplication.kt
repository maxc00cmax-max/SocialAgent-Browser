package com.example

import android.app.Application
import android.content.Context
import android.system.Os
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.backend.AppContainer

class SocialAgentApplication : Application(), ImageLoaderFactory {

    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }

    companion object {
        init {
            configureMesaEnvironment()
        }

        private fun configureMesaEnvironment() {
            try {
                Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                Os.setenv("GALLIUM_DRIVER", "llvmpipe", true)
                Os.setenv("MESA_LOADER_DRIVER_OVERRIDE", "swrast", true)
                Os.setenv("MESA_DEBUG", "silent", true)
                Os.setenv("MESA_LOG_LEVEL", "none", true)
                Os.setenv("MESA_LOG_FILE", "/dev/null", true)
                Os.setenv("EGL_LOG_LEVEL", "fatal", true)
                Os.setenv("VK_LOADER_DEBUG", "none", true)
                Os.setenv("MESA_VK_DEVICE_SELECT_FORCE_DEFAULT_DEVICE", "1", true)
            } catch (t: Throwable) {
                Log.w("SocialAgentApp", "Mesa env setup: ${t.message}")
            }
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        configureMesaEnvironment()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .allowHardware(false)
            .crossfade(true)
            .build()
    }
}
