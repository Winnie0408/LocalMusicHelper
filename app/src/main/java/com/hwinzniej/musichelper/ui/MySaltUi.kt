package com.hwinzniej.musichelper.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.hwinzniej.musichelper.utils.Tools
import com.moriafly.salt.ui.ItemOutHalfSpacer
import com.moriafly.salt.ui.ItemOutSpacer
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.dialog.DialogTitle
import com.moriafly.salt.ui.popup.PopupMenu
import com.moriafly.salt.ui.popup.PopupState

@UnstableSaltApi
@Composable
fun YesNoDialog(
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    title: String,
    content: String?,
    cancelText: String = stringResource(id = R.string.cancel_button_text),
    confirmText: String = stringResource(id = R.string.ok_button_text),
    confirmButtonColor: Color = SaltTheme.colors.highlight,
    enableHaptic: Boolean = false,
    enableConfirmButton: Boolean = true,
    drawContent: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        MyVibrationEffect(context, enableHaptic).dialog()
    }
    BasicDialog(
        onDismissRequest = onDismiss,
        properties = properties,
    ) {
        ItemOutSpacer()
        DialogTitle(text = title)
        content?.let {
            ItemOutSpacer()
            ItemText(text = it, fontSize = 13.sp)
        }
        drawContent?.let {
            ItemOutHalfSpacer()
            drawContent.invoke()
        }
        ItemOutSpacer()
        Row(
            modifier = Modifier.padding(horizontal = SaltTheme.dimens.outerHorizontalPadding)
        ) {
            TextButton(
                onClick = {
                    onCancel()
                },
                modifier = Modifier.weight(1f),
                text = cancelText,
                textColor = SaltTheme.colors.subText,
                backgroundColor = SaltTheme.colors.subBackground,
                enableHaptic = enableHaptic
            )
            Spacer(modifier = Modifier.width(SaltTheme.dimens.outerHorizontalPadding))
            AnimatedContent(
                modifier = Modifier.weight(1f),
                targetState = enableConfirmButton,
                label = "",
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }) {
                TextButton(
                    onClick = {
                        onConfirm()
                    }, text = confirmText,
                    backgroundColor = confirmButtonColor,
                    enableHaptic = enableHaptic,
                    enabled = it
                )
            }
        }
        ItemOutSpacer()
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
        (LocalView.current.parent as DialogWindowProvider).window.setDimAmount(0.5f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SaltTheme.dimens.dialogCorner))
                .background(color = SaltTheme.colors.background)
        ) {
            content()
        }
    }
}

@UnstableSaltApi
@Composable
fun ItemPopup(  //TODO 根据文字长度自动调整宽度；在对话框内点击，右侧弹出位置可能会出界
    state: PopupState,
    enabled: Boolean = true,
    iconPainter: Painter? = null,
    iconPaddingValues: PaddingValues = PaddingValues(0.dp),
    iconColor: Color? = null,
    text: String,
    sub: String? = null,
    selectedItem: String = "",
    popupWidth: Int = 160,
    rightSubWeight: Float = 0.5f,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    Box {
        val boxWidth = remember { mutableFloatStateOf(0f) }
        val clickOffsetX = remember { mutableFloatStateOf(0f) }
        val interactionSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            clickOffsetX.floatValue = offset.x
                            state.expend()
                        },
                        onPress = { offset ->
                            val press = PressInteraction.Press(offset)
                            interactionSource.emit(press)
                            tryAwaitRelease()
                            interactionSource.emit(PressInteraction.Release(press))
                        })
                }
                .indication(
                    interactionSource = interactionSource,
                    indication = rememberRipple()
                )
                .onGloballyPositioned { layoutCoordinates ->
                    boxWidth.floatValue = layoutCoordinates.size.width.toFloat()
                }
                .padding(
                    horizontal = SaltTheme.dimens.innerHorizontalPadding,
                    vertical = SaltTheme.dimens.innerVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        )
        {
            iconPainter?.let {
                Image(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(iconPaddingValues),
                    painter = iconPainter,
                    contentDescription = null,
                    colorFilter = iconColor?.let { ColorFilter.tint(iconColor) }
                )
                Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
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
            Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
            Text(
                modifier = Modifier
                    .weight(if (selectedItem.isEmpty()) 0.001f else rightSubWeight),
                text = selectedItem,
                color = SaltTheme.colors.subText,
                style = SaltTheme.textStyles.main,
                fontSize = 14.sp,
                textAlign = TextAlign.End
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
            modifier = Modifier.width(popupWidth.dp),  //TODO 宽度
            expanded = state.expend,
            onDismissRequest = {
                state.dismiss()
            },
            offset = DpOffset(
                Tools().calPopupLocation(
                    context.resources.displayMetrics.density,
                    clickOffsetX.floatValue,
                    popupWidth,
                    LocalConfiguration.current.screenWidthDp
                ).dp,
                0.dp
            ),
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
    content: String?,
    confirmText: String = stringResource(id = R.string.ok_button_text),
    fontSize: TextUnit = 13.sp,
    enableHaptic: Boolean = false,
    drawContent: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        MyVibrationEffect(context, enableHaptic).dialog()
    }
    BasicDialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        ItemOutSpacer()
        DialogTitle(text = title)
        content?.let {
            ItemOutSpacer()
            ItemText(text = it, fontSize = fontSize)
        }
        drawContent?.let {
            ItemOutHalfSpacer()
            drawContent.invoke()
        }
        ItemOutSpacer()
        TextButton(
            onClick = {
                onDismissRequest()
            },
            modifier = Modifier
                .padding(horizontal = SaltTheme.dimens.outerHorizontalPadding),
            text = confirmText,
            enableHaptic = enableHaptic
        )
        ItemOutSpacer()
    }
}

