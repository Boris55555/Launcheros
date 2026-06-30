package com.boris55555.launcheros

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.os.Build

@Composable
fun SettingsScreen(
    favoritesRepository: FavoritesRepository,
    onChooseAlarmAppClicked: () -> Unit,
    onChooseCalendarAppClicked: () -> Unit,
    onChooseSwipeLeftAppClicked: () -> Unit,
    onChooseSwipeRightAppClicked: () -> Unit,
    onBirthdaysClicked: () -> Unit,
    onHiddenAppsClicked: () -> Unit
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var hasPhonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasBluetoothPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(
            (context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager).canScheduleExactAlarms()
        )
    }
    var hasUsageStatsPermission by remember {
        mutableStateOf(
            run {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                val mode = appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
                mode == android.app.AppOpsManager.MODE_ALLOWED
            }
        )
    }
    val alarmAppPackage by favoritesRepository.alarmAppPackage.collectAsState()
    val calendarAppPackage by favoritesRepository.calendarAppPackage.collectAsState()
    val weekStartsOnSunday by favoritesRepository.weekStartsOnSunday.collectAsState()
    val disableDuraSpeedNotifications by favoritesRepository.disableDuraSpeedNotifications.collectAsState()
    val use24hFormat by favoritesRepository.use24hFormat.collectAsState()
    val showCameraShortcut by favoritesRepository.showCameraShortcut.collectAsState()
    val showNotificationPreviews by favoritesRepository.showNotificationPreviews.collectAsState()
    val notificationMaxCharacters by favoritesRepository.notificationMaxCharacters.collectAsState()

    var isDefaultLauncher by remember { mutableStateOf(false) }

    // Check if we are the default launcher
    fun checkDefaultLauncher() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        isDefaultLauncher = resolveInfo?.activityInfo?.packageName == context.packageName
    }

    val favoriteCount by favoritesRepository.favoriteCount.collectAsState()

    val alarmAppName = remember(alarmAppPackage) {
        alarmAppPackage?.let {
            try {
                val packageManager = context.packageManager
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                "Unknown"
            }
        } ?: "Not Set"
    }

    val calendarAppName = remember(calendarAppPackage) {
        calendarAppPackage?.let {
            try {
                val packageManager = context.packageManager
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                "Unknown"
            }
        } ?: "Not Set"
    }

    // This makes the permission status update if the user grants it and returns to the app
    LaunchedEffect(Unit) {
        while(true) {
            checkDefaultLauncher()
            val isEnabled = isNotificationServiceEnabled(context)
            if (isEnabled != hasNotificationPermission) {
                hasNotificationPermission = isEnabled
            }
            
            val phoneEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
            if (phoneEnabled != hasPhonePermission) {
                hasPhonePermission = phoneEnabled
            }

            val contactsEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            if (contactsEnabled != hasContactsPermission) {
                hasContactsPermission = contactsEnabled
            }

            val smsEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
            if (smsEnabled != hasSmsPermission) {
                hasSmsPermission = smsEnabled
            }

            val btEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (btEnabled != hasBluetoothPermission) {
                hasBluetoothPermission = btEnabled
            }
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val alarmEnabled = alarmManager.canScheduleExactAlarms()
            if (alarmEnabled != hasExactAlarmPermission) {
                hasExactAlarmPermission = alarmEnabled
            }

            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val usageMode = appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
            val usageEnabled = usageMode == android.app.AppOpsManager.MODE_ALLOWED
            if (usageEnabled != hasUsageStatsPermission) {
                hasUsageStatsPermission = usageEnabled
            }

            delay(1000)
        }
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val eInkSwitchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = Color.Black,
        checkedBorderColor = Color.Black,
        uncheckedThumbColor = Color.Black,
        uncheckedTrackColor = Color.White,
        uncheckedBorderColor = Color.Black
    )

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPhonePermission = permissions.values.all { it }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBluetoothPermission = isGranted
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            HeaderSection()

            PermissionSection(
                context = context,
                hasNotificationPermission = hasNotificationPermission,
                hasPhonePermission = hasPhonePermission,
                hasContactsPermission = hasContactsPermission,
                hasSmsPermission = hasSmsPermission,
                hasBluetoothPermission = hasBluetoothPermission,
                hasExactAlarmPermission = hasExactAlarmPermission,
                hasUsageStatsPermission = hasUsageStatsPermission,
                phonePermissionLauncher = phonePermissionLauncher,
                contactsPermissionLauncher = contactsPermissionLauncher,
                smsPermissionLauncher = smsPermissionLauncher,
                bluetoothPermissionLauncher = bluetoothPermissionLauncher
            )

            AddQuickActionsSection(
                onBirthdaysClicked = onBirthdaysClicked
            )

            AppSelectionSection(
                alarmAppName = alarmAppName,
                calendarAppName = calendarAppName,
                onChooseAlarmAppClicked = onChooseAlarmAppClicked,
                onChooseCalendarAppClicked = onChooseCalendarAppClicked
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("App Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
            HorizontalDivider(color = Color.Black)

            HiddenAppsSection(
                onHiddenAppsClicked = onHiddenAppsClicked
            )

            DefaultLauncherSection(context, isDefaultLauncher)

            HomeScreenSection(
                favoritesRepository = favoritesRepository,
                use24hFormat = use24hFormat,
                eInkSwitchColors = eInkSwitchColors
            )

            GeneralFontSection(
                favoritesRepository = favoritesRepository,
                weekStartsOnSunday = weekStartsOnSunday,
                eInkSwitchColors = eInkSwitchColors
            )

            ExperimentalSection(
                context = context,
                favoritesRepository = favoritesRepository,
                disableDuraSpeedNotifications = disableDuraSpeedNotifications,
                eInkSwitchColors = eInkSwitchColors
            )
        }

        if (scrollState.canScrollBackward) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Scroll Up",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .clickable {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(0)
                        }
                    },
                tint = Color.Black
            )
        }

        if (scrollState.canScrollForward) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll Down",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .clickable {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                tint = Color.Black
            )
        }
    }
}

