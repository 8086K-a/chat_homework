@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chat.ui.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.chat.data.local.entity.ConversationEntity
import com.example.chat.model.ChatMessage
import com.example.chat.model.Role
import com.example.chat.ui.theme.ChatAIDarkSidebar
import com.example.chat.ui.theme.ChatAIDarkChatBg
import com.example.chat.ui.theme.ChatAIDarkUserBubble
import com.example.chat.ui.theme.ChatAIDarkInputBg
import com.example.chat.ui.theme.ChatAIDarkBorder
import com.example.chat.ui.theme.ChatAIDarkTextPrimary
import com.example.chat.ui.theme.ChatAIDarkTextSecondary
import com.example.chat.ui.theme.ChatAISidebar
import com.example.chat.ui.theme.ChatAIChatBg
import com.example.chat.ui.theme.ChatAIUserBubble
import com.example.chat.ui.theme.ChatAIInputBg
import com.example.chat.ui.theme.ChatAIBorder
import com.example.chat.ui.theme.ChatAITextPrimary
import com.example.chat.ui.theme.ChatAITextSecondary
import com.example.chat.ui.theme.ChatAILogoGreen
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle

@Composable
fun ChatScreen(
    conversations: List<ConversationEntity>,
    currentConversationId: String?,
    messages: List<ChatMessage>,
    inputText: String,
    isLoading: Boolean,
    streamingContent: String,
    error: String?,
    onClearError: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    selectedImageDataUrl: String?,
    onImageSelected: (String) -> Unit,
    onClearSelectedImage: () -> Unit,
    onSettingsClick: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme2()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val bgColor = if (isDark) ChatAIDarkChatBg else ChatAIChatBg
    val sidebarColor = if (isDark) ChatAIDarkSidebar else ChatAISidebar
    val inputBg = if (isDark) ChatAIDarkInputBg else ChatAIInputBg
    val inputBorder = if (isDark) ChatAIDarkBorder else ChatAIBorder
    val textPrimary = if (isDark) ChatAIDarkTextPrimary else ChatAITextPrimary
    val textSecondary = if (isDark) ChatAIDarkTextSecondary else ChatAITextSecondary
    val userBubble = if (isDark) ChatAIDarkUserBubble else ChatAIUserBubble
    val drawerScrim = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f)

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = drawerScrim,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .requiredWidth(300.dp),
                drawerShape = RoundedCornerShape(0.dp),
                drawerContainerColor = sidebarColor,
                drawerContentColor = textPrimary,
                drawerTonalElevation = 0.dp,
            ) {
                DrawerContent(
                    conversations = conversations,
                    currentConversationId = currentConversationId,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isDark = isDark,
                    onClose = { scope.launch { drawerState.close() } },
                    onNewChat = {
                        onNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    onSelectChat = { id ->
                        onSelectConversation(id)
                        scope.launch { drawerState.close() }
                    },
                    onDeleteChat = { id ->
                        onDeleteConversation(id)
                    },
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = bgColor,
            topBar = {
                TopBar(
                    bgColor = bgColor,
                    textPrimary = textPrimary,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSettingsClick = onSettingsClick,
                )
            },
            bottomBar = {
                if (error != null) {
                    Text(
                        text = error,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                InputArea(
                    inputText = inputText,
                    onInputChange = onInputChange,
                    onSend = onSendMessage,
                    isLoading = isLoading,
                    selectedImageDataUrl = selectedImageDataUrl,
                    onImageSelected = onImageSelected,
                    onClearSelectedImage = onClearSelectedImage,
                    bgColor = bgColor,
                    inputBg = inputBg,
                    inputBorder = inputBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                )
            }
        ) { innerPadding ->
            val isStreaming = streamingContent.isNotEmpty()
            val streamingMessageId = if (isStreaming) {
                if (messages.lastOrNull()?.role == Role.Assistant) messages.lastOrNull()?.id else "streaming-assistant"
            } else {
                null
            }

            val displayMessages = if (isStreaming) {
                if (messages.lastOrNull()?.role == Role.Assistant) {
                    messages.mapIndexed { idx, msg ->
                        if (idx == messages.lastIndex) {
                            msg.copy(content = streamingContent)
                        } else msg
                    }
                } else {
                    messages + listOf(
                        ChatMessage(
                            id = "streaming-assistant",
                            role = Role.Assistant,
                            content = streamingContent,
                        )
                    )
                }
            } else {
                messages
            }
            val showAssistantLoading = isLoading && streamingContent.isBlank()

            if (displayMessages.isEmpty() && !isLoading) {
                HomeContent(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                )
            } else {
                ChatMessageList(
                    messages = displayMessages,
                    streamingMessageId = streamingMessageId,
                    showLoadingIndicator = showAssistantLoading,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    userBubble = userBubble,
                    isDark = isDark,
                    onCopyAssistant = { content ->
                        clipboardManager.setText(AnnotatedString(content))
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    },
                    onRegenerate = onRegenerate,
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    bgColor: Color,
    textPrimary: Color,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    TopAppBar(
        modifier = Modifier.padding(horizontal = 2.dp),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = bgColor,
            titleContentColor = textPrimary,
            actionIconContentColor = textPrimary,
            navigationIconContentColor = textPrimary,
        ),
        title = {
            Text(
                text = "ChatAI",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Menu, contentDescription = "菜单", modifier = Modifier.size(20.dp))
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "设置", modifier = Modifier.size(19.dp))
            }
        }
    )
}

@Composable
private fun DrawerContent(
    conversations: List<ConversationEntity>,
    currentConversationId: String?,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
) {
    val sidebarColor = if (isDark) ChatAIDarkSidebar else ChatAISidebar

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(sidebarColor)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ChatAILogoGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("C", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("ChatAI", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close, "关闭",
                    tint = textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // New chat
        Surface(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent,
            onClick = onNewChat,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF2B2C2F) else Color(0xFFF4F4F5)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("新聊天", fontSize = 13.sp, color = textPrimary)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            color = if (isDark) Color(0xFF4D4D4F) else Color(0xFFE5E5E5),
            thickness = 0.5.dp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "历史记录", fontSize = 11.sp, fontWeight = FontWeight.Medium,
            color = textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(conversations, key = { it.id }) { conv ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (conv.id == currentConversationId)
                        (if (isDark) Color(0xFF343641) else Color(0xFFF0F0F1))
                    else Color.Transparent,
                    onClick = { onSelectChat(conv.id) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = conv.title,
                            fontSize = 13.sp,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onDeleteChat(conv.id) },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Default.Close, "删除",
                                tint = textSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            color = if (isDark) Color(0xFF4D4D4F) else Color(0xFFE5E5E5),
            thickness = 0.5.dp,
        )

        // User section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ChatAILogoGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("用户", fontSize = 13.sp, color = textPrimary)
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    textPrimary: Color,
    textSecondary: Color,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ChatAILogoGreen),
            contentAlignment = Alignment.Center,
        ) {
            Text("C", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "有什么可以帮助你的？",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = textPrimary,
        )
    }
}

@Composable
private fun ChatMessageList(
    messages: List<ChatMessage>,
    streamingMessageId: String?,
    showLoadingIndicator: Boolean,
    modifier: Modifier = Modifier,
    textPrimary: Color,
    textSecondary: Color,
    userBubble: Color,
    isDark: Boolean,
    onCopyAssistant: (String) -> Unit,
    onRegenerate: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(
                message = message,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                userBubble = userBubble,
                isDark = isDark,
                isStreaming = message.id == streamingMessageId,
                onCopyAssistant = onCopyAssistant,
                onRegenerate = onRegenerate,
            )
        }
        if (showLoadingIndicator) {
            item {
                LoadingIndicator(textSecondary)
            }
        }
    }
}

@Composable
private fun LoadingIndicator(textSecondary: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(ChatAILogoGreen),
            contentAlignment = Alignment.Center,
        ) {
            Text("C", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = textSecondary,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    textPrimary: Color,
    textSecondary: Color,
    userBubble: Color,
    isDark: Boolean,
    isStreaming: Boolean,
    onCopyAssistant: (String) -> Unit,
    onRegenerate: () -> Unit,
) {
    val isUser = message.role == Role.User
    val streamingPreview = if (!isUser && isStreaming) {
        remember(message.content) { buildStreamingMarkdownPreview(message.content) }
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        if (isUser) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = userBubble,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        if (message.imageDataUrl != null) {
                            val imageBitmap = remember(message.imageDataUrl) {
                                decodeDataUrlToImageBitmap(message.imageDataUrl)
                            }
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = "图片",
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                                if (message.content.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                color = textPrimary,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(ChatAILogoGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("C", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "ChatAI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (isStreaming) {
                        if (!streamingPreview?.markdown.isNullOrBlank()) {
                            MarkdownText(
                                markdown = streamingPreview!!.markdown,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = textPrimary,
                                    fontSize = 16.sp,
                                    lineHeight = 28.sp,
                                ),
                                linkColor = ChatAILogoGreen,
                            )
                        }
                        if (!streamingPreview?.tail.isNullOrBlank()) {
                            Text(
                                text = streamingPreview!!.tail,
                                color = textPrimary,
                                fontSize = 16.sp,
                                lineHeight = 28.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    } else {
                        MarkdownText(
                            markdown = message.content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = textPrimary,
                                fontSize = 16.sp,
                                lineHeight = 28.sp,
                            ),
                            linkColor = ChatAILogoGreen,
                        )
                    }
                    if (message.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            ActionIconButton(
                                icon = Icons.Default.ContentCopy,
                                contentDescription = "复制",
                                color = textSecondary,
                                onClick = { onCopyAssistant(message.content) },
                            )
                            ActionIconButton(
                                icon = Icons.Default.Refresh,
                                contentDescription = "重试",
                                color = textSecondary,
                                onClick = onRegenerate,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class StreamingMarkdownPreview(
    val markdown: String,
    val tail: String,
)

private fun buildStreamingMarkdownPreview(content: String): StreamingMarkdownPreview {
    val lastNewlineIndex = content.lastIndexOf('\n')
    if (lastNewlineIndex < 0) return StreamingMarkdownPreview(markdown = "", tail = content)

    val stableMarkdown = content.substring(0, lastNewlineIndex + 1)
    val tail = content.substring(lastNewlineIndex + 1)
    val fenceCount = "```".toRegex().findAll(stableMarkdown).count()
    val markdownForRender = if (fenceCount % 2 == 1) {
        stableMarkdown + "\n```"
    } else {
        stableMarkdown
    }
    return StreamingMarkdownPreview(markdown = markdownForRender, tail = tail)
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    color: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val containerColor = when {
        isPressed -> color.copy(alpha = 0.18f)
        isHovered -> color.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    val iconAlpha = when {
        isPressed -> 1f
        isHovered -> 0.95f
        else -> 0.8f
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(30.dp),
            interactionSource = interactionSource,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = color.copy(alpha = iconAlpha),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun InputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    selectedImageDataUrl: String?,
    onImageSelected: (String) -> Unit,
    onClearSelectedImage: () -> Unit,
    bgColor: Color,
    inputBg: Color,
    inputBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            val dataUrl = uriToDataUrl(context, it)
            if (dataUrl != null) {
                onImageSelected(dataUrl)
            } else {
                Toast.makeText(context, "读取图片失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        bitmap?.let {
            onImageSelected(bitmapToDataUrl(it))
        }
    }

    var attachmentMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (selectedImageDataUrl != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSystemInDarkTheme2()) Color(0xFF2B2C2F) else Color(0xFFF3F4F6),
                border = BorderStroke(1.dp, inputBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("已选择 1 张图片", color = textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClearSelectedImage, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "移除图片", tint = textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(1.5.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.16f)),
            shape = RoundedCornerShape(28.dp),
            color = inputBg,
            border = BorderStroke(1.dp, inputBorder),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    val plusInteraction = remember { MutableInteractionSource() }
                    val plusHovered by plusInteraction.collectIsHoveredAsState()
                    val plusPressed by plusInteraction.collectIsPressedAsState()
                    val plusBg = when {
                        plusPressed -> if (isSystemInDarkTheme2()) Color(0xFF3A3A4A) else Color(0xFFD1D5DB)
                        plusHovered -> if (isSystemInDarkTheme2()) Color(0xFF3A3A4A) else Color(0xFFE5E7EB)
                        else -> Color.Transparent
                    }

                    Surface(
                        onClick = {
                            attachmentMenuExpanded = true
                        },
                        shape = CircleShape,
                        color = plusBg,
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "附件",
                                tint = if (isSystemInDarkTheme2()) Color(0xFFACACBE) else Color(0xFF6B6C7B),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = attachmentMenuExpanded,
                        onDismissRequest = { attachmentMenuExpanded = false },
                        modifier = Modifier
                            .background(
                                if (isSystemInDarkTheme2()) Color(0xFF2B2C2F) else Color.White,
                                RoundedCornerShape(12.dp),
                            )
                            .border(
                                0.5.dp,
                                if (isSystemInDarkTheme2()) Color(0xFF4D4D4F) else Color(0xFFE5E5E5),
                                RoundedCornerShape(12.dp),
                            ),
                    ) {
                        DropdownMenuItem(
                            text = { Text("拍照", color = textPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            onClick = {
                                attachmentMenuExpanded = false
                                cameraLauncher.launch(null)
                            },
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = if (isSystemInDarkTheme2()) Color(0xFF4D4D4F) else Color(0xFFE5E5E5),
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "上传照片和文件",
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "⌘U",
                                        color = textSecondary.copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            onClick = {
                                attachmentMenuExpanded = false
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                TextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    placeholder = {
                        Text(
                            "给 ChatAI 发送消息",
                            fontSize = 15.sp,
                            color = textSecondary.copy(alpha = 0.58f),
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = textPrimary, fontSize = 15.sp, lineHeight = 22.sp,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = textPrimary,
                    ),
                    maxLines = 6,
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = textSecondary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    val hasContent = inputText.isNotBlank() || selectedImageDataUrl != null
                    if (hasContent) {
                        Surface(
                            onClick = onSend,
                            shape = CircleShape,
                            color = ChatAILogoGreen,
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send, "发送",
                                    tint = Color.White, modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }

        Text(
            "ChatAI 也可能会犯错。请核查重要信息。",
            fontSize = 12.sp,
            color = textSecondary.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun isSystemInDarkTheme2(): Boolean {
    return androidx.compose.foundation.isSystemInDarkTheme()
}

private fun bitmapToDataUrl(bitmap: Bitmap): String {
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
    val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    return "data:image/jpeg;base64,$base64"
}

private fun uriToDataUrl(context: android.content.Context, uri: android.net.Uri): String? {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "image/jpeg"
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "data:$mime;base64,$base64"
}

private fun decodeDataUrlToImageBitmap(dataUrl: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val encoded = dataUrl.substringAfter("base64,", "")
        if (encoded.isBlank()) return null
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
