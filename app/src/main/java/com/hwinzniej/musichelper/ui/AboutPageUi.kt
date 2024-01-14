package com.hwinzniej.musichelper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwinzniej.musichelper.R
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import kotlinx.coroutines.launch

@OptIn(UnstableSaltApi::class, ExperimentalFoundationApi::class)
@Composable
fun AboutPageUi(
    settingsPageState: PagerState
) {
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = settingsPageState.currentPage == 1) {
        coroutineScope.launch {
            settingsPageState.animateScrollToPage(
                0, animationSpec = spring(2f)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {
                coroutineScope.launch {
                    settingsPageState.animateScrollToPage(
                        0, animationSpec = spring(2f)
                    )
                }
            },
            text = stringResource(id = R.string.about_function_name),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = SaltTheme.colors.background)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null
                )
                Text(
                    text = stringResource(
                        id = R.string.app_name
                    ), fontSize = 20.sp, color = SaltTheme.colors.text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        id = R.string.app_version
                    ), color = SaltTheme.colors.text
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            RoundedColumn {
//                ItemTitle(text = stringResource(id = R.string.update))
                Item(
                    onClick = { /*TODO*/ }, text = stringResource(id = R.string.developers)
                )
                Item(
                    onClick = { /*TODO*/ }, text = stringResource(id = R.string.check_for_updates)
                )
                Item(
                    onClick = { /*TODO*/ }, text = stringResource(id = R.string.open_source_licence)
                )
                Item(
                    onClick = { /*TODO*/ }, text = stringResource(id = R.string.pc_salt_converter)
                )
                Item(
                    onClick = { /*TODO*/ }, text = stringResource(id = R.string.buy_me_a_coffee)
                )
            }

            RoundedColumn {
                Item(
                    onClick = { /*TODO*/ },
                    text = stringResource(id = R.string.join_qq_group),
                    iconPainter = painterResource(id = R.drawable.ic_check)
                )
                Item(
                    onClick = { /*TODO*/ },
                    text = stringResource(id = R.string.follow_on_bilibili),
                    iconPainter = painterResource(id = R.drawable.ic_check)
                )
                Item(
                    onClick = { /*TODO*/ },
                    text = stringResource(id = R.string.open_source_github),
                    sub = stringResource(id = R.string.open_source_sub),
                    iconPainter = painterResource(id = R.drawable.ic_check)
                )
                Item(
                    onClick = { /*TODO*/ },
                    text = stringResource(id = R.string.open_source_gitee),
                    sub = stringResource(id = R.string.open_source_sub),
                    iconPainter = painterResource(id = R.drawable.ic_check)
                )
            }

        }
    }
}

@OptIn(UnstableSaltApi::class, ExperimentalFoundationApi::class)
@Preview
@Composable
fun Preview1() {
//    AboutPageUi()
}