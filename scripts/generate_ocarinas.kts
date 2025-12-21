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
        "white","orange","magenta","light_blue",
        "yellow","lime","pink","gray","light_gray",
        "cyan","purple","blue","brown","green","red","black"
    )

    // Generate JSON files
    colors.forEach { color ->
        val file = File(base, "${color}_ocarina.json")
        try {
            file.writeText("""
                {
                  "parent": "item/generated",
                  "textures": { "layer0": "friendsforlife:item/${color}_ocarina" }
                }
            """.trimIndent())
            println("Created ${file.absolutePath}")
        } catch (e: Exception) {
            println("Failed to create ${file.name}")
            e.printStackTrace()
        }
    }

    // Write recipes
//    val recipeBase = File(projectRoot, "../src/main/resources/data/friendsforlife/recipes")
    val recipeBase = File(projectRoot, "../src/main/resources/data/friendsforlife/recipe")
    for (color in colors) {
        val file = File(recipeBase, "${color}_ocarina.json")
        // 1.21
        file.writeText("""
            {
              "type": "minecraft:crafting_shaped",
              "category": "misc",
              "pattern": [
                "   ",
                "TTT",
                "   "
              ],
              "key": {
                "T": { "item": "minecraft:${color}_terracotta" }
              },
              "result": { "count": 1, "id": "friendsforlife:${color}_ocarina" }
            }
        """.trimIndent())
//        1.20
//        file.writeText("""
//            {
//              "type": "minecraft:crafting_shaped",
//              "pattern": [
//                "   ",
//                "TTT",
//                "   "
//              ],
//              "key": {
//                "T": { "item": "minecraft:${color}_terracotta" }
//              },
//              "result": { "item": "friendsforlife:${color}_ocarina" }
//            }
//        """.trimIndent())
        println("Created ${color}_ocarina recipe")
    }

    println("Done generating all ocarina models")
}

// Call main
main()
