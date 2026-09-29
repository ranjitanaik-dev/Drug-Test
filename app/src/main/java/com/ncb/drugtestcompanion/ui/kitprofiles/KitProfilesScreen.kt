package com.ncb.drugtestcompanion.ui.kitprofiles

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncb.drugtestcompanion.ui.theme.BorderGray
import com.ncb.drugtestcompanion.ui.theme.CanvasBackground
import com.ncb.drugtestcompanion.ui.theme.PrimaryNavy
import com.ncb.drugtestcompanion.ui.theme.StatusGreen
import com.ncb.drugtestcompanion.ui.theme.StatusGreenContainer
import com.ncb.drugtestcompanion.ui.theme.TextCharcoal
import com.ncb.drugtestcompanion.ui.theme.TextMuted

data class ReagentKitItem(
    val name: String,
    val targetAnalyte: String,
    val lotNumber: String,
    val expiry: String,
    val description: String,
    val targetHueRange: String,
    val status: String = "ACTIVE"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitProfilesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kitProfiles = listOf(
        ReagentKitItem(
            name = "Duquenois-Levine",
            targetAnalyte = "Cannabinoids (THC / Hashish)",
            lotNumber = "LOT-3088",
            expiry = "May 2027",
            description = "Acetaldehyde and vanillin with concentrated HCl followed by chloroform extraction.",
            targetHueRange = "265° - 295°"
        ),
        ReagentKitItem(
            name = "Fast Blue B",
            targetAnalyte = "THC / Cannabinoids",
            lotNumber = "LOT-SS31",
            expiry = "Nov 2026",
            description = "Diazotized dye reacting with phenolic cannabinoid group producing red-violet stain.",
            targetHueRange = "260° - 305°"
        ),
        ReagentKitItem(
            name = "Marquis Reagent",
            targetAnalyte = "Amphetamine / Methamphetamine",
            lotNumber = "LOT-9142",
            expiry = "Aug 2027",
            description = "Formaldehyde in concentrated sulfuric acid. Produces distinct orange to red-brown chromophore.",
            targetHueRange = "15° - 45°"
        ),
        ReagentKitItem(
            name = "Scott Reagent",
            targetAnalyte = "Cocaine HCl / Base",
            lotNumber = "LOT-7823",
            expiry = "Dec 2027",
            description = "Cobalt thiocyanate 3-phase extraction. Positive turns organic lower layer vivid turquoise-blue.",
            targetHueRange = "185° - 235°"
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
                            text = "Kit Profiles & Reagents",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextCharcoal
                        )
                        Text(
                            text = "${kitProfiles.size} active chemical profiles",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(kitProfiles, key = { it.name }) { kit ->
                    ReagentKitCard(kit = kit)
                }
            }
        }
    }
}

@Composable
private fun ReagentKitCard(kit: ReagentKitItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = kit.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextCharcoal
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = kit.status,
                        color = PrimaryNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Target Analyte: ${kit.targetAnalyte}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextCharcoal
            )

            Text(
                text = "Lot: ${kit.lotNumber} • Expiry: ${kit.expiry}",
                fontSize = 11.sp,
                color = TextMuted
            )

            Text(
                text = kit.description,
                fontSize = 12.sp,
                color = TextCharcoal,
                lineHeight = 16.sp
            )

            Text(
                text = "Target Hue Range: ${kit.targetHueRange}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy
            )
        }
    }
}
