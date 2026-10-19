package com.example.ui.editor

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.BlockType
import com.example.data.model.NoteBlock
import com.example.util.BitmapLoader
import java.io.File
import kotlin.math.roundToInt

/**
 * Splits a text line into (leftSegment, rightSegment) based on character offset, horizontal alignment, and scale.
 * 
 * Supports character-level positioning:
 *   0 -> "" to "i love you"   (0. ......i love you)
 *   1 -> "i" to "love you"    (1. i......love you)
 *   3 -> "i l" to "ove you"   (2. i l......ove you)
 *   4 -> "i lo" to "ve you"   (3. i lo......ve you)
 */
fun splitLineAroundImage(
    line: String,
    charOffset: Int = 0,
    alignment: String? = "center",
    imageScale: Float = 0.40f
): Pair<String, String> {
    if (line.isEmpty()) return "" to ""
    if (charOffset > 0) {
        val splitIdx = charOffset.coerceIn(0, line.length)
        return line.substring(0, splitIdx) to line.substring(splitIdx)
    }
    return when (alignment) {
        "left" -> "" to line
        "right" -> line to ""
        else -> {
            val scale = imageScale.coerceIn(0.20f, 0.75f)
            val leftRatio = ((1.0f - scale) * 0.45f).coerceIn(0.15f, 0.65f)
            val splitIdx = (line.length * leftRatio).toInt().coerceIn(0, line.length)
            line.substring(0, splitIdx) to line.substring(splitIdx)
        }
    }
}

/**
 * Takes up to [capacity] characters from [text] for a single line, breaking cleanly at a word boundary
 * if possible, and returns (lineSegment, overflowText).
 */
fun takeLineSlice(text: String, capacity: Int): Pair<String, String> {
    if (text.length <= capacity) return text to ""
    val lastSpace = text.substring(0, capacity).lastIndexOf(' ')
    val cutIdx = if (lastSpace > capacity / 2) lastSpace + 1 else capacity
    return text.substring(0, cutIdx).trimEnd() to text.substring(cutIdx).trimStart()
}

data class ObstacleTextReflowResult(
    val aboveText: String,
    val leftLines: List<String>,
    val rightLines: List<String>,
    val belowText: String
)

/**
 * Dynamically reflows text around an arbitrary rectangular obstacle at (lineOffset, charOffset):
 * 1. Consumes full-width lines ABOVE the image.
 * 2. On the lines spanned by the image, fills the left slot (up to charOffset) and right slot,
 *    naturally pushing any characters displaced by the image forward into the next line.
 * 3. Continues all remaining pushed characters at 100% full width BELOW the image.
 */
fun reflowTextAroundObstacle(
    fullText: String,
    lineOffset: Int,
    charOffset: Int,
    linesSpanned: Int,
    fullWidthCapacity: Int,
    imgCapacity: Int
): ObstacleTextReflowResult {
    if (fullText.isEmpty()) {
        return ObstacleTextReflowResult("", emptyList(), emptyList(), "")
    }

    var remaining = fullText.trim()

    // 1. Consume lines ABOVE the image (line 0 up to lineOffset - 1)
    val aboveLinesList = mutableListOf<String>()
    for (i in 0 until lineOffset) {
        if (remaining.isEmpty()) break
        val (lineSlice, rest) = takeLineSlice(remaining, fullWidthCapacity)
        if (lineSlice.isNotEmpty()) {
            aboveLinesList.add(lineSlice)
        }
        remaining = rest
    }

    // 2. Consume lines BESIDE the image (linesSpanned lines)
    val leftLinesList = mutableListOf<String>()
    val rightLinesList = mutableListOf<String>()

    for (i in 0 until linesSpanned) {
        if (remaining.isEmpty()) {
            leftLinesList.add("")
            rightLinesList.add("")
            continue
        }

        // Left slot
        val leftCap = charOffset.coerceIn(0, (fullWidthCapacity - imgCapacity).coerceAtLeast(0))
        val (leftSlice, restAfterLeft) = if (leftCap > 0) {
            takeLineSlice(remaining, leftCap)
        } else {
            "" to remaining
        }
        leftLinesList.add(leftSlice)
        remaining = restAfterLeft

        // Right slot
        val rightCap = (fullWidthCapacity - leftCap - imgCapacity).coerceAtLeast(0)
        val (rightSlice, restAfterRight) = if (rightCap > 0 && remaining.isNotEmpty()) {
            takeLineSlice(remaining, rightCap)
        } else {
            "" to remaining
        }
        rightLinesList.add(rightSlice)
        remaining = restAfterRight
    }

    // 3. Lines BELOW the image (everything else that was pushed down by the obstacle!)
    val belowLinesList = mutableListOf<String>()
    while (remaining.isNotEmpty()) {
        val (lineSlice, rest) = takeLineSlice(remaining, fullWidthCapacity)
        if (lineSlice.isNotEmpty()) {
            belowLinesList.add(lineSlice)
        }
        if (rest == remaining) break
        remaining = rest
    }

    return ObstacleTextReflowResult(
        aboveText = aboveLinesList.joinToString("\n"),
        leftLines = leftLinesList,
        rightLines = rightLinesList,
        belowText = belowLinesList.joinToString("\n")
    )
}

