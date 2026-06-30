package com.xlr8.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xlr8.app.R
import com.xlr8.app.ui.theme.BrandBlue

/** Small NEW badge used on freshly-aired episodes/shows. */
@Composable
fun NewBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.badge_new),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(BrandBlue)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
