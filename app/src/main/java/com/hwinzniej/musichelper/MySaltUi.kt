package com.hwinzniej.musichelper

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.moriafly.salt.ui.ItemSpacer
import com.moriafly.salt.ui.ItemText
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.dialog.DialogTitle
import com.moriafly.salt.ui.popup.PopupMenu
import com.moriafly.salt.ui.popup.PopupState

@UnstableSaltApi
@Composable
fun YesNoDialog(
    onDismiss: () -> Unit,
    onNegative: () -> Unit,
    onPositive: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    title: String,
    content: String,
    noText: String,
    yesText: String,
    customContent: @Composable () -> Unit = {},
    onlyComposeView: Boolean = false
) {
    BasicDialog(
        onDismissRequest = onDismiss,
        properties = properties,
    ) {
        DialogTitle(text = title)
        if (onlyComposeView) Spacer(modifier = Modifier.height(8.dp * 2))
        if (!onlyComposeView) {
            ItemSpacer()
            ItemText(text = content)
            Spacer(modifier = Modifier.height(8.dp * 2))
        }
        customContent()
        if (onlyComposeView) Spacer(modifier = Modifier.height(8.dp * 2))
        Row(
            modifier = Modifier.padding(horizontal = SaltTheme.dimens.outerHorizontalPadding)
        ) {
            TextButton(
                onClick = {
                    onNegative()
                },
                modifier = Modifier.weight(1f),
                text = noText.uppercase(),
                textColor = SaltTheme.colors.subText,
                backgroundColor = SaltTheme.colors.subBackground
            )
            Spacer(modifier = Modifier.width(12.dp))
            TextButton(
                onClick = {
                    onPositive()
                }, modifier = Modifier.weight(1f), text = yesText.uppercase()
            )
        }
    }
}

@Composable
fun BasicDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        (LocalView.current.parent as DialogWindowProvider).window.setDimAmount(0.4f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(color = SaltTheme.colors.background)
                .padding(vertical = 8.dp * 2)
        ) {
            content()
        }

    }
}

@UnstableSaltApi
@Composable
fun ItemPopup(
    state: PopupState,
    enabled: Boolean = true,
    iconPainter: Painter? = null,
    iconPaddingValues: PaddingValues = PaddingValues(0.dp),
    iconColor: Color? = null,
    text: String,
    sub: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var popupLocation = 1
    val halfScreenWidth = LocalConfiguration.current.screenHeightDp / 2
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .clickable(enabled = enabled) {
                    state.expend()
                }
//                .pointerInput(Unit) {
//                    detectTapGestures(onTap = { offset ->
//                        state.expend()
////                        if (offset.x > halfScreenWidth)
////                            println("right")
////                        else
////                            println("left")
//                        popupLocation = if (offset.x > halfScreenWidth)
//                            300
//                        else
//                            0
//
//                    })
//                }
                .padding(horizontal = SaltTheme.dimens.innerHorizontalPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            iconPainter?.let {
                Image(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(iconPaddingValues),
                    painter = iconPainter,
                    contentDescription = null,
                    colorFilter = iconColor?.let { ColorFilter.tint(iconColor) }
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = text,
                    color = if (enabled) SaltTheme.colors.text else SaltTheme.colors.subText,
                    style = SaltTheme.textStyles.main
                )
                if (sub != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sub,
                        style = SaltTheme.textStyles.sub
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                modifier = Modifier
                    .size(20.dp),
                painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                contentDescription = null,
                tint = SaltTheme.colors.subText
            )


        }
        PopupMenu(
            expanded = state.expend,
            onDismissRequest = {
                state.dismiss()
            }
        ) {
            content()
        }
    }
}