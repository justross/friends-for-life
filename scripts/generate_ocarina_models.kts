#!/usr/bin/env kotlin

import java.io.File

fun main() {
    // Detect project root
    val projectRoot = File(System.getProperty("user.dir"))
    println("Project root: $projectRoot")

    // Target directory for generated JSON files
    val base = File(projectRoot, "../src/main/resources/assets/friendsforlife/models/item")
    println("Base path: ${base.absolutePath}")

    // Create directories if they don't exist
    if (!base.exists() && !base.mkdirs()) {
        println("Could not create base directory: ${base.absolutePath}")
        return
    }

    // List of colors
    val colors = listOf(
        "white", "orange", "magenta", "light_blue",
        "yellow", "lime", "pink", "gray", "light_gray",
        "cyan", "purple", "blue", "brown", "green", "red", "black"
    )

    // Write recipes
    val modelBase = File(projectRoot, "../versions/1.21.10-fabric/src/main/resources/assets/friendsforlife/items")
    for (color in colors) {
        val file = File(modelBase, "${color}_ocarina.json")
        // 1.21
        file.writeText(
            """
{
  "model": {
    "type": "minecraft:model",
    "model": "friendsforlife:item/${color}_ocarina"
  }
}
        """.trimIndent()
        )
        println("Created ${color}_ocarina model")
    }

    println("Done generating all ocarina item models")
}

// Call main
main()
