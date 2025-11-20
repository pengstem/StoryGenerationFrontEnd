package com.example.storygeneration.data.model

data class Scene(
    val id: String,
    val title: String,
    val description: String,
    val shots: List<Shot> = emptyList()
)