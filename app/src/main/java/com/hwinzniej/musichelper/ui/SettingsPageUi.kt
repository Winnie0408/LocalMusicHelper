package com.hwinzniej.musichelper.ui

import android.content.res.Resources
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.activity.SettingsPage
import com.hwinzniej.musichelper.data.DataStoreConstants
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.hwinzniej.musichelper.utils.Tools
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    dataStore: DataStore<Preferences>
) {
    val context = LocalContext.current
    val themeModePopupMenuState = rememberPopupState()
    val languagePopupMenuState = rememberPopupState()
    val coroutineScope = rememberCoroutineScope()

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
                    iconColor = SaltTheme.colors.text
                )
                ItemPopup(
                    state = themeModePopupMenuState,
                    text = stringResource(R.string.theme_mode_switcher_text),
                    selectedItem = when (selectedThemeMode.intValue) {
                        0 -> stringResource(R.string.light_mode)
                        1 -> stringResource(R.string.dark_mode)
                        2 -> stringResource(R.string.follow_system)
                        else -> ""
                    },
                    popupWidth = 140,
                    iconPainter = painterResource(id = R.drawable.app_theme),
                    iconColor = SaltTheme.colors.text,
                    iconPaddingValues = PaddingValues(
                        start = 2.dp,
                        end = 2.dp,
                        top = 2.dp,
                        bottom = 2.dp
                    )
                ) {
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(context, enableHaptic.value).click()
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
                            MyVibrationEffect(context, enableHaptic.value).click()
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
                            MyVibrationEffect(context, enableHaptic.value).click()
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
                        iconPaddingValues = PaddingValues(
                            start = 1.dp,
                            end = 1.dp,
                            top = 1.dp,
                            bottom = 1.dp
                        )
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
                                var hasRoot = false
                                try {
                                    hasRoot =
                                        (File("/system/bin/su").exists() || File("/system/xbin/su").exists())
                                } catch (_: Exception) {
                                }
                                if (hasRoot) {
                                    try {
                                        if (Tools().execShellCmdWithRoot("ls /data")
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
                                                    it
                                            }
                                        }
                                    } catch (_: Exception) {
                                    }
                                } else {
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
                                        it
                                }
                            }
                        }
                    },
                    text = stringResource(R.string.use_root_access_switcher_title),
                    sub = stringResource(R.string.use_root_access_switcher_sub),
                    enableHaptic = enableHaptic.value,
                    iconPainter = painterResource(id = R.drawable.root_access),
                    iconColor = SaltTheme.colors.text
                )
            }

            RoundedColumn {
                ItemTitle(text = stringResource(R.string.language))
                ItemValue(
                    text = stringResource(R.string.system_language),
                    rightSub = "${Resources.getSystem().configuration.locales[0].language} - ${Resources.getSystem().configuration.locales[0].country}"
                )
                ItemPopup(
                    state = languagePopupMenuState,
                    text = stringResource(R.string.app_language),
                    selectedItem = when (selectedLanguage.value) {
                        "system" -> stringResource(R.string.follow_system)
                        "zh" -> stringResource(R.string.chinese_s)
                        "en" -> stringResource(R.string.english)
                        "ko" -> stringResource(R.string.korean)
                        else -> ""
                    },
                    popupWidth = 180,
                    iconPainter = painterResource(id = R.drawable.language),
                    iconColor = SaltTheme.colors.text,
                    iconPaddingValues = PaddingValues(
                        start = 1.dp,
                        end = 1.dp,
                        top = 1.dp,
                        bottom = 1.dp
                    )
                ) {
                    PopupMenuItem(
                        onClick = {
                            MyVibrationEffect(context, enableHaptic.value).click()
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
                            MyVibrationEffect(context, enableHaptic.value).click()
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
                            MyVibrationEffect(context, enableHaptic.value).click()
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
                            MyVibrationEffect(context, enableHaptic.value).click()
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
                        iconPaddingValues = PaddingValues(
                            start = 1.5.dp,
                            end = 1.5.dp,
                            top = 1.5.dp,
                            bottom = 1.5.dp
                        )
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
                    )
                )
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
                    iconPaddingValues = PaddingValues(
                        start = 1.5.dp,
                        end = 1.5.dp,
                        top = 1.5.dp,
                        bottom = 1.5.dp
                    )
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
                    iconPaddingValues = PaddingValues(
                        start = 1.5.dp,
                        end = 1.5.dp,
                        top = 1.5.dp,
                        bottom = 1.5.dp
                    )
                )
            }
        }
    }
}