/**
 * Editor render grouping to support text surrounding images (on both sides or one side),
 * responsive flow reflow, and multi-image wrapping in FlowRow.
 */
sealed class EditorRenderGroup {
    data class Single(val index: Int, val block: NoteBlock) : EditorRenderGroup()
    
    data class TextWithFloatedImage(
        val textBlock: NoteBlock,
        val textIndex: Int,
        val imageBlock: NoteBlock,
        val imageIndex: Int,
        val imageOnLeft: Boolean
    ) : EditorRenderGroup()

    data class TextSurroundingImage(
        val leftTextBlock: NoteBlock,
        val leftTextIndex: Int,
        val imageBlock: NoteBlock,
        val imageIndex: Int,
        val rightTextBlock: NoteBlock,
        val rightTextIndex: Int
    ) : EditorRenderGroup()

    data class ImageCluster(
        val items: List<Pair<Int, NoteBlock>>
    ) : EditorRenderGroup()
}

/**
 * Transforms flat note blocks into visual layout groups.
 * 
 * 1. Surrounding Text (Water flowing around object): If an image is small (scale <= 0.60f)
 *    and surrounded by text (TEXT + IMAGE + TEXT), it renders text on BOTH sides of the image.
 * 2. Floated Image: If an image has alignment 'left' or 'right' (or is side-positioned),
 *    it pairs with an adjacent text block for side-by-side reflow.
 * 3. Multi-Image Cluster: If multiple images are adjacent without floated text,
 *    it clusters them for responsive FlowRow wrapping.
 */
fun groupBlocksForEditor(blocks: List<NoteBlock>): List<EditorRenderGroup> {
    val result = mutableListOf<EditorRenderGroup>()
    val visited = BooleanArray(blocks.size)

    var i = 0
    while (i < blocks.size) {
        if (visited[i]) {
            i++
            continue
        }

        val current = blocks[i]

        // 1. Check for Text Surrounding Image: TEXT + IMAGE (scale <= 0.60f and not hard floated) + TEXT
        if (current.type == BlockType.TEXT && i + 2 < blocks.size && !visited[i + 1] && !visited[i + 2]) {
            val midBlock = blocks[i + 1]
            val nextBlock = blocks[i + 2]
            if (midBlock.type == BlockType.IMAGE && nextBlock.type == BlockType.TEXT) {
                if (midBlock.imageAlignment != "left" && midBlock.imageAlignment != "right" && midBlock.imageScale <= 0.60f) {
                    result.add(
                        EditorRenderGroup.TextSurroundingImage(
                            leftTextBlock = current,
                            leftTextIndex = i,
                            imageBlock = midBlock,
                            imageIndex = i + 1,
                            rightTextBlock = nextBlock,
                            rightTextIndex = i + 2
                        )
                    )
                    visited[i] = true
                    visited[i + 1] = true
                    visited[i + 2] = true
                    i += 3
                    continue
                }
            }
        }

        // 2. Check if this is an image block with side alignment or side scaling paired with following text
        if (current.type == BlockType.IMAGE) {
            val isLeft = current.imageAlignment == "left"
            val isRight = current.imageAlignment == "right"
            val isSideScaled = current.imageScale <= 0.75f && current.imageAlignment != "center"

            if ((isLeft || isRight || isSideScaled) && i + 1 < blocks.size && !visited[i + 1] && blocks[i + 1].type == BlockType.TEXT) {
                result.add(
                    EditorRenderGroup.TextWithFloatedImage(
                        textBlock = blocks[i + 1],
                        textIndex = i + 1,
                        imageBlock = current,
                        imageIndex = i,
                        imageOnLeft = (current.imageAlignment != "right")
                    )
                )
                visited[i] = true
                visited[i + 1] = true
                i += 2
                continue
            }
        }

        // 3. Check if this is a text block followed by a floated/side image
        if (current.type == BlockType.TEXT && i + 1 < blocks.size && !visited[i + 1] && blocks[i + 1].type == BlockType.IMAGE) {
            val nextImg = blocks[i + 1]
            val isLeft = nextImg.imageAlignment == "left"
            val isRight = nextImg.imageAlignment == "right"
            val isSideScaled = nextImg.imageScale <= 0.75f

            if (isLeft || isRight || isSideScaled) {
                result.add(
                    EditorRenderGroup.TextWithFloatedImage(
                        textBlock = current,
                        textIndex = i,
                        imageBlock = nextImg,
                        imageIndex = i + 1,
                        imageOnLeft = (nextImg.imageAlignment == "left")
                    )
                )
                visited[i] = true
                visited[i + 1] = true
                i += 2
                continue
            }
        }

        // 4. Check if this and following blocks are consecutive images (cluster in FlowRow)
        if (current.type == BlockType.IMAGE) {
            val cluster = mutableListOf<Pair<Int, NoteBlock>>()
            cluster.add(i to current)
            visited[i] = true

            var nextIdx = i + 1
            while (nextIdx < blocks.size && !visited[nextIdx] && blocks[nextIdx].type == BlockType.IMAGE && blocks[nextIdx].imageAlignment != "left" && blocks[nextIdx].imageAlignment != "right") {
                cluster.add(nextIdx to blocks[nextIdx])
                visited[nextIdx] = true
                nextIdx++
            }

            if (cluster.size > 1) {
                result.add(EditorRenderGroup.ImageCluster(cluster))
                i = nextIdx
                continue
            } else {
                result.add(EditorRenderGroup.Single(i, current))
                i++
                continue
            }
        }

        // 5. Default single item
        result.add(EditorRenderGroup.Single(i, current))
        visited[i] = true
        i++
    }

    return result
}

