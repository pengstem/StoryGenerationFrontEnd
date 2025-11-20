package com.example.storygeneration.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.storygeneration.data.model.Transition
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ShotDetailScreen(navController: NavController) {
    val (prompt, setPrompt) = remember { mutableStateOf("A misty forest at dawn with tent") }
    val (transition, setTransition) = remember { mutableStateOf(Transition.KEN_BURNS) }
    val (narration, setNarration) = remember { mutableStateOf("") }
    val (status, setStatus) = remember { mutableStateOf("Generated") }
    val (dropdownExpanded, setDropdownExpanded) = remember { mutableStateOf(false) }

    val statusColor = when (status) {
        "Generated" -> Color(0xFF4CAF50)
        "Generating" -> Color(0xFFFFC107)
        else -> Color(0xFF9E9E9E)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button and title
        Surface(
            color = TopAppBarDefaults.topAppBarColors().containerColor,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Shot Detail",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        // Image placeholder
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF4CAF50))
        ) {
            // Status label
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .background(statusColor, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = status,
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }

        // Prompt field
        OutlinedTextField(
            value = prompt,
            onValueChange = setPrompt,
            label = { Text("Shot description") },
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
        )

        // Video Transition dropdown
        Box(modifier = Modifier.padding(top = 16.dp)) {
            Text("Video Transition")
            OutlinedTextField(
                value = transition.name.replace('_', ' '),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                trailingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown arrow",
                        modifier = Modifier.clickable { setDropdownExpanded(!dropdownExpanded) })
                })

            // Dropdown menu
            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { setDropdownExpanded(false) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Transition.entries.forEach { transitionOption ->
                    DropdownMenuItem(
                        text = { Text(transitionOption.name.replace('_', ' ')) },
                        onClick = {
                            setTransition(transitionOption)
                            setDropdownExpanded(false)
                        })
                }
            }
        }

        // Narration field
        OutlinedTextField(
            value = narration,
            onValueChange = setNarration,
            label = { Text("Narration text") },
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
        )

        // Generate button
        Button(
            onClick = {
                setStatus("Generating")
                // Simulate image generation delay
                kotlinx.coroutines.MainScope().launch {
                    kotlinx.coroutines.delay(2000)
                    setStatus("Generated")
                }
            }, modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
        ) {
            Text("Generate Image")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShotDetailScreenPreview() {
    val navController = rememberNavController()
    ShotDetailScreen(navController = navController)
}