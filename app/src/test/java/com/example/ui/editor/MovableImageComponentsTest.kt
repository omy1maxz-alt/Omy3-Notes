package com.example.ui.editor

import com.example.data.model.BlockType
import com.example.data.model.NoteBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovableImageComponentsTest {

    @Test
    fun testSingleTextBlockGrouping() {
        val blocks = listOf(
            NoteBlock(type = BlockType.TEXT, content = "Hello world")
        )
        val groups = groupBlocksForEditor(blocks)
        assertEquals(1, groups.size)
        assertTrue(groups[0] is EditorRenderGroup.Single)
        val single = groups[0] as EditorRenderGroup.Single
        assertEquals("Hello world", single.block.content)
    }

    @Test
    fun testImageFloatedLeftReflowsAdjacentText() {
        val blocks = listOf(
            NoteBlock(type = BlockType.IMAGE, imagePath = "sample.png", imageAlignment = "left", imageScale = 0.4f),
            NoteBlock(type = BlockType.TEXT, content = "This text appears to the right of the image without changing note content.")
        )
        val groups = groupBlocksForEditor(blocks)
        assertEquals(1, groups.size)
        assertTrue(groups[0] is EditorRenderGroup.TextWithFloatedImage)
        val floated = groups[0] as EditorRenderGroup.TextWithFloatedImage
        assertTrue(floated.imageOnLeft)
        assertEquals(0.4f, floated.imageBlock.imageScale)
        assertEquals("This text appears to the right of the image without changing note content.", floated.textBlock.content)
    }

    @Test
    fun testTextFollowedByFloatedRightImage() {
        val blocks = listOf(
            NoteBlock(type = BlockType.TEXT, content = "First paragraph of note"),
            NoteBlock(type = BlockType.IMAGE, imagePath = "chart.png", imageAlignment = "right", imageScale = 0.35f)
        )
        val groups = groupBlocksForEditor(blocks)
        assertEquals(1, groups.size)
        assertTrue(groups[0] is EditorRenderGroup.TextWithFloatedImage)
        val floated = groups[0] as EditorRenderGroup.TextWithFloatedImage
        assertEquals(false, floated.imageOnLeft)
        assertEquals(0.35f, floated.imageBlock.imageScale)
        assertEquals("First paragraph of note", floated.textBlock.content)
    }

    @Test
    fun testMultipleImagesClusterInFlowRow() {
        val blocks = listOf(
            NoteBlock(type = BlockType.IMAGE, imagePath = "img1.png", imageAlignment = "center", imageScale = 0.3f),
            NoteBlock(type = BlockType.IMAGE, imagePath = "img2.png", imageAlignment = "center", imageScale = 0.3f),
            NoteBlock(type = BlockType.IMAGE, imagePath = "img3.png", imageAlignment = "center", imageScale = 0.3f)
        )
        val groups = groupBlocksForEditor(blocks)
        assertEquals(1, groups.size)
        assertTrue(groups[0] is EditorRenderGroup.ImageCluster)
        val cluster = groups[0] as EditorRenderGroup.ImageCluster
        assertEquals(3, cluster.items.size)
    }

    @Test
    fun testTextSurroundingImageGrouping() {
        val blocks = listOf(
            NoteBlock(type = BlockType.TEXT, content = "Text text text text"),
            NoteBlock(type = BlockType.IMAGE, imagePath = "icon.png", imageAlignment = "center", imageScale = 0.35f),
            NoteBlock(type = BlockType.TEXT, content = "text text")
        )
        val groups = groupBlocksForEditor(blocks)
        assertEquals(1, groups.size)
        assertTrue(groups[0] is EditorRenderGroup.TextSurroundingImage)
        val surround = groups[0] as EditorRenderGroup.TextSurroundingImage
        assertEquals("Text text text text", surround.leftTextBlock.content)
        assertEquals("text text", surround.rightTextBlock.content)
        assertEquals(0.35f, surround.imageBlock.imageScale)
    }

    @Test
    fun testSplitLineAroundImage() {
        val line = "i love you"
        
        // 0. ......i love you
        val (p0Left, p0Right) = splitLineAroundImage(line, charOffset = 0, alignment = "left")
        assertEquals("", p0Left)
        assertEquals("i love you", p0Right)

        // 1. i......love you (split after 'i')
        val (p1Left, p1Right) = splitLineAroundImage(line, charOffset = 1)
        assertEquals("i", p1Left)
        assertEquals(" love you", p1Right)

        // 2. i l......ove you (split after 'l')
        val (p2Left, p2Right) = splitLineAroundImage(line, charOffset = 3)
        assertEquals("i l", p2Left)
        assertEquals("ove you", p2Right)

        // 3. i lo......ve you (split after 'o')
        val (p3Left, p3Right) = splitLineAroundImage(line, charOffset = 4)
        assertEquals("i lo", p3Left)
        assertEquals("ve you", p3Right)

        // Verify underlying text is strictly preserved
        assertEquals(line, p3Left + p3Right)
    }

    @Test
    fun testTakeLineSlice() {
        val longText = "is an offline-first, private notebook with rich blocks"
        val (slice, overflow) = takeLineSlice(longText, 22)
        assertEquals("is an offline-first,", slice)
        assertEquals("private notebook with rich blocks", overflow)
        assertEquals(longText, "$slice $overflow")

        val shortText = "love you"
        val (shortSlice, shortOverflow) = takeLineSlice(shortText, 22)
        assertEquals("love you", shortSlice)
        assertEquals("", shortOverflow)
    }

    @Test
    fun testCenteringImageSeparatesParagraphs() {
        val blocks = listOf(
            NoteBlock(type = BlockType.TEXT, content = "Paragraph 1"),
            NoteBlock(type = BlockType.IMAGE, imagePath = "hero.png", imageAlignment = "center", imageScale = 0.8f),
            NoteBlock(type = BlockType.TEXT, content = "Paragraph 2")
        )
        val groups = groupBlocksForEditor(blocks)
        assertEquals("Paragraph 2", (groups[2] as EditorRenderGroup.Single).block.content)
    }

    @Test
    fun testReflowTextAroundObstaclePushesDisplacedCharsDown() {
        val paragraph = "The quick brown fox jumps over the lazy dog and runs through the forest with great speed."
        val result = reflowTextAroundObstacle(
            fullText = paragraph,
            lineOffset = 0,
            charOffset = 4, // Left slot fits "The"
            linesSpanned = 2,
            fullWidthCapacity = 30,
            imgCapacity = 14
        )

        // Line 0 left should fit "The"
        assertEquals("The", result.leftLines[0])
        // Line 0 right should fit text after image
        assertTrue(result.rightLines[0].isNotEmpty())
        // Total text pushed below should contain the remaining words
        assertTrue(result.belowText.isNotEmpty())
        assertTrue(result.belowText.contains("great speed"))
    }
}
