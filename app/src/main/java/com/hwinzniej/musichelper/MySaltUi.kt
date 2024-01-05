package com.hwinzniej.musichelper

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.moriafly.salt.ui.ItemSpacer
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
    selectedItem: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    Box {
        val boxWidth = remember { mutableStateOf(0f) }
        val clickOffsetX = remember { mutableStateOf(0f) }
        val interactionSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        clickOffsetX.value = offset.x
                        tryAwaitRelease()
                        state.expend()
                        interactionSource.emit(PressInteraction.Release(press))
                    })
                }
                .indication(
                    interactionSource = interactionSource,
                    indication = rememberRipple()
                )
                .onGloballyPositioned { layoutCoordinates ->
                    boxWidth.value = layoutCoordinates.size.width.toFloat()
                }
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
            Text(
                text = selectedItem,
                color = SaltTheme.colors.subText,
                style = SaltTheme.textStyles.main,
                fontSize = 14.sp
            )
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
            },
            offset = if (boxWidth.value / 2 > clickOffsetX.value) DpOffset(
                16.dp,
                0.dp
            ) else DpOffset((boxWidth.value / 6).dp, 0.dp)
        ) {
            content()
        }
    }
}

@UnstableSaltApi
@Composable
fun YesDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    title: String,
    content: String
) {
    com.moriafly.salt.ui.dialog.BasicDialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        DialogTitle(text = title)
        ItemSpacer()
        ItemText(text = content)
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = {
                onDismissRequest()
            },
            modifier = Modifier
                .padding(horizontal = SaltTheme.dimens.outerHorizontalPadding),
            text = stringResource(id = R.string.ok_button_text).uppercase()
        )
    }
}

@Composable
fun ItemText(
    text: String
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SaltTheme.dimens.innerHorizontalPadding),
        style = TextStyle(
            fontSize = 14.sp,
            color = SaltTheme.colors.subText
        )
    )
}