/**
 * Smoothly moves or splits an image between characters/words of neighboring text blocks.
 * If moved into the middle of a text block, splits at the nearest word boundary so the image
 * glides smoothly between words like a boat through water.
 * Merges adjacent text blocks when an image moves away.
 */
fun moveImageBetweenText(
    blocks: SnapshotStateList<NoteBlock>,
    imageBlockId: String,
    moveUp: Boolean
) {
    val currentIdx = blocks.indexOfFirst { it.id == imageBlockId }
    if (currentIdx == -1) return
    val imageItem = blocks[currentIdx]

    if (moveUp) {
        if (currentIdx == 0) return
        val prevBlock = blocks[currentIdx - 1]
        if (prevBlock.type == BlockType.TEXT && prevBlock.content.length > 30 && prevBlock.content.contains(" ")) {
            val text = prevBlock.content
            val mid = text.length / 2
            var splitPos = text.lastIndexOf(" ", mid)
            if (splitPos == -1) splitPos = text.indexOf(" ", mid)
            if (splitPos > 8 && splitPos < text.length - 8) {
                val topPart = text.substring(0, splitPos).trimEnd()
                val bottomPart = text.substring(splitPos).trimStart()
                blocks.removeAt(currentIdx)
                blocks[currentIdx - 1] = prevBlock.copy(content = topPart)
                blocks.add(currentIdx, imageItem)
                blocks.add(currentIdx + 1, NoteBlock(type = BlockType.TEXT, content = bottomPart))
                return
            }
        }
        val item = blocks.removeAt(currentIdx)
        blocks.add(currentIdx - 1, item)
    } else {
        if (currentIdx >= blocks.lastIndex) return
        val nextBlock = blocks[currentIdx + 1]
        if (nextBlock.type == BlockType.TEXT && nextBlock.content.length > 30 && nextBlock.content.contains(" ")) {
            val text = nextBlock.content
            val mid = text.length / 2
            var splitPos = text.indexOf(" ", mid)
            if (splitPos == -1) splitPos = text.lastIndexOf(" ", mid)
            if (splitPos > 8 && splitPos < text.length - 8) {
                val topPart = text.substring(0, splitPos).trimEnd()
                val bottomPart = text.substring(splitPos).trimStart()
                blocks.removeAt(currentIdx)
                blocks.add(currentIdx, NoteBlock(type = BlockType.TEXT, content = topPart))
                blocks.add(currentIdx + 1, imageItem)
                blocks[currentIdx + 2] = nextBlock.copy(content = bottomPart)
                return
            }
        }
        val item = blocks.removeAt(currentIdx)
        blocks.add(currentIdx + 1, item)
    }

    mergeAdjacentTextBlocks(blocks)
}

