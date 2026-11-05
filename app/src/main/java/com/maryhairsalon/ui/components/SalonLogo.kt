/**
 * SalonLogo.kt
 *
 * Circular clipped image composable rendering the Mary Hair Salon brand icon.
 */
package com.maryhairsalon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.maryhairsalon.R

/**
 * The salon logo, clipped to a circle so the black square around the PNG
 * disappears. Uses res/drawable/icon — change the name below if the file is ever renamed.
 */
@Composable
fun SalonLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.icon),
        contentDescription = "Mary Hair Salon logo",
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(CircleShape)
    )
}
