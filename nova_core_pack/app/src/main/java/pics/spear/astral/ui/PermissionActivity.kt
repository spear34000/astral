package pics.spear.astral.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pics.spear.astral.util.PermissionUtils
import pics.spear.astral.ui.theme.AstralTheme
import pics.spear.astral.ui.components.AstralScreen
import pics.spear.astral.ui.components.VanguardCard
import pics.spear.astral.util.AstralStorage
import java.io.File
import androidx.core.app.ActivityCompat
import android.Manifest
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class PermissionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstralTheme {
                PermissionScreen(
                    onGrant = {
                        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    },
                    onGrantStorage = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            startActivity(PermissionUtils.storageAccessSettingsIntent(this))
                        } else {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                1201
                            )
                        }
                    },
                    onContinue = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionUtils.hasNotificationAccess(this) && PermissionUtils.hasStorageAccess(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

@Composable
fun PermissionScreen(onGrant: () -> Unit, onGrantStorage: () -> Unit, onContinue: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasNotification by remember { mutableStateOf(PermissionUtils.hasNotificationAccess(context)) }
    var hasStorage by remember { mutableStateOf(PermissionUtils.hasStorageAccess(context)) }
    val storagePath = remember {
        File(AstralStorage.botScriptsDir(context), "").absolutePath
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotification = PermissionUtils.hasNotificationAccess(context)
                hasStorage = PermissionUtils.hasStorageAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AstralScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("권한이 필요합니다", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    "알림 접근 권한이 있어야 메시지를 읽고 봇이 정상 동작합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                VanguardCard(modifier = Modifier.fillMaxWidth(), borderAlpha = 0.2f) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PermissionStep(
                            icon = Icons.Default.Warning,
                            title = "알림 접근 허용",
                            body = "설정 > 알림 접근 > Astral"
                        )
                        PermissionStep(
                            icon = Icons.Default.Warning,
                            title = "저장소 권한 허용",
                            body = "외부 저장소 /sdcard/astral 사용"
                        )
                        PermissionStep(
                            icon = Icons.Default.CheckCircle,
                            title = "앱으로 돌아오기",
                            body = "자동으로 확인 후 계속 진행됩니다"
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                VanguardCard(modifier = Modifier.fillMaxWidth(), borderAlpha = 0.2f) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("스크립트 저장 위치", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = storagePath,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                PermissionStatusRow(
                    label = "알림 접근",
                    enabled = hasNotification
                )
                PermissionStatusRow(
                    label = "저장소 권한",
                    enabled = hasStorage
                )

                Button(
                    onClick = onGrantStorage,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("저장소 권한 설정", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("알림 권한 설정", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onContinue) {
                    Text("권한 없이 계속", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun PermissionStep(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionStatusRow(label: String, enabled: Boolean) {
    val statusText = if (enabled) "허용됨" else "필요"
    val statusColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.weight(1f))
        Text(statusText, style = MaterialTheme.typography.labelMedium, color = statusColor, fontFamily = FontFamily.Monospace)
    }
}
