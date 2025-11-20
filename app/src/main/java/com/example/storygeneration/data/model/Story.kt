package com.example.storygeneration.data.model

data class Story(
    val id: String,
    val title: String,
    val content: String,
    val style: Style,
    val scenes: List<Scene> = emptyList()
)

enum class Style {
    Movie, Animation, Realistic
}