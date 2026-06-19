package com.ventgui.app.ui.screens.races

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ventgui.app.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun NumberScrollPickerDialog(
    title: String,
    range: IntRange,
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedValue by remember { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(
            modifier = Modifier.fillMaxWidth(0.85f),
            color = CyberCyan,
            variant = "solid"
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth().wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedValue - range.first).coerceAtLeast(0))
                val coroutineScope = rememberCoroutineScope()
                val density = LocalDensity.current
                
                Box(
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(CyberCyan.copy(alpha = 0.1f))
                    )
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(range.toList()) { value ->
                            val isSelected = selectedValue == value
                            Text(
                                text = value.toString(),
                                color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.4f),
                                fontSize = if (isSelected) 22.sp else 16.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .wrapContentHeight(Alignment.CenterVertically)
                                    .clickable { 
                                        selectedValue = value 
                                        coroutineScope.launch {
                                            listState.animateScrollToItem((value - range.first).coerceAtLeast(0))
                                        }
                                    }
                            )
                        }
                    }
                }
                
                LaunchedEffect(listState.isScrollInProgress) {
                    if (!listState.isScrollInProgress) {
                        val firstVisible = listState.firstVisibleItemIndex
                        val offset = listState.firstVisibleItemScrollOffset
                        val itemHeightPx = with(density) { 40.dp.toPx() }
                        val index = if (offset > itemHeightPx / 2) firstVisible + 1 else firstVisible
                        val targetVal = range.elementAtOrNull(index)
                        if (targetVal != null) {
                            selectedValue = targetVal
                            if (firstVisible != index || offset > 0) {
                                listState.animateScrollToItem(index)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) { 
                        Text("CANCELAR", color = Color.White.copy(alpha = 0.6f), maxLines = 1) 
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedValue) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MidnightBlue),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SELECIONAR", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun TimeScrollPickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    var selectedHour by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 0) }
    var selectedMinute by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull() ?: 0) }
    var selectedSecond by remember { mutableStateOf(parts.getOrNull(2)?.toIntOrNull() ?: 0) }
    val density = LocalDensity.current
    
    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(
            modifier = Modifier.fillMaxWidth(0.9f),
            color = CyberCyan,
            variant = "solid"
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth().wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Selecionar Tempo", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hour picker
                    Box(modifier = Modifier.weight(1f).height(120.dp)) {
                        val hoursList = (0..23).toList()
                        val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedHour)
                        val coroutineScope = rememberCoroutineScope()
                        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(CyberCyan.copy(alpha = 0.1f)))
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                items(hoursList) { h ->
                                    val isSel = selectedHour == h
                                    Text(
                                        text = h.toString().padStart(2, '0'), 
                                        color = if (isSel) CyberCyan else Color.White.copy(alpha = 0.4f), 
                                        fontSize = if (isSel) 20.sp else 15.sp, 
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, 
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .wrapContentHeight(Alignment.CenterVertically)
                                            .clickable { 
                                                selectedHour = h 
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(h)
                                                }
                                            }
                                    )
                                }
                            }
                        }
                        LaunchedEffect(listState.isScrollInProgress) {
                            if (!listState.isScrollInProgress) {
                                val firstVisible = listState.firstVisibleItemIndex
                                val offset = listState.firstVisibleItemScrollOffset
                                val itemHeightPx = with(density) { 40.dp.toPx() }
                                val index = if (offset > itemHeightPx / 2) firstVisible + 1 else firstVisible
                                val targetVal = hoursList.getOrNull(index)
                                if (targetVal != null) {
                                    selectedHour = targetVal
                                    if (firstVisible != index || offset > 0) {
                                        listState.animateScrollToItem(index)
                                    }
                                }
                            }
                        }
                    }
                    
                    Text(":", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                    
                    // Minute picker
                    Box(modifier = Modifier.weight(1f).height(120.dp)) {
                        val minutesList = (0..59).toList()
                        val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedMinute)
                        val coroutineScope = rememberCoroutineScope()
                        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(CyberCyan.copy(alpha = 0.1f)))
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                items(minutesList) { m ->
                                    val isSel = selectedMinute == m
                                    Text(
                                        text = m.toString().padStart(2, '0'), 
                                        color = if (isSel) CyberCyan else Color.White.copy(alpha = 0.4f), 
                                        fontSize = if (isSel) 20.sp else 15.sp, 
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, 
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .wrapContentHeight(Alignment.CenterVertically)
                                            .clickable { 
                                                selectedMinute = m 
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(m)
                                                }
                                            }
                                    )
                                }
                            }
                        }
                        LaunchedEffect(listState.isScrollInProgress) {
                            if (!listState.isScrollInProgress) {
                                val firstVisible = listState.firstVisibleItemIndex
                                val offset = listState.firstVisibleItemScrollOffset
                                val itemHeightPx = with(density) { 40.dp.toPx() }
                                val index = if (offset > itemHeightPx / 2) firstVisible + 1 else firstVisible
                                val targetVal = minutesList.getOrNull(index)
                                if (targetVal != null) {
                                    selectedMinute = targetVal
                                    if (firstVisible != index || offset > 0) {
                                        listState.animateScrollToItem(index)
                                    }
                                }
                            }
                        }
                    }
                    
                    Text(":", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                    
                    // Second picker
                    Box(modifier = Modifier.weight(1f).height(120.dp)) {
                        val secondsList = (0..59).toList()
                        val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedSecond)
                        val coroutineScope = rememberCoroutineScope()
                        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(CyberCyan.copy(alpha = 0.1f)))
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                items(secondsList) { s ->
                                    val isSel = selectedSecond == s
                                    Text(
                                        text = s.toString().padStart(2, '0'), 
                                        color = if (isSel) CyberCyan else Color.White.copy(alpha = 0.4f), 
                                        fontSize = if (isSel) 20.sp else 15.sp, 
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, 
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .wrapContentHeight(Alignment.CenterVertically)
                                            .clickable { 
                                                selectedSecond = s 
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(s)
                                                }
                                            }
                                    )
                                }
                            }
                        }
                        LaunchedEffect(listState.isScrollInProgress) {
                            if (!listState.isScrollInProgress) {
                                val firstVisible = listState.firstVisibleItemIndex
                                val offset = listState.firstVisibleItemScrollOffset
                                val itemHeightPx = with(density) { 40.dp.toPx() }
                                val index = if (offset > itemHeightPx / 2) firstVisible + 1 else firstVisible
                                val targetVal = secondsList.getOrNull(index)
                                if (targetVal != null) {
                                    selectedSecond = targetVal
                                    if (firstVisible != index || offset > 0) {
                                        listState.animateScrollToItem(index)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) { 
                        Text("CANCELAR", color = Color.White.copy(alpha = 0.6f), maxLines = 1) 
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val timeStr = "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}:${selectedSecond.toString().padStart(2, '0')}"
                            onConfirm(timeStr)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MidnightBlue),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SELECIONAR", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}
