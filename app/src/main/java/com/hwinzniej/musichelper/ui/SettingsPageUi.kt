package com.hwinzniej.musichelper.ui

import android.content.res.Resources
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.activity.SettingsPage
import com.hwinzniej.musichelper.data.DataStoreConstants
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.hwinzniej.musichelper.utils.Tools
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableSaltApi::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsPageUi(
    settingsPage: SettingsPage,
    enableDynamicColor: MutableState<Boolean>,
    selectedThemeMode: MutableIntState,
    selectedLanguage: MutableState<String>,
    useRootAccess: MutableState<Boolean>,
    enableAutoCheckUpdate: MutableState<Boolean>,
    settingsPageState: PagerState,
    enableHaptic: MutableState<Boolean>,
    dataStore: DataStore<Preferences>,
    encryptServer: MutableState<String>,
    hapticStrength: MutableIntState
) {
    val context = LocalContext.current
    val themeModePopupMenuState = rememberPopupState()
    val languagePopupMenuState = rememberPopupState()
    val hapticStrengthPopupMenuState = rememberPopupState()
    val coroutineScope = rememberCoroutineScope()
    var showSelectEncryptServerDialog by remember { mutableStateOf(false) }
    var umFileLegal by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = showSelectEncryptServerDialog) {
        if (showSelectEncryptServerDialog) {
            settingsPage.serverPing.forEach { (i, _) ->
                settingsPage.serverPing[i] += " …"
            }
            settingsPage.checkServerPing()
        }
    }

    LaunchedEffect(key1 = settingsPage.umFileLegal.value) {
        umFileLegal = settingsPage.umFileLegal.value
    }

    if (showSelectEncryptServerDialog) {
        var selectedEncryptServer by remember { mutableStateOf(encryptServer.value) }
        YesNoDialog(
            onDismiss = { showSelectEncryptServerDialog = false },
            onCancel = { showSelectEncryptServerDialog = false },
            onConfirm = {
                encryptServer.value = selectedEncryptServer
                coroutineScope.launch(Dispatchers.IO) {
                    dataStore.edit { settings ->
                        settings[DataStoreConstants.KEY_ENCRYPT_SERVER] =
                            encryptServer.value
                    }
                }
                showSelectEncryptServerDialog = false
            },
            title = stringResource(id = R.string.select_encrypt_server_title),
            content = null,
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        ) {
            Box {
                if (settingsPage.showDialogProgressBar.value) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f),
                        color = SaltTheme.colors.highlight,
                        trackColor = SaltTheme.colors.background
                    )
                }
                RoundedColumn {
                    ItemTitle(text = stringResource(R.string.optional_servers))
                    ItemCheck(
                        state = selectedEncryptServer == "cf",
                        onChange = {
                            selectedEncryptServer = "cf"
                        },
                        text = "Cloudflare",
                        sub = settingsPage.serverPing[0],
                        iconAtLeft = false,
                        enableHaptic = enableHaptic.value,
                        hapticStrength = hapticStrength.intValue
                    )
                    ItemCheck(
                        state = selectedEncryptServer == "gx1",
                        onChange = {
                            selectedEncryptServer = "gx1"
                        },
                        text = "${context.getString(R.string.guangxi_china)} 1",
                        sub = settingsPage.serverPing[1],
                        iconAtLeft = false,
                        enableHaptic = enableHaptic.value,
                        hapticStrength = hapticStrength.intValue
                    )
                    ItemCheck(
                        state = selectedEncryptServer == "gx2",
                        onChange = {
                            selectedEncryptServer = "gx2"
                        },
                        text = "${context.getString(R.string.guangxi_china)} 2",
                        sub = settingsPage.serverPing[2],
                        iconAtLeft = false,
                        enableHaptic = enableHaptic.value,
                        hapticStrength = hapticStrength.intValue
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    )
    {
        TitleBar(
            onBack = { },
            text = stringResource(id = R.string.settings_function_name),
            showBackBtn = false
        )
        Column(
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxSize()
                .background(color = SaltTheme.colors.background)
                .verticalScroll(rememberScrollState())
        ) {
            RoundedColumn {
                ItemTitle(text = stringResource(R.string.user_interface))
                ItemSwitcher(
                    state = enableDynamicColor.value,
                    onChange = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.KEY_ENABLE_DYNAMIC_COLOR] = it
                                }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.android_12_and_above_only),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    text = stringResource(R.string.dynamic_color_switcher_text),
                    sub = stringResource(R.string.dynamic_color_switcher_sub),
                    enableHaptic = enableHaptic.value,
                    iconPainter = painterResource(id = R.drawable.color),
                    iconColor = SaltTheme.colors.text,
                    hapticStrength = hapticStrength.intValue
                )
                ItemPopup(
                    state = themeModePopupMenuState,
                    iconPainter = painterResource(id = R.drawable.app_theme),
                    iconPaddingValues = PaddingValues(all = 2.dp),
                    iconColor = SaltTheme.colors.text,
                    text = stringResource(R.string.theme_mode_switcher_text),
                    selectedItem = when (selectedThemeMode.intValue) {
                        0 -> stringResource(R.string.light_mode)
                        1 -> stringResource(R.string.dark_mode)
                        2 -> stringResource(R.string.follow_system)
                        else -> ""
                    },
                    popupWidth = 140
                ) {
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                hapticStrength.intValue
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.KEY_THEME_MODE] = 2
                                }
                            }
                            themeModePopupMenuState.dismiss()
                        },
                        selected = selectedThemeMode.intValue == 2,
                        text = stringResource(R.string.follow_system),
                        iconPainter = painterResource(id = R.drawable.android),
                        iconColor = SaltTheme.colors.text
                    )
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                hapticStrength.intValue
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.KEY_THEME_MODE] = 0
                                }
                            }
                            themeModePopupMenuState.dismiss()
                        },
                        selected = selectedThemeMode.intValue == 0,
                        text = stringResource(R.string.light_mode),
                        iconPainter = painterResource(id = R.drawable.light_color),
                        iconColor = SaltTheme.colors.text
                    )
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                hapticStrength.intValue
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.KEY_THEME_MODE] = 1
                                }
                            }
                            themeModePopupMenuState.dismiss()
                        },
                        selected = selectedThemeMode.intValue == 1,
                        text = stringResource(R.string.dark_mode),
                        iconPainter = painterResource(id = R.drawable.dark_color),
                        iconColor = SaltTheme.colors.text,
                        iconPaddingValues = PaddingValues(all = 1.dp)
                    )
                }
            }

            RoundedColumn {
                ItemTitle(text = stringResource(R.string.file_access))
                ItemSwitcher(
                    state = useRootAccess.value,
                    onChange = {
                        coroutineScope.launch(Dispatchers.IO) {
                            if (it) {
                                try {
                                    if (Tools().execShellCmd("ls /data | grep data")
                                            .contains("Permission denied")
                                    ) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.no_grant_root_access),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        dataStore.edit { settings ->
                                            settings[DataStoreConstants.KEY_USE_ROOT_ACCESS] =
                                                true
                                        }
                                        if (enableHaptic.value) {
                                            MyVibrationEffect(
                                                context,
                                                enableHaptic.value,
                                                hapticStrength.intValue
                                            ).turnOn()
                                        }
                                    }
                                } catch (_: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.no_root_access),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.KEY_USE_ROOT_ACCESS] =
                                        false
                                }
                                if (enableHaptic.value) {
                                    MyVibrationEffect(
                                        context,
                                        enableHaptic.value,
                                        hapticStrength.intValue
                                    ).turnOff()
                                }
                            }
                        }
                    },
                    text = stringResource(R.string.use_root_access_switcher_title),
                    sub = stringResource(R.string.use_root_access_switcher_sub),
                    enableHaptic = false,
                    iconPainter = painterResource(id = R.drawable.root_access),
                    iconColor = SaltTheme.colors.text,
                    hapticStrength = hapticStrength.intValue
                )
                Item(
                    onClick = { showSelectEncryptServerDialog = true },
                    text = stringResource(id = R.string.select_encrypt_server),
                    rightSub = when (encryptServer.value) {
                        "cf" -> "Cloudflare"
                        "gx1" -> "${context.getString(R.string.guangxi_china)} 1"
                        "gx2" -> "${context.getString(R.string.guangxi_china)} 2"
                        else -> ""
                    },
                    iconPainter = painterResource(id = R.drawable.server),
                    iconColor = SaltTheme.colors.text,
                    sub = stringResource(id = R.string.choose_string_encrypt_server).replace(
                        "#n",
                        "\n"
                    ),
                )
            }

            if (context.packageManager.getApplicationInfo(
                    context.packageName,
                    0
                ).targetSdkVersion == 28
            ) {
                RoundedColumn {
                    ItemTitle(text = stringResource(R.string.music_unlock))
                    AnimatedContent(
                        targetState = umFileLegal,
                        label = "",
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        }
                    ) {
                        Item(
                            onClick = { settingsPage.selectUmFile() },
                            text = stringResource(id = R.string.select_um_executable_file),
                            sub = if (it)
                                stringResource(id = R.string.can_use_um_now)
                            else
                                stringResource(id = R.string.cant_provide_um_file),
                            rightSub = if (it)
                                stringResource(id = R.string.um_imported)
                            else
                                stringResource(id = R.string.um_not_imported),
                        )
                    }
                }
            }

            RoundedColumn {
                ItemTitle(text = stringResource(R.string.language))
                ItemValue(
                    text = stringResource(R.string.system_language),
                    rightSub = "${Resources.getSystem().configuration.locales[0].language} - ${Resources.getSystem().configuration.locales[0].country}"
                )
                ItemPopup(
                    state = languagePopupMenuState,
                    iconPainter = painterResource(id = R.drawable.language),
                    iconPaddingValues = PaddingValues(all = 1.dp),
                    iconColor = SaltTheme.colors.text,
                    text = stringResource(R.string.app_language),
                    selectedItem = when (selectedLanguage.value) {
                        "system" -> stringResource(R.string.follow_system)
                        "zh" -> stringResource(R.string.chinese_s)
                        "en" -> stringResource(R.string.english)
                        "ko" -> stringResource(R.string.korean)
                        else -> ""
                    },
                    popupWidth = 180
                ) {
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                hapticStrength.intValue
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.KEY_LANGUAGE] = "system"
                                }
                            }
                            languagePopupMenuState.dismiss()
                        },
                        selected = selectedLanguage.value == "system",
                        text = stringResource(R.string.follow_system),
                        iconPainter = painterResource(id = R.drawable.android),
                        iconColor = SaltTheme.colors.text
                    )
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                hapticStrength.intValue
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.KEY_LANGUAGE] = "zh"
                                }
                            }
                            languagePopupMenuState.dismiss()
                        },
                        selected = selectedLanguage.value == "zh",
                        text = stringResource(R.string.chinese_s),
                        iconPainter = painterResource(id = R.drawable.chinese),
                        iconColor = SaltTheme.colors.text
                    )
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                hapticStrength.intValue
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.KEY_LANGUAGE] = "en"
                                }
                            }
                            languagePopupMenuState.dismiss()
                        },
                        selected = selectedLanguage.value == "en",
                        text = stringResource(R.string.english),
                        iconPainter = painterResource(id = R.drawable.english),
                        iconColor = SaltTheme.colors.text
                    )
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                hapticStrength.intValue
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.KEY_LANGUAGE] = "ko"
                                }
                            }
                            languagePopupMenuState.dismiss()
                        },
                        selected = selectedLanguage.value == "ko",
                        text = stringResource(R.string.korean),
                        iconPainter = painterResource(id = R.drawable.korean),
                        iconColor = SaltTheme.colors.text,
                        iconPaddingValues = PaddingValues(all = 1.5.dp)
                    )
                }
            }

            RoundedColumn {
                ItemTitle(text = stringResource(R.string.haptic))
                ItemSwitcher(
                    state = enableHaptic.value,
                    onChange = {
                        coroutineScope.launch {
                            dataStore.edit { settings ->
                                settings[DataStoreConstants.KEY_ENABLE_HAPTIC] = it
                            }
                        }
                    },
                    text = stringResource(id = R.string.haptic_feedfback_switcher_title),
                    sub = stringResource(id = R.string.haptic_feedfback_switcher_sub),
                    enableHaptic = enableHaptic.value,
                    iconPainter = painterResource(id = R.drawable.haptic),
                    iconColor = SaltTheme.colors.text,
                    iconPaddingValues = PaddingValues(
                        start = 2.dp,
                        end = 2.dp,
                        top = 1.5.dp,
                        bottom = 1.5.dp
                    ),
                    hapticStrength = hapticStrength.intValue
                )
                ItemPopup(
                    state = hapticStrengthPopupMenuState,
                    text = stringResource(id = R.string.haptic_strength),
                    selectedItem = when (hapticStrength.intValue) {
                        1 -> stringResource(R.string.weak)
                        2 -> stringResource(R.string.normal_weak)
                        3 -> stringResource(R.string.normal)
                        4 -> stringResource(R.string.normal_strong)
                        5 -> stringResource(R.string.strong)
                        else -> ""
                    },
                    popupWidth = 130,
                ) {
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                1
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.HAPTIC_STRENGTH] = 1
                                }
                            }
                            hapticStrengthPopupMenuState.dismiss()
                        },
                        text = stringResource(R.string.weak),
                        selected = hapticStrength.intValue == 1
                    )
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                2
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.HAPTIC_STRENGTH] = 2
                                }
                            }
                            hapticStrengthPopupMenuState.dismiss()
                        },
                        text = stringResource(R.string.normal_weak),
                        selected = hapticStrength.intValue == 2
                    )
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                3
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.HAPTIC_STRENGTH] = 3
                                }
                            }
                            hapticStrengthPopupMenuState.dismiss()
                        },
                        text = stringResource(R.string.normal),
                        selected = hapticStrength.intValue == 3
                    )
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                4
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.HAPTIC_STRENGTH] = 4
                                }
                            }
                            hapticStrengthPopupMenuState.dismiss()
                        },
                        text = stringResource(R.string.normal_strong),
                        selected = hapticStrength.intValue == 4
                    )
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(
                                context,
                                enableHaptic.value,
                                5
                            ).click()
                            coroutineScope.launch {
                                dataStore.edit { settings ->
                                    settings[DataStoreConstants.HAPTIC_STRENGTH] = 5
                                }
                            }
                            hapticStrengthPopupMenuState.dismiss()
                        },
                        text = stringResource(R.string.strong),
                        selected = hapticStrength.intValue == 5
                    )
                }
            }

            RoundedColumn {
                ItemTitle(text = stringResource(R.string.version))
                ItemSwitcher(
                    state = enableAutoCheckUpdate.value,
                    onChange = {
                        coroutineScope.launch {
                            dataStore.edit { settings ->
                                settings[DataStoreConstants.KEY_ENABLE_AUTO_CHECK_UPDATE] = it
                            }
                        }
                    },
                    text = stringResource(id = R.string.check_updates_at_start),
                    enableHaptic = enableHaptic.value,
                    iconPainter = painterResource(id = R.drawable.auto_check_update),
                    iconColor = SaltTheme.colors.text,
                    iconPaddingValues = PaddingValues(all = 1.5.dp),
                    hapticStrength = hapticStrength.intValue
                )
            }

            RoundedColumn {
                ItemTitle(text = stringResource(R.string.other))
                Item(
                    onClick = {
                        coroutineScope.launch {
                            settingsPageState.animateScrollToPage(
                                1,
                                animationSpec = spring(2f)
                            )
                        }
                    },
                    text = stringResource(id = R.string.about),
                    rightSub = context.packageManager.getPackageInfo(
                        context.packageName,
                        0
                    ).versionName,
                    iconPainter = painterResource(id = R.drawable.about),
                    iconColor = SaltTheme.colors.text,
                    iconPaddingValues = PaddingValues(all = 1.5.dp)
                )
            }
        }
    }
}