@Composable
fun HeaderSection() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
        Icon(Icons.Default.Tune, contentDescription = "Control Panel Icon", tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Control Panel", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
    }
}

@Composable
fun PermissionSection(
    context: Context,
    hasNotificationPermission: Boolean,
    hasPhonePermission: Boolean,
    hasContactsPermission: Boolean,
    hasSmsPermission: Boolean,
    hasBluetoothPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    hasUsageStatsPermission: Boolean,
    phonePermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    contactsPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    smsPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    bluetoothPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    var isExpanded by remember { mutableStateOf(false) }
    val allGranted = hasNotificationPermission && hasPhonePermission && 
                     hasContactsPermission && hasSmsPermission && 
                     hasBluetoothPermission && hasExactAlarmPermission &&
                     hasUsageStatsPermission

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Permissions", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.Black
                )
                if (allGranted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Check, 
                        contentDescription = "All Permissions Granted",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.Black
            )
        }
        
        if (isExpanded) {
            Spacer(modifier = Modifier.height(16.dp))
            PermissionRow(
                title = "Notification Access",
                isGranted = hasNotificationPermission,
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            )

            PermissionRow(
                title = "Phone Call Access",
                isGranted = hasPhonePermission,
                onClick = {
                    phonePermissionLauncher.launch(
                        arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)
                    )
                }
            )

            PermissionRow(
                title = "Contact Access",
                isGranted = hasContactsPermission,
                onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
            )

            PermissionRow(
                title = "SMS Access",
                isGranted = hasSmsPermission,
                onClick = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS) }
            )

            PermissionRow(
                title = "Bluetooth Access",
                isGranted = hasBluetoothPermission,
                onClick = { bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }
            )

            PermissionRow(
                title = "Exact Alarm Access",
                isGranted = hasExactAlarmPermission,
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Error opening exact alarm settings", e)
                    }
                }
            )

            PermissionRow(
                title = "Usage Access",
                isGranted = hasUsageStatsPermission,
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        })
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                }
            )
        }
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun PermissionRow(title: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted) { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 18.sp, color = Color.Black)
        Text(if (isGranted) "Granted" else "Tap to grant", color = Color.Black)
    }
    HorizontalDivider(color = Color.Black)
}

