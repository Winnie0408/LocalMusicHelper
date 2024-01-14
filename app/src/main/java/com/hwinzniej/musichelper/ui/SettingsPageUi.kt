package com.hwinzniej.musichelper.ui

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.activity.SettingsPage
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.PopupMenuItem
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.launch

@OptIn(UnstableSaltApi::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsPageUi(
    settingsPage: SettingsPage,
    enableDynamicColor: MutableState<Boolean>,
    selectedThemeMode: MutableIntState,
    selectedLanguage: MutableIntState,
    useRootAccess: MutableState<Boolean>,
    enableAutoCheckUpdate: MutableState<Boolean>,
    settingsPageState: PagerState
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
                    onChange = { enableDynamicColor.value = it },
                    text = stringResource(R.string.dynamic_color_switcher_text),
                    sub = stringResource(R.string.dynamic_color_switcher_sub)
                )
                ItemPopup(
                    state = themeModePopupMenuState,
                    text = stringResource(R.string.theme_mode_switcher_text),
                    selectedItem = when (selectedThemeMode.intValue) {
                        0 -> stringResource(R.string.light_mode)
                        1 -> stringResource(R.string.dark_mode)
                        2 -> stringResource(R.string.follow_system)
                        else -> ""
                    }
                ) {
                    PopupMenuItem(
                        onClick = {
                            selectedThemeMode.intValue = 0
                            themeModePopupMenuState.dismiss()
                        },
                        selected = selectedThemeMode.intValue == 0,
                        text = stringResource(R.string.light_mode)
                    )
                    PopupMenuItem(
                        onClick = {
                            selectedThemeMode.intValue = 1
                            themeModePopupMenuState.dismiss()
                        },
                        selected = selectedThemeMode.intValue == 1,
                        text = stringResource(R.string.dark_mode)
                    )
                    PopupMenuItem(
                        onClick = {
                            selectedThemeMode.intValue = 2
                            themeModePopupMenuState.dismiss()
                        },
                        selected = selectedThemeMode.intValue == 2,
                        text = stringResource(R.string.follow_system),
                    )
                }
            }

            RoundedColumn {
                ItemTitle(text = stringResource(R.string.file_access))
                ItemSwitcher(
                    state = useRootAccess.value,
                    onChange = { useRootAccess.value = it },
                    text = stringResource(R.string.use_root_access_switcher_title),
                    sub = stringResource(R.string.use_root_access_switcher_sub)
                )
            }

            RoundedColumn {
                ItemTitle(text = stringResource(R.string.language))
                ItemValue(
                    text = stringResource(R.string.system_language),
                    sub = "${context.resources.configuration.locales[0].language} - ${context.resources.configuration.locales[0].country}"
                )
                ItemPopup(
                    state = languagePopupMenuState,
                    text = stringResource(R.string.app_language),
                    selectedItem = when (selectedLanguage.intValue) {
                        0 -> stringResource(R.string.chinese_s)
                        1 -> stringResource(R.string.english)
                        else -> ""
                    }
                ) {
                    PopupMenuItem(
                        onClick = {
                            selectedLanguage.intValue = 0
                            languagePopupMenuState.dismiss()
                        },
                        selected = selectedLanguage.intValue == 0,
                        text = stringResource(R.string.chinese_s),
                    )
                    PopupMenuItem(
                        onClick = {
                            selectedLanguage.intValue = 1
                            languagePopupMenuState.dismiss()
                        },
                        selected = selectedLanguage.intValue == 1,
                        text = stringResource(R.string.english),
                    )
                }
            }

            RoundedColumn {
                ItemTitle(text = stringResource(R.string.version))
                ItemSwitcher(
                    state = enableAutoCheckUpdate.value,
                    onChange = { enableAutoCheckUpdate.value = it },
                    text = stringResource(id = R.string.check_updates_at_start)
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
                    rightSub = stringResource(id = R.string.app_version)
                )
            }
        }
    }
}