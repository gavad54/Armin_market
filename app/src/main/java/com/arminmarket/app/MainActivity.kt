package com.arminmarket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF06263A)
private val DeepNavy = Color(0xFF031827)
private val Teal = Color(0xFF00C9BD)
private val TealDark = Color(0xFF008D91)
private val Page = Color(0xFFF3F7FA)
private val Ink = Color(0xFF102E43)
private val Muted = Color(0xFF6E8493)
private val Line = Color(0xFFE2EBF1)

private data class MarketCategory(val title: String, val icon: String, val subtitle: String)
private data class MarketProduct(val name: String, val category: String, val price: String, val badge: String, val seller: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ArminMarketApp() }
    }
}

@Composable
private fun ArminMarketApp() {
    val categories = listOf(
        MarketCategory("موبایل و تبلت", "▣", "گوشی، تبلت و لوازم جانبی"),
        MarketCategory("لپ‌تاپ و کامپیوتر", "▤", "کامپیوتر و تجهیزات"),
        MarketCategory("خانه و زندگی", "⌂", "لوازم کاربردی خانه"),
        MarketCategory("خودرو و موتور", "◇", "قطعات و لوازم"),
        MarketCategory("املاک", "⌂", "خرید، فروش و اجاره"),
        MarketCategory("خدمات", "⚒", "متخصصان و خدمات"),
        MarketCategory("مد و پوشاک", "♧", "پوشاک و اکسسوری"),
        MarketCategory("ورزش و سفر", "↗", "تجهیزات و سرگرمی")
    )
    val products = listOf(
        MarketProduct("گوشی هوشمند مدل A24", "موبایل و تبلت", "۱۲٬۸۰۰٬۰۰۰ تومان", "پیشنهاد ویژه", "فروشگاه تکنولوژی"),
        MarketProduct("هدفون بی‌سیم", "لپ‌تاپ و کامپیوتر", "۲٬۴۵۰٬۰۰۰ تومان", "محبوب", "دیجیتال‌کالا"),
        MarketProduct("ساعت هوشمند", "موبایل و تبلت", "۳٬۷۵۰٬۰۰۰ تومان", "تازه", "اسمارت‌شاپ")
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var cart by remember { mutableStateOf(listOf<MarketProduct>()) }
    var requestTitle by remember { mutableStateOf("") }
    var requestSent by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<MarketProduct?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = TealDark, onPrimary = Color.White, secondary = Teal,
        background = Page, surface = Color.White, onSurface = Ink
    )) {
        Scaffold(
            containerColor = Page,
            topBar = {
                if (selectedTab != 4) TopAppBarArmin(
                    onProfile = { selectedTab = 4 },
                    onRequests = { selectedTab = 2 }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White, contentColor = Ink, tonalElevation = 12.dp) {
                    val destinations = listOf(
                        Triple("خانه", "⌂", 0), Triple("دسته‌ها", "▦", 1),
                        Triple("ثبت نیاز", "+", 2), Triple("پیشنهادها", "⇄", 3), Triple("حساب من", "●", 4)
                    )
                    destinations.forEach { item ->
                        NavigationBarItem(
                            selected = selectedTab == item.third,
                            onClick = { selectedTab = item.third },
                            icon = {
                                Box(Modifier.size(if (item.third == 2) 34.dp else 27.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedTab == item.third) Teal.copy(alpha = 0.16f) else Color.Transparent),
                                    contentAlignment = Alignment.Center) {
                                    Text(item.second, color = if (selectedTab == item.third) TealDark else Muted, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            label = { Text(item.first, fontSize = 10.sp, maxLines = 1) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealDark, selectedTextColor = TealDark,
                                indicatorColor = Teal.copy(alpha = 0.12f), unselectedTextColor = Muted
                            )
                        )
                    }
                }
            },
            snackbarHost = {
                notice?.let { message ->
                    Snackbar(Modifier.padding(12.dp), action = {
                        TextButton(onClick = { notice = null }) { Text("باشه") }
                    }) { Text(message) }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        search, { search = it }, categories,
                        products.filter {
                            (search.isBlank() || it.name.contains(search, true) || it.category.contains(search, true)) &&
                                (selectedCategory == null || it.category == selectedCategory)
                        },
                        selectedCategory, { selectedCategory = if (selectedCategory == it) null else it },
                        { product -> cart = cart + product; notice = "به سبد خرید آزمایشی اضافه شد." },
                        { selectedProduct = it }, { selectedTab = 1 }, { selectedTab = 2 }
                    )
                    1 -> CategoriesScreen(categories, search, { search = it }) { category ->
                        selectedCategory = category.title
                        selectedTab = 0
                    }
                    2 -> RequestScreen(
                        requestTitle, { requestTitle = it }, requestSent,
                        {
                            if (requestTitle.trim().length < 4) notice = "لطفاً نیاز خود را با چند کلمه توضیح دهید."
                            else {
                                requestSent = true
                                notice = "درخواست در نمونه نمایشی ثبت شد؛ هنوز به سرور متصل نیست."
                            }
                        },
                        { requestTitle = ""; requestSent = false }
                    )
                    3 -> OffersScreen { notice = "انتخاب پیشنهاد در نسخه نمایشی ثبت شد؛ هنوز معامله واقعی انجام نمی‌شود." }
                    else -> ProfileScreen(cart.size, { selectedTab = 3 }) { notice = it }
                }
                selectedProduct?.let { product ->
                    ProductDetailSheet(product, { selectedProduct = null }, {
                        cart = cart + product
                        selectedProduct = null
                        notice = "به سبد خرید آزمایشی اضافه شد."
                    })
                }
            }
        }
    }
}

