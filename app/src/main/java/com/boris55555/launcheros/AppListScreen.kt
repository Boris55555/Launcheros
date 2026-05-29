package com.boris55555.launcheros

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun AppListScreen(
    isPickerMode: Boolean = false,
    onAppSelected: ((AppInfo?) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    onShowSettingsClicked: (() -> Unit)? = null,
    favoritesRepository: FavoritesRepository,
    usageRepository: UsageRepository? = null,
    onLockedLetterChanged: (Char?) -> Unit,
    lockedLetter: Char?,
    onAppLaunched: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val notifications by NotificationListener.notifications.collectAsState()
    val customNames by favoritesRepository.customNames.collectAsState()
    val showAppIcons by favoritesRepository.showAppIcons.collectAsState()
    val showTop10Stars by favoritesRepository.showTop10Stars.collectAsState()
    val sexyMode by favoritesRepository.sexyMode.collectAsState()
    val fontSizeAllApps by favoritesRepository.fontSizeAllApps.collectAsState()
    val preferredAppList by favoritesRepository.preferredAppList.collectAsState()
    val usageMap by (usageRepository?.usageMap ?: MutableStateFlow<Map<String, Int>>(emptyMap())).collectAsState()
    val favorites by favoritesRepository.favorites.collectAsState()
    val hiddenApps by favoritesRepository.hiddenApps.collectAsState()

    var appToEdit by remember { mutableStateOf<AppInfo?>(null) }
    var refreshKey by remember { mutableStateOf(0) } // State to trigger refresh

    val uninstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshKey++ // Trigger recomposition
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val apps = remember(isPickerMode, customNames, refreshKey) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        
        // Custom App Info for Sexy Launcher's own Control Panel
        val controlPanelApp = AppInfo(
            name = "Control Panel",
            packageName = context.packageName,
            isSystemApp = true
        )

        val hiddenPackages = setOf(
            "com.android.calendar",
            "com.android.deskclock",
            "com.android.fmradio",
            "com.android.documentsui",
            "com.android.gallery3d",
            "com.mediatek.gnss.nonframeworklbs",
            "com.phdtaui.mainactivity",
            "com.android.stk",
            "com.android.quicksearchbox",
            "com.android.settings",
            "org.chromium.webview_shell",
            "com.google.android.webview",
            "com.android.webview"
        )

        val appList = resolveInfoList.mapNotNull { resolveInfo ->
            try {
                val pkgName = resolveInfo.activityInfo.packageName
                // Hide the launcher itself from the list since we add it manually with a different name
                if (pkgName == context.packageName) return@mapNotNull null

                val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        pkgName.startsWith("com.mudita")

                val originalName = resolveInfo.loadLabel(packageManager).toString()
                val customName = customNames[pkgName]
                AppInfo(
                    name = customName ?: originalName,
                    packageName = pkgName,
                    customName = customName,
                    isSystemApp = isSystemApp
                )
            } catch (e: Exception) {
                null
            }
        }.toMutableList()

        // Add Sexy Launcher Control Panel to the list
        appList.add(controlPanelApp)

        appList.filter {
            it.packageName !in hiddenPackages && 
            it.packageName !in hiddenApps &&
            // Don't show the launcher itself in picker mode
            (!isPickerMode || it.packageName != context.packageName)
        }.sortedBy { it.name }
    }

    if (appToEdit != null) {
        val app = appToEdit!!
        RenameAppDialog(
            appInfo = app,
            onDismiss = { appToEdit = null },
            onRename = { newName ->
                favoritesRepository.saveCustomName(app.packageName, newName)
                appToEdit = null
            },
            onUninstall = if (!app.isSystemApp) {
                {
                    appToEdit = null // Dismiss the dialog immediately
                    val intent = Intent(Intent.ACTION_DELETE)
                    intent.data = Uri.parse("package:${app.packageName}")
                    intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    uninstallLauncher.launch(intent)
                }
            } else null,
            showAppIcons = showAppIcons,
            isHidden = app.packageName in hiddenApps,
            onToggleHide = { favoritesRepository.toggleHiddenApp(app.packageName) }
        )
    }

    val appsPerPage = 9
    val pageCount = (apps.size + appsPerPage - 1) / appsPerPage
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { page ->
                val startIndex = page * appsPerPage
                val endIndex = minOf(startIndex + appsPerPage, apps.size)
                val pageApps = apps.subList(startIndex, endIndex)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pageApps) { app ->
                        AppGridItem(
                            app = app,
                            showAppIcons = true,
                            notifications = notifications,
                            onClick = {
                                if (isPickerMode) {
                                    onAppSelected?.invoke(app)
                                } else {
                                    if (app.packageName == context.packageName) {
                                        onShowSettingsClicked?.invoke()
                                    } else {
                                        onAppLaunched?.invoke(app.packageName)
                                        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                                        if (launchIntent != null) {
                                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(launchIntent)
                                        }
                                    }
                                }
                            },
                            onLongClick = { appToEdit = app }
                        )
                    }
                }
            }

            // Bottom Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home/Back Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        } else {
                            onDismiss?.invoke()
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .border(2.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (pagerState.currentPage > 0) "Back" else "Home",
                            tint = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (pagerState.currentPage > 0) "Back" else "Home",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }

                // Page Dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pageCount) { index ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .border(1.dp, Color.Black, CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) Color.Black else Color.Transparent,
                                    CircleShape
                                )
                        )
                    }
                }

                // Next Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            if (pagerState.currentPage < pageCount - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .border(2.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Next", fontSize = 16.sp, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun AlphabetScroller(
    modifier: Modifier = Modifier,
    alphabet: List<Char>,
    onLetterSelected: (Char) -> Unit
) {
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var columnSize by remember { mutableStateOf(IntSize.Zero) }

    fun updateSelectedLetter(y: Float) {
        if (columnSize.height <= 0 || alphabet.isEmpty()) return
        val letterHeight = columnSize.height.toFloat() / alphabet.size
        val index = (y / letterHeight).toInt().coerceIn(0, alphabet.lastIndex)
        val letter = alphabet.getOrNull(index)
        if (letter != null && letter != selectedLetter) {
            selectedLetter = letter
            onLetterSelected(letter)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .onSizeChanged { columnSize = it }
                .pointerInput(alphabet) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        updateSelectedLetter(down.position.y)

                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach {
                                updateSelectedLetter(it.position.y)
                                it.consume()
                            }
                        } while (event.changes.any { it.pressed })

                        selectedLetter = null
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { letter ->
                val isSelected = selectedLetter == letter
                Text(
                    text = letter.toString(),
                    modifier = Modifier
                        .padding(4.dp)
                        .offset(x = if (isSelected) (-24).dp else 0.dp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppInfo,
    showAppIcons: Boolean,
    notifications: List<StatusBarNotification>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    fontSizeAdjustment: Int = 0,
    isTop10Item: Boolean = false,
    rank: Int = 0
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val missedCallsCount by NotificationListener.missedCallsCount.collectAsState()

    val appIcon: Drawable? = try {
        packageManager.getApplicationIcon(app.packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    val isPhoneApp = app.packageName == "com.mudita.dial" || 
                     app.packageName.contains("dialer") || 
                     app.packageName.contains("telecom")

    val appNotifications = notifications.filter { 
        it.packageName.equals(app.packageName, ignoreCase = true) 
    }
    val totalCount = if (isPhoneApp) {
        val otherCallNotificationsCount = appNotifications.filter { it.notification.category != Notification.CATEGORY_MISSED_CALL }.sumOf { getNotificationCount(it) }
        otherCallNotificationsCount + missedCallsCount
    } else {
        appNotifications.sumOf { getNotificationCount(it) }
    }

    val isMuditaApp = app.packageName.startsWith("com.mudita")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp) // Reduced from 90.dp to give more space for names
                .then(
                    if (!isMuditaApp) {
                        Modifier.border(2.dp, Color.Black, RoundedCornerShape(24.dp))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconPadding = if (isMuditaApp) 0.dp else 10.dp // Slightly reduced padding
            
            if (appIcon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = appIcon),
                    contentDescription = "${app.name} icon",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(iconPadding)
                )
            } else {
                Text(
                    text = app.name.take(1).uppercase(),
                    fontSize = 32.sp, // Slightly smaller font
                    fontWeight = FontWeight.Bold
                )
            }

            if (totalCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                        .size(24.dp)
                        .background(Color.Black, shape = CircleShape)
                        .border(1.dp, Color.White, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (totalCount > 9) "!" else totalCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = app.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppInfo,
    showAppIcons: Boolean,
    notifications: List<StatusBarNotification>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    fontSizeAdjustment: Int = 0,
    isTop10Item: Boolean = false,
    rank: Int = 0
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val missedCallsCount by NotificationListener.missedCallsCount.collectAsState()

    val appIcon: Drawable? = if (showAppIcons) {
        try {
            packageManager.getApplicationIcon(app.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    } else {
        null
    }

    // Determine if this is the phone app to include missed calls count
    val isPhoneApp = app.packageName == "com.mudita.dial" || 
                     app.packageName.contains("dialer") || 
                     app.packageName.contains("telecom")

    val appNotifications = notifications.filter { 
        it.packageName.equals(app.packageName, ignoreCase = true) 
    }
    val totalCount = if (isPhoneApp) {
        val otherCallNotificationsCount = appNotifications.filter { it.notification.category != Notification.CATEGORY_MISSED_CALL }.sumOf { getNotificationCount(it) }
        otherCallNotificationsCount + missedCallsCount
    } else {
        appNotifications.sumOf { getNotificationCount(it) }
    }

    val fontSize = if (isTop10Item) 30 else 24
    val rowHeight = if (isTop10Item) (52 + fontSizeAdjustment * 2).dp else 60.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (rank in 1..10) {
            Column(
                modifier = Modifier.width(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = rank.toString(),
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        } else if (isTop10Item) {
            // Keep the same space even when stars are hidden to keep alignment
            Spacer(modifier = Modifier.width(48.dp))
        } else {
            // Default space for alphabetical list
            Spacer(modifier = Modifier.width(32.dp))
        }

        Row(
            modifier = Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showAppIcons) {
                val iconSize = if (isTop10Item) 48.dp else 40.dp
                Box(modifier = Modifier.size(iconSize)) {
                    if (appIcon != null) {
                        Image(
                            painter = rememberDrawablePainter(drawable = appIcon),
                            contentDescription = "${app.name} icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = app.name,
                fontSize = (fontSize + fontSizeAdjustment).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (totalCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .border(BorderStroke(3.dp, Color.Black), shape = CircleShape)
                        .background(color = Color.White, shape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (totalCount > 99) "99+" else totalCount.toString(),
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
