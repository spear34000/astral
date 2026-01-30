package pics.spear.astral.ui

import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import pics.spear.astral.ui.theme.AstralTheme
import pics.spear.astral.ui.components.AstralHeader
import pics.spear.astral.ui.components.AstralPill
import pics.spear.astral.ui.components.AstralScreen
import pics.spear.astral.ui.components.VanguardCard

class InfoFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AstralTheme { InfoScreen() }
            }
        }
    }
}

@Composable
fun InfoScreen() {
    val context = LocalContext.current
    AstralScreen {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            AstralHeader(
                title = "Astral Core",
                subtitle = "모바일 자동화 엔진 v1.0.0",
                align = Alignment.CenterHorizontally
            )

            Spacer(modifier = Modifier.height(16.dp))

            VanguardCard(Modifier.fillMaxWidth(), borderAlpha = 0.25f) {
                Column(Modifier.padding(20.dp)) {
                    Text("시스템 개요", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Astral은 모바일 자동화를 빠르게 설계하고, 안정적으로 실행하는 런타임 엔진입니다. 복합 로직과 이벤트 흐름을 하나의 코어에서 통합 관리합니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AstralPill("엔진", "V1.0.0")
                        AstralPill("브리지", "Connected")
                        AstralPill("채널", "Kakao")
                    }
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Text("SPEAR PICS", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                    Text("© 2026 Distributed Intelligence", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            VanguardCard(Modifier.fillMaxWidth(), borderAlpha = 0.25f) {
                Column(Modifier.padding(20.dp)) {
                    Text("개발자 정보", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("개발자", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp))
                        Text("spear", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("소속 팀", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp))
                        Text("Team stend", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            VanguardCard(Modifier.fillMaxWidth(), borderAlpha = 0.25f) {
                Column(Modifier.padding(20.dp)) {
                    Text("개발자 후원하기", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("스피어는 쉽고 편리하게 봇을 개발할 수 있게 자원봉사중이에요!", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            val intent = Intent(context, SupportActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    ) {
                        Text("후원 페이지 열기")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "BUILD QUIETLY. MOVE PRECISELY.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 32.dp).alpha(0.7f)
            )
        }
    }
}
