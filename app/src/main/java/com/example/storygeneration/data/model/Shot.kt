package com.example.storygeneration.data.model

data class Shot(
    val id: String,
    val prompt: String,
    val narration: String,
    val imageUrl: String,
    val transition: Transition = Transition.CROSSFADE
)

enum class Transition {
    KEN_BURNS, CROSSFADE, VOLUME_MIX
}