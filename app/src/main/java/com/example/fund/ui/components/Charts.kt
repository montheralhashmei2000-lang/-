package com.example.fund.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.abs

enum class ChartTimeframe(val title: String) {
    WEEKLY("أسبوعي"),
    MONTHLY("شهري"),
    YEARLY("سنوي")
}

@Composable
fun TrendLineChart(
    dataPoints: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = FundPrimary,
    accentColor: Color = FundGold,
    weeklyDataPoints: List<Pair<String, Double>>? = null,
    yearlyDataPoints: List<Pair<String, Double>>? = null
) {
    if (dataPoints.isEmpty()) return

    var activeTimeframe by remember { mutableStateOf(ChartTimeframe.MONTHLY) }
    val resolvedPoints = remember(activeTimeframe, dataPoints, weeklyDataPoints, yearlyDataPoints) {
        when (activeTimeframe) {
            ChartTimeframe.WEEKLY -> weeklyDataPoints ?: listOf(
                "السبت" to (dataPoints.lastOrNull()?.second ?: 1_200_000.0) * 0.14,
                "الأحد" to (dataPoints.lastOrNull()?.second ?: 1_200_000.0) * 0.19,
                "الاثنين" to (dataPoints.lastOrNull()?.second ?: 1_200_000.0) * 0.16,
                "الثلاثاء" to (dataPoints.lastOrNull()?.second ?: 1_200_000.0) * 0.22,
                "الأربعاء" to (dataPoints.lastOrNull()?.second ?: 1_200_000.0) * 0.28,
                "الخميس" to (dataPoints.lastOrNull()?.second ?: 1_200_000.0) * 0.31
            )
            ChartTimeframe.MONTHLY -> dataPoints
            ChartTimeframe.YEARLY -> yearlyDataPoints ?: listOf(
                "2021" to 9_400_000.0,
                "2022" to 12_850_000.0,
                "2023" to 16_200_000.0,
                "2024" to 19_600_000.0,
                "2025" to 23_400_000.0,
                "2026" to 28_950_000.0
            )
        }
    }

    var selectedIndex by remember(activeTimeframe) { mutableStateOf<Int?>(resolvedPoints.lastIndex) }

    val maxVal = (resolvedPoints.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "مؤشر نمو الإيرادات (${activeTimeframe.title})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "الأعلى: %,d ﷼ (اضغط على أي نقطة للتفاصيل)".format(maxVal.toLong()),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FundPrimary,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Timeframe Switcher Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChartTimeframe.entries.forEach { tf ->
                val isSelected = activeTimeframe == tf
                FilterChip(
                    selected = isSelected,
                    onClick = { activeTimeframe = tf },
                    label = { Text(tf.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundPrimary.copy(alpha = 0.16f),
                        selectedLabelColor = FundPrimary
                    )
                )
            }
        }

        // Interactive Tooltip Banner for the selected point
        AnimatedVisibility(visible = selectedIndex != null && selectedIndex!! in resolvedPoints.indices) {
            val idx = selectedIndex?.coerceIn(0, resolvedPoints.lastIndex) ?: 0
            val (periodLabel, periodVal) = resolvedPoints[idx]
            Surface(
                color = FundDeepHeader,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الفترة المختارة: $periodLabel",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%,d ﷼".format(periodVal.toLong()),
                        color = FundGoldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }

        // Interactive Canvas with tap detection
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 12.dp)
                .pointerInput(resolvedPoints) {
                    detectTapGestures { offset ->
                        val count = resolvedPoints.size
                        if (count > 0) {
                            val w = size.width.toFloat()
                            val spacing = w / (count - 1).coerceAtLeast(1)
                            var closestIdx = 0
                            var bestDist = Float.MAX_VALUE
                            for (i in 0 until count) {
                                val px = w - (i * spacing)
                                val dist = abs(offset.x - px)
                                if (dist < bestDist) {
                                    bestDist = dist
                                    closestIdx = i
                                }
                            }
                            selectedIndex = closestIdx
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val count = resolvedPoints.size
            val spacing = width / (count - 1).coerceAtLeast(1)

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

            val points = resolvedPoints.mapIndexed { index, pair ->
                val x = width - (index * spacing)
                val ratio = ((pair.second - minVal) / (maxVal - minVal)).toFloat()
                val y = height - (ratio * (height - 24.dp.toPx())) - 12.dp.toPx()
                Offset(x, y)
            }

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

            points.forEachIndexed { idx, pt ->
                val isActive = selectedIndex == idx
                if (isActive) {
                    drawLine(
                        color = accentColor.copy(alpha = 0.6f),
                        start = Offset(pt.x, 0f),
                        end = Offset(pt.x, height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                    drawCircle(color = accentColor.copy(alpha = 0.3f), radius = 11.dp.toPx(), center = pt)
                }
                drawCircle(color = lineColor, radius = if (isActive) 7.dp.toPx() else 5.5.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = pt)
                drawCircle(color = accentColor, radius = 2.dp.toPx(), center = pt)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            resolvedPoints.forEachIndexed { idx, (label, value) ->
                val isSelected = selectedIndex == idx
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) FundPrimary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { selectedIndex = idx }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) FundPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (value >= 1_000_000.0) "%.1fم".format(value / 1_000_000.0)
                        else "%dك".format((value / 1_000.0).toInt()),
                        fontSize = 9.sp,
                        color = FundPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
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
    var selectedSliceIdx by remember { mutableStateOf<Int?>(null) }

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
                    color = StatusDanger,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(106.dp)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    items.forEachIndexed { idx, item ->
                        val isFocused = selectedSliceIdx == null || selectedSliceIdx == idx
                        val strokeW = if (selectedSliceIdx == idx) 16.dp.toPx() else 13.dp.toPx()
                        val sweep = ((item.second / total) * 360f).toFloat()
                        drawArc(
                            color = if (isFocused) item.third else item.third.copy(alpha = 0.3f),
                            startAngle = startAngle,
                            sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }

                val focused = selectedSliceIdx?.let { items.getOrNull(it) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = focused?.let { "${((it.second / total) * 100).toInt()}%" } ?: "${items.size} أبواب",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = focused?.third ?: FundPrimary,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = focused?.first?.take(8) ?: centerSubtext.ifBlank { "100%" },
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items.forEachIndexed { idx, item ->
                    val percent = ((item.second / total) * 100).toInt()
                    val isSelected = selectedSliceIdx == idx
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) item.third.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable {
                                selectedSliceIdx = if (selectedSliceIdx == idx) null else idx
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
                            maxLines = 1,
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
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
                color = FundPrimary,
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
            )
        }

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
                        color = MaterialTheme.colorScheme.onSurface,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}

/**
 * Interactive Grouped Bar Chart comparing Revenue vs. Expense across Regions.
 */
@Composable
fun RegionalComparisonBarChart(
    regionsData: List<Triple<String, Double, Double>>, // Triple(RegionName, Revenue, Expense)
    modifier: Modifier = Modifier
) {
    if (regionsData.isEmpty()) return
    val maxVal = regionsData.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1.0) ?: 1.0
    var selectedRegionIdx by remember { mutableStateOf<Int?>(0) }

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
                text = "مقارنة الإيرادات بالمصروفات حسب المنطقة",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.size(8.dp).background(StatusSuccess, CircleShape))
                    Text("إيرادات", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.size(8.dp).background(StatusDanger, CircleShape))
                    Text("مصروفات", fontSize = 10.sp)
                }
            }
        }

        AnimatedVisibility(visible = selectedRegionIdx != null && selectedRegionIdx!! in regionsData.indices) {
            val (name, rev, exp) = regionsData[selectedRegionIdx!!]
            val net = rev - exp
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, FundPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FundPrimary)
                    Text(
                        text = "قبض: %,d | صرف: %,d | صافي: %,d ﷼".format(rev.toLong(), exp.toLong(), net.toLong()),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (net >= 0) StatusSuccess else StatusDanger,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            regionsData.forEachIndexed { idx, (regionName, rev, exp) ->
                val isSelected = selectedRegionIdx == idx
                val revRatio = (rev / maxVal).toFloat().coerceIn(0.06f, 1f)
                val expRatio = (exp / maxVal).toFloat().coerceIn(0.06f, 1f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) FundPrimary.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { selectedRegionIdx = idx }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .fillMaxHeight(revRatio)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(StatusSuccess)
                        )
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .fillMaxHeight(expRatio)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(StatusDanger)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = regionName,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                        color = if (isSelected) FundPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
