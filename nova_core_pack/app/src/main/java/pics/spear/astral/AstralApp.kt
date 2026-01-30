package pics.spear.astral

import android.app.Application
import android.app.Activity
import android.os.Bundle
import pics.spear.astral.nativebridge.ActivityProvider
import pics.spear.astral.script.AstralScriptRuntime
import pics.spear.astral.runtime.ProotManager
import pics.spear.astral.ui.editor.TextMateRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AstralApp : Application() {
    companion object {
        init {
            android.util.Log.d("AstralApp", "Static block - Class Loaded")
        }
    }

    override fun onCreate() {
        android.util.Log.d("AstralApp", "onCreate - START")
        super.onCreate()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("AstralCrash", "FATAL UNCAUGHT EXCEPTION in thread ${thread.name}", throwable)
        }

        val prootManager = ProotManager(this)
        if (prootManager.isInitialized()) {
            CoroutineScope(Dispatchers.IO).launch {
                android.util.Log.d("AstralApp", "Initializing Node.js Runtime...")
                try {
                    val runtime = AstralScriptRuntime(this@AstralApp)
                    val result = runtime.initialize()
                    if (result.isSuccess) {
                        android.util.Log.d("AstralApp", "Node.js Runtime initialized successfully!")
                    } else {
                        android.util.Log.e("AstralApp", "Node.js initialization failed: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("AstralApp", "Fatal error during Node.js initialization", e)
                }
            }
        } else {
            android.util.Log.d("AstralApp", "Runtime not initialized yet - defer initialization")
        }

        TextMateRegistry.ensureInitialized(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                ActivityProvider.set(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                val current = ActivityProvider.get()
                if (current === activity) ActivityProvider.set(null)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
