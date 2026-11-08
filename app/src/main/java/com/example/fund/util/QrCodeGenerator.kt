package com.example.fund.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 140.dp,
    dotColor: Color = Color(0xFF003300),
    bgColor: Color = Color.White
) {
    val matrixSize = 25
    val grid = remember(content) {
        generateQrMatrix(content, matrixSize)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFD8E4D2), RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cellW = size.width / matrixSize
            val cellH = size.height / matrixSize

            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (grid[r][c]) {
                        drawRect(
                            color = dotColor,
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW, cellH)
                        )
                    }
                }
            }
        }
    }
}

private fun generateQrMatrix(text: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) { false } }

    fun drawFinder(top: Int, left: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                val isCenter = r in 2..4 && c in 2..4
                matrix[top + r][left + c] = isBorder || isCenter
            }
        }
    }

    // Three standard position corners
    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    // Timing lines
    for (i in 7 until size - 7) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // Deterministic pseudo-data from content hash and chars
    val hash = text.hashCode().absoluteValue
    var bitIndex = 0
    val chars = text.toByteArray()

    for (r in 0 until size) {
        for (c in 0 until size) {
            val inFinder1 = r < 8 && c < 8
            val inFinder2 = r < 8 && c >= size - 8
            val inFinder3 = r >= size - 8 && c < 8
            val isTiming = (r == 6 && c in 7 until size - 7) || (c == 6 && r in 7 until size - 7)

            if (!inFinder1 && !inFinder2 && !inFinder3 && !isTiming) {
                val charVal = if (chars.isNotEmpty()) chars[bitIndex % chars.size].toInt() else 0
                val bit = ((hash xor (r * 31 + c * 17) xor charVal) + bitIndex) % 3 == 0
                matrix[r][c] = bit
                bitIndex++
            }
        }
    }

    return matrix
}
