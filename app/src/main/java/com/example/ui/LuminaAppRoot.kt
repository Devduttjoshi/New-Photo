package com.example.ui

import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun LuminaAppRoot(
    viewModel: SlideshowViewModel
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val isTv = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
    }

    val isPortrait = configuration.screenHeightDp > configuration.screenWidthDp

    when {
        isTv -> {
            TvSlideshowScreen(viewModel = viewModel)
        }
        isPortrait -> {
            TouchPortraitSlideshowScreen(viewModel = viewModel)
        }
        else -> {
            TouchLandscapeSlideshowScreen(viewModel = viewModel)
        }
    }
}
