package com.ventgui.app.ui.screens.content

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.JoinedRaceResult
import com.ventgui.app.data.model.Race
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.io.InputStream
import com.ventgui.app.ui.components.*
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentFactoryScreen(innerPadding: PaddingValues, onOpenDrawer: () -> Unit) {
    var races by remember { mutableStateOf<List<Race>>(emptyList()) }
    var athletes by remember { mutableStateOf<List<Athlete>>(emptyList()) }
    var selectedRace by rememberSaveable { mutableStateOf<Race?>(null) }
    var selectedAthletes by rememberSaveable { mutableStateOf(setOf<Athlete>()) }
    var athletesResults by remember { mutableStateOf<Map<String, JoinedRaceResult>>(emptyMap()) }
    
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                races = SupabaseClient.client.from("races").select().decodeList<Race>().sortedByDescending { it.date }
                athletes = SupabaseClient.client.from("athletes").select().decodeList<Athlete>()
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    // Fetch results for all selected athletes when race or athletes change
    LaunchedEffect(selectedRace, selectedAthletes) {
        if (selectedRace != null && selectedAthletes.isNotEmpty()) {
            try {
                val resultsMap = mutableMapOf<String, JoinedRaceResult>()
                selectedAthletes.forEach { athlete ->
                    val res = SupabaseClient.client.from("race_results")
                        .select {
                            filter {
                                eq("race_id", selectedRace!!.id!!)
                                eq("athlete_id", athlete.id!!)
                            }
                        }
                        .decodeSingleOrNull<JoinedRaceResult>()
                    if (res != null) {
                        resultsMap[athlete.id!!] = res
                    }
                }
                athletesResults = resultsMap
            } catch (e: Exception) {
                // Handle error
            }
        } else {
            athletesResults = emptyMap()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        PremiumMeshBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // --- FIXED HEADER ---
            PremiumTopBar(
                onLeftClick = onOpenDrawer,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.studio_production_title),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = stringResource(R.string.studio_production_subtitle),
                    color = CyberCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

            // Step 1: Select Race
            SectionTitle(stringResource(R.string.studio_choose_race), Icons.Rounded.Flag)
            ScrollableRow {
                races.forEach { race ->
                    SelectableChip(
                        text = race.title,
                        isSelected = selectedRace?.id == race.id,
                        onClick = { 
                            selectedRace = race
                            selectedAthletes = emptySet() // Reset athletes when race changes
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step 2: Select Athletes (Multi-select)
            SectionTitle(stringResource(R.string.studio_select_athletes), Icons.Rounded.People)
            ScrollableRow {
                athletes.forEach { athlete ->
                    val isSelected = selectedAthletes.any { it.id == athlete.id }
                    SelectableChip(
                        text = athlete.name,
                        isSelected = isSelected,
                        onClick = { 
                            selectedAthletes = if (isSelected) {
                                selectedAthletes.filter { it.id != athlete.id }.toSet()
                            } else {
                                selectedAthletes + athlete
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Step 4: AI Prompt Generator
            if (selectedRace != null && selectedAthletes.isNotEmpty()) {
                SectionTitle(stringResource(R.string.studio_generated_prompts), Icons.Rounded.AutoAwesome)
                
                val athletesNames = selectedAthletes.joinToString(", ") { it.name }
                val detailedResults = selectedAthletes.joinToString("\n") { athlete ->
                    val res = athletesResults[athlete.id]
                    val resultDetail = if (res != null) {
                        context.getString(R.string.studio_prompt_result_position, res.position)
                    } else {
                        context.getString(R.string.studio_prompt_result_participation)
                    }
                    context.getString(R.string.studio_prompt_result_line, athlete.name, resultDetail)
                }
                
                val isMultiple = selectedAthletes.size > 1
                
                // --- DESIGN PROMPT ---
                val themeText = if (isMultiple) {
                    context.getString(R.string.studio_prompt_theme_multi)
                } else {
                    context.getString(R.string.studio_prompt_theme_single)
                }
                val graphicDesignPrompt = context.getString(
                    R.string.studio_prompt_design_template,
                    themeText,
                    athletesNames,
                    selectedRace!!.title,
                    detailedResults
                )
                PromptCard(
                    title = stringResource(R.string.studio_graphic_design),
                    subtitle = stringResource(R.string.studio_graphic_design_desc),
                    icon = Icons.Rounded.Brush,
                    prompt = graphicDesignPrompt
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- CAPTION PROMPT ---
                val highlightText = if (isMultiple) {
                    context.getString(R.string.studio_prompt_highlight_multi)
                } else {
                    context.getString(R.string.studio_prompt_highlight_single)
                }
                val gritText = if (isMultiple) {
                    context.getString(R.string.studio_prompt_grit_multi)
                } else {
                    context.getString(R.string.studio_prompt_grit_single)
                }
                val captionPrompt = context.getString(
                    R.string.studio_prompt_caption_template,
                    selectedRace!!.title,
                    highlightText,
                    detailedResults,
                    selectedRace!!.category ?: "",
                    selectedRace!!.date ?: "",
                    gritText
                )
                PromptCard(
                    title = stringResource(R.string.studio_social_caption),
                    subtitle = stringResource(R.string.studio_social_caption_desc),
                    icon = Icons.Rounded.ShortText,
                    prompt = captionPrompt
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
}

@Composable
fun PromptCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, prompt: String) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    HyperGlassCard(
        modifier = Modifier.fillMaxWidth(),
        color = CyberCyan
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(CyberCyan.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = prompt,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PremiumButton(
                text = stringResource(R.string.studio_copy_prompt),
                onClick = { 
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(prompt))
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.ContentCopy
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun ScrollableRow(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
fun SelectableChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    HyperGlassCard(
        onClick = onClick,
        variant = if (isSelected) "solid" else "glass",
        cornerRadius = 12,
        borderColor = if (isSelected) CyberCyan else Color.White,
        borderAlpha = if (isSelected) 0.8f else 0.1f
    ) {
        Box(
            modifier = Modifier
                .background(if (isSelected) CyberCyan else Color.Transparent)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = text,
                color = if (isSelected) MidnightBlue else Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
