package com.anselm.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.anselm.location.LocationApplication.Companion.app
import kotlinx.serialization.encoding.CompositeEncoder

private val allPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.FOREGROUND_SERVICE,
    Manifest.permission.FOREGROUND_SERVICE_LOCATION,
    Manifest.permission.POST_NOTIFICATIONS,
)

private fun checkPermissions(context: Context): Boolean {
    return allPermissions.all {
        ActivityCompat.checkSelfPermission(
            context,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PermissionScreen(navController: NavController) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { it ->
        if ( it.all { it.value } ) {
            navController.navigate(NavigationItem.Home.route)
        }
    }

    DisposableEffect(LocalContext.current) {
        app.hideTopBar.value = true
        app.hideBottomBar.value = true

        if ( ! checkPermissions(context) ) {
            launcher.launch(allPermissions)
        } else {
            // We shouldn't be there in the first place.
            navController.navigate(NavigationItem.Home.route)
        }
        onDispose {
            app.hideTopBar.value = false
            app.hideBottomBar.value = false
        }
    }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(id = R.drawable.permission_background),
                contentScale = ContentScale.FillBounds,
                alpha = 0.45f
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Column (
            modifier = Modifier
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val context = LocalContext.current
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Permissions required",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "In order to use this application, you must grant it location permission" +
                        " while using the app.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Button(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    )
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                },
            ) {
                Text("Grant Permissions")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}