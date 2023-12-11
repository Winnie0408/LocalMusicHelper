package com.hwinzniej.musichelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hwinzniej.musichelper.ui.theme.MusicHelperTheme
import com.moriafly.salt.ui.BottomBar
import com.moriafly.salt.ui.BottomBarItem
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemSpacer
import com.moriafly.salt.ui.ItemText
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicHelperTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
//                        Greeting("Android")
                        MainUI()
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@OptIn(UnstableSaltApi::class)
@Composable
private fun MainUI() {
    val scanResult = remember {
        mutableStateOf("扫描结果将会显示在这里")
    }
    val coroutineScope = rememberCoroutineScope()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {

            },
            text = "扫描"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(color = SaltTheme.colors.background)
//                .verticalScroll(rememberScrollState())
        ) {
            RoundedColumn {
                ItemTitle(text = "扫描控制")
                ItemSpacer()
                ItemText(text = "点击按钮以开始")
                ItemSpacer()
                TextButton(onClick = {
                    scanResult.value = ""
                    coroutineScope.launch {
                        for (i in 1..100) {
                            delay(100L)
                            scanResult.value =
                                "第 $i 首歌：aaabbb-cccddd-eeefff\n" + scanResult.value
                        }
                    }
                }, text = "开始扫描")
            }
            RoundedColumn {
                ItemTitle(text = "扫描结果")
                ItemContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(color = SaltTheme.colors.background)
                    ) {
                        item {
                            Text(
                                text = scanResult.value,
                                fontSize = 20.sp,
                                style = TextStyle(
                                    lineHeight = 1.5.em,
                                ),
                            )
                        }
                    }
                }
            }
        }
        BottomBar {
            BottomBarItem(
                state = true,
                onClick = {

                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = "二维码"
            )
            BottomBarItem(
                state = false,
                onClick = {

                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = "认证"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MusicHelperTheme {
        MainUI()
    }
}