@Composable
fun ItemText(
    text: String,
    fontSize: TextUnit = 12.sp,
    verticalPadding: Dp = 0.dp
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SaltTheme.dimens.innerHorizontalPadding,
                vertical = verticalPadding
            ),
        style = TextStyle(
            fontSize = fontSize,
            color = SaltTheme.colors.subText
        )
    )
}

@UnstableSaltApi
@Composable
fun ItemCheck(
    state: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    text: String,
    iconAtLeft: Boolean = false,
    sub: String? = null,
    hideIcon: Boolean = false,
    enableHaptic: Boolean = false,
    minHeightIn: Dp = 50.dp
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeightIn)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled) {
                MyVibrationEffect(context, enableHaptic).click()
                onChange(!state)
            }
            .padding(
                horizontal = SaltTheme.dimens.innerHorizontalPadding,
                vertical = SaltTheme.dimens.innerVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!hideIcon) {
            if (iconAtLeft) {
                Icon(
                    modifier = Modifier
                        .size(24.dp),
                    painter = if (state) painterResource(id = R.drawable.ic_check) else painterResource(
                        id = R.drawable.ic_uncheck
                    ),
                    contentDescription = null,
                    tint = SaltTheme.colors.highlight
                )
                Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
            }
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
            sub?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sub,
                    style = SaltTheme.textStyles.sub
                )
            }
        }
        if (!hideIcon) {
            if (!iconAtLeft) {
                Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
                Icon(
                    modifier = Modifier
                        .size(24.dp),
                    painter = if (state) painterResource(id = R.drawable.ic_check) else painterResource(
                        id = R.drawable.ic_uncheck
                    ),
                    contentDescription = null,
                    tint = SaltTheme.colors.highlight
                )
            }
        }
    }
}

@Composable
fun Item(
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconPainter: Painter? = null,
    iconPaddingValues: PaddingValues = PaddingValues(0.dp),
    iconColor: Color? = null,
    text: String,
    sub: String? = null,
    subColor: Color = SaltTheme.colors.subText,
    rightSub: String? = null,
    rightSubColor: Color? = null,
    indication: Indication? = rememberRipple()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(
                enabled = enabled,
                onClick = { onClick() },
                indication = indication,
                interactionSource = remember {
                    MutableInteractionSource()
                }
            )
            .padding(
                horizontal = SaltTheme.dimens.innerHorizontalPadding,
                vertical = SaltTheme.dimens.innerVerticalPadding
            ),
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
            Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
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
            sub?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sub,
                    color = subColor,
                    style = SaltTheme.textStyles.sub
                )
            }
        }
        Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
        rightSub?.let {
            Text(
                text = it,
                style = SaltTheme.textStyles.main,
                color = rightSubColor ?: SaltTheme.colors.subText,
                fontSize = 14.sp
            )
        }
        Icon(
            modifier = Modifier
                .size(20.dp),
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = SaltTheme.colors.subText
        )
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color = Color.White,
    backgroundColor: Color = SaltTheme.colors.highlight,
    enabled: Boolean = true,
    enableHaptic: Boolean = false
) {
    BasicButton(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        backgroundColor = if (enabled) backgroundColor else Color(0xFF8C8C8C),
        enableHaptic = enableHaptic,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth(),
            color = textColor,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = SaltTheme.textStyles.main
        )
    }
}

/**
 * Basic button.
 */
@Composable
fun BasicButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    backgroundColor: Color = SaltTheme.colors.highlight,
    enableHaptic: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .semantics {
                role = Role.Button
            }
            .clip(RoundedCornerShape(SaltTheme.dimens.corner))
            .background(color = backgroundColor)
            .clickable(enabled = enabled) {
                MyVibrationEffect(context, enableHaptic).click()
                onClick()
            }
            .padding(SaltTheme.dimens.contentPadding)
    ) {
        content()
    }
}

@Composable
fun ItemValue(
    text: String,
    sub: String? = null,
    rightSub: String? = null,
    clickable: Boolean = false,
    onClick: () -> Unit = {},
    textWeight: Float = 1f,
    rightSubWeight: Float = 1f,
) {
    Column(modifier = Modifier.clickable(enabled = clickable) { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp)
                .padding(
                    horizontal = SaltTheme.dimens.innerHorizontalPadding,
                    vertical = SaltTheme.dimens.innerVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(textWeight)) {
                Text(
                    text = text,
                    style = SaltTheme.textStyles.main
                )
                sub?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sub,
                        color = SaltTheme.colors.subText,
                        style = SaltTheme.textStyles.sub
                    )
                }
            }
            rightSub?.let {
                Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
                SelectionContainer(
                    modifier = Modifier
                        .weight(rightSubWeight),
                ) {
                    Text(
                        text = rightSub,
                        color = SaltTheme.colors.subText,
                        fontSize = 15.sp,
                        textAlign = TextAlign.End,
                        style = SaltTheme.textStyles.main
                    )
                }
            }
        }
    }
}

@Composable
fun ItemEdit(
    modifier: Modifier = Modifier,
    text: String,
    onChange: (String) -> Unit,
    backgroundColor: Color = SaltTheme.colors.subText.copy(alpha = 0.1f),
    paddingValues: PaddingValues = PaddingValues(
        horizontal = SaltTheme.dimens.innerHorizontalPadding,
        vertical = SaltTheme.dimens.innerVerticalPadding
    ),
    hint: String? = null,
    hintColor: Color = SaltTheme.colors.subText,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    actionContent: (@Composable () -> Unit)? = null,
    showClearButton: Boolean = false,
    onClear: () -> Unit = {},
    enableHaptic: Boolean = false,
    iconPainter: Painter? = null,
    iconPaddingValues: PaddingValues = PaddingValues(0.dp),
    iconColor: Color? = null,
    singleLine: Boolean = false
) {
    val context = LocalContext.current
    BasicTextField(
        value = text,
        onValueChange = onChange,
        modifier = modifier.padding(paddingValues),
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = SaltTheme.textStyles.main,
        visualTransformation = visualTransformation,
        cursorBrush = SolidColor(SaltTheme.colors.highlight),
        singleLine = singleLine,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SaltTheme.dimens.corner))
                    .background(color = backgroundColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
                iconPainter?.let {
                    Image(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(iconPaddingValues),
                        painter = iconPainter,
                        contentDescription = null,
                        colorFilter = iconColor?.let { ColorFilter.tint(iconColor) }
                    )
                    Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = SaltTheme.dimens.contentPadding),
                ) {
                    innerTextField()
                    if (hint != null && text.isEmpty()) {
                        Text(
                            text = hint,
                            color = hintColor,
                            style = SaltTheme.textStyles.main
                        )
                    }
                }
                if (text.isNotBlank() && showClearButton) {
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                MyVibrationEffect(context, enableHaptic).click()
                                onClear()
                            }
                            .alpha(0.7f),
                        painter = painterResource(id = R.drawable.ic_clear),
                        contentDescription = null,
                        tint = SaltTheme.colors.subText
                    )
                }
                if (actionContent != null) {
                    actionContent()
                } else {
                    Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
                }
            }
        }
    )
}

@Composable
fun ItemSwitcher(
    state: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    iconPainter: Painter? = null,
    iconPaddingValues: PaddingValues = PaddingValues(0.dp),
    iconColor: Color? = null,
    text: String,
    sub: String? = null,
    enableHaptic: Boolean = false
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled) {
                if (!state)
                    MyVibrationEffect(context, enableHaptic).turnOn()
                else
                    MyVibrationEffect(context, enableHaptic).turnOff()
                onChange(!state)
            }
            .padding(
                horizontal = SaltTheme.dimens.innerHorizontalPadding,
                vertical = SaltTheme.dimens.innerVerticalPadding
            ),
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
            Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
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
            sub?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sub,
                    style = SaltTheme.textStyles.sub
                )
            }
        }
        Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
        val backgroundColor by animateColorAsState(
            targetValue = if (state) SaltTheme.colors.highlight else SaltTheme.colors.subText.copy(
                alpha = 0.1f
            ),
            animationSpec = spring(),
            label = "backgroundColor"
        )
        Box(
            modifier = Modifier
                .size(46.dp, 26.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawRect(color = backgroundColor)
                }
                .padding(5.dp)
        ) {
            val layoutDirection = LocalLayoutDirection.current
            val translationX by animateDpAsState(
                targetValue = if (state) {
                    when (layoutDirection) {
                        LayoutDirection.Ltr -> 20.dp
                        LayoutDirection.Rtl -> (-20).dp
                    }
                } else {
                    0.dp
                },
                animationSpec = spring(),
                label = "startPadding"
            )
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        this.translationX = translationX.toPx()
                    }
                    .size(16.dp)
                    .border(width = 4.dp, color = Color.White, shape = CircleShape)
            )
        }
    }
}

