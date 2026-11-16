package com.example.fund.util

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 140.dp,
    dotColor: Color = Color(0xFF003300),
    bgColor: Color = Color.White
) {
    val grid = remember(content) {
        generateQrMatrix(content, 0)
    }
    val matrixSize = grid.size.coerceAtLeast(1)

    Box(
        modifier = modifier
            .size(sizeDp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFD8E4D2), RoundedCornerShape(8.dp))
            .padding(5.dp)
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

fun generateQrSvg(content: String, sizePx: Int = 78, dotColorHex: String = "#003300"): String {
    val grid = generateQrMatrix(content, 0)
    val matrixSize = grid.size.coerceAtLeast(1)
    val rects = StringBuilder()
    for (r in 0 until matrixSize) {
        for (c in 0 until matrixSize) {
            if (grid[r][c]) {
                rects.append("<rect x=\"$c\" y=\"$r\" width=\"1\" height=\"1\" fill=\"$dotColorHex\"/>")
            }
        }
    }
    return """<svg xmlns="http://www.w3.org/2000/svg" width="$sizePx" height="$sizePx" viewBox="-1 -1 ${matrixSize + 2} ${matrixSize + 2}" style="background:#FFFFFF;border:1px solid #D8E4D2;border-radius:6px;">$rects</svg>"""
}

/**
 * Generates a standard ISO/IEC 18004 QR matrix using ZXing QRCodeWriter.
 * When [size] > 0, passes preferred square dimension hint; when [size] <= 0, picks natural QR module grid.
 */
fun generateQrMatrix(text: String, size: Int = 0): Array<BooleanArray> {
    val payload = text.ifEmpty { "EMPTY" }
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
    )
    val requestedSize = if (size > 0) size else 0
    val bitMatrix = QRCodeWriter().encode(
        payload,
        BarcodeFormat.QR_CODE,
        requestedSize,
        requestedSize,
        hints
    )
    val rows = bitMatrix.height
    val cols = bitMatrix.width
    return Array(rows) { r ->
        BooleanArray(cols) { c -> bitMatrix.get(c, r) }
    }
}

/**
 * Renders a scannable ISO/IEC 18004 QR Bitmap for PDF rendering and image sharing.
 */
fun generateQrBitmap(
    content: String,
    sizePx: Int = 240,
    darkColorInt: Int = AndroidColor.parseColor("#003300"),
    lightColorInt: Int = AndroidColor.WHITE
): Bitmap {
    val grid = generateQrMatrix(content, sizePx.coerceAtLeast(64))
    val h = grid.size
    val w = if (h > 0) grid[0].size else sizePx.coerceAtLeast(64)
    val pixels = IntArray(w * h)
    for (r in 0 until h) {
        val row = grid[r]
        val offset = r * w
        for (c in 0 until w) {
            pixels[offset + c] = if (row[c]) darkColorInt else lightColorInt
        }
    }
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    return bmp
}
