package com.example.snapeats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private val ColorCarbs   = Color(0xFF2196F3)   // blue
private val ColorFat     = Color(0xFFFFC107)   // amber
private val ColorProtein = Color(0xFF4CAF50)   // green

@Composable
fun FoodCard(
    food: Food,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(16.dp)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF161B22)
        )
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
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF21262D), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Restaurant,
                            contentDescription = null,
                            tint = Color(0xFF8B949E),
                            modifier = Modifier.size(28.dp)
                        )
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE6EDF3),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Calorie badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${food.calories} kcal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF21262D))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "⚡ Offline data",
                    fontSize = 11.sp,
                    color = Color(0xFFFFC107)
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
            color = Color(0xFF8B949E)
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

        val carbsW   = (carbs / total) * w
        val fatW     = (fat / total) * w
        val proteinW = (protein / total) * w

        var x = 0f

        drawRoundRect(
            color = ColorCarbs,
            topLeft = Offset(x, 0f),
            size = Size(carbsW, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
        )
        x += carbsW

        if (fatW > 0f) {
            drawRect(
                color = ColorFat,
                topLeft = Offset(x, 0f),
                size = Size(fatW, h)
            )
            x += fatW
        }

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
