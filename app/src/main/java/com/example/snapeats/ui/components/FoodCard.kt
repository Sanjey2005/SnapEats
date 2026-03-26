package com.example.snapeats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.snapeats.domain.model.Food

private val ColorCarbs = Color(0xFF2196F3)   // blue
private val ColorFat = Color(0xFFFFC107)     // amber
private val ColorProtein = Color(0xFF4CAF50) // green

@Composable
fun FoodCard(
    food: Food,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (food.thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = food.thumbnailUrl,
                        contentDescription = food.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Calorie badge
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${food.calories} kcal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Macro label row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MacroLabel(label = "C", value = food.carbs, color = ColorCarbs)
                    MacroLabel(label = "F", value = food.fat, color = ColorFat)
                    MacroLabel(label = "P", value = food.protein, color = ColorProtein)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Macro proportion bar
                MacroBar(
                    carbs = food.carbs,
                    fat = food.fat,
                    protein = food.protein,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }
        }

        // Offline badge
        if (food.isOffline) {
            Surface(
                color = Color(0xFFFFF3E0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚡ Offline data",
                    fontSize = 11.sp,
                    color = Color(0xFFE65100),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MacroLabel(label: String, value: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "$label ${value.toInt()}g",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MacroBar(
    carbs: Float,
    fat: Float,
    protein: Float,
    modifier: Modifier = Modifier
) {
    val total = carbs + fat + protein
    if (total <= 0f) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val radius = h / 2f

        val carbsW = (carbs / total) * w
        val fatW = (fat / total) * w
        val proteinW = (protein / total) * w

        var x = 0f

        // Draw carbs segment
        drawRoundRect(
            color = ColorCarbs,
            topLeft = Offset(x, 0f),
            size = Size(carbsW, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
        )
        x += carbsW

        // Draw fat segment
        if (fatW > 0f) {
            drawRect(
                color = ColorFat,
                topLeft = Offset(x, 0f),
                size = Size(fatW, h)
            )
            x += fatW
        }

        // Draw protein segment — rounded right end
        if (proteinW > 0f) {
            drawRoundRect(
                color = ColorProtein,
                topLeft = Offset(x, 0f),
                size = Size(proteinW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
            )
        }
    }
}
