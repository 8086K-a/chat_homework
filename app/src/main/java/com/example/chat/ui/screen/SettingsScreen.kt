@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chat.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chat.data.remote.OpenAiService
import com.example.chat.ui.theme.ChatAILogoGreen

data class ApiConfig(
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
)

@Composable
fun SettingsScreen(
    currentBaseUrl: String,
    currentApiKey: String,
    currentModelName: String,
    onSaveConfig: (ApiConfig) -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    var baseUrl by remember { mutableStateOf(currentBaseUrl) }
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var modelName by remember { mutableStateOf(currentModelName) }
    var customModelName by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(currentBaseUrl) {
        baseUrl = currentBaseUrl
    }

    LaunchedEffect(currentApiKey) {
        apiKey = currentApiKey
    }

    LaunchedEffect(currentModelName) {
        modelName = currentModelName
        customModelName = if (currentModelName == OpenAiService.DEFAULT_MODEL) "" else currentModelName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F8))
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text("设置", color = Color(0xFF202123), fontWeight = FontWeight.SemiBold)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "返回",
                        tint = Color(0xFF202123),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFF7F7F8),
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                "API 配置",
                color = Color(0xFF202123),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "可修改 Base URL、API Key 与模型；模型默认值固定",
                color = Color(0xFF6B6C7B),
                fontSize = 13.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditableConfigField(
                title = "Base URL",
                value = baseUrl,
                onValueChange = {
                    baseUrl = it
                    saved = false
                },
                placeholder = "https://dashscope.aliyuncs.com/compatible-mode",
            )

            Spacer(modifier = Modifier.height(12.dp))

            EditableConfigField(
                title = "API Key",
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    saved = false
                },
                placeholder = "sk-...",
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "模型",
                color = Color(0xFF202123),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ModelOption(
                    label = "${OpenAiService.DEFAULT_MODEL}（默认）",
                    selected = modelName == OpenAiService.DEFAULT_MODEL,
                    onClick = {
                        modelName = OpenAiService.DEFAULT_MODEL
                        customModelName = ""
                        saved = false
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = customModelName,
                onValueChange = { value ->
                    customModelName = value
                    modelName = value.trim().ifBlank { OpenAiService.DEFAULT_MODEL }
                    saved = false
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("自定义模型") },
                placeholder = { Text("例如：qwen-max") },
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "默认模型固定不可修改；留空将继续使用默认模型",
                color = Color(0xFF6B6C7B),
                fontSize = 12.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSaveConfig(
                        ApiConfig(
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            modelName = modelName,
                        )
                    )
                    saved = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ChatAILogoGreen),
            ) {
                Text(
                    if (saved) "已保存" else "保存配置",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider(color = Color(0xFFE5E5E5))

            Spacer(modifier = Modifier.height(32.dp))

            // Account Section
            Text(
                "账号",
                color = Color(0xFF202123),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onEditProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF202123),
                ),
            ) {
                Text(
                    "修改用户信息",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                ),
            ) {
                Text(
                    "退出登录",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EditableConfigField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                title,
                color = Color(0xFF6B6C7B),
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(placeholder) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ChatAILogoGreen,
                    unfocusedBorderColor = Color(0xFFE5E5E5),
                ),
            )
        }
    }
}

@Composable
private fun ModelOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) ChatAILogoGreen else Color(0xFFE5E5E5)
    val bgColor = if (selected) Color(0xFFEFFAF4) else Color.White
    val textColor = if (selected) Color(0xFF127A45) else Color(0xFF202123)
    val borderWidth: Dp = if (selected) 1.5.dp else 1.dp

    Surface(
        modifier = modifier
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.SmartToy, null, tint = textColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
