@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.linguaflix.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import com.linguaflix.app.BuildConfig
import com.linguaflix.app.data.Settings
@Composable internal fun SettingsScreen(s: Settings,state: AppState,vm: AppViewModel) {
 var reset by remember { mutableStateOf(false) }; var about by remember { mutableStateOf(false) }
 var endpoint by remember { mutableStateOf("") }; var key by remember { mutableStateOf("") }; var model by remember { mutableStateOf("") }
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item { Heading("تنظیمات","یادگیری، به شیوه تو") }
  item { Panel { Text("ظاهر برنامه",fontWeight=FontWeight.Bold); FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("system" to "سیستم","dark" to "تاریک","light" to "روشن").forEach { (value,label) -> FilterChip(s.theme==value,{vm.saveSettings(s.copy(theme=value))},label={Text(label)}) } }; Text("اندازه متن: ${(s.fontScale*100).toInt()}٪"); var size by remember(s.fontScale) { mutableFloatStateOf(s.fontScale) }; Slider(size,{size=it},onValueChangeFinished={vm.saveSettings(s.copy(fontScale=size))},valueRange=.85f..1.4f) } }
  item { Panel { Text("سطح زبان",fontWeight=FontWeight.Bold); FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { levels.forEach { level -> FilterChip(s.level==level,{vm.saveSettings(s.copy(level=level))},label={Text(level)}) } }; Text("هدف روزانه: ${s.dailyGoal} دقیقه"); var goal by remember(s.dailyGoal) { mutableFloatStateOf(s.dailyGoal.toFloat()) }; Slider(goal,{goal=it},onValueChangeFinished={vm.saveSettings(s.copy(dailyGoal=goal.toInt()))},valueRange=5f..60f,steps=10) } }
  item { Panel { Text("صدا و تلفظ",fontWeight=FontWeight.Bold); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { FilterChip(s.accent=="US",{vm.saveSettings(s.copy(accent="US"))},label={Text("آمریکایی")}); FilterChip(s.accent=="GB",{vm.saveSettings(s.copy(accent="GB"))},label={Text("بریتانیایی")}) }; Row(verticalAlignment=Alignment.CenterVertically) { Text("صدای آموزشی",Modifier.weight(1f)); Switch(s.audio,{vm.saveSettings(s.copy(audio=it))}) }; Text("پخش صوت به موتور TTS و صدای انگلیسی نصب‌شده روی گوشی نیاز دارد.",style=MaterialTheme.typography.bodySmall) } }
  if(BuildConfig.DEBUG) item { Accordion("اتصال API • ویژه توسعه") { Text(if(state.apiConnected) "API متصل است؛ زیرنویس انتخاب‌شده برای سرویس ارسال می‌شود." else "حالت Demo فعال است. کلید فقط در حافظه این نشست نگهداری می‌شود."); CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Column(verticalArrangement=Arrangement.spacedBy(10.dp)) { OutlinedTextField(endpoint,{endpoint=it},label={Text("HTTPS chat/completions endpoint")},singleLine=true,modifier=Modifier.fillMaxWidth()); OutlinedTextField(key,{key=it},label={Text("API key (session only)")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth()); OutlinedTextField(model,{model=it},label={Text("Model")},singleLine=true,modifier=Modifier.fillMaxWidth()) } }; Text("با اتصال، متن دیالوگ و پرسش به ارائه‌دهنده انتخابی فرستاده می‌شود. کلید در APK یا فایل تنظیمات ذخیره نمی‌شود.",style=MaterialTheme.typography.bodySmall); Button(onClick={vm.configure(endpoint,key,model);key=""},enabled=endpoint.isNotBlank()&&key.isNotBlank()&&model.isNotBlank()&&!state.busy) { Text("اتصال برای این نشست") }; if(state.apiConnected) TextButton(onClick=vm::disconnect) { Text("بازگشت به حالت Demo") } } }
  item { Panel { Text("داده‌های روی گوشی",fontWeight=FontWeight.Bold); TextButton(onClick=vm::clearCache) { Text("پاک‌کردن کش آموزش‌های تولیدشده") }; TextButton(onClick={reset=true}) { Text("پاک‌کردن درس‌ها، لغات و پیشرفت",color=MaterialTheme.colorScheme.error) }; TextButton(onClick={about=true}) { Text("درباره LinguaFlix") } } }
 }
 if(reset) AlertDialog(onDismissRequest={reset=false},title={Text("پاک‌کردن اطلاعات؟")},text={Text("تمام زیرنویس‌ها، درس‌های ذخیره‌شده، لغات و پیشرفت حذف می‌شوند. درس‌های نمونه دوباره اضافه خواهند شد. تنظیمات حفظ می‌شوند.")},confirmButton={TextButton(onClick={vm.reset();reset=false}){Text("پاک‌کردن")}},dismissButton={TextButton(onClick={reset=false}){Text("انصراف")}})
 if(about) AlertDialog(onDismissRequest={about=false},title={Text("LinguaFlix 1.0")},text={Text("یادگیری انگلیسی با دیالوگ‌ها. داده‌ها روی گوشی ذخیره می‌شوند. محتوای نمونه ساختگی است. جستجو محلی است؛ دانلود فیلم یا زیرنویس انجام نمی‌شود. برای انتشار نسخه متصل به AI از واسط امن استفاده کنید.")},confirmButton={TextButton(onClick={about=false}){Text("بستن")}})
}