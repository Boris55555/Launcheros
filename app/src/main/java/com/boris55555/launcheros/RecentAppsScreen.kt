package com.boris55555.launcheros

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.google.accompanist.drawablepainter.rememberDrawablePainter

import android.app.usage.UsageStatsManager
import java.util.Calendar

@Composable
fun RecentAppsScreen(
    onDismiss: () -> Unit,
    killedApps: Set<String> = emptySet(),
    onKillApp: (String) -> Unit
) {
    val context = LocalContext.current
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val packageManager = context.packageManager
    
    var runningApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var memoryInfo by remember { mutableStateOf<ActivityManager.MemoryInfo?>(null) }

    fun refreshRunningApps() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        memoryInfo = memInfo

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, -1) // Look at apps active in the last hour
        
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )

        val appList = mutableListOf<AppInfo>()
        val seenPackages = mutableSetOf<String>()

        // Sort by last time used to get truly "recent" apps
        stats.sortedByDescending { it.lastTimeUsed }.forEach { usageStats ->
            val pkgName = usageStats.packageName
            
            if (pkgName != context.packageName && 
                !seenPackages.contains(pkgName) && 
                !killedApps.contains(pkgName) &&
                usageStats.totalTimeInForeground > 0) {
                try {
                    val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkgName)
                    
                    if (launchIntent != null) {
                        val name = packageManager.getApplicationLabel(appInfo).toString()
                        val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                        
                        appList.add(AppInfo(name, pkgName, isSystemApp = isSystem))
                        seenPackages.add(pkgName)
                    }
                } catch (e: Exception) {
                    // Package not found
                }
            }
        }
        runningApps = appList
    }

    LaunchedEffect(Unit, killedApps) {
        refreshRunningApps()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Running Apps",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onDismiss() },
                    tint = Color.Black
                )
            }
            
            memoryInfo?.let { mem ->
                val totalGB = String.format(java.util.Locale.US, "%.1f", mem.totalMem / (1024.0 * 1024.0 * 1024.0))
                val availGB = String.format(java.util.Locale.US, "%.1f", mem.availMem / (1024.0 * 1024.0 * 1024.0))
                val usedGB = String.format(java.util.Locale.US, "%.1f", (mem.totalMem - mem.availMem) / (1024.0 * 1024.0 * 1024.0))
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "RAM: $usedGB GB / $totalGB GB used ($availGB GB free)",
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (runningApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No apps running in background", color = Color.Black)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(runningApps) { app ->
                        RunningAppItem(
                            app = app,
                            onClick = {
                                val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launchIntent)
                                    onDismiss()
                                }
                            },
                            onClose = {
                                try {
                                    activityManager.killBackgroundProcesses(app.packageName)
                                    onKillApp(app.packageName)
                                    // Memory info won't change instantly, but we can try
                                    val newMemInfo = ActivityManager.MemoryInfo()
                                    activityManager.getMemoryInfo(newMemInfo)
                                    memoryInfo = newMemInfo
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RunningAppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val icon = remember(app.packageName) {
        try {
            packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (icon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = icon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = app.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color.Black, CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Kill",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
