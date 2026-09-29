package com.ncb.drugtestcompanion.ui.home

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncb.drugtestcompanion.BuildConfig
import com.ncb.drugtestcompanion.R
import com.ncb.drugtestcompanion.audio.VoiceAnnouncementManager
import com.ncb.drugtestcompanion.localization.AppLanguage
import com.ncb.drugtestcompanion.ui.common.GradientButton
import com.ncb.drugtestcompanion.ui.common.LanguageSelectorButton
import com.ncb.drugtestcompanion.ui.common.LanguageSelectorDialog
import com.ncb.drugtestcompanion.ui.common.ShieldIcon
import com.ncb.drugtestcompanion.ui.theme.BorderGray
import com.ncb.drugtestcompanion.ui.theme.CanvasBackground
import com.ncb.drugtestcompanion.ui.theme.CardTintBlue
import com.ncb.drugtestcompanion.ui.theme.DeepNavy
import com.ncb.drugtestcompanion.ui.theme.LightBlueCardGradient
import com.ncb.drugtestcompanion.ui.theme.MidnightGradient
import com.ncb.drugtestcompanion.ui.theme.PrimaryNavy
import com.ncb.drugtestcompanion.ui.theme.StatusGreen
import com.ncb.drugtestcompanion.ui.theme.StatusGreenContainer
import com.ncb.drugtestcompanion.ui.theme.StatusRed
import com.ncb.drugtestcompanion.ui.theme.StatusRedContainer
import com.ncb.drugtestcompanion.ui.theme.TealAccent
import com.ncb.drugtestcompanion.ui.theme.TealContainer
import com.ncb.drugtestcompanion.ui.theme.TextCharcoal
import com.ncb.drugtestcompanion.ui.theme.TextMuted
import kotlinx.coroutines.delay
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

    // Voice Announcement Greeting ONLY on Initial App Open ("Welcome")
    val voiceManager = remember(context) { VoiceAnnouncementManager(context.applicationContext) }
    var hasGreetedInitialOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasGreetedInitialOpen) {
            voiceManager.speak("Welcome")
            hasGreetedInitialOpen = true
        }
    }

    // Section scroll position targets
    var aboutY by remember { mutableStateOf(0) }
    var howItWorksY by remember { mutableStateOf(0) }
    var featuresY by remember { mutableStateOf(0) }

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
        color = CanvasBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Navigation Bar (Contains Entry 1: Top-Right Officer Login)
            HomeTopBar(
                currentLanguage = currentLanguage,
                onOpenLanguageDialog = { showLanguageDialog = true },
                onNavigateToLogin = onNavigateToLogin
            )

            // 2. Main Portal Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Hero Section (Contains Entry 2: Below "Digital Field Drug Testing" Section)
                HeroSectionImage5(
                    onNavigateToLogin = onNavigateToLogin
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. AUTO-SLIDING FEATURE CAROUSEL (5 Project Slides)
                AutoSlidingFeatureCarousel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. ML DEMO OPTION BUTTON (Kept for Demonstration)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToMlTest,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PrimaryNavy),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        ShieldIcon(size = 18.dp, color = PrimaryNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_ml_test),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Supported Field Drug Testing Equipment & Kits Showcase Section
                FieldKitsShowcaseSection()

                // 6. Workflow Steps Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { howItWorksY = it.positionInRoot().y.toInt() }
                ) {
                    WorkflowStepsSection()
                }

                // 7. Evidence Integrity Section
                Box(
                    modifier = Modifier.onGloballyPositioned { featuresY = it.positionInRoot().y.toInt() }
                ) {
                    EvidenceIntegritySection()
                }

                // 8. Niriksh Professional Footer Section (No "More" item)
                Box(modifier = Modifier.onGloballyPositioned { aboutY = it.positionInRoot().y.toInt() }) {
                    PortalFooterSection()
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    currentLanguage: AppLanguage,
    onOpenLanguageDialog: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MidnightGradient)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShieldIcon(size = 28.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                LanguageSelectorButton(
                    currentLanguage = currentLanguage,
                    onOpenDialog = onOpenLanguageDialog
                )

                Spacer(modifier = Modifier.width(8.dp))

                // ENTRY 1: Top-Right Officer Login
                GradientButton(
                    text = stringResource(R.string.nav_login),
                    onClick = onNavigateToLogin,
                    height = 36.dp,
                    fontSize = 12,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.nav_login),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HeroSectionImage5(
    onNavigateToLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MidnightGradient)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34D399))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.home_hero_pill),
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.home_hero_title),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.home_hero_subtitle),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ENTRY 2: Below "Digital Field Drug Testing" Section Officer Login
            GradientButton(
                text = "→ " + stringResource(R.string.home_btn_login),
                onClick = onNavigateToLogin,
                height = 48.dp,
                fontSize = 14,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            HeroDeviceMockupGraphic()
        }
    }
}

