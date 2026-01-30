package pics.spear.astral.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import pics.spear.astral.script.AstralScriptRuntime
import pics.spear.astral.util.PermissionUtils
import pics.spear.astral.ui.components.AstralScreen
import androidx.compose.ui.graphics.luminance
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import pics.spear.astral.R
import pics.spear.astral.ui.theme.AstralTheme

/**
 * Initialization Activity - Shows Node.js runtime setup progress
 * 
 * This screen appears on first launch when PRoot and Alpine rootfs
 * need to be downloaded and extracted.
 */
class InitializationActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AstralTheme {
                InitializationScreen(
                    onComplete = {
                        if (PermissionUtils.hasNotificationAccess(this) && PermissionUtils.hasStorageAccess(this)) {
                            startActivity(android.content.Intent(this, MainActivity::class.java))
                        } else {
                            startActivity(android.content.Intent(this, PermissionActivity::class.java))
                        }
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun InitializationScreen(onComplete: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("VANGUARD_CORE 초기화 중...") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        try {
            status = "NEURAL_V8_BRIDGE 로딩 중..."
            val runtime = AstralScriptRuntime(context)
            
            while(progress < 0.95f) {
                progress += 0.015f
                delay(40)
            }
            
            val result = runtime.initialize()
            
            if (result.isSuccess) {
                progress = 1f
                status = "SYSTEM_LEVEL_READY"
                delay(1000)
                onComplete()
            } else {
                throw result.exceptionOrNull() ?: Exception("Bridge failure")
            }
            
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Initialization", "Setup failed", e)
            error = e.message ?: "호스트 연결에 실패했습니다"
            status = "CRITICAL_SYSTEM_ERROR"
        }
    }
    
    AstralScreen {
        Column(
            modifier = Modifier.fillMaxSize().padding(36.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "ASTRAL",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "SYSTEM INITIALIZATION",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelLarge
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            if (error == null) {
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val imageLoader = ImageLoader.Builder(context)
                    .components {
                        add(SvgDecoder.Factory())
                        add(GifDecoder.Factory())
                    }
                    .build()
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if (isDark) R.raw.astral_loading_dark else R.raw.astral_loading_light)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.primary
                                    )
                                ),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(18.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = status.uppercase(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "TARGET_ENV: PRODUCTION_STATION\nVERSION: 1.0.0\nRUNTIME: ACTIVE\nUPLINK: ENCRYPTED",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace
                )
                
            } else {
                Text(
                    text = "치명적 오류 발생",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("시스템 종료", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
