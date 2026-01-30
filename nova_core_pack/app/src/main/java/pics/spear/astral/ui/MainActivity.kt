package pics.spear.astral.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import pics.spear.astral.R
import pics.spear.astral.databinding.ActivityMainBinding
import pics.spear.astral.util.PermissionUtils
import android.content.Intent

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("MainActivity", "onCreate - START")
        super.onCreate(savedInstanceState)
        android.util.Log.d("MainActivity", "super.onCreate() completed")
        
        try {
            android.util.Log.d("MainActivity", "Inflating binding...")
            binding = ActivityMainBinding.inflate(layoutInflater)
            android.util.Log.d("MainActivity", "Binding inflated")
            
            android.util.Log.d("MainActivity", "Setting content view...")
            setContentView(binding.root)
            android.util.Log.d("MainActivity", "Content view set")
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Failed to inflate or set content", e)
            throw e
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(DashboardFragment())
                R.id.nav_scripts -> replaceFragment(BotsFragment())
                R.id.nav_logs -> replaceFragment(LogsFragment())
                R.id.nav_settings -> replaceFragment(SettingsFragment())
                R.id.nav_info -> replaceFragment(InfoFragment())
                else -> false
            }
        }

        if (savedInstanceState == null) {
            android.util.Log.d("MainActivity", "Checking system initialization...")
            val prootManager = pics.spear.astral.runtime.ProotManager(this)
            if (!prootManager.isInitialized()) {
                android.util.Log.d("MainActivity", "System not initialized - launching InitializationActivity")
                startActivity(Intent(this, InitializationActivity::class.java))
                finish()
                return
            }

            android.util.Log.d("MainActivity", "Checking permissions...")
            if (!PermissionUtils.hasNotificationAccess(this) || !PermissionUtils.hasStorageAccess(this)) {
                android.util.Log.d("MainActivity", "Permission missing - launching PermissionActivity")
                startActivity(Intent(this, PermissionActivity::class.java))
                finish()
                return
            }
            android.util.Log.d("MainActivity", "Checks passed")
            val startTab = intent.getIntExtra("select_tab", R.id.nav_home)
            binding.bottomNav.selectedItemId = startTab
        }
        android.util.Log.d("MainActivity", "onCreate - COMPLETE")
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        val startTab = intent.getIntExtra("select_tab", -1)
        if (startTab != -1) {
            binding.bottomNav.selectedItemId = startTab
        }
    }

    fun selectTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }
}


