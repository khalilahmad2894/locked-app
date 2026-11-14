package com.lockedapp.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockedapp.vault.VaultViewModel
import com.lockedapp.vault.ui.theme.NotesAccent
import com.lockedapp.vault.ui.theme.VaultVoid
import kotlinx.coroutines.launch

@Composable
fun NoteEditorScreen(viewModel: VaultViewModel, onDone: () -> Unit) {
    val existing = viewModel.activeNoteItem
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(existing == null) }

    LaunchedEffect(existing?.id) {
        if (existing != null) {
            content = viewModel.readTextNote(existing)
        }
        loaded = true
    }

    fun save() {
        val text = content
        scope.launch {
            viewModel.saveTextNote(existing, text)
            onDone()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultVoid)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                if (existing == null) "New note" else "Edit note",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            IconButton(onClick = { save() }) {
                Icon(Icons.Filled.Check, contentDescription = "Save", tint = NotesAccent)
            }
        }

        if (loaded) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Start typing...", color = Color.White.copy(alpha = 0.35f)) },
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = NotesAccent
                )
            )
        }
    }
}
