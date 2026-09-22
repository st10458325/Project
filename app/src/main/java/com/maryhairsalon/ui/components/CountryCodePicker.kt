package com.maryhairsalon.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maryhairsalon.data.repository.DialCodeOption

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
            value = selected?.let { "${it.dialCode} ${it.countryCode}" } ?: "Code",
            onValueChange = {},
            readOnly = true,
            label = { Text("Code") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().width(130.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.dialCode}  ${option.countryName}") },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}