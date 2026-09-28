package com.ncb.drugtestcompanion.ui.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.ncb.drugtestcompanion.BuildConfig
import com.ncb.drugtestcompanion.R
import com.ncb.drugtestcompanion.localization.AppLanguage
import com.ncb.drugtestcompanion.ui.common.LanguageSelectorButton
import com.ncb.drugtestcompanion.ui.common.LanguageSelectorDialog
import com.ncb.drugtestcompanion.ui.common.ShieldIcon
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onNavigateToLogin: () -> Unit,
    onNavigateToMlTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Section scroll position targets
    var aboutY by remember { mutableStateOf(0) }
    var howItWorksY by remember { mutableStateOf(0) }
    var featuresY by remember { mutableStateOf(0) }
    var contactY by remember { mutableStateOf(0) }

    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = onSelectLanguage,
            onDismiss = { showLanguageDialog = false }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Navigation Bar
            HomeTopBar(
                currentLanguage = currentLanguage,
                onOpenLanguageDialog = { showLanguageDialog = true },
                onNavigateToLogin = onNavigateToLogin,
                onScrollToSection = { target ->
                    coroutineScope.launch {
                        when (target) {
                            "about" -> scrollState.animateScrollTo(aboutY)
                            "how_it_works" -> scrollState.animateScrollTo(howItWorksY)
                            "features" -> scrollState.animateScrollTo(featuresY)
                            "contact" -> scrollState.animateScrollTo(contactY)
                        }
                    }
                }
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                HeroSection(
                    onNavigateToLogin = onNavigateToLogin,
                    onLearnMore = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(aboutY)
                        }
                    }
                )

                Box(modifier = Modifier.onGloballyPositioned { aboutY = it.positionInRoot().y.toInt() }) {
                    AboutSection()
                }

                Box(modifier = Modifier.onGloballyPositioned { howItWorksY = it.positionInRoot().y.toInt() }) {
                    HowItWorksSection()
                }

                Box(modifier = Modifier.onGloballyPositioned { featuresY = it.positionInRoot().y.toInt() }) {
                    KeyFeaturesSection()
                }

                ImportantNoticeSection()

                Box(modifier = Modifier.onGloballyPositioned { contactY = it.positionInRoot().y.toInt() }) {
                    ContactSupportSection(
                        onSendMessage = {
                            Toast.makeText(context, "Message form is available in the prototype.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                FooterSection(
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToMlTest = onNavigateToMlTest,
                    onScrollToSection = { target ->
                        coroutineScope.launch {
                            when (target) {
                                "about" -> scrollState.animateScrollTo(aboutY)
                                "how_it_works" -> scrollState.animateScrollTo(howItWorksY)
                                "features" -> scrollState.animateScrollTo(featuresY)
                                "contact" -> scrollState.animateScrollTo(contactY)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    currentLanguage: AppLanguage,
    onOpenLanguageDialog: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onScrollToSection: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF0F2942),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo & Subtitle
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShieldIcon(size = 32.dp, color = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Language Selector Button
                LanguageSelectorButton(
                    currentLanguage = currentLanguage,
                    onOpenDialog = onOpenLanguageDialog
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Officer Login Header Button
                OutlinedButton(
                    onClick = onNavigateToLogin,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.nav_login),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.nav_login), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Menu Icon for Sections
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Navigation Menu",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nav_about)) },
                            onClick = {
                                onScrollToSection("about")
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nav_how_it_works)) },
                            onClick = {
                                onScrollToSection("how_it_works")
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nav_features)) },
                            onClick = {
                                onScrollToSection("features")
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nav_contact)) },
                            onClick = {
                                onScrollToSection("contact")
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    onNavigateToLogin: () -> Unit,
    onLearnMore: () -> Unit
) {
    Surface(
        color = Color(0xFF0F2942).copy(alpha = 0.04f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "DrugTest Companion",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2942)
            )

            Text(
                text = "Digital Companion for Field Drug Testing",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1976D2),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "A mobile-assisted solution for standardized field-test documentation, presumptive field-test result recording, and tamper-evident digital evidence management.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Officer Login",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Officer Login", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onLearnMore,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF0F2942)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Learn More",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF0F2942)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Learn More", color = Color(0xFF0F2942), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Professional Graphic Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeroIllustration(modifier = Modifier.fillMaxWidth().height(160.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Authorized Field Testing & Reference Calibration Environment",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Draw Field Desk Surface
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(w * 0.05f, h * 0.1f),
            size = Size(w * 0.9f, h * 0.8f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Draw Reference Card Representation
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.12f, h * 0.25f),
            size = Size(w * 0.35f, h * 0.5f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // ArUco Markers Representation
        val mSize = w * 0.06f
        drawRect(Color.Black, topLeft = Offset(w * 0.14f, h * 0.28f), size = Size(mSize, mSize))
        drawRect(Color.Black, topLeft = Offset(w * 0.39f, h * 0.28f), size = Size(mSize, mSize))
        drawRect(Color.Black, topLeft = Offset(w * 0.14f, h * 0.65f), size = Size(mSize, mSize))
        drawRect(Color.Black, topLeft = Offset(w * 0.39f, h * 0.65f), size = Size(mSize, mSize))

        // Smartphone Frame Representation
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(w * 0.55f, h * 0.2f),
            size = Size(w * 0.32f, h * 0.6f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        // Screen View
        drawRoundRect(
            color = Color(0xFF0284C7),
            topLeft = Offset(w * 0.57f, h * 0.25f),
            size = Size(w * 0.28f, h * 0.5f),
            cornerRadius = CornerRadius(6f, 6f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutSection() {
    Column(
        modifier = Modifier.padding(20.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShieldIcon(size = 28.dp, color = Color(0xFF0F2942))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "About the Solution",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F2942)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "DrugTest Companion is a prototype mobile solution designed to assist authorized field personnel in documenting colorimetric field-test observations and maintaining a structured digital evidence record.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "The application supports:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F2942)
                )

                Spacer(modifier = Modifier.height(12.dp))

                val capabilities = listOf(
                    "Standardized image capture",
                    "Reference-card based color calibration",
                    "Presumptive result classification",
                    "Timestamped digital records",
                    "Operator identification",
                    "Location information",
                    "Cryptographic image hashing",
                    "Digitally signed evidence",
                    "Integrity verification",
                    "Searchable test history"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    capabilities.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorksSection() {
    Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color(0xFF0F2942),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "How It Works",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2942)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            HowItWorksCard("01", "Start a Case", "Officer begins a new field-test case.")
            HowItWorksCard("02", "Capture", "The completed test device and project calibration reference card are captured in one image.")
            HowItWorksCard("03", "Analyze", "The application performs image-quality checking, reference-card processing, ROI analysis and presumptive result classification.")
            HowItWorksCard("04", "Secure the Record", "The application creates a timestamped evidence record with image hash, operator information, location information and digital signature.")
        }
    }
}

@Composable
private fun HowItWorksCard(
    stepNumber: String,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0F2942),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stepNumber,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F2942)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KeyFeaturesSection() {
    Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFF0F2942),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Key Features",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2942)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FeatureCard("1. Image Capture", "Structured camera capture for field-test documentation.")
            FeatureCard("2. Reference Calibration", "Reference-card based color normalization.")
            FeatureCard("3. Presumptive Classification", "Records POSITIVE, NEGATIVE or INCONCLUSIVE prototype results.")
            FeatureCard("4. Digital Evidence", "Creates a structured evidence record with cryptographic image hashing.")
            FeatureCard("5. Integrity Verification", "Allows stored evidence to be checked for modification.")
            FeatureCard("6. Searchable History", "Provides access to previously recorded test cases.")
        }
    }
}

@Composable
private fun FeatureCard(title: String, description: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2942)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImportantNoticeSection() {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            border = BorderStroke(1.dp, Color(0xFFBBDEFB)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Notice",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Important Notice",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "This application is a prototype for presumptive field-test documentation. Results generated by the application are not a substitute for laboratory confirmation or other authorized confirmatory procedures.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0D47A1)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactSupportSection(onSendMessage: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(20.dp)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF0F2942),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contact & Support",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F2942)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Project Support",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F2942)
                )
                Text(
                    text = "DrugTest Companion — Prototype Solution",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Contact details can be configured for the authorized deployment environment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSendMessage,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Message", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FooterSection(
    onNavigateToLogin: () -> Unit,
    onNavigateToMlTest: () -> Unit = {},
    onScrollToSection: (String) -> Unit
) {
    Surface(
        color = Color(0xFF0F2942),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShieldIcon(size = 28.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "DrugTest Companion",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Digital Companion for Field Drug Testing",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { onScrollToSection("about") }) {
                    Text("About", color = Color.White, fontSize = 12.sp)
                }
                TextButton(onClick = { onScrollToSection("how_it_works") }) {
                    Text("How It Works", color = Color.White, fontSize = 12.sp)
                }
                TextButton(onClick = { onScrollToSection("features") }) {
                    Text("Features", color = Color.White, fontSize = 12.sp)
                }
                TextButton(onClick = { onScrollToSection("contact") }) {
                    Text("Contact", color = Color.White, fontSize = 12.sp)
                }
                TextButton(onClick = onNavigateToLogin) {
                    Text("Officer Login", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prototype Application",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )

                if (BuildConfig.DEBUG) {
                    TextButton(onClick = onNavigateToMlTest) {
                        Text("ML Test", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                }

                Text(
                    text = "Field Officer Edition",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
