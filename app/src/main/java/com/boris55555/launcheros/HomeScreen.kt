package com.boris55555.launcheros

import android.app.Activity
import android.app.AlarmManager
import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.provider.CallLog
import android.util.Log
import android.telecom.TelecomManager
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boris55555.launcheros.birthdays.BirthdaysRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    favoritesRepository: FavoritesRepository,
    birthdaysRepository: BirthdaysRepository,
    onShowAllAppsClicked: () -> Unit,
    onShowNotificationsClicked: () -> Unit,
    onShowBirthdaysClicked: () -> Unit,
    onLaunchAppClicked: (String) -> Unit,
    onShowSettingsClicked: () -> Unit,
    onShowRunningAppsClicked: () -> Unit,
    onEditFavorite: (Int) -> Unit,
    currentPage: Int,
    onCurrentPageChanged: (Int) -> Unit,
    bluetoothState: MainActivity.BluetoothState
) {
    MainHomeScreen(
        favoritesRepository,
        birthdaysRepository,
        onShowAllAppsClicked,
        onShowNotificationsClicked,
        onShowBirthdaysClicked,
        onLaunchAppClicked,
        onShowSettingsClicked,
        onShowRunningAppsClicked,
        onEditFavorite,
        currentPage,
        onCurrentPageChanged,
        bluetoothState
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainHomeScreen(
    favoritesRepository: FavoritesRepository,
    birthdaysRepository: BirthdaysRepository,
    onShowAllAppsClicked: () -> Unit,
    onShowNotificationsClicked: () -> Unit,
    onShowBirthdaysClicked: () -> Unit,
    onLaunchAppClicked: (String) -> Unit,
    onShowSettingsClicked: () -> Unit,
    onShowRunningAppsClicked: () -> Unit,
    onEditFavorite: (Int) -> Unit,
    currentPage: Int,
    onCurrentPageChanged: (Int) -> Unit,
    bluetoothState: MainActivity.BluetoothState
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val customNames by favoritesRepository.customNames.collectAsState()
    val favorites by favoritesRepository.favorites.collectAsState()
    val favoriteCount by favoritesRepository.favoriteCount.collectAsState()
    val batteryThreshold by favoritesRepository.batteryThreshold.collectAsState()
    val alarmAppPackage by favoritesRepository.alarmAppPackage.collectAsState()
    val calendarAppPackage by favoritesRepository.calendarAppPackage.collectAsState()
    val isHomeLocked by favoritesRepository.isHomeLocked.collectAsState()
    val use24hFormat by favoritesRepository.use24hFormat.collectAsState()
    val showAppIcons by favoritesRepository.showAppIcons.collectAsState()
    val birthdays by birthdaysRepository.birthdays.collectAsState()
    val homeNote by favoritesRepository.homeNote.collectAsState()
    val homeNoteTitle by favoritesRepository.homeNoteTitle.collectAsState()
    val showNotesButton by favoritesRepository.showNotesButton.collectAsState()
    val notificationsInStatusBar by favoritesRepository.notificationsInStatusBar.collectAsState()
    val enableRunningApps by favoritesRepository.enableRunningApps.collectAsState()

    val batteryLevel by context.batteryLevel().collectAsState(initial = null)
    val signalLevel by context.signalLevel().collectAsState(initial = 0)

    var showRefreshOverlay by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var tempShowBatteryPercentage by remember { mutableStateOf(false) }

    LaunchedEffect(tempShowBatteryPercentage) {
        if (tempShowBatteryPercentage) {
            delay(3000)
            tempShowBatteryPercentage = false
        }
    }

    val preferredApps = remember(favorites, customNames) {
        favorites.map { pkgName ->
            if (pkgName == null) {
                AppInfo("", "", isSystemApp = false)
            } else {
                try {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        `package` = pkgName
                    }
                    packageManager.queryIntentActivities(intent, 0).firstOrNull()?.let { resolveInfo ->
                        val originalName = resolveInfo.loadLabel(packageManager).toString()
                        val customName = customNames[pkgName]
                        val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        AppInfo(
                            name = customName ?: originalName,
                            packageName = resolveInfo.activityInfo.packageName,
                            customName = customName,
                            isSystemApp = isSystem || pkgName.startsWith("com.mudita")
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    val notifications by NotificationListener.notifications.collectAsState()

    // Media Controller State
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var mediaMetadata by remember { mutableStateOf<MediaMetadata?>(null) }
    var playbackState by remember { mutableStateOf<PlaybackState?>(null) }
    var dismissedMediaId by remember { mutableStateOf<String?>(null) }

    val controllerCallback = remember {
        object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                mediaMetadata = metadata
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                playbackState = state
                if (state?.state == PlaybackState.STATE_STOPPED || state?.state == PlaybackState.STATE_NONE) {
                    mediaController = null
                }
            }
        }
    }

    val mediaSessionManager = remember { context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager }
    val componentName = remember { ComponentName(context, NotificationListener::class.java) }

    fun refreshMediaController() {
        if (!isNotificationServiceEnabled(context)) {
            Log.w("HomeScreen", "Notification access not enabled, cannot get media sessions")
            return
        }

        // Try getting sessions for our component first
        var controllers = try {
            mediaSessionManager.getActiveSessions(componentName)
        } catch (e: Exception) {
            null
        }
        
        // Fallback: If no sessions found for our component, try getting ALL sessions
        // (This might require more permissions on some Android versions, but NLS usually can see all)
        if (controllers.isNullOrEmpty()) {
            controllers = try {
                mediaSessionManager.getActiveSessions(null)
            } catch (e: Exception) { null }
        }

        Log.d("HomeScreen", "Found ${controllers?.size ?: 0} active sessions.")

        // Find the most relevant controller. Priority: Playing > Paused > Buffering > Any
        val activeController = controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PAUSED }
            ?: controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_BUFFERING }
            ?: controllers?.firstOrNull()

        if (activeController != null) {
            Log.d("HomeScreen", "Using active controller: ${activeController.packageName}, state: ${activeController.playbackState?.state}")
            val currentMediaId = activeController.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) 
                ?: activeController.packageName
            
            // If this media was manually dismissed, don't show it unless it's playing
            if (dismissedMediaId == currentMediaId && activeController.playbackState?.state != PlaybackState.STATE_PLAYING) {
                Log.d("HomeScreen", "Media $currentMediaId is dismissed and not playing, hiding.")
                mediaController = null
                return
            }

            if (activeController.packageName != mediaController?.packageName) {
                mediaController?.unregisterCallback(controllerCallback)
                mediaController = activeController
                activeController.registerCallback(controllerCallback)
            }
            // Always update state from the current active controller
            mediaMetadata = activeController.metadata
            playbackState = activeController.playbackState
            
            // Reset dismissed ID if we started playing something new or state became playing
            if (activeController.playbackState?.state == PlaybackState.STATE_PLAYING) {
                dismissedMediaId = null
            }
        } else {
            if (mediaController != null) {
                Log.d("HomeScreen", "No active sessions found, clearing media controller")
                mediaController?.unregisterCallback(controllerCallback)
                mediaController = null
                mediaMetadata = null
                playbackState = null
            }
        }
    }

    DisposableEffect(context) {
        if (!isNotificationServiceEnabled(context)) {
            return@DisposableEffect onDispose {}
        }

        val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            refreshMediaController()
        }

        mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
        refreshMediaController()

        onDispose {
            mediaController?.unregisterCallback(controllerCallback)
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        }
    }

    // Refresh when returning to home or when notifications change
    LaunchedEffect(notifications) {
        refreshMediaController()
    }

    // Periodic refresh to ensure MiniPlayer appears even if session listener misses an event
    LaunchedEffect(Unit) {
        while (true) {
            if (isNotificationServiceEnabled(context)) {
                refreshMediaController()
            }
            delay(3000)
        }
    }

    val pageCount = (favoriteCount + 3) / 4
    var nextAlarmTime by remember { mutableStateOf<String?>(null) }
    var nextAlarmDay by remember { mutableStateOf<String?>(null) }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun updateAlarm() {
        val nextAlarmClock = alarmManager.nextAlarmClock
        if (nextAlarmClock != null) {
            val now = Calendar.getInstance()
            val alarmCalendar = Calendar.getInstance().apply { timeInMillis = nextAlarmClock.triggerTime }
            
            nextAlarmTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(alarmCalendar.time)
            
            val diffDays = alarmCalendar.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR)
            nextAlarmDay = when {
                diffDays == 0 -> null
                diffDays == 1 || (diffDays < 0 && diffDays > -360) -> "Tomorrow"
                else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(alarmCalendar.time)
            }
        } else {
            nextAlarmTime = null
            nextAlarmDay = null
        }
    }

    LaunchedEffect(Unit) {
        updateAlarm()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateAlarm()
            }
        }
        context.registerReceiver(receiver, IntentFilter("android.app.action.NEXT_ALARM_CLOCK_CHANGED"))
    }

    if (showEditNoteDialog) {
        var tempTitle by remember { mutableStateOf(homeNoteTitle) }
        var tempContent by remember { mutableStateOf(homeNote) }

        AlertDialog(
            onDismissRequest = { showEditNoteDialog = false },
            title = { Text("Edit Note", color = Color.Black) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempContent,
                        onValueChange = { tempContent = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black
                        )
                    )
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    EInkButton(onClick = {
                        favoritesRepository.saveHomeNoteTitle("")
                        favoritesRepository.saveHomeNote("")
                        showEditNoteDialog = false
                    }) {
                        Text("Clear")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    EInkButton(onClick = { showEditNoteDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    EInkButton(onClick = {
                        favoritesRepository.saveHomeNoteTitle(tempTitle)
                        favoritesRepository.saveHomeNote(tempContent)
                        showEditNoteDialog = false
                    }) {
                        Text("Save")
                    }
                }
            },
            dismissButton = null,
            containerColor = Color.White
        )
    }

    fun handleSwipe(action: String) {
        when (action) {
            "notifications" -> onShowNotificationsClicked()
            "birthdays" -> onShowBirthdaysClicked()
            else -> if (action.startsWith("app:")) {
                onLaunchAppClicked(action.substring(4))
            }
        }
    }

    val missedCallsCount by NotificationListener.missedCallsCount.collectAsState()
    val unreadSmsCount by NotificationListener.unreadSmsCount.collectAsState()
    val activeCall by NotificationListener.activeCall.collectAsState()

    val phoneBadgeCount = remember(notifications, missedCallsCount, activeCall) {
        val callNotifications = notifications.filter { getNotificationCategory(it, context) == NotificationCategory.CALLS }
        val otherCallNotificationsCount = callNotifications.filter { it.notification.category != android.app.Notification.CATEGORY_MISSED_CALL }.sumOf { getNotificationCount(it) }
        val hasOngoingCallNotification = callNotifications.any { (it.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0 }
        var count = otherCallNotificationsCount + missedCallsCount
        if (activeCall != null && !hasOngoingCallNotification) {
            count += 1
        }
        count
    }

    val smsBadgeCount = remember(notifications, unreadSmsCount) {
        val messageNotifications = notifications.filter { getNotificationCategory(it, context) == NotificationCategory.MESSAGES }
        val messageNotificationsByApp = messageNotifications.groupBy { it.packageName }
        val smsAppPackages = listOf("com.mudita.messages", "com.android.messaging", "com.google.android.apps.messaging")
        var smsAppNotificationsCount = 0
        messageNotificationsByApp.forEach { (pkg, notifs) ->
            val count = getSmartNotificationCount(notifs)
            if (pkg in smsAppPackages) {
                smsAppNotificationsCount += count
            }
        }
        maxOf(unreadSmsCount, smsAppNotificationsCount)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isHomeLocked) {
                detectTapGestures(
                    onLongPress = { if (!isHomeLocked) onShowSettingsClicked() },
                    onDoubleTap = { showRefreshOverlay = true }
                )
            }
            .homeScreenGestures(
                gesturesEnabled = true,
                favoritesArea = null,
                onSwipeUp = {},
                onSwipeLeft = { if (enableRunningApps) onShowRunningAppsClicked() },
                onSwipeRight = { if (enableRunningApps) onShowRunningAppsClicked() },
                onFavoritesSwipeUp = {},
                onFavoritesSwipeDown = {}
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HomeStatusBar(
                favoritesRepository,
                tempShowBatteryPercentage,
                batteryLevel,
                signalLevel,
                notifications,
                bluetoothState,
                notificationsInStatusBar,
                onShowNotificationsClicked
            ) {
                tempShowBatteryPercentage = true
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 0.dp)
            ) {
                ClockSection(
                    favoritesRepository = favoritesRepository,
                    use24hFormat = use24hFormat,
                    nextAlarmTime = nextAlarmTime,
                    nextAlarmDay = nextAlarmDay,
                    alarmAppPackage = alarmAppPackage,
                    calendarAppPackage = calendarAppPackage,
                    packageManager = packageManager,
                    context = context
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                MediaSection(
                    mediaController = mediaController,
                    playbackState = playbackState,
                    mediaMetadata = mediaMetadata,
                    packageManager = packageManager,
                    context = context,
                    onClose = { 
                        val pkgName = mediaController?.packageName
                        try {
                            // 1. Stop playback
                            mediaController?.transportControls?.stop()
                            
                            // 2. Try to kill the background process
                            if (pkgName != null) {
                                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                                am.killBackgroundProcesses(pkgName)
                                Log.d("HomeScreen", "Requested kill for $pkgName")
                            }
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Error closing media app", e)
                        }

                        dismissedMediaId = mediaMetadata?.getString(MediaMetadata.METADATA_KEY_TITLE) 
                            ?: pkgName
                        mediaController = null 
                    }
                )
                
                NotificationSection(
                    notifications = notifications,
                    bluetoothState = bluetoothState,
                    birthdays = birthdays,
                    notificationsInStatusBar = notificationsInStatusBar,
                    onShowNotificationsClicked = onShowNotificationsClicked
                )
            }

            BottomNavigationSection(
                phoneBadgeCount = phoneBadgeCount,
                smsBadgeCount = smsBadgeCount,
                onShowAllAppsClicked = onShowAllAppsClicked,
                context = context,
                packageManager = packageManager,
                showCameraShortcut = favoritesRepository.showCameraShortcut.collectAsState().value
            )
        }

        if (showRefreshOverlay) {
            RefreshOverlay { showRefreshOverlay = false }
        }
    }
}

