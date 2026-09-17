package com.tbtechs.folioreader.ui.reader

import android.graphics.Bitmap
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tbtechs.folioreader.domain.model.PdfWord

@Composable
fun PdfPageView(
    pageIndex: Int,
    bitmap: Bitmap?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    pdfDimensions: Pair<Int, Int>? = null,
    highlightedWords: List<PdfWord> = emptyList(),
    allPageWords: List<PdfWord> = emptyList(),
    isVocabAssistanceEnabled: Boolean = true,
    selectedWord: PdfWord? = null,
    onWordSelected: (PdfWord) -> Unit = {},
    onTextLongPress: (selectedText: String, selectionOffset: Offset, pageIndex: Int) -> Unit = { _, _, _ -> },
    selectedTextSelection: TextSelectionAction? = null,
    onExplainClick: (String) -> Unit = {},
    onCopyClick: (String) -> Unit = {},
    onDefineClick: (String) -> Unit = {},
    classroomCursorIndex: Int? = null
) {
    val classroomWord = if (classroomCursorIndex != null) allPageWords.getOrNull(classroomCursorIndex) else null
    val isClassroomActive = classroomCursorIndex != null && classroomWord != null

    val infiniteTransition = rememberInfiniteTransition(label = "classroom_pulse_$pageIndex")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "classroom_pulse_alpha_$pageIndex"
    )

    var scale by remember(pageIndex) { mutableFloatStateOf(1f) }
    var offset by remember(pageIndex) { mutableStateOf(Offset.Zero) }
    var displayedSize by remember(pageIndex) { mutableStateOf(IntSize.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = newScale
        if (newScale > 1f) {
            offset += panChange * newScale
        } else {
            offset = Offset.Zero
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(2.dp))
            .clip(RoundedCornerShape(2.dp))
            .testTag("pdf_page_$pageIndex"),
        color = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .onSizeChanged { displayedSize = it }
                .pointerInput(pageIndex, highlightedWords, allPageWords, isVocabAssistanceEnabled, pdfDimensions, isClassroomActive) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val curW = displayedSize.width.toFloat()
                            val curH = displayedSize.height.toFloat()
                            val pdfW = pdfDimensions?.first?.toFloat() ?: (bitmap?.width?.toFloat() ?: curW)
                            val pdfH = pdfDimensions?.second?.toFloat() ?: (bitmap?.height?.toFloat() ?: curH)
                            val scaleX = if (pdfW > 0f) curW / pdfW else 1f
                            val scaleY = if (pdfH > 0f) curH / pdfH else 1f

                            var tappedWord: PdfWord? = null
                            if (!isClassroomActive) {
                                val touchMargin = 16f
                                // 1. First priority: highlighted difficult words
                                if (isVocabAssistanceEnabled && highlightedWords.isNotEmpty()) {
                                    tappedWord = highlightedWords.firstOrNull { word ->
                                        val left = word.x * scaleX - touchMargin
                                        val top = word.y * scaleY - touchMargin
                                        val right = (word.x + word.width) * scaleX + touchMargin
                                        val bottom = (word.y + word.height) * scaleY + touchMargin
                                        tapOffset.x in left..right && tapOffset.y in top..bottom
                                    }
                                }
                                // 2. Second priority: any word on the current page
                                if (tappedWord == null && allPageWords.isNotEmpty()) {
                                    tappedWord = allPageWords.firstOrNull { word ->
                                        val left = word.x * scaleX - touchMargin
                                        val top = word.y * scaleY - touchMargin
                                        val right = (word.x + word.width) * scaleX + touchMargin
                                        val bottom = (word.y + word.height) * scaleY + touchMargin
                                        tapOffset.x in left..right && tapOffset.y in top..bottom
                                    }
                                    // 3. Fallback: nearest word within a 36px touch radius
                                    if (tappedWord == null) {
                                        val maxDistSq = 36f * 36f
                                        val nearest = allPageWords.minByOrNull { word ->
                                            val cx = (word.x + word.width / 2f) * scaleX
                                            val cy = (word.y + word.height / 2f) * scaleY
                                            val dx = tapOffset.x - cx
                                            val dy = tapOffset.y - cy
                                            dx * dx + dy * dy
                                        }
                                        if (nearest != null) {
                                            val cx = (nearest.x + nearest.width / 2f) * scaleX
                                            val cy = (nearest.y + nearest.height / 2f) * scaleY
                                            val dx = tapOffset.x - cx
                                            val dy = tapOffset.y - cy
                                            if (dx * dx + dy * dy <= maxDistSq) {
                                                tappedWord = nearest
                                            }
                                        }
                                    }
                                }
                            }

                            if (tappedWord != null) {
                                onWordSelected(tappedWord)
                            } else {
                                onTap()
                            }
                        },
                        onLongPress = { longPressOffset ->
                            if (allPageWords.isNotEmpty()) {
                                val curW = displayedSize.width.toFloat()
                                val curH = displayedSize.height.toFloat()
                                val pdfW = pdfDimensions?.first?.toFloat() ?: (bitmap?.width?.toFloat() ?: curW)
                                val pdfH = pdfDimensions?.second?.toFloat() ?: (bitmap?.height?.toFloat() ?: curH)
                                val scaleX = if (pdfW > 0f) curW / pdfW else 1f
                                val scaleY = if (pdfH > 0f) curH / pdfH else 1f

                                val nearestWordIdx = allPageWords.indices.minByOrNull { idx ->
                                    val word = allPageWords[idx]
                                    val centerX = (word.x + word.width / 2f) * scaleX
                                    val centerY = (word.y + word.height / 2f) * scaleY
                                    val dx = longPressOffset.x - centerX
                                    val dy = longPressOffset.y - centerY
                                    dx * dx + dy * dy
                                }
                                if (nearestWordIdx != null) {
                                    val nearestWord = allPageWords[nearestWordIdx]
                                    var start = nearestWordIdx
                                    while (start > 0) {
                                        val prevText = allPageWords[start - 1].text.trim()
                                        if (prevText.endsWith(".") || prevText.endsWith("?") || prevText.endsWith("!")) {
                                            break
                                        }
                                        start--
                                        if (nearestWordIdx - start >= 25) break
                                    }
                                    var end = nearestWordIdx
                                    while (end < allPageWords.size - 1) {
                                        val curText = allPageWords[end].text.trim()
                                        if (curText.endsWith(".") || curText.endsWith("?") || curText.endsWith("!")) {
                                            break
                                        }
                                        end++
                                        if (end - nearestWordIdx >= 25) break
                                    }
                                    val sentence = allPageWords.subList(start, end + 1).joinToString(" ") { it.text }
                                    val wordOffset = Offset(nearestWord.x * scaleX, nearestWord.y * scaleY)
                                    onTextLongPress(sentence, wordOffset, pageIndex)
                                }
                            }
                        },
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.2f
                                offset = Offset.Zero
                            }
                        }
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(state = transformState, enabled = true),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null && !bitmap.isRecycled) {
                val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Page ${pageIndex + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )

                // Highlight overlay: pulsing classroom cursor (blue), vocabulary underlines (amber), or active word selection (blue)
                val hasOverlay = isClassroomActive ||
                    (isVocabAssistanceEnabled && highlightedWords.isNotEmpty()) ||
                    (selectedWord != null && selectedWord.page == pageIndex)

                if (hasOverlay) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .testTag("page_highlight_overlay_$pageIndex")
                    ) {
                        val pdfW = pdfDimensions?.first?.toFloat() ?: bitmap.width.toFloat()
                        val pdfH = pdfDimensions?.second?.toFloat() ?: bitmap.height.toFloat()
                        val scaleX = if (pdfW > 0f) size.width / pdfW else 1f
                        val scaleY = if (pdfH > 0f) size.height / pdfH else 1f

                        if (isClassroomActive && classroomWord != null) {
                            // Suppress all amber vocabulary highlights and draw classroom cursor
                            val rectLeft = classroomWord.x * scaleX
                            val rectTop = classroomWord.y * scaleY
                            val rectWidth = (classroomWord.width * scaleX).coerceAtLeast(8f)
                            val rectHeight = (classroomWord.height * scaleY).coerceAtLeast(8f)

                            val classroomColor = Color(0xFF1976D2)
                            val cornerRadiusPx = 4.dp.toPx()

                            // Fill: Color(0xFF1976D2) with pulsing alpha (0.3f to 0.7f)
                            drawRoundRect(
                                color = classroomColor.copy(alpha = pulseAlpha),
                                topLeft = Offset(rectLeft, rectTop),
                                size = Size(rectWidth, rectHeight),
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )

                            // Stroke: 2dp solid stroke border: #1976D2 at alpha 0.9f
                            drawRoundRect(
                                color = classroomColor.copy(alpha = 0.9f),
                                topLeft = Offset(rectLeft, rectTop),
                                size = Size(rectWidth, rectHeight),
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        } else if (isVocabAssistanceEnabled && highlightedWords.isNotEmpty()) {
                            val highlightColor = Color(0xFFFFB300).copy(alpha = 0.22f)
                            val underlineColor = Color(0xFFD97706) // Rich amber for clear vocabulary underline
                            val underlineStroke = 2.dp.toPx()

                            for (word in highlightedWords) {
                                val rectLeft = word.x * scaleX
                                val rectTop = word.y * scaleY
                                val rectWidth = (word.width * scaleX).coerceAtLeast(4f)
                                val rectHeight = (word.height * scaleY).coerceAtLeast(4f)
                                val rectBottom = rectTop + rectHeight

                                // 1. Subtle soft tint behind the word
                                drawRoundRect(
                                    color = highlightColor,
                                    topLeft = Offset(rectLeft, rectTop),
                                    size = Size(rectWidth, rectHeight),
                                    cornerRadius = CornerRadius(3f, 3f)
                                )

                                // 2. Crisp, prominent UNDERLINE right beneath the word text
                                drawLine(
                                    color = underlineColor,
                                    start = Offset(rectLeft, rectBottom + 1f),
                                    end = Offset(rectLeft + rectWidth, rectBottom + 1f),
                                    strokeWidth = underlineStroke,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // Draw selection box around selected word if on this page
                        if (selectedWord != null && selectedWord.page == pageIndex) {
                            val selLeft = (selectedWord.x * scaleX - 3f).coerceAtLeast(0f)
                            val selTop = (selectedWord.y * scaleY - 2f).coerceAtLeast(0f)
                            val selWidth = (selectedWord.width * scaleX + 6f).coerceAtLeast(8f)
                            val selHeight = (selectedWord.height * scaleY + 4f).coerceAtLeast(8f)
                            val selColor = Color(0xFF2563EB) // Royal blue selection

                            drawRoundRect(
                                color = selColor.copy(alpha = 0.18f),
                                topLeft = Offset(selLeft, selTop),
                                size = Size(selWidth, selHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            drawRoundRect(
                                color = selColor.copy(alpha = 0.85f),
                                topLeft = Offset(selLeft, selTop),
                                size = Size(selWidth, selHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                        }
                    }
                }
            } else {
                // Skeleton / shimmer placeholder while rendering
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.707f) // Standard ISO 216 page aspect ratio
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        strokeWidth = 2.5.dp
                    )
                }
            }

            // Floating action bar above the long-press selection (Copy, Define, Explain)
            if (selectedTextSelection != null && selectedTextSelection.pageIndex == pageIndex) {
                val density = LocalDensity.current
                val barWidthPx = with(density) { 270.dp.toPx() }
                val barHeightPx = with(density) { 48.dp.toPx() }
                val curW = displayedSize.width.toFloat()
                val rawX = selectedTextSelection.offset.x
                val rawY = selectedTextSelection.offset.y
                val barX = (rawX - barWidthPx / 2f).coerceIn(12f, (curW - barWidthPx - 12f).coerceAtLeast(12f))
                val barY = (rawY - barHeightPx - 8f).coerceAtLeast(8f)

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .offset { IntOffset(barX.toInt(), barY.toInt()) }
                        .testTag("floating_selection_action_bar")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        // Copy
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onCopyClick(selectedTextSelection.text) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("action_copy_text")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Copy",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Define / Look Up
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onDefineClick(selectedTextSelection.text) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("action_define_text")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = "Define",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Define",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Explain with AI
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onExplainClick(selectedTextSelection.text) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("explain_this_floating_action")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Explain",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Explain",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