@Composable
fun DefaultLauncherSection(context: Context, isDefault: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Set as default launcher", fontSize = 18.sp, color = Color.Black)
        if (isDefault) {
            Icon(Icons.Default.Check, contentDescription = "Default Launcher", tint = Color.Black)
        }
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun AddQuickActionsSection(
    onBirthdaysClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Add:", fontSize = 18.sp, color = Color.Black)
        Row {
            EInkButton(onClick = onBirthdaysClicked) {
                Text("Birthday")
            }
        }
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun HiddenAppsSection(
    onHiddenAppsClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHiddenAppsClicked() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Hidden Apps", fontSize = 18.sp, color = Color.Black)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Black)
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun AppSelectionSection(
    alarmAppName: String,
    calendarAppName: String,
    onChooseAlarmAppClicked: () -> Unit,
    onChooseCalendarAppClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChooseAlarmAppClicked() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Choose alarm app", fontSize = 18.sp, color = Color.Black)
        Text(alarmAppName, color = Color.Black)
    }

    HorizontalDivider(color = Color.Black)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChooseCalendarAppClicked() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Choose calendar app", fontSize = 18.sp, color = Color.Black)
        Text(calendarAppName, color = Color.Black)
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun HomeScreenSection(
    favoritesRepository: FavoritesRepository,
    use24hFormat: Boolean,
    eInkSwitchColors: androidx.compose.material3.SwitchColors
) {
    val batteryThreshold by favoritesRepository.batteryThreshold.collectAsState()
    val showCameraShortcut by favoritesRepository.showCameraShortcut.collectAsState()
    val notificationsInStatusBar by favoritesRepository.notificationsInStatusBar.collectAsState()
    val enableRunningApps by favoritesRepository.enableRunningApps.collectAsState()
    val dateFormat by favoritesRepository.dateFormat.collectAsState()
    var showBatteryDropdown by remember { mutableStateOf(false) }
    var showDateFormatDropdown by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(32.dp))
    Text("Homescreen settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = Color.Black)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Use 24h clock format", fontSize = 18.sp, color = Color.Black)
        Switch(
            checked = use24hFormat,
            onCheckedChange = { favoritesRepository.saveUse24hFormat(it) },
            colors = eInkSwitchColors
        )
    }

    HorizontalDivider(color = Color.Black)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Enable Running Apps gesture", fontSize = 18.sp, color = Color.Black)
        Switch(
            checked = enableRunningApps,
            onCheckedChange = { favoritesRepository.saveEnableRunningApps(it) },
            colors = eInkSwitchColors
        )
    }

    HorizontalDivider(color = Color.Black)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Notifications in Status Bar", fontSize = 18.sp, color = Color.Black)
        Switch(
            checked = notificationsInStatusBar,
            onCheckedChange = { favoritesRepository.saveNotificationsInStatusBar(it) },
            colors = eInkSwitchColors
        )
    }

    HorizontalDivider(color = Color.Black)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDateFormatDropdown = true }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Date format", fontSize = 18.sp, color = Color.Black)
            val currentLabel = when (dateFormat) {
                "dd.MM.yyyy" -> "pp.kk.vvvv"
                "EEEE, d. MMMM" -> "Default (Weekday, Day Month)"
                "d. MMMM yyyy" -> "Day Month Year"
                "yyyy-MM-dd" -> "Year-Month-Day"
                "MM/dd/yyyy" -> "Month/Day/Year"
                else -> dateFormat
            }
            Text(currentLabel, color = Color.Black)
        }

        DropdownMenu(
            expanded = showDateFormatDropdown,
            onDismissRequest = { showDateFormatDropdown = false },
            modifier = Modifier.background(Color.White)
        ) {
            listOf(
                "pp.kk.vvvv" to "dd.MM.yyyy",
                "Default (Weekday, d. MMMM)" to "EEEE, d. MMMM",
                "d. MMMM yyyy" to "d. MMMM yyyy",
                "yyyy-MM-dd" to "yyyy-MM-dd",
                "MM/dd/yyyy" to "MM/dd/yyyy"
            ).forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label, color = Color.Black) },
                    onClick = {
                        favoritesRepository.saveDateFormat(value)
                        showDateFormatDropdown = false
                    }
                )
            }
        }
    }

    HorizontalDivider(color = Color.Black)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Show Camera shortcut", fontSize = 18.sp, color = Color.Black)
        Switch(
            checked = showCameraShortcut,
            onCheckedChange = { favoritesRepository.saveShowCameraShortcut(it) },
            colors = eInkSwitchColors
        )
    }

    HorizontalDivider(color = Color.Black)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBatteryDropdown = true }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Show battery percentage", fontSize = 18.sp, color = Color.Black)
            val currentText = when {
                batteryThreshold >= 100 -> "Always"
                batteryThreshold < 0 -> "Never"
                else -> "Below $batteryThreshold%"
            }
            Text(currentText, color = Color.Black)
        }

        DropdownMenu(
            expanded = showBatteryDropdown,
            onDismissRequest = { showBatteryDropdown = false },
            modifier = Modifier.background(Color.White)
        ) {
            listOf(
                "Always" to 100,
                "Below 50%" to 50,
                "Below 40%" to 40,
                "Below 30%" to 30,
                "Below 20%" to 20,
                "Below 10%" to 10,
                "Never" to -1
            ).forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label, color = Color.Black) },
                    onClick = {
                        favoritesRepository.saveBatteryThreshold(value)
                        showBatteryDropdown = false
                    }
                )
            }
        }
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun GeneralFontSection(
    favoritesRepository: FavoritesRepository,
    weekStartsOnSunday: Boolean,
    eInkSwitchColors: androidx.compose.material3.SwitchColors
) {
    Spacer(modifier = Modifier.height(32.dp))
    Text("General settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = Color.Black)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("The week starts on Sunday", fontSize = 18.sp, color = Color.Black)
        Switch(
            checked = weekStartsOnSunday,
            onCheckedChange = { favoritesRepository.saveWeekStartsOnSunday(it) },
            colors = eInkSwitchColors
        )
    }

    HorizontalDivider(color = Color.Black)
}

