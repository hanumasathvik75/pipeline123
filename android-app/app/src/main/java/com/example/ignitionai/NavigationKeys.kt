package com.example.ignitionai

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Dashboard : NavKey
@Serializable data object Scan : NavKey
@Serializable data object Results : NavKey
@Serializable data object AIReport : NavKey
