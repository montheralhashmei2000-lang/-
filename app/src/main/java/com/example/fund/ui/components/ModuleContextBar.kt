package com.example.fund.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.model.UserRole
import com.example.fund.ui.navigation.AppRoute
import com.example.fund.ui.navigation.HubContextGroup
import com.example.fund.ui.navigation.Screen
import com.example.ui.theme.*

@Composable
fun ModuleContextBar(
    contextGroup: HubContextGroup,
    currentScreen: Screen,
    currentRole: UserRole,
    onSelectParentHub: (AppRoute) -> Unit,
    onSelectSibling: (AppRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleItems = contextGroup.items.filter { item ->
        !(currentRole == UserRole.VIEWER && item.screen == Screen.USERS)
    }
    val activeIdx = visibleItems.indexOfFirst { it.screen == currentScreen }.coerceAtLeast(0)
    val listState = rememberLazyListState()

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0 && visibleItems.isNotEmpty()) {
            listState.animateScrollToItem(activeIdx)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("module_context_bar")
    ) {
        Column {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Parent Hub Breadcrumb Pill
                item(key = "parent_hub_pill") {
                    Surface(
                        color = FundGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, FundGold.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectParentHub(contextGroup.hubRoute) }
                            .testTag("breadcrumb_parent_hub")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = contextGroup.hubTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FundGoldDark
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = FundGoldDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Sibling Module Pills for instant 1-tap switching
                itemsIndexed(visibleItems, key = { _, item -> item.screen.name }) { _, item ->
                    val selected = item.screen == currentScreen
                    Surface(
                        color = if (selected) FundPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (!selected) onSelectSibling(item.route)
                            }
                            .testTag("sibling_tab_${item.screen.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (selected) Color.White else FundPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = item.shortTitle,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        }
    }
}
