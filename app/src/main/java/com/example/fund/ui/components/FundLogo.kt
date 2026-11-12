package com.example.fund.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import java.io.ByteArrayOutputStream

@Composable
fun FundLogoImage(
    customLogoBase64: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "شعار الصندوق",
    contentScale: ContentScale = ContentScale.Crop
) {
    val decodedBitmap: ImageBitmap? = remember(customLogoBase64) {
        if (customLogoBase64.isBlank()) {
            null
        } else {
            try {
                val bytes = Base64.decode(customLogoBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.ic_fund_logo),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier.clip(CircleShape)
        )
    }
}

fun encodeImageUriToBase64(context: Context, uri: Uri, maxDim: Int = 256): String? {
    return try {
        val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        val width = rawBitmap.width
        val height = rawBitmap.height
        val scale = if (width > maxDim || height > maxDim) {
            minOf(maxDim.toFloat() / width.toFloat(), maxDim.toFloat() / height.toFloat())
        } else {
            1f
        }
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
        } else {
            rawBitmap
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}