@UnstableSaltApi
@Composable
fun PopupMenuItem(
    onClick: () -> Unit,
    selected: Boolean? = null,
    text: String,
    sub: String? = null,
    iconPainter: Painter? = null,
    iconPaddingValues: PaddingValues = PaddingValues(0.dp),
    iconColor: Color? = null
) {
    Row(
        modifier = Modifier
            .semantics {
                this.role = Role.RadioButton

                if (selected != null) {
                    this.toggleableState = when (selected) {
                        true -> ToggleableState.On
                        false -> ToggleableState.Off
                    }
                }
            }
            .clickable {
                onClick()
            }
            .fillMaxWidth()
            .sizeIn(
                minWidth = 180.dp,
                maxWidth = 280.dp,
                minHeight = 0.dp
            )
            .background(if (selected == true) SaltTheme.colors.highlight.copy(alpha = 0.1f) else Color.Unspecified)
            .padding(
                SaltTheme.dimens.innerHorizontalPadding,
                SaltTheme.dimens.innerVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Text(
                text = text,
                color = if (selected == true) SaltTheme.colors.highlight else SaltTheme.colors.text,
                style = SaltTheme.textStyles.main
            )
            sub?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sub,
                    color = if (selected == true) SaltTheme.colors.highlight else SaltTheme.colors.subText,
                    style = SaltTheme.textStyles.sub
                )
            }
        }
        iconPainter?.let {
            Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding * 2))
            Image(
                modifier = Modifier
                    .size(24.dp)
                    .padding(iconPaddingValues),
                painter = iconPainter,
                contentDescription = null,
                colorFilter = iconColor?.let {
                    if (selected == true) ColorFilter.tint(SaltTheme.colors.highlight) else ColorFilter.tint(
                        iconColor
                    )
                }
            )
        }
    }
}

@Composable
fun ItemTitle(
    text: String,
    paddingValues: PaddingValues = PaddingValues(
        horizontal = SaltTheme.dimens.innerHorizontalPadding,
        vertical = SaltTheme.dimens.innerVerticalPadding
    )
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues),
        color = SaltTheme.colors.highlight,
        fontWeight = FontWeight.Bold,
        style = SaltTheme.textStyles.sub
    )
}

@Composable
fun FloatingActionButton(
    expanded: MutableState<Boolean>,
    heightFold: Dp = 50.dp,
    heightExpand: Dp = 200.dp,
    widthFold: Dp = 50.dp,
    widthExpand: Dp = 200.dp,
    backgroundColorFold: Color = SaltTheme.colors.highlight,
    backgroundColorExpand: Color = SaltTheme.colors.background,
    paddingValues: PaddingValues = PaddingValues(bottom = 32.dp, end = 24.dp),
    iconSize: Dp = 20.dp,
    iconPainter: Painter = painterResource(id = R.drawable.plus_no_circle),
    iconTintColor: Color = SaltTheme.colors.subBackground,
    enableHaptic: Boolean = false,
    drawContent: @Composable (() -> Unit)
) {
    val transition = updateTransition(targetState = expanded, label = "transition")

    val height by transition.animateDp(
        transitionSpec = { spring(stiffness = 600f) },
        label = "height"
    ) { if (it.value) heightExpand else heightFold }

    val width by transition.animateDp(
        transitionSpec = { spring(stiffness = 600f) },
        label = "width"
    ) { if (it.value) widthExpand else widthFold }

    val background by transition.animateColor(
        transitionSpec = { spring(stiffness = 600f) },
        label = "background"
    ) { if (it.value) backgroundColorExpand else backgroundColorFold }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Box(
            modifier = Modifier
                .height(height)
                .width(width)
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .align(Alignment.BottomEnd),
        ) {
            Crossfade(
                targetState = expanded, animationSpec = tween(durationMillis = 250),
                label = ""
            ) { isExpanded ->
                if (isExpanded.value) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        drawContent.invoke()
                    }
                } else {
                    BasicButton(
                        modifier = Modifier
                            .fillMaxSize(),
                        onClick = { expanded.value = true },
                        enableHaptic = enableHaptic,
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(iconSize)
                                .align(Alignment.Center),
                            painter = iconPainter,
                            contentDescription = null,
                            tint = iconTintColor
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview() {
}