/**
 * Auto-Sliding Feature Carousel (5 Slides)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AutoSlidingFeatureCarousel(
    modifier: Modifier = Modifier
) {
    val pageCount = 5
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Auto-slide every 3.5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            val nextPage = (pagerState.currentPage + 1) % pageCount
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) { page ->
            CarouselSlideCard(page = page)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pagination Indicators: ● ○ ○ ○ ○
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PrimaryNavy else BorderGray)
                )
            }
        }
    }
}

@Composable
private fun CarouselSlideCard(page: Int) {
    val (titleRes, descRes, imageRes) = when (page) {
        0 -> Triple(R.string.carousel_slide_1_title, R.string.carousel_slide_1_desc, R.drawable.kit_home_hero)
        1 -> Triple(R.string.carousel_slide_2_title, R.string.carousel_slide_2_desc, R.drawable.kit_rapidfor_k2)
        2 -> Triple(R.string.carousel_slide_3_title, R.string.carousel_slide_3_desc, R.drawable.kit_mobiledetect)
        3 -> Triple(R.string.carousel_slide_4_title, R.string.carousel_slide_4_desc, R.drawable.kit_proscreen)
        else -> Triple(R.string.carousel_slide_5_title, R.string.carousel_slide_5_desc, R.drawable.kit_tox_analyser)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBlueCardGradient)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = TealContainer
                ) {
                    Text(
                        text = "FEATURE 0${page + 1}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(titleRes),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextCharcoal,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(descRes),
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .width(110.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = stringResource(titleRes),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            }
        }
    }
}

/**
 * Field Drug Testing Equipment & Kits Showcase Section
 */
