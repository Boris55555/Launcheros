package com.boris55555.launcheros

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HiddenAppsScreen(
    favoritesRepository: FavoritesRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val hiddenApps by favoritesRepository.hiddenApps.collectAsState()

    val allApps = remember {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        resolveInfoList.map { resolveInfo ->
            val pkgName = resolveInfo.activityInfo.packageName
            val name = resolveInfo.loadLabel(packageManager).toString()
            pkgName to name
        }.sortedBy { it.second }
    }

    val hiddenAppsList = remember(allApps, hiddenApps) {
        allApps.filter { it.first in hiddenApps }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Text(
                "Hidden Apps",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (hiddenAppsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hidden apps", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(hiddenAppsList) { (pkg, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(pkg, fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { favoritesRepository.toggleHiddenApp(pkg) }) {
                            Icon(Icons.Default.Visibility, contentDescription = "Unhide", tint = Color.Black)
                        }
                    }
                    HorizontalDivider(color = Color.Black)
                }
            }
        }
    }
}