@Composable
fun ExperimentalSection(
    context: Context,
    favoritesRepository: FavoritesRepository,
    disableDuraSpeedNotifications: Boolean,
    eInkSwitchColors: androidx.compose.material3.SwitchColors
) {
    Spacer(modifier = Modifier.height(32.dp))
    Text("Extra Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = Color.Black)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Open App Notifications", fontSize = 18.sp, color = Color.Black)
        EInkButton(onClick = {
            try {
                // Try to open the specific "All Apps" notification list
                val intent = Intent("android.settings.ALL_APPS_NOTIFICATION_SETTINGS")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    // Fallback to the component name you mentioned
                    val intent = Intent()
                    intent.setClassName("com.android.settings", "com.android.settings.Settings\$NotificationAppListActivity")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    try {
                        // Last fallback to general notification settings
                        val intent = Intent("android.settings.NOTIFICATION_SETTINGS")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e3: Exception) {
                        Toast.makeText(context, "Could not open notification settings", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }) {
            Text("Open")
        }
    }

    HorizontalDivider(color = Color.Black)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Disable DuraSpeed notifications", fontSize = 18.sp, color = Color.Black)
        Switch(
            checked = disableDuraSpeedNotifications,
            onCheckedChange = { favoritesRepository.saveDisableDuraSpeedNotifications(it) },
            colors = eInkSwitchColors
        )
    }

    HorizontalDivider(color = Color.Black)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Open DuraSpeed", fontSize = 18.sp, color = Color.Black)
        EInkButton(onClick = {
            val pkgName = "com.mediatek.duraspeed"
            try {
                // Method 1: Standard App Info
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$pkgName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    // Method 2: Alternative App Info (some older devices)
                    val intent = Intent()
                    intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.data = android.net.Uri.parse("package:$pkgName")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    android.util.Log.e("SettingsScreen", "Failed to open DuraSpeed settings", e2)
                    Toast.makeText(context, "Could not open settings for $pkgName", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("Open")
        }
    }

    HorizontalDivider(color = Color.Black)
}
