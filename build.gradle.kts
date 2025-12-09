// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Déclaration explicite des plugins avec versions pour éviter l'échec de résolution
    id("com.android.application") version "8.13.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}