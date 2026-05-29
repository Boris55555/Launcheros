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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Pets
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Phone
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
    onEditFavorite: (Int) -> Unit,
    currentPage: Int,
    onCurrentPageChanged: (Int) -> Unit,
    bluetoothState: MainActivity.BluetoothState
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var showRefreshOverlay by remember { mutableStateOf(false) }

    val favoritePackages by favoritesRepository.favorites.collectAsState()
    val favoriteCount by favoritesRepository.favoriteCount.collectAsState()
    val alarmAppPackage by favoritesRepository.alarmAppPackage.collectAsState()
    val calendarAppPackage by favoritesRepository.calendarAppPackage.collectAsState()
    val isHomeLocked by favoritesRepository.isHomeLocked.collectAsState()
    val customNames by favoritesRepository.customNames.collectAsState()
    val birthdays by birthdaysRepository.birthdays.collectAsState()
    val gesturesEnabled by favoritesRepository.gesturesEnabled.collectAsState()
    val swipeLeftAction by favoritesRepository.swipeLeftAction.collectAsState()
    val swipeRightAction by favoritesRepository.swipeRightAction.collectAsState()
    val keepAllAppsButton = true // Always on
    val showAppIcons by favoritesRepository.showAppIcons.collectAsState()
    val showNotificationPreviews by favoritesRepository.showNotificationPreviews.collectAsState()
    val sexyMode by favoritesRepository.sexyMode.collectAsState()
    val sexyAlignment by favoritesRepository.sexyAlignment.collectAsState()
    val enableCameraShortcut = true // Always on
    val showNotesButton by favoritesRepository.showNotesButton.collectAsState()
    val homeNote by favoritesRepository.homeNote.collectAsState()
    val homeNoteTitle by favoritesRepository.homeNoteTitle.collectAsState()
    val fontSizeHome by favoritesRepository.fontSizeHome.collectAsState()
    val use24hFormat by favoritesRepository.use24hFormat.collectAsState()

    val fontSizeAdjustment = when (fontSizeHome) {
        "Small" -> -2
        "Big" -> 2
        else -> 0
    }

    val favoriteApps = remember(favoritePackages, customNames) {
        favoritePackages.map { pkgName ->
            if (pkgName == null) null
            else {
                try {
                    val intent = Intent(Intent.ACTION_MAIN, null).apply {
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
            
            nextAlarmDay = when {
                now.get(Calendar.YEAR) == alarmCalendar.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == alarmCalendar.get(Calendar.DAY_OF_YEAR) -> null
                
                now.get(Calendar.YEAR) == alarmCalendar.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) + 1 == alarmCalendar.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"
                
                else -> SimpleDateFormat("EEE", Locale.getDefault()).format(alarmCalendar.time)
            }
        } else {
            nextAlarmTime = null
            nextAlarmDay = null
        }
    }

    DisposableEffect(context) {
        val receiver = AlarmUpdateReceiver(::updateAlarm)
        context.registerReceiver(receiver, IntentFilter("android.app.action.NEXT_ALARM_CLOCK_CHANGED"))
        updateAlarm() // Initial update
        onDispose { context.unregisterReceiver(receiver) }
    }

    if (currentPage >= pageCount) {
        onCurrentPageChanged(0)
    }

    val batteryLevel by context.batteryLevel().collectAsState(initial = null)
    val signalLevel by context.signalLevel().collectAsState(initial = 0)
    var showStatusDetails by remember { mutableStateOf(false) }

    LaunchedEffect(showStatusDetails) {
        if (showStatusDetails) {
            delay(3000)
            showStatusDetails = false
        }
    }
    var favoritesArea by remember { mutableStateOf<Rect?>(null) }
    var showEditNoteDialog by remember { mutableStateOf(false) }

    if (showEditNoteDialog) {
        var tempTitle by remember { mutableStateOf(homeNoteTitle) }
        var tempContent by remember { mutableStateOf(homeNote) }
        AlertDialog(
            onDismissRequest = { showEditNoteDialog = false },
            title = { Text("Edit Home Note", color = Color.Black) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempContent,
                        onValueChange = { tempContent = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EInkButton(onClick = {
                        tempTitle = ""
                        tempContent = ""
                        favoritesRepository.saveHomeNoteTitle("")
                        favoritesRepository.saveHomeNote("")
                        showEditNoteDialog = false
                    }) {
                        Text("Clear")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Bar (Copy of assets screenshot, no clock)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignalIcon(signalLevel)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "LTE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                BatteryIcon(batteryLevel)
            }

            // Central Section: Clock and Date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top, // Changed from Center to Top
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 24.dp) // Reduced padding to move it higher
            ) {
                // Next Alarm (Above Clock)
                if (nextAlarmTime != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            alarmAppPackage?.let { pkg ->
                                val intent = packageManager.getLaunchIntentForPackage(pkg)
                                if (intent != null) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessAlarm,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = nextAlarmTime!! + (if (nextAlarmDay != null) " ($nextAlarmDay)" else ""),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
                
                // Extra Large Clock
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
                
                // Date
                Box(modifier = Modifier.clickable {
                    calendarAppPackage?.let { pkg ->
                        val intent = packageManager.getLaunchIntentForPackage(pkg)
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
                }) {
                    DateText()
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // MiniPlayer (Below Date)
                if (mediaController != null && (playbackState?.state == PlaybackState.STATE_PLAYING || playbackState?.state == PlaybackState.STATE_PAUSED)) {
                    MiniPlayer(
                        mediaMetadata = mediaMetadata,
                        playbackState = playbackState,
                        mediaController = mediaController,
                        onClose = { 
                            dismissedMediaId = mediaMetadata?.getString(MediaMetadata.METADATA_KEY_TITLE) 
                                ?: mediaController?.packageName
                            mediaController = null 
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Birthdays and Notifications
                val today = LocalDate.now()
                val todaysBirthdays = birthdays.filter { it.date.month == today.month && it.date.dayOfMonth == today.dayOfMonth }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NotificationIndicator(
                        notifications = notifications,
                        bluetoothState = bluetoothState,
                        onClick = onShowNotificationsClicked
                    )
                    
                    if (todaysBirthdays.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    todaysBirthdays.forEach { birthday ->
                        val age = ChronoUnit.YEARS.between(birthday.date, today)
                        Text(
                            text = "🎂 ${birthday.name} ($age)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                                // Fallback if categorical intent fails
                                val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))
                                smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(smsIntent)
                            }
                        }
                    }
                )
                
                HomeNavButton(
                    icon = Icons.Default.Apps,
                    label = "Apps",
                    onClick = onShowAllAppsClicked
                )
            }
        }

        // Refresh overlay on top
        if (showRefreshOverlay) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black))
            LaunchedEffect(Unit) {
                delay(120L)
                showRefreshOverlay = false
            }
        }
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
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-10).dp)
                        .size(24.dp)
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
            fontWeight = FontWeight.Normal,
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
fun BatteryIcon(state: BatteryState?) {
    if (state == null) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (state.isCharging) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Charging",
                modifier = Modifier.size(16.dp),
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        Text(text = "${state.level}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.width(6.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Battery Body
            Box(
                modifier = Modifier
                    .width(35.dp)
                    .height(18.dp)
                    .border(2.dp, Color.Black, RoundedCornerShape(2.dp))
                    .padding(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    val segments = 5
                    val activeSegments = (state.level / (100f / segments)).toInt().coerceIn(0, segments)
                    
                    for (i in 1..segments) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (i <= activeSegments) Color.Black else Color.Transparent)
                        )
                    }
                }
            }
            // Battery Tip
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(8.dp)
                    .background(Color.Black, RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp))
            )
        }
    }
}
