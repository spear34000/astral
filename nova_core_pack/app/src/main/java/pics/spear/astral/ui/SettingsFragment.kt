package pics.spear.astral.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import pics.spear.astral.ui.theme.AstralTheme
import pics.spear.astral.ui.components.AstralHeader
import pics.spear.astral.ui.components.AstralScreen
import pics.spear.astral.ui.components.VanguardCard
import pics.spear.astral.prefs.AstralPrefs

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AstralTheme { SettingsScreen() }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var editorFontSize by remember { mutableStateOf(AstralPrefs.getEditorFontSize(context)) }
    var editorWordWrap by remember { mutableStateOf(AstralPrefs.getEditorWordWrap(context)) }
    var editorLineNumbers by remember { mutableStateOf(AstralPrefs.getEditorLineNumbers(context)) }
    AstralScreen {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            AstralHeader(
                title = "설정",
                subtitle = "자동화 엔진과 알림 동작을 조정합니다."
            )
            Spacer(modifier = Modifier.height(16.dp))

            VanguardCard(Modifier.fillMaxWidth(), borderAlpha = 0.2f) {
                Column(Modifier.padding(16.dp)) {
                    Text("엔진 상태", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusBadge(text = "자동 시작", active = true)
                        StatusBadge(text = "브리지 연결", active = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "핵심 설정") {
                SettingToggleRow(Icons.Default.Settings, "자동 시작", "기기 재시작 후 자동 실행", true)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), modifier = Modifier.padding(vertical = 10.dp))
                SettingToggleRow(Icons.Default.Notifications, "상태 표시", "실시간 작업 상태 표시", false)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), modifier = Modifier.padding(vertical = 10.dp))
                SettingToggleRow(Icons.Default.Lock, "보안 강화", "스크립트 실행 권한 강화", true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "에디터") {
                SettingSliderRow(
                    title = "글꼴 크기",
                    value = editorFontSize,
                    range = 12f..22f,
                    onValueChange = {
                        editorFontSize = it
                        AstralPrefs.setEditorFontSize(context, it)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), modifier = Modifier.padding(vertical = 10.dp))
                SettingToggleRow(Icons.Default.Settings, "줄바꿈", "긴 줄을 자동 줄바꿈", editorWordWrap) {
                    editorWordWrap = it
                    AstralPrefs.setEditorWordWrap(context, it)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), modifier = Modifier.padding(vertical = 10.dp))
                SettingToggleRow(Icons.Default.Settings, "라인 번호", "좌측 라인 번호 표시", editorLineNumbers) {
                    editorLineNumbers = it
                    AstralPrefs.setEditorLineNumbers(context, it)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "런타임") {
                SettingInfoRow("Engine", "Astral V1.0.0")
                SettingInfoRow("Bridge", "Connected")
            }
        }
    }
}

@Composable
fun SettingToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit = {}) {
    var isChecked by remember { mutableStateOf(checked) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            isChecked,
            {
                isChecked = it
                onChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SettingSliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text("${value.toInt()}pt", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    VanguardCard(Modifier.fillMaxWidth(), borderAlpha = 0.2f) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingInfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
    }
}