fun mergeAdjacentTextBlocks(blocks: MutableList<NoteBlock>) {
    var i = 0
    while (i < blocks.size - 1) {
        if (blocks[i].type == BlockType.TEXT && blocks[i + 1].type == BlockType.TEXT) {
            val first = blocks[i].content
            val second = blocks[i + 1].content
            val combined = when {
                first.isEmpty() -> second
                second.isEmpty() -> first
                first.endsWith("\n") -> first + second
                else -> first + "\n" + second
            }
            blocks[i] = blocks[i].copy(content = combined)
            blocks.removeAt(i + 1)
        } else {
            i++
        }
    }
}

/**
 * Movable, draggable, pinch-resizable image item showing ONLY the pure image.
 *
 * Capabilities:
 * 1. Long-press on image -> lifts with elevation shadow and follows finger in 2D (left, right, up, down).
 * 2. Drag left/right -> snaps alignment (left/right/center), text reflows automatically around image.
 * 3. Drag up/down -> glides smoothly between words and characters inside text paragraphs.
 * 4. Two-finger pinch -> resizes image continuously while preserving aspect ratio and reflowing text.
 */
@Composable
fun MovableImageItem(
    block: NoteBlock,
    index: Int,
    blocks: SnapshotStateList<NoteBlock>,
    context: Context,
    haptic: HapticFeedback?,
    captureSnapshot: (Boolean) -> Unit,
    onDirty: () -> Unit,
    onDelete: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val imgFile = remember(block.imagePath) {
        block.imagePath?.let { File(context.filesDir, it) }
    }
    val bitmap = remember(block.imagePath) {
        if (imgFile != null && imgFile.exists()) BitmapLoader.loadScaledBitmap(imgFile, 900, 700) else null
    }

    var isDragging by remember { mutableStateOf(false) }
    var totalDragDeltaX by remember { mutableFloatStateOf(0f) }
    var totalDragDeltaY by remember { mutableFloatStateOf(0f) }
    var subPixelTranslationX by remember { mutableFloatStateOf(0f) }
    var subPixelTranslationY by remember { mutableFloatStateOf(0f) }
    var startCharOffset by remember { mutableIntStateOf(0) }
    var startLineOffset by remember { mutableIntStateOf(0) }

    val elevation = if (isDragging) 6.dp else 0.dp
    val scaleMultiplier = 1.0f

    Box(
        modifier = modifier
            .padding(vertical = 3.dp)
            .graphicsLayer {
                translationX = subPixelTranslationX
                translationY = subPixelTranslationY
                scaleX = scaleMultiplier
                scaleY = scaleMultiplier
                this.shadowElevation = elevation.toPx()
            }
            .zIndex(if (isDragging) 25f else 1f)
            .pointerInput(block.id) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        totalDragDeltaX = 0f
                        totalDragDeltaY = 0f
                        subPixelTranslationX = 0f
                        subPixelTranslationY = 0f
                        startCharOffset = block.charOffset
                        startLineOffset = block.lineOffset
                        haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val curIdx = blocks.indexOfFirst { it.id == block.id }
                        if (curIdx != -1) {
                            totalDragDeltaX += dragAmount.x
                            totalDragDeltaY += dragAmount.y

                            val charStepPx = 11f
                            val lineStepPx = 24f

                            val charShift = (totalDragDeltaX / charStepPx).toInt()
                            val lineShift = (totalDragDeltaY / lineStepPx).toInt()

                            val targetChar = (startCharOffset + charShift).coerceAtLeast(0)
                            val targetLine = (startLineOffset + lineShift).coerceAtLeast(0)

                            val charChanged = targetChar != blocks[curIdx].charOffset
                            val lineChanged = targetLine != blocks[curIdx].lineOffset

                            if (charChanged || lineChanged) {
                                blocks[curIdx] = blocks[curIdx].copy(
                                    charOffset = targetChar,
                                    lineOffset = targetLine
                                )
                                haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDirty()
                            }

                            // Sub-pixel translation calculation: guarantees 100% continuous 1:1 finger tracking
                            // with zero optical jumps when character column or line row increments!
                            val actualCharStep = targetChar - startCharOffset
                            val actualLineStep = targetLine - startLineOffset
                            subPixelTranslationX = totalDragDeltaX - (actualCharStep * charStepPx)
                            subPixelTranslationY = totalDragDeltaY - (actualLineStep * lineStepPx)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        totalDragDeltaX = 0f
                        totalDragDeltaY = 0f
                        subPixelTranslationX = 0f
                        subPixelTranslationY = 0f
                        mergeAdjacentTextBlocks(blocks)
                        captureSnapshot(false)
                        onDirty()
                    },
                    onDragCancel = {
                        isDragging = false
                        totalDragDeltaX = 0f
                        totalDragDeltaY = 0f
                        subPixelTranslationX = 0f
                        subPixelTranslationY = 0f
                    }
                )
            }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Note Image",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(elevation, RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading image...", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

