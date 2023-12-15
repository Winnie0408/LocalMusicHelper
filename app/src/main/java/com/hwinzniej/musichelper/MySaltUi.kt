package com.hwinzniej.musichelper

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.moriafly.salt.ui.ItemSpacer
import com.moriafly.salt.ui.ItemText
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.dialog.BasicDialog
import com.moriafly.salt.ui.dialog.DialogTitle

@UnstableSaltApi
@Composable
fun YesNoDialog(
    onDismiss:() -> Unit,
    onNegative: () -> Unit,
    onPositive: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    title: String,
    content: String,
    noText:String,
    yesText:String
) {
    BasicDialog(
        onDismissRequest = onDismiss,
        properties = properties
    ) {
        DialogTitle(text = title)
        ItemSpacer()
        ItemText(text = content)
        Spacer(modifier = Modifier.height(8.dp * 2))
        Row(
            modifier = Modifier.padding(horizontal = SaltTheme.dimens.outerHorizontalPadding)
        ) {
            TextButton(
                onClick = {
                    onNegative()
                },
                modifier = Modifier
                    .weight(1f),
                text = noText.uppercase(),
                textColor = SaltTheme.colors.subText,
                backgroundColor = Color.Transparent
            )
            Spacer(modifier = Modifier.width(12.dp))
            TextButton(
                onClick = {
                    onPositive()
                },
                modifier = Modifier
                    .weight(1f),
                text = yesText.uppercase()
            )
        }
    }
}