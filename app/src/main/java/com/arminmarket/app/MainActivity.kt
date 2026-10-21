package com.arminmarket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MarketCategory(val title: String, val description: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ArminMarketApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArminMarketApp() {
    val categories = listOf(
        MarketCategory("کالای دیجیتال", "موبایل، لوازم جانبی و تجهیزات"),
        MarketCategory("خانه و زندگی", "وسایل کاربردی خانه"),
        MarketCategory("مد و پوشاک", "پوشاک و اکسسوری"),
        MarketCategory("زیبایی و سلامت", "محصولات مراقبت شخصی")
    )
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("آرمین مارکت", fontWeight = FontWeight.Bold)
                            Text("بازار هوشمند خرید و فروش", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf("خانه", "دسته‌بندی‌ها", "سبد خرید", "حساب من").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Text(listOf("⌂", "▦", "🛒", "●")[index]) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("سلام! به آرمین مارکت خوش آمدید", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("نسخه اولیه رابط کاربری؛ اتصال فروشگاه و سفارش‌ها هنوز باید پیاده‌سازی شود.")
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("جست‌وجوی کالا") },
                        singleLine = true
                    )
                    Text("دسته‌بندی‌ها", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(categories.filter { it.title.contains(search, true) || it.description.contains(search, true) || search.isBlank() }) { category ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(category.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("توجه: این نسخه نمایشی است و هنوز کالاهای واقعی، پرداخت، حساب کاربری و سرور ندارد.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