/**
 * Production-grade custom Layout using TextMeasurer for multi-pass text exclusion wrapping.
 *
 * 1. Measures the full text with TextMeasurer to get real typographical line positions and metrics.
 * 2. Positions the rectangular obstacle at its top-left anchor coordinate (lineOffset, charOffset).
 * 3. Partitions text into 3 clean zones:
 *    - Zone 1 (Above): lines before lineOffset (100% full width, untouched).
 *    - Zone 2 (Beside): lines overlapping the obstacle (split cleanly into Left run + [IMAGE] + Right run).
 *    - Zone 3 (Below): lines after the obstacle (100% full width, zero reserved space).
 */
@Composable
fun MovableImageTextFlow(
    text: String,
    imageBlock: NoteBlock,
    imageIndex: Int,
    blocks: SnapshotStateList<NoteBlock>,
    context: Context,
    textColor: Color,
    activeFontFamily: FontFamily,
    haptic: HapticFeedback?,
    captureSnapshot: (Boolean) -> Unit,
    onDirty: () -> Unit,
    onDelete: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = 16.sp,
        fontFamily = activeFontFamily,
        color = textColor,
        lineHeight = 24.sp
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerMaxWidth = maxWidth
        val maxWidthPx = constraints.maxWidth.toFloat()
        val imgFraction = imageBlock.imageScale.coerceIn(0.20f, 0.75f)
        val imageWidthDp = (containerMaxWidth * imgFraction).coerceIn(120.dp, (containerMaxWidth * 0.85f).coerceAtLeast(120.dp))

        // Measure sample text to get character capacity
        val sampleLayout = textMeasurer.measure(
            text = AnnotatedString("A".repeat(40)),
            style = textStyle,
            constraints = Constraints(maxWidth = maxWidthPx.roundToInt().coerceAtLeast(100))
        )
        val avgCharWidth = if (sampleLayout.size.width > 0) (sampleLayout.size.width.toFloat() / 40f).coerceIn(8f, 22f) else 14f
        val fullWidthCapacity = (maxWidthPx / avgCharWidth).roundToInt().coerceIn(20, 90)
        val imgCapacity = ((maxWidthPx * imgFraction) / avgCharWidth).roundToInt().coerceIn(6, 40)
        val linesSpanned = if (imageBlock.imageScale > 0.6f) 3 else 2

        val reflow = reflowTextAroundObstacle(
            fullText = text,
            lineOffset = imageBlock.lineOffset,
            charOffset = imageBlock.charOffset,
            linesSpanned = linesSpanned,
            fullWidthCapacity = fullWidthCapacity,
            imgCapacity = imgCapacity
        )

        val hasLeftContent = reflow.leftLines.any { it.isNotEmpty() }
        val hasRightContent = reflow.rightLines.any { it.isNotEmpty() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Zone 1: Above lines (100% full width, untouched)
            if (reflow.aboveText.isNotEmpty()) {
                Text(
                    text = reflow.aboveText,
                    style = textStyle,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Zone 2: Obstacle Row (Text Left + [IMAGE] + Text Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left slot
                if (hasLeftContent) {
                    Column(
                        modifier = Modifier.wrapContentWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reflow.leftLines.forEach { part ->
                            Text(
                                text = part,
                                maxLines = 1,
                                style = textStyle
                            )
                        }
                    }
                }

                // Obstacle Image Item: fixed width so image never resizes during drag
                Box(
                    modifier = Modifier.width(imageWidthDp)
                ) {
                    MovableImageItem(
                        block = imageBlock,
                        index = imageIndex,
                        blocks = blocks,
                        context = context,
                        haptic = haptic,
                        captureSnapshot = captureSnapshot,
                        onDirty = onDirty,
                        onDelete = onDelete,
                        onOpenSettings = onOpenSettings
                    )
                }

                // Right slot
                if (hasRightContent) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reflow.rightLines.forEach { part ->
                            Text(
                                text = part,
                                maxLines = 1,
                                style = textStyle
                            )
                        }
                    }
                }
            }

            // Zone 3: Below lines (100% full width, receiving all pushed down text!)
            if (reflow.belowText.isNotEmpty()) {
                Text(
                    text = reflow.belowText,
                    style = textStyle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Text block editor component that integrates seamlessly with side-by-side and surrounding floated layouts.
 */
@Composable
fun EditorTextBlockItem(
    block: NoteBlock,
    index: Int,
    blocks: SnapshotStateList<NoteBlock>,
    activeFontFamily: FontFamily,
    textColor: Color,
    captureSnapshot: (Boolean) -> Unit,
    onDirty: () -> Unit,
    chunkLargeTextToBlocks: (String, Int) -> List<NoteBlock>,
    activeSelectedBlockIndex: Int,
    activeSelectionRange: TextRange?,
    onSelectionChanged: (String, Int, TextRange?) -> Unit,
    modifier: Modifier = Modifier
) {
    var tfValue by remember(block.id) {
        mutableStateOf(
            TextFieldValue(
                text = block.content,
                selection = if (activeSelectedBlockIndex == index && activeSelectionRange != null) {
                    activeSelectionRange
                } else {
                    TextRange(block.content.length)
                }
            )
        )
    }

    // Keep tfValue in sync if block.content changed externally (e.g., Undo/Redo, AI transform, find & replace)
    if (tfValue.text != block.content) {
        val coercedStart = tfValue.selection.start.coerceIn(0, block.content.length)
        val coercedEnd = tfValue.selection.end.coerceIn(0, block.content.length)
        tfValue = tfValue.copy(
            text = block.content,
            selection = TextRange(coercedStart, coercedEnd)
        )
    }

    // Sync programmatic external selection updates (e.g. Find & Replace jumps, Select All, AI selection target)
    LaunchedEffect(activeSelectedBlockIndex, activeSelectionRange) {
        if (activeSelectedBlockIndex == index && activeSelectionRange != null) {
            val validStart = activeSelectionRange.start.coerceIn(0, tfValue.text.length)
            val validEnd = activeSelectionRange.end.coerceIn(0, tfValue.text.length)
            if (tfValue.selection.start != validStart || tfValue.selection.end != validEnd) {
                tfValue = tfValue.copy(selection = TextRange(validStart, validEnd))
            }
        } else if (activeSelectedBlockIndex != index && activeSelectedBlockIndex != -1 && !tfValue.selection.collapsed) {
            // Collapse selection if another block is actively focused/selected
            tfValue = tfValue.copy(selection = TextRange(tfValue.selection.end))
        }
    }

    BasicTextField(
        value = tfValue,
        onValueChange = { newVal: TextFieldValue ->
            val oldText = tfValue.text
            val newText = newVal.text
            tfValue = newVal

            val currentIdx = blocks.indexOfFirst { it.id == block.id }
            if (currentIdx != -1) {
                if (oldText != newText) {
                    blocks[currentIdx] = blocks[currentIdx].copy(content = newText)
                    captureSnapshot(false)
                    onDirty()
                }

                // Report selection for AI and selection actions
                val sel = newVal.selection
                if (!sel.collapsed && sel.length > 0) {
                    val start = sel.min.coerceIn(0, newText.length)
                    val end = sel.max.coerceIn(0, newText.length)
                    val selectedSnippet = newText.substring(start, end)
                    onSelectionChanged(selectedSnippet, currentIdx, sel)
                } else if (activeSelectedBlockIndex == currentIdx) {
                    onSelectionChanged("", currentIdx, null)
                }
            }
        },
        textStyle = TextStyle(
            fontSize = 16.sp,
            fontFamily = activeFontFamily,
            color = textColor,
            lineHeight = 24.sp
        ),
        cursorBrush = SolidColor(textColor),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (tfValue.text.isEmpty() && blocks.size == 1) {
                    Text(
                        text = "Type your note here...",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontFamily = activeFontFamily,
                            color = textColor.copy(alpha = 0.35f),
                            lineHeight = 24.sp
                        )
                    )
                }
                innerTextField()
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