@Composable
private fun TopAppBarArmin(onProfile: () -> Unit, onRequests: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("ARMIN MARKET", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
            Text("آرمین مارکت", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallAction("＋", "ثبت نیاز", onRequests)
            SmallAction("●", "حساب", onProfile)
        }
    }
}

@Composable
private fun SmallAction(icon: String, label: String, onClick: () -> Unit) {
    Surface(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        color = Page, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(icon, color = TealDark, fontWeight = FontWeight.Bold)
            Text(label, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomeScreen(
    search: String, onSearch: (String) -> Unit, categories: List<MarketCategory>,
    products: List<MarketProduct>, selectedCategory: String?, onCategory: (String) -> Unit,
    onAdd: (MarketProduct) -> Unit, onProduct: (MarketProduct) -> Unit,
    onAllCategories: () -> Unit, onNewRequest: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("جست‌وجوی کالا، خدمت یا نیاز...", fontSize = 13.sp) },
                    leadingIcon = { Text("⌕", color = TealDark, fontSize = 24.sp) },
                    singleLine = true, shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealDark, unfocusedBorderColor = Line,
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                    )
                )
                HeroBanner(onNewRequest)
            }
        }
        item {
            SectionHeader("دسته‌بندی‌های پرطرفدار", "مشاهده همه", onAllCategories)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                categories.chunked(4).forEach { rowCategories ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowCategories.forEach { category ->
                            CategoryTile(category, selectedCategory == category.title, Modifier.weight(1f)) { onCategory(category.title) }
                        }
                        repeat(4 - rowCategories.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        item {
            SectionHeader("پیشنهادهای منتخب", "برای شما", {})
            Text("نمونه‌های نمایشی؛ موجودی و قیمت واقعی هنوز متصل نشده است.", color = Muted, fontSize = 11.sp)
        }
        if (products.isEmpty()) item { EmptyState("نتیجه‌ای پیدا نشد", "عبارت جست‌وجو یا دسته‌بندی را تغییر دهید.") }
        else items(products) { product ->
            ProductCard(product, { onProduct(product) }, { onAdd(product) })
        }
        item {
            Surface(color = Navy, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onNewRequest)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Teal.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Text("✦", color = Teal, fontSize = 24.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("دنبال بهترین پیشنهاد هستید؟", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("نیازتان را ثبت کنید تا فروشندگان پیشنهاد بدهند.", color = Color(0xFFBBD0DB), fontSize = 11.sp)
                    }
                    Text("←", color = Teal, fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(onNewRequest: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(DeepNavy, Navy, Color(0xFF07556A))))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = Teal.copy(alpha = 0.16f), shape = RoundedCornerShape(30.dp)) {
                Text("بازار رقابتی هوشمند", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Teal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("نیازت را بگو؛", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("برای بهترین معامله پیشنهاد بگیر.", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("درخواستت را ثبت کن و قیمت، زمان و شرایط فروشندگان را مقایسه کن.", color = Color(0xFFC7D9E2), fontSize = 12.sp, lineHeight = 19.sp)
            Button(onClick = onNewRequest, shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = DeepNavy),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)) {
                Text("＋  ثبت درخواست جدید", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(action, color = TealDark, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onClick))
    }
}

@Composable
private fun CategoryTile(category: MarketCategory, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.height(94.dp).clickable(onClick = onClick),
        color = if (selected) Teal.copy(alpha = 0.12f) else Color.White,
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if (selected) TealDark else Line)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(33.dp).clip(RoundedCornerShape(11.dp)).background(if (selected) Teal.copy(alpha = 0.2f) else Color(0xFFEAF5FA)),
                contentAlignment = Alignment.Center) {
                Text(category.icon, color = TealDark, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            Text(category.title, color = Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ProductCard(product: MarketProduct, onClick: () -> Unit, onAdd: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp),
        color = Color.White, border = BorderStroke(1.dp, Line), shadowElevation = 1.dp) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFE6F5F8), Color(0xFFD6E7F1)))), contentAlignment = Alignment.Center) {
                Text("◇", color = TealDark, fontSize = 43.sp, fontWeight = FontWeight.Light)
                Surface(Modifier.align(Alignment.TopStart).padding(5.dp), color = Teal, shape = RoundedCornerShape(7.dp)) {
                    Text(product.badge, Modifier.padding(horizontal = 5.dp, vertical = 3.dp), color = DeepNavy, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(product.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(product.seller, color = Muted, fontSize = 11.sp)
                Text("★  ۴٫۸  ·  فروشنده نمونه", color = Color(0xFFB47B16), fontSize = 10.sp)
                Text(product.price, color = TealDark, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onAdd, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = TealDark)) {
                        Text("افزودن", fontSize = 10.sp)
                    }
                    Text("جزئیات ←", color = Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun CategoriesScreen(categories: List<MarketCategory>, search: String, onSearch: (String) -> Unit, onSelect: (MarketCategory) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("دسته‌بندی‌ها", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
        Text("از دسته مناسب شروع کنید.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
        OutlinedTextField(search, onSearch, modifier = Modifier.fillMaxWidth(), placeholder = { Text("جست‌وجوی دسته") }, singleLine = true, shape = RoundedCornerShape(15.dp))
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories.filter { search.isBlank() || it.title.contains(search, true) || it.subtitle.contains(search, true) }) { category ->
                Surface(Modifier.fillMaxWidth().clickable { onSelect(category) }, color = Color.White,
                    shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line)) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFEAF5FA)), contentAlignment = Alignment.Center) {
                            Text(category.icon, color = TealDark, fontSize = 23.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(category.title, color = Ink, fontWeight = FontWeight.Bold)
                            Text(category.subtitle, color = Muted, fontSize = 11.sp)
                        }
                        Text("←", color = TealDark, fontSize = 19.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestScreen(title: String, onTitle: (String) -> Unit, sent: Boolean, onSubmit: () -> Unit, onReset: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("ثبت درخواست جدید", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text("نیازتان را واضح بنویسید تا بتوانید پیشنهادهای قابل مقایسه بگیرید.", color = Muted, fontSize = 12.sp)
        }
        item {
            Surface(color = Color.White, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text("چه چیزی نیاز دارید؟", color = Ink, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = title, onValueChange = onTitle, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثلاً: گوشی با حافظه ۲۵۶ گیگابایت") }, minLines = 3, shape = RoundedCornerShape(14.dp))
                    Text("دسته‌بندی، بودجه و محدوده جغرافیایی در نسخه کامل به این فرم افزوده می‌شود.", color = Muted, fontSize = 11.sp)
                    Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDark)) { Text("ثبت درخواست آزمایشی", fontWeight = FontWeight.Bold) }
                    if (sent) {
                        Text("درخواست در همین نمونه ثبت شده است؛ برای دریافت پیشنهاد واقعی هنوز اتصال سرور لازم است.", color = TealDark, fontSize = 12.sp)
                        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("ثبت درخواست دیگر") }
                    }
                }
            }
        }
        item { TrustNote() }
    }
}

@Composable
private fun OffersScreen(onSelect: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("پیشنهادهای فروشندگان", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("نمونه مقایسه برای نمایش طراحی؛ پیشنهادها واقعی نیستند.", color = Muted, fontSize = 12.sp)
        }
        items(listOf(
            Triple("فروشگاه تکنولوژی", "۱۲٬۸۰۰٬۰۰۰ تومان", "ارسال: ۲ روز کاری"),
            Triple("اسمارت‌شاپ", "۱۳٬۱۰۰٬۰۰۰ تومان", "ارسال: فردا"),
            Triple("دیجیتال‌پلاس", "۱۲٬۶۵۰٬۰۰۰ تومان", "تحویل حضوری")
        )) { offer ->
            Surface(color = Color.White, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8F6F5)), contentAlignment = Alignment.Center) {
                            Text("✓", color = TealDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(offer.first, color = Ink, fontWeight = FontWeight.Bold)
                            Text("★  ۴٫۸  ·  فروشنده نمونه", color = Muted, fontSize = 10.sp)
                        }
                        Text(offer.second, color = TealDark, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                    HorizontalDivider(color = Line)
                    Text(offer.third, color = Muted, fontSize = 11.sp)
                    OutlinedButton(onClick = onSelect, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("انتخاب آزمایشی پیشنهاد") }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(cartCount: Int, onOrders: () -> Unit, onNotice: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(DeepNavy, Navy, Color(0xFF07556A)))).padding(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("حساب کاربری", color = Teal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("به آرمین مارکت خوش آمدید", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                    Text("ورود، ثبت‌نام و پروفایل واقعی در مرحله اتصال سرویس پیاده‌سازی می‌شود.", color = Color(0xFFC7D9E2), fontSize = 12.sp)
                    OutlinedButton(onClick = { onNotice("ورود و ثبت‌نام هنوز به سرویس احراز هویت متصل نیست.") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("ورود / ثبت‌نام") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("سبد آزمایشی", cartCount.toString(), Modifier.weight(1f))
                StatCard("درخواست‌ها", "۰", Modifier.weight(1f))
                StatCard("پیام‌ها", "۰", Modifier.weight(1f))
            }
        }
        items(listOf("اطلاعات حساب کاربری", "درخواست‌های من", "پیشنهادهای من", "پیام‌ها و اعلان‌ها", "تنظیمات و حریم خصوصی", "راهنما و پشتیبانی")) { label ->
            Surface(Modifier.fillMaxWidth().clickable {
                if (label == "پیشنهادهای من" || label == "درخواست‌های من") onOrders()
                else onNotice("$label در نسخه کامل پس از اتصال سرویس فعال می‌شود.")
            }, color = Color.White, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Line)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("←", color = TealDark, fontSize = 18.sp)
                }
            }
        }
        item { TrustNote() }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = TealDark, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TrustNote() {
    Surface(color = Color(0xFFE8F5F5), shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✓", color = TealDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Column {
                Text("انتخاب آگاهانه، معامله مطمئن‌تر", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("پیش از معامله، قیمت و شرایط فروشنده را بررسی کنید.", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("⌕", color = TealDark, fontSize = 32.sp)
            Text(title, color = Ink, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProductDetailSheet(product: MarketProduct, onDismiss: () -> Unit, onAdd: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color.White,
        title = { Text(product.name, color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.fillMaxWidth().height(115.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFE6F5F8), Color(0xFFD6E7F1)))), contentAlignment = Alignment.Center) {
                    Text("◇", color = TealDark, fontSize = 52.sp)
                }
                Text(product.price, color = TealDark, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("دسته: " + product.category, color = Muted, fontSize = 12.sp)
                Text("فروشنده: " + product.seller, color = Muted, fontSize = 12.sp)
                Text("این مورد نمونه نمایشی است؛ موجودی، مشخصات و فروشنده واقعی تأیید نشده‌اند.", color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = { Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = TealDark)) { Text("افزودن آزمایشی به سبد") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}
