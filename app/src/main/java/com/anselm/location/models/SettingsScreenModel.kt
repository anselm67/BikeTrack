package com.anselm.location.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SettingsScreenModel : ViewModel() {
    var progress by mutableStateOf(0f)
    var showProgress by mutableStateOf(false)
}