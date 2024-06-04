package com.ourdatamine.lock_screen_notification
import kotlinx.serialization.Serializable

@Serializable
data class ColorSetting(val color: String, val untilMinutes: Int);
@Serializable
data class GoalStatus(val name: String, val settings: List<ColorSetting>);
