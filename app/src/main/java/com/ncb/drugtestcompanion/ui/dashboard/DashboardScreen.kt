package com.ncb.drugtestcompanion.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncb.drugtestcompanion.R
import com.ncb.drugtestcompanion.audio.VoiceAnnouncementManager
import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.localization.AppLanguage
import com.ncb.drugtestcompanion.ui.common.LanguageSelectorDialog
import com.ncb.drugtestcompanion.ui.common.TestTubeIcon
import com.ncb.drugtestcompanion.ui.theme.BorderBlueLight
import com.ncb.drugtestcompanion.ui.theme.BorderGray
import com.ncb.drugtestcompanion.ui.theme.CardTintBlue
import com.ncb.drugtestcompanion.ui.theme.DeepNavy
import com.ncb.drugtestcompanion.ui.theme.LightBlueCardGradient
import com.ncb.drugtestcompanion.ui.theme.MidnightGradient
import com.ncb.drugtestcompanion.ui.theme.PrimaryGradient
import com.ncb.drugtestcompanion.ui.theme.PrimaryNavy
import com.ncb.drugtestcompanion.ui.theme.SoftLightBlueBackgroundGradient
import com.ncb.drugtestcompanion.ui.theme.StatusGreen
import com.ncb.drugtestcompanion.ui.theme.StatusGreenContainer
import com.ncb.drugtestcompanion.ui.theme.StatusRed
import com.ncb.drugtestcompanion.ui.theme.StatusRedContainer
import com.ncb.drugtestcompanion.ui.theme.TealContainer
import com.ncb.drugtestcompanion.ui.theme.TextCharcoal
import com.ncb.drugtestcompanion.ui.theme.TextMuted
import com.ncb.drugtestcompanion.viewmodel.HistoryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom Light Soft Gradients
private val LightCyanCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFE0F2FE))
)

