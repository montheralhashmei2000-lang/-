package com.example.fund.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun TrendLineChart(
    dataPoints: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = FundPrimary,
    accentColor: Color = FundGold
) {
    if (dataPoints.isEmpty()) return

    val maxVal = (dataPoints.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    val minVal = 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "نمو الإيرادات الشهرية (آخر 6 أشهر)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "الأعلى: %,d ﷼".format(maxVal.toLong()),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FundPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas with horizontal inset so circles never clip at edges, and reversed X for RTL alignment
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .padding(horizontal = 12.dp)
        ) {
            val width = size.width
            val height = size.height
            val count = dataPoints.size
            val spacing = width / (count - 1).coerceAtLeast(1)

            // Draw horizontal grid lines
            for (i in 0..3) {
                val y = height * (i / 3f)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            }

            // In RTL layout, index 0 ("يونيو") is on the right (width) and last index ("نوفمبر") is on the left (0f)
            val points = dataPoints.mapIndexed { index, pair ->
                val x = width - (index * spacing)
                val ratio = ((pair.second - minVal) / (maxVal - minVal)).toFloat()
                val y = height - (ratio * (height - 24.dp.toPx())) - 12.dp.toPx()
                Offset(x, y)
            }

            // Draw area gradient under curve
            val fillPath = Path().apply {
                moveTo(points.first().x, height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw smooth cubic curve
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cx = (prev.x + curr.x) / 2f
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
            }
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw data points
            points.forEach { pt ->
                drawCircle(color = lineColor, radius = 6.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = pt)
                drawCircle(color = accentColor, radius = 2.dp.toPx(), center = pt)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Month + Compact Value Labels Row (aligned in RTL with the points)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dataPoints.forEach { (month, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = month,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%.1fم".format(value / 1_000_000.0),
                        fontSize = 9.sp,
                        color = FundPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    items: List<Triple<String, Double, Color>>,
    modifier: Modifier = Modifier,
    centerText: String = "",
    centerSubtext: String = ""
) {
    val total = items.sumOf { it.second }.coerceAtLeast(1.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "توزيع المصروفات والمساعدات المعتمدة",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (centerText.isNotEmpty()) {
                Text(
                    text = centerText,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = StatusDanger
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Clean Donut Ring with compact non-overlapping center label
            Box(
                modifier = Modifier
                    .size(106.dp)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    val strokeW = 13.dp.toPx()

                    items.forEach { item ->
                        val sweep = ((item.second / total) * 360f).toFloat()
                        drawArc(
                            color = item.third,
                            startAngle = startAngle,
                            sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${items.size} أبواب",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = FundPrimary
                    )
                    Text(
                        text = "100%",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Detailed Legend with Amount + Percentage
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items.forEach { item ->
                    val percent = ((item.second / total) * 100).toInt()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(item.third, CircleShape)
                            )
                            Text(
                                text = item.first,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${"%,d".format(item.second.toLong())} ﷼ ($percent%)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = item.third,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProportionalBarChart(
    items: List<Triple<String, Int, Color>>,
    modifier: Modifier = Modifier
) {
    val total = items.sumOf { it.second }.coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مؤشر إنجاز طلبات المساعدة",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "الإجمالي: $total طلب",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FundPrimary
            )
        }

        // Stacked Progress Bar with proper clipping so rounded ends are crisp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFEEEEEE))
        ) {
            items.forEach { item ->
                val fraction = (item.second.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(fraction)
                            .fillMaxHeight()
                            .background(item.third)
                    )
                }
            }
        }

        // Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(item.third, CircleShape)
                    )
                    Text(
                        text = "${item.first}: ${item.second}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
