package com.auraflow.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.Night
import com.auraflow.ui.theme.TextPrimary

@Composable
fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NeonCyan,
            selectedLabelColor = Night,
            labelColor = TextPrimary
        )
    )
}