@Composable
fun HomeStatusBar(
    favoritesRepository: FavoritesRepository,
    tempShowBatteryPercentage: Boolean,
    batteryLevel: BatteryState?,
    signalLevel: Int,
    notifications: List<android.service.notification.StatusBarNotification>,
    bluetoothState: MainActivity.BluetoothState,
    notificationsInStatusBar: Boolean,
    onNotificationClick: () -> Unit,
    onBatteryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            SignalIcon(signalLevel)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "LTE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (notificationsInStatusBar) {
                SmallNotificationIndicator(
                    notifications = notifications,
                    bluetoothState = bluetoothState,
                    onClick = onNotificationClick
                )
            }
        }

        val batteryThresholdState by favoritesRepository.batteryThreshold.collectAsState()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onBatteryClick
            )) {
                BatteryIcon(batteryLevel, batteryThresholdState, tempShowBatteryPercentage)
            }
        }
    }
}

@Composable
fun SmallNotificationIndicator(
    notifications: List<android.service.notification.StatusBarNotification>,
    bluetoothState: MainActivity.BluetoothState = MainActivity.BluetoothState.Disabled,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val missedCallsCount by NotificationListener.missedCallsCount.collectAsState()
    val unreadSmsCount by NotificationListener.unreadSmsCount.collectAsState()
    val activeCall by NotificationListener.activeCall.collectAsState()

    val groupedNotifications = remember(notifications, missedCallsCount, unreadSmsCount, activeCall, bluetoothState) {
        val result = mutableMapOf<NotificationCategory, Int>()
        
        notifications.forEach { sbn ->
            val category = getNotificationCategory(sbn, context)
            if (category != NotificationCategory.CALLS && category != NotificationCategory.MESSAGES) {
                val count = getNotificationCount(sbn)
                result[category] = (result[category] ?: 0) + count
            }
        }

        if (bluetoothState is MainActivity.BluetoothState.Connected) {
            result[NotificationCategory.BLUETOOTH] = 1
        }

        val callNotifications = notifications.filter { getNotificationCategory(it, context) == NotificationCategory.CALLS }
        val otherCallNotificationsCount = callNotifications.filter { it.notification.category != android.app.Notification.CATEGORY_MISSED_CALL }.sumOf { getNotificationCount(it) }
        val hasOngoingCallNotification = callNotifications.any { (it.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0 }

        var finalCallCount = otherCallNotificationsCount + missedCallsCount
        if (activeCall != null && !hasOngoingCallNotification) {
            finalCallCount += 1
        }

        if (finalCallCount > 0) {
            result[NotificationCategory.CALLS] = finalCallCount
        }

        val messageNotifications = notifications.filter { getNotificationCategory(it, context) == NotificationCategory.MESSAGES }
        val messageNotificationsByApp = messageNotifications.groupBy { it.packageName }
        val smsAppPackages = listOf("com.mudita.messages", "com.android.messaging", "com.google.android.apps.messaging")
        
        var smsAppNotificationsCount = 0
        var otherMessageNotificationsCount = 0
        
        messageNotificationsByApp.forEach { (pkg, notifs) ->
            val count = getSmartNotificationCount(notifs)
            if (pkg in smsAppPackages) {
                smsAppNotificationsCount += count
            } else {
                otherMessageNotificationsCount += count
            }
        }
        
        val finalMessageCount = otherMessageNotificationsCount + maxOf(unreadSmsCount, smsAppNotificationsCount)
        if (finalMessageCount > 0) {
            result[NotificationCategory.MESSAGES] = finalMessageCount
        }

        result
    }

    if (groupedNotifications.isNotEmpty()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            groupedNotifications.entries.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = entry.key.icon,
                        contentDescription = entry.key.name,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    if (entry.key != NotificationCategory.BLUETOOTH) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = entry.value.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
                if (index < groupedNotifications.size - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
fun ClockSection(
    favoritesRepository: FavoritesRepository,
    use24hFormat: Boolean,
    nextAlarmTime: String?,
    nextAlarmDay: String?,
    alarmAppPackage: String?,
    calendarAppPackage: String?,
    packageManager: PackageManager,
    context: Context
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 12.dp)
    ) {
        if (nextAlarmTime != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccessAlarm,
                    contentDescription = "Next Alarm",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = nextAlarmTime + (if (nextAlarmDay != null) " ($nextAlarmDay)" else ""),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        var currentTime by remember(use24hFormat) {
            val pattern = if (use24hFormat) "HH:mm" else "h:mm a"
            mutableStateOf(SimpleDateFormat(pattern, Locale.getDefault()).format(Calendar.getInstance().time))
        }
        LaunchedEffect(use24hFormat) {
            while (true) {
                val pattern = if (use24hFormat) "HH:mm" else "h:mm a"
                currentTime = SimpleDateFormat(pattern, Locale.getDefault()).format(Calendar.getInstance().time)
                delay(1000)
            }
        }
        
        Text(
            text = buildAnnotatedString {
                if (use24hFormat) {
                    append(currentTime)
                } else {
                    val parts = currentTime.split(" ")
                    if (parts.size >= 2) {
                        append(parts[0])
                        withStyle(style = SpanStyle(fontSize = 30.sp)) {
                            append(" " + parts[1])
                        }
                    } else {
                        append(currentTime)
                    }
                }
            },
            fontSize = 80.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            modifier = Modifier.clickable {
                alarmAppPackage?.let { pkg ->
                    val intent = packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(modifier = Modifier.clickable {
            calendarAppPackage?.let { pkg ->
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
        }) {
            DateText(favoritesRepository)
        }
    }
}

@Composable
fun MediaSection(
    mediaController: MediaController?,
    playbackState: PlaybackState?,
    mediaMetadata: MediaMetadata?,
    packageManager: PackageManager,
    context: Context,
    onClose: () -> Unit
) {
    if (mediaController != null && (playbackState?.state == PlaybackState.STATE_PLAYING || playbackState?.state == PlaybackState.STATE_PAUSED)) {
        MiniPlayer(
            mediaMetadata = mediaMetadata,
            playbackState = playbackState,
            mediaController = mediaController,
            onClose = onClose,
            onClick = {
                mediaController.packageName?.let { pkg ->
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun NotificationSection(
    notifications: List<android.service.notification.StatusBarNotification>,
    bluetoothState: MainActivity.BluetoothState,
    birthdays: List<com.boris55555.launcheros.birthdays.Birthday>,
    notificationsInStatusBar: Boolean,
    onShowNotificationsClicked: () -> Unit
) {
    val today = LocalDate.now()
    val todaysBirthdays = remember(birthdays, today) {
        birthdays.filter { it.date.month == today.month && it.date.dayOfMonth == today.dayOfMonth }
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!notificationsInStatusBar) {
            NotificationIndicator(
                notifications = notifications,
                bluetoothState = bluetoothState,
                onClick = onShowNotificationsClicked
            )
            if (todaysBirthdays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (todaysBirthdays.isNotEmpty()) {
            todaysBirthdays.forEach { birthday ->
                val age = ChronoUnit.YEARS.between(birthday.date, today)
                Text(
                    text = "🎂 ${birthday.name} ($age)",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun BottomNavigationSection(
    phoneBadgeCount: Int,
    smsBadgeCount: Int,
    onShowAllAppsClicked: () -> Unit,
    context: Context,
    packageManager: PackageManager,
    showCameraShortcut: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        HomeNavButton(
            icon = Icons.Default.Phone,
            label = "Phone",
            badgeCount = phoneBadgeCount,
            isMudita = true,
            packageName = "com.mudita.dial",
            onClick = {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                if (telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE) {
                    try {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                            telecomManager.showInCallScreen(false)
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                type = CallLog.Calls.CONTENT_TYPE
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            type = CallLog.Calls.CONTENT_TYPE
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                } else {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        type = CallLog.Calls.CONTENT_TYPE
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        )
        
        HomeNavButton(
            icon = Icons.Default.Sms,
            label = "SMS",
            badgeCount = smsBadgeCount,
            isMudita = true,
            packageName = "com.mudita.messages",
            onClick = {
                val muditaSms = "com.mudita.messages"
                val launchIntent = packageManager.getLaunchIntentForPackage(muditaSms)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                } else {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_APP_MESSAGING)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))
                        smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(smsIntent)
                    }
                }
            }
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showCameraShortcut) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(2.dp, Color.Black, CircleShape)
                        .background(Color.White, CircleShape)
                        .clickable {
                            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val launchIntent = packageManager.getLaunchIntentForPackage("com.mudita.camera")
                                    ?: packageManager.getLaunchIntentForPackage("com.android.camera")
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launchIntent)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            HomeNavButton(
                icon = Icons.Default.Apps,
                label = "Apps",
                onClick = onShowAllAppsClicked
            )
        }
    }
}

@Composable
fun RefreshOverlay(onDismiss: () -> Unit) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black))
    LaunchedEffect(Unit) {
        delay(120L)
        onDismiss()
    }
}

@Composable
fun HomeNavButton(
    icon: ImageVector,
    label: String,
    badgeCount: Int = 0,
    isMudita: Boolean = false,
    packageName: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    val muditaIcon: android.graphics.drawable.Drawable? = remember(packageName) {
        if (isMudita && packageName != null) {
            try {
                packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(85.dp) // Reduced from 90.dp
                .then(
                    if (muditaIcon == null) {
                        Modifier.border(2.dp, Color.Black, RoundedCornerShape(24.dp))
                    } else Modifier
                )
                .padding(if (muditaIcon == null) 20.dp else 2.dp), // Reduced padding for Mudita icons
            contentAlignment = Alignment.Center
        ) {
            if (muditaIcon != null) {
                Image(
                    painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(drawable = muditaIcon),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Black
                )
            }
            
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(26.dp)
                        .background(Color.Black, shape = CircleShape)
                        .border(1.dp, Color.White, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 9) "!" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

data class BatteryState(val level: Int, val isCharging: Boolean)

fun Context.batteryLevel(): Flow<BatteryState> = callbackFlow {
    val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                             status == BatteryManager.BATTERY_STATUS_FULL
            
            val percentage = if (level != -1 && scale != -1) {
                (level * 100 / scale.toFloat()).toInt()
            } else {
                -1
            }
            trySend(BatteryState(percentage, isCharging))
        }
    }
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    registerReceiver(batteryReceiver, filter)
    awaitClose { unregisterReceiver(batteryReceiver) }
}

fun Context.signalLevel(): Flow<Int> = callbackFlow {
    val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                trySend(signalStrength.level)
            }
        }
        telephonyManager.registerTelephonyCallback(mainExecutor, callback)
        awaitClose { telephonyManager.unregisterTelephonyCallback(callback) }
    } else {
        // Fallback for older versions if needed, though Mudita Kompakt is Android 11+
        trySend(0)
        awaitClose { }
    }
}

@Composable
fun SignalIcon(level: Int) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((i * 4).dp)
                    .then(
                        if (i <= level) {
                            Modifier.background(Color.Black)
                        } else {
                            Modifier.border(1.dp, Color.Black)
                        }
                    )
            )
        }
    }
}

@Composable
fun BatteryIcon(state: BatteryState?, threshold: Int, forceShow: Boolean = false) {
    if (state == null) return
    
    val shouldShowPercentage = when {
        forceShow -> true
        threshold >= 100 -> true
        threshold < 0 -> false
        else -> state.level <= threshold || state.isCharging
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (state.isCharging) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Charging",
                modifier = Modifier.size(16.dp),
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        if (shouldShowPercentage) {
            Text(text = "${state.level}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.width(6.dp))
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Battery Body
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(13.dp)
                    .border(1.5.dp, Color.Black, RoundedCornerShape(1.dp))
                    .padding(1.5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(state.level / 100f)
                        .background(Color.Black)
                )
            }
            // Battery Tip
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(5.dp)
                    .background(Color.Black, RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp))
            )
        }
    }
}
