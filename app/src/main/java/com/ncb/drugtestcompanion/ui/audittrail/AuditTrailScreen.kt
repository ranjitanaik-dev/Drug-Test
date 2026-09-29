package com.ncb.drugtestcompanion.ui.audittrail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncb.drugtestcompanion.ui.theme.BorderGray
import com.ncb.drugtestcompanion.ui.theme.CanvasBackground
import com.ncb.drugtestcompanion.ui.theme.StatusGreen
import com.ncb.drugtestcompanion.ui.theme.StatusGreenContainer
import com.ncb.drugtestcompanion.ui.theme.TextCharcoal
import com.ncb.drugtestcompanion.ui.theme.TextMuted

data class AuditLogItem(
    val eventName: String,
    val description: String,
    val operatorId: String,
    val timestamp: String,
    val status: String = "SUCCESS"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditTrailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val auditEvents = listOf(
        AuditLogItem(
            eventName = "MFA_VERIFIED",
            description = "2-Factor device authentication verified successfully",
            operatorId = "OP-1047",
            timestamp = "29 Sep, 08:25:55 AM"
        ),
        AuditLogItem(
            eventName = "LOGIN_ATTEMPT",
            description = "Operator initiated login session",
            operatorId = "OP-1047",
            timestamp = "29 Sep, 08:25:54 AM"
        ),
        AuditLogItem(
            eventName = "MFA_VERIFIED",
            description = "2-Factor device authentication verified successfully",
            operatorId = "OP-1047",
            timestamp = "29 Sep, 07:45:47 AM"
        ),
        AuditLogItem(
            eventName = "LOGIN_ATTEMPT",
            description = "Operator initiated login session",
            operatorId = "OP-1047",
            timestamp = "29 Sep, 07:45:46 AM"
        ),
        AuditLogItem(
            eventName = "SECURITY_VERIFICATION_PASS",
            description = "Operator re-verified authorization credentials for submission chain of custody",
            operatorId = "OP-1047",
            timestamp = "29 Sep, 02:34:06 AM"
        ),
        AuditLogItem(
            eventName = "DEMO_SESSION_STARTED",
            description = "New test initiated: DEMO-20260929-4738",
            operatorId = "OP-1047",
            timestamp = "29 Sep, 02:34:00 AM"
        ),
        AuditLogItem(
            eventName = "TEST_SESSION_STARTED",
            description = "New test initiated: NRK-20260929-2647",
            operatorId = "OP-1047",
            timestamp = "29 Sep, 02:33:55 AM"
        ),
        AuditLogItem(
            eventName = "MFA_VERIFIED",
            description = "2-Factor device authentication verified successfully",
            operatorId = "OP-1047",
            timestamp = "29 Sep, 02:32:42 AM"
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = CanvasBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextCharcoal
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Security Audit Trail",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextCharcoal
                        )
                        Text(
                            text = "22 immutable events recorded",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = CanvasBackground
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(auditEvents, key = { "${it.eventName}_${it.timestamp}" }) { event ->
                    AuditEventCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun AuditEventCard(event: AuditLogItem) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.eventName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextCharcoal
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StatusGreenContainer
                ) {
                    Text(
                        text = event.status,
                        color = StatusGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = event.description,
                fontSize = 12.sp,
                color = TextCharcoal
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Operator: ${event.operatorId}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = event.timestamp,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}
