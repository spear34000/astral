package pics.spear.astral.nativebridge

import android.app.Activity
import java.lang.ref.WeakReference

object ActivityProvider {
    @Volatile
    private var ref: WeakReference<Activity>? = null

    fun set(activity: Activity?) {
        ref = if (activity == null) null else WeakReference(activity)
    }

    fun get(): Activity? = ref?.get()
}



