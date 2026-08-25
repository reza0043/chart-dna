package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

object CouncilIconHelper {
    val availableIconNames = listOf(
        "trending" to "روند و استراتژی",
        "code" to "کد و فنی",
        "pie_chart" to "مالی و بودجه",
        "balance" to "حقوقی و قانون",
        "brush" to "طراحی و UX",
        "rocket" to "رشد و بازاریابی",
        "security" to "امنیت و ریسک",
        "settings" to "عملیات و فرآیند",
        "people" to "منابع انسانی",
        "psychology" to "هوش و شناخت",
        "science" to "تحقیق و توسعه",
        "campaign" to "روابط عمومی",
        "public" to "بین‌الملل",
        "support_agent" to "پشتیبانی و CRM",
        "cloud" to "زیرساخت ابری",
        "verified" to "کیفیت و استاندارد",
        "radar" to "رصد رقبا",
        "warning" to "مدیریت بحران",
        "eco" to "پایداری و ESG",
        "auto_awesome" to "ارجاع و تشخیص هوشمند"
    )

    fun getIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "trending" -> Icons.AutoMirrored.Filled.TrendingUp
            "code" -> Icons.Default.Code
            "pie_chart" -> Icons.Default.PieChart
            "balance" -> Icons.Default.Balance
            "brush" -> Icons.Default.Brush
            "rocket" -> Icons.Default.RocketLaunch
            "security" -> Icons.Default.Security
            "settings" -> Icons.Default.Settings
            "people" -> Icons.Default.People
            "psychology" -> Icons.Default.Psychology
            "science" -> Icons.Default.Science
            "campaign" -> Icons.Default.Campaign
            "public" -> Icons.Default.Public
            "support_agent" -> Icons.Default.SupportAgent
            "cloud" -> Icons.Default.Cloud
            "verified" -> Icons.Default.Verified
            "radar" -> Icons.Default.Radar
            "warning" -> Icons.Default.Warning
            "eco" -> Icons.Default.Eco
            "auto_awesome" -> Icons.Default.AutoAwesome
            else -> Icons.Default.AutoAwesome
        }
    }
}
