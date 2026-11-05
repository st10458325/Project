/**
 * CountryCodePicker.kt
 *
 * Dropdown menu picker composable for choosing international calling codes in phone input fields.
 */
package com.maryhairsalon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maryhairsalon.data.repository.DialCodeOption
import com.maryhairsalon.ui.theme.SurfaceLighter

/** A compact dial-code dropdown meant to sit beside a phone number field. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryCodePicker(
    selected: DialCodeOption?,
    options: List<DialCodeOption>,
    onSelect: (DialCodeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let { "${it.dialCode} ${it.countryCode}" } ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Code") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = authFieldColors(),
            modifier = Modifier.menuAnchor().width(140.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceLighter)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.dialCode}  ${option.countryName}", color = Color.White) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}
