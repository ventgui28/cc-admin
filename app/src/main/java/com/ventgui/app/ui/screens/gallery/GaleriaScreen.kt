package com.ventgui.app.ui.screens.gallery

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.model.SocialPost
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import com.ventgui.app.ui.components.*
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GaleriaScreen(
    innerPadding: PaddingValues, 
    onSelectionModeChange: (Boolean) -> Unit = {},
    onOpenDrawer: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.gallery_tab_photos), stringResource(R.string.gallery_tab_models))
    
    var posts by remember { mutableStateOf<List<SocialPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Selection States
    var selectedPostIds by remember { mutableStateOf(setOf<String>()) }
    var postToShowFull by remember { mutableStateOf<SocialPost?>(null) }
    
    val isInSelectionMode = selectedPostIds.isNotEmpty()

    LaunchedEffect(isInSelectionMode) {
        onSelectionModeChange(isInSelectionMode)
    }

    val fetchPosts = {
        scope.launch {
            try {
                if (!isRefreshing) isLoading = true
                val result = SupabaseClient.client.postgrest.from("social_posts")
                    .select()
                    .decodeList<SocialPost>()
                posts = result.sortedByDescending { it.created_at }
            } catch (e: Exception) {
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchPosts() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        PremiumMeshBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- FIXED HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.05f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) { Icon(Icons.Rounded.Menu, null, tint = Color.White) }

                // LOGO
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(Color.White, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text("C", color = MidnightBlue, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CANTANHEDE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("CYCLING HUB", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }

                // Balance placeholder
                Box(modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Header Section
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = if (isInSelectionMode) stringResource(R.string.gallery_selected) else stringResource(R.string.gallery_digital),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (isInSelectionMode) stringResource(R.string.gallery_selected_count, selectedPostIds.size) else stringResource(R.string.gallery_social_caption),
                    color = Color(0xFF80D8FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Tabs
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CyberCyan else Color.Transparent)
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title.uppercase(),
                            color = if (isSelected) MidnightBlue else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF80D8FF))
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        fetchPosts()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (selectedTab) {
                        0 -> GalleryGridView(
                            posts = posts.filter { !it.image_url.isNullOrBlank() },
                            selectedIds = selectedPostIds,
                            onLongClick = { id -> 
                                if (!isInSelectionMode) selectedPostIds = setOf(id)
                            },
                            onClick = { post ->
                                if (isInSelectionMode) {
                                    selectedPostIds = if (selectedPostIds.contains(post.id)) {
                                        selectedPostIds - post.id!!
                                    } else {
                                        selectedPostIds + post.id!!
                                    }
                                } else {
                                    postToShowFull = post
                                }
                            }
                        )
                        1 -> ModelosTextsView(posts)
                    }
                }
            }
        }

        // Selection Action Bar
        AnimatedVisibility(
            visible = isInSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                HyperGlassCard(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    color = Color(0xFFFF5252)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.sponsors_selected_bar_title, selectedPostIds.size), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text(stringResource(R.string.common_irreversible_action), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { selectedPostIds = emptySet() }) {
                                Text(stringResource(R.string.common_cancel), color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { 
                                    scope.launch {
                                        try {
                                            selectedPostIds.forEach { id ->
                                                SupabaseClient.client.postgrest.from("social_posts").delete { filter { eq("id", id) } }
                                            }
                                            selectedPostIds = emptySet()
                                            fetchPosts()
                                        } catch (e: Exception) {}
                                    }
                                },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.common_delete), tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Full Image Dialog
        if (postToShowFull != null) {
            FullImageDialog(post = postToShowFull!!, onDismiss = { postToShowFull = null })
        }
    }
}