@Composable
private fun FieldKitsShowcaseSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFF00C9A7).copy(alpha = 0.15f),
            border = BorderStroke(1.dp, Color(0xFF00C9A7))
        ) {
            Text(
                text = "FIELD KITS & DIGITAL EVIDENCE TECHNOLOGY",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0D9488),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Supported Field Testing Equipment",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextCharcoal
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Automated camera-assisted colorimetry for rapid test kit boxes, pouches, abuse cups & forensic suites",
            fontSize = 12.sp,
            color = TextMuted,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EquipmentKitCard(
                title = "RapidFor™ K2 Synthetic Kit",
                subtitle = "Synthetic Cannabis Rapid Box",
                category = "Reagent Kit Box",
                colorAccent = Color(0xFF059669),
                imageType = KitImageType.K2_BOX,
                modifier = Modifier.weight(1f)
            )

            EquipmentKitCard(
                title = "MobileDetect™ MDT Pouch",
                subtitle = "Automated Dual-Well Pouch",
                category = "Color Reaction Pouch",
                colorAccent = Color(0xFF0284C7),
                imageType = KitImageType.MDT_POUCH,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EquipmentKitCard(
                title = "ProScreen™ Multi-Panel Cup",
                subtitle = "6-Panel Abuse Test Cup",
                category = "Multi-Panel Fluid Cup",
                colorAccent = Color(0xFF7C3AED),
                imageType = KitImageType.PROSCREEN_CUP,
                modifier = Modifier.weight(1f)
            )

            EquipmentKitCard(
                title = "Police Field Suite",
                subtitle = "TOX-Analyser & Evidence Case",
                category = "Law Enforcement Suite",
                colorAccent = Color(0xFF0F2942),
                imageType = KitImageType.EVIDENCE_SUITE,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private enum class KitImageType { K2_BOX, MDT_POUCH, PROSCREEN_CUP, EVIDENCE_SUITE }

@Composable
private fun EquipmentKitCard(
    title: String,
    subtitle: String,
    category: String,
    colorAccent: Color,
    imageType: KitImageType,
    modifier: Modifier = Modifier
) {
    val drawableResId = when (imageType) {
        KitImageType.K2_BOX -> R.drawable.kit_rapidfor_k2
        KitImageType.MDT_POUCH -> R.drawable.kit_mobiledetect
        KitImageType.PROSCREEN_CUP -> R.drawable.kit_proscreen
        KitImageType.EVIDENCE_SUITE -> R.drawable.kit_tox_analyser
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, colorAccent.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightBlueCardGradient)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = drawableResId),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = colorAccent.copy(alpha = 0.12f)
            ) {
                Text(
                    text = category.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorAccent,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextCharcoal
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun HeroDeviceMockupGraphic() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D253A)),
        border = BorderStroke(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(185.dp)
                        .height(265.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF071828))
                        .padding(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF030D18))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A1F33))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ShieldIcon(size = 12.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DTC", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = StatusGreenContainer
                            ) {
                                Text("SECURE", color = StatusGreen, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.kit_home_hero),
                                contentDescription = "Field Mobile Equipment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val cornerLen = 16f
                                val strokeWidth = 3f
                                val bracketColor = Color(0xFF00E5FF)

                                drawPath(Path().apply {
                                    moveTo(0f, cornerLen)
                                    lineTo(0f, 0f)
                                    lineTo(cornerLen, 0f)
                                }, color = bracketColor, style = Stroke(width = strokeWidth))

                                drawPath(Path().apply {
                                    moveTo(w - cornerLen, 0f)
                                    lineTo(w, 0f)
                                    lineTo(w, cornerLen)
                                }, color = bracketColor, style = Stroke(width = strokeWidth))

                                drawPath(Path().apply {
                                    moveTo(0f, h - cornerLen)
                                    lineTo(0f, h)
                                    lineTo(cornerLen, h)
                                }, color = bracketColor, style = Stroke(width = strokeWidth))

                                drawPath(Path().apply {
                                    moveTo(w - cornerLen, h)
                                    lineTo(w, h)
                                    lineTo(w, h - cornerLen)
                                }, color = bracketColor, style = Stroke(width = strokeWidth))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF00A6A6).copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "• " + stringResource(R.string.home_phone_ref_detected),
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                                    modifier = Modifier
                                        .width(130.dp)
                                        .height(75.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ReagentAmpouleGraphic(Color(0xFF0284C7), "MARQUIS")
                                        ReagentAmpouleGraphic(Color(0xFF7C3AED), "MECKE")
                                        ReagentAmpouleGraphic(Color(0xFFD97706), "FROEHDE")
                                        ReagentAmpouleGraphic(Color(0xFFDC2626), "SIMON")
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = stringResource(R.string.home_phone_instruction),
                                    color = Color.White,
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .background(Color(0xFF0A1F33)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("DTC Reference", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
                            Text("CAL-CARD-V2", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF0284C7)))
                                Box(modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF7C3AED)))
                                Box(modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFD97706)))
                                Box(modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFDC2626)))
                                Box(modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF059669)))
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(StatusGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.home_phone_record_created),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextCharcoal
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SHA-256: 4821...F29",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusGreenContainer
                            ) {
                                Text(
                                    text = "SEALED & AUDITABLE",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReagentAmpouleGraphic(color: Color, name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .padding(2.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(name, fontSize = 6.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
    }
}

@Composable
private fun WorkflowStepsSection() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Text(
                    text = stringResource(R.string.home_wf_pill),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.home_wf_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.home_wf_subtitle),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            WorkflowCardItem(stringResource(R.string.wf_01_num), stringResource(R.string.wf_01_title), stringResource(R.string.wf_01_desc), stringResource(R.string.badge_validated))
            Spacer(modifier = Modifier.height(10.dp))
            WorkflowCardItem(stringResource(R.string.wf_02_num), stringResource(R.string.wf_02_title), stringResource(R.string.wf_02_desc), stringResource(R.string.badge_validated))
            Spacer(modifier = Modifier.height(10.dp))
            WorkflowCardItem(stringResource(R.string.wf_03_num), stringResource(R.string.wf_03_title), stringResource(R.string.wf_03_desc), stringResource(R.string.badge_auditable))
            Spacer(modifier = Modifier.height(10.dp))
            WorkflowCardItem(stringResource(R.string.wf_04_num), stringResource(R.string.wf_04_title), stringResource(R.string.wf_04_desc), stringResource(R.string.badge_auditable))
            Spacer(modifier = Modifier.height(10.dp))
            WorkflowCardItem(stringResource(R.string.wf_05_num), stringResource(R.string.wf_05_title), stringResource(R.string.wf_05_desc), stringResource(R.string.badge_auditable))
            Spacer(modifier = Modifier.height(10.dp))
            WorkflowCardItem(stringResource(R.string.wf_06_num), stringResource(R.string.wf_06_title), stringResource(R.string.wf_06_desc), stringResource(R.string.badge_auditable))
        }
    }
}

@Composable
private fun WorkflowCardItem(num: String, title: String, desc: String, badge: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(num, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryNavy)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
                    Surface(shape = RoundedCornerShape(4.dp), color = CardTintBlue) {
                        Text(badge, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(desc, fontSize = 11.sp, color = TextMuted)
            }
        }
    }
}

@Composable
private fun EvidenceIntegritySection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = TealContainer
        ) {
            Text(
                text = stringResource(R.string.home_integrity_pill),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.home_integrity_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextCharcoal
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.home_integrity_subtitle),
            fontSize = 12.sp,
            color = TextMuted,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TrustFeatureCard(
                stringResource(R.string.trust_01_title),
                stringResource(R.string.trust_01_desc),
                Modifier.weight(1f)
            )
            TrustFeatureCard(
                stringResource(R.string.trust_02_title),
                stringResource(R.string.trust_02_desc),
                Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TrustFeatureCard(
                stringResource(R.string.trust_03_title),
                stringResource(R.string.trust_03_desc),
                Modifier.weight(1f)
            )
            TrustFeatureCard(
                stringResource(R.string.trust_04_title),
                stringResource(R.string.trust_04_desc),
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrustFeatureCard(title: String, desc: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, fontSize = 10.sp, color = TextMuted, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun PortalFooterSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MidnightGradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShieldIcon(size = 22.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.footer_description),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.footer_tagline),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.footer_copyright),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}