private val LightTealCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFE6FFFA))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    officerId: String,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onStartNewCase: () -> Unit,
    onViewHistory: () -> Unit,
    onNavigateToKitProfiles: () -> Unit = {},
    onNavigateToAuditTrail: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = hiltViewModel()
) {
    val records by historyViewModel.testRecords.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Voice Welcome Announcement on Dashboard Open ("Welcome")
    val voiceManager = remember(context) { VoiceAnnouncementManager(context.applicationContext) }
    LaunchedEffect(officerId) {
        voiceManager.speak("Welcome")
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedBottomTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Sync state and toast trigger
    var isSyncing by remember { mutableStateOf(false) }
    var showSyncBanner by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isSyncing) 360f else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "syncRotation"
    )

    val syncMsg = stringResource(R.string.sync_all_up_to_date)

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = onSelectLanguage,
            onDismiss = { showLanguageDialog = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightGradient)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Logo + Niriksh Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TestTubeIcon(size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Niriksh",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Field Unit — 07 • Insp. R. Verma",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Right: Circular Sync Button
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable {
                                if (!isSyncing) {
                                    coroutineScope.launch {
                                        isSyncing = true
                                        showSyncBanner = true
                                        delay(800)
                                        isSyncing = false
                                        refreshTrigger++
                                        Toast.makeText(context, syncMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Data",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(rotationAngle)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                // 1. HOME
                NavigationBarItem(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) },
                    label = { Text(stringResource(R.string.nav_home), fontSize = 11.sp, fontWeight = if (selectedBottomTab == 0) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNavy,
                        selectedTextColor = PrimaryNavy,
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )

                // 2. START NEW TEST (Bottom Nav Entry)
                NavigationBarItem(
                    selected = selectedBottomTab == 1,
                    onClick = {
                        selectedBottomTab = 1
                        onStartNewCase()
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_start_new_test)) },
                    label = { Text(stringResource(R.string.btn_start_new_test), fontSize = 11.sp, fontWeight = if (selectedBottomTab == 1) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNavy,
                        selectedTextColor = PrimaryNavy,
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )

                // 3. TEST HISTORY
                NavigationBarItem(
                    selected = selectedBottomTab == 2,
                    onClick = {
                        selectedBottomTab = 2
                        onViewHistory()
                    },
                    icon = { Icon(Icons.AutoMirrored.Default.List, contentDescription = stringResource(R.string.btn_history)) },
                    label = { Text(stringResource(R.string.btn_history), fontSize = 11.sp, fontWeight = if (selectedBottomTab == 2) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNavy,
                        selectedTextColor = PrimaryNavy,
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )

                // 4. PROFILE / SETTINGS
                NavigationBarItem(
                    selected = selectedBottomTab == 3,
                    onClick = {
                        selectedBottomTab = 3
                        onNavigateToSettings()
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.nav_profile)) },
                    label = { Text(stringResource(R.string.nav_profile), fontSize = 11.sp, fontWeight = if (selectedBottomTab == 3) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNavy,
                        selectedTextColor = PrimaryNavy,
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color.Transparent
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoftLightBlueBackgroundGradient)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 0. OFFICER WELCOME BANNER CARD
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderBlueLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightBlueCardGradient)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = PrimaryNavy.copy(alpha = 0.1f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = PrimaryNavy,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = stringResource(R.string.dashboard_welcome_officer),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextCharcoal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${stringResource(R.string.dashboard_officer_id)}: $officerId",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = StatusGreenContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(StatusGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_active),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. HERO ACTION CARD: START NEW TEST (Gradient Hero Banner)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryGradient)
                            .clickable { onStartNewCase() }
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        TestTubeIcon(size = 28.dp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = stringResource(R.string.dash_start_new_test),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.dash_start_new_test_sub),
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                                contentDescription = "Start",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 2. Sync Status Banner Card
                if (showSyncBanner) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = syncMsg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextCharcoal
                                )
                            }
                        }
                    }
                }

                // 3. Search Bar Field
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_placeholder_history), fontSize = 13.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = PrimaryNavy,
                            unfocusedBorderColor = BorderGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. AUTO-SLIDING FEATURE CAROUSEL
                item {
                    DashboardAutoSlidingCarousel(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 5. SYSTEM STATUS SECTION
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "SYSTEM STATUS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SystemStatusItem("Camera", "Ready")
                                SystemStatusItem("GPS", "Locked")
                                SystemStatusItem("Storage", "14.2 GB")
                                SystemStatusItem("Sync", "Synced")
                            }
                        }
                    }
                }

                // 6. QUICK ACTIONS GRID
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "QUICK ACTIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionCard(
                                title = stringResource(R.string.dash_test_history),
                                icon = Icons.AutoMirrored.Default.List,
                                badgeCount = records.size.coerceAtLeast(4),
                                gradient = LightBlueCardGradient,
                                modifier = Modifier.weight(1f)
                            ) { onViewHistory() }

                            QuickActionCard(
                                title = stringResource(R.string.dash_kit_reagents),
                                icon = Icons.Default.Person,
                                gradient = LightCyanCardGradient,
                                modifier = Modifier.weight(1f)
                            ) { onNavigateToKitProfiles() }

                            QuickActionCard(
                                title = stringResource(R.string.dash_audit_trail),
                                icon = Icons.Default.Info,
                                gradient = LightTealCardGradient,
                                modifier = Modifier.weight(1f)
                            ) { onNavigateToAuditTrail() }
                        }
                    }
                }

                // 7. RECENT FIELD TESTING ACTIVITY
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.dash_recent_activity),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )

                        TextButton(onClick = onViewHistory) {
                            Text(
                                text = stringResource(R.string.btn_view_all),
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Filtered/refreshed records list
                val filterQuery = searchQuery.trim().lowercase()
                val displayRecords = if (records.isNotEmpty()) {
                    records.filter {
                        it.testId.lowercase().contains(filterQuery) ||
                                it.kitId.lowercase().contains(filterQuery) ||
                                it.result.lowercase().contains(filterQuery)
                    }
                } else emptyList()

                if (displayRecords.isNotEmpty()) {
                    items(displayRecords.take(6), key = { it.testId }) { record ->
                        val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(record.timestamp))
                        RecentTestCardItem(
                            testId = record.testId,
                            subjectId = "SUB-${record.testId.takeLast(4)}",
                            reagentInfo = "${record.kitId} • $timeStr",
                            result = record.result
                        )
                    }
                } else {
                    // Demo records matching reference screenshot
                    item {
                        RecentTestCardItem(
                            testId = "NRK-20260928-7603",
                            subjectId = "SUB-5958",
                            reagentInfo = "Scott Reagent (Cocaine HCl / Base) • 28 Sep, 09:06 PM",
                            result = stringResource(R.string.result_negative)
                        )
                    }
                    item {
                        RecentTestCardItem(
                            testId = "NRK-20260928-0012",
                            subjectId = "SUB-4587",
                            reagentInfo = "Marquis Reagent (Amphetamine) • 28 Sep, 08:46 PM",
                            result = stringResource(R.string.result_negative)
                        )
                    }
                    item {
                        RecentTestCardItem(
                            testId = "NRK-20260928-0011",
                            subjectId = "SUB-4586",
                            reagentInfo = "Fast Blue B (THC / Cannabinoids) • 28 Sep, 07:49 PM",
                            result = stringResource(R.string.result_positive)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Auto-Sliding Feature Carousel on Dashboard Screen (5 Project Slides)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardAutoSlidingCarousel(
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
                .height(170.dp)
        ) { page ->
            DashboardCarouselSlideCard(page = page)
        }

        Spacer(modifier = Modifier.height(8.dp))

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
private fun DashboardCarouselSlideCard(page: Int) {
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
        border = BorderStroke(1.dp, BorderBlueLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBlueCardGradient)
                .padding(14.dp),
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
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(titleRes),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextCharcoal,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(descRes),
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .width(90.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = stringResource(titleRes),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun SystemStatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(StatusGreen)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, color = TextMuted)
        }
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    badgeCount: Int? = null,
    gradient: Brush = LightBlueCardGradient,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, BorderBlueLight),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PrimaryNavy,
                    modifier = Modifier.size(24.dp)
                )

                badgeCount?.let { count ->
                    Surface(
                        shape = CircleShape,
                        color = PrimaryNavy,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = count.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextCharcoal
            )
        }
    }
}

@Composable
private fun RecentTestCardItem(
    testId: String,
    subjectId: String,
    reagentInfo: String,
    result: String
) {
    val isPositive = result.contains("POSITIVE", ignoreCase = true)
    val statusContainerColor = if (isPositive) StatusRedContainer else StatusGreenContainer
    val statusTextColor = if (isPositive) StatusRed else StatusGreen
    val statusDotColor = if (isPositive) StatusRed else StatusGreen

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored Status Indicator Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = testId,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextCharcoal
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• $subjectId",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = reagentInfo,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusContainerColor
            ) {
                Text(
                    text = result,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusTextColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}
