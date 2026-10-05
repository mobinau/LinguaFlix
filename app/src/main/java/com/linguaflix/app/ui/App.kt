@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.linguaflix.app.ui
import android.speech.tts.TextToSpeech
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import com.linguaflix.app.BuildConfig
import com.linguaflix.app.data.*
import com.linguaflix.app.domain.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
internal val Purple = Color(0xFFA78BFA)
internal val Teal = Color(0xFF2DD4BF)
internal val Navy = Color(0xFF0B1120)
internal val levels = listOf("A1","A2","B1","B2","C1","C2")
private val titles = listOf("خانه","جستجو","کتابخانه","لغات من","پیشرفت","تنظیمات")
private val icons = listOf(Icons.Rounded.Home,Icons.Rounded.Search,Icons.AutoMirrored.Rounded.LibraryBooks,Icons.Rounded.Bookmark,Icons.Rounded.Insights,Icons.Rounded.Settings)
@Composable fun LinguaFlix(vm: AppViewModel) {
 val state by vm.state.collectAsStateWithLifecycle()
 val settings by vm.settings.collectAsStateWithLifecycle()
 val lessons by vm.lessons.collectAsStateWithLifecycle()
 val words by vm.words.collectAsStateWithLifecycle()
 val attempts by vm.attempts.collectAsStateWithLifecycle()
 val sessions by vm.sessions.collectAsStateWithLifecycle()
 var tab by rememberSaveable { mutableIntStateOf(0) }
 val dark = settings.theme == "dark" || (settings.theme=="system" && isSystemInDarkTheme())
 val scheme = if(dark) darkColorScheme(primary=Purple,secondary=Teal,background=Navy,surface=Color(0xFF131D32),surfaceContainer=Color(0xFF1B2640)) else lightColorScheme(primary=Color(0xFF7252CC),secondary=Color(0xFF00897B),background=Color(0xFFF5F7FC),surface=Color.White,surfaceContainer=Color(0xFFEBEFF9))
 val density=LocalDensity.current
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalDensity provides Density(density.density,density.fontScale*settings.fontScale)) {
  MaterialTheme(colorScheme=scheme) {
   val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
   DisposableEffect(owner,state.lesson?.id,state.index) {
    var started: Long?=null
    val observer=LifecycleEventObserver { _,event ->
     if(event==Lifecycle.Event.ON_RESUME && state.index != null) started=SystemClock.elapsedRealtime()
     if(event==Lifecycle.Event.ON_PAUSE) { started?.let { vm.recordTime((SystemClock.elapsedRealtime()-it)/1000) }; started=null }
    }
    if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && state.index!=null) started=SystemClock.elapsedRealtime()
    owner.lifecycle.addObserver(observer)
    onDispose { owner.lifecycle.removeObserver(observer); started?.let { vm.recordTime((SystemClock.elapsedRealtime()-it)/1000) } }
   }
   val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::import) }
   val importAction = { importer.launch(arrayOf("application/x-subrip","application/srt","text/plain","application/octet-stream")) }
   Surface(Modifier.fillMaxSize(),color=scheme.background) {
    when {
     !state.ready -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
     !settings.onboarded -> Onboarding(settings) { vm.saveSettings(it.copy(onboarded=true)) }
     else -> Scaffold(containerColor=scheme.background,bottomBar={ if(state.lesson==null) NavigationBar { titles.forEachIndexed { i,title -> NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(icons[i],title)},label={Text(title,maxLines=1,fontSize=10.sp)}) } } }) { padding ->
      Box(Modifier.fillMaxSize().padding(padding)) {
       if(state.lesson!=null) {
        BackHandler { if(state.exerciseIndex!=null) vm.stopExercises() else if(state.index!=null) vm.openLesson(state.lesson!!) else vm.closeLesson() }
        when { state.exerciseIndex!=null -> ExerciseScreen(state,vm)
         state.index!=null -> TeachingScreen(state,settings,vm)
         else -> DialogueScreen(state,vm) }
       } else when(tab) {
        0 -> HomeScreen(settings,lessons,words,sessions,vm,{tab=it},importAction)
        1 -> SearchScreen(lessons,vm,importAction)
        2 -> LibraryScreen(lessons,vm)
        3 -> WordsScreen(words,vm)
        4 -> ProgressScreen(settings,lessons,words,attempts,sessions)
        5 -> SettingsScreen(settings,state,vm)
       }
       if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
      }
     }
    }
    state.error?.let { message -> AlertDialog(onDismissRequest=vm::dismissError,title={Text("عملیات انجام نشد")},text={Text(message)},confirmButton={TextButton(onClick=vm::dismissError){Text("متوجه شدم")}}) }
   }
  }
 }
}
@Composable internal fun English(text: String, modifier: Modifier=Modifier, large: Boolean=false) {
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text(text,modifier.fillMaxWidth(),style=if(large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,textAlign=TextAlign.Start) }
}
@Composable internal fun Heading(title: String, subtitle: String?=null) { Column(Modifier.padding(vertical=8.dp)) { Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold); subtitle?.let { Text(it,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=6.dp)) } } }
@Composable internal fun Panel(modifier: Modifier=Modifier, content: @Composable ColumnScope.()->Unit) { Card(modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content) } }
@Composable internal fun Empty(message: String) { Panel { Icon(Icons.AutoMirrored.Rounded.MenuBook,null,tint=MaterialTheme.colorScheme.primary); Text(message) } }
@Composable private fun Onboarding(s: Settings, finish: (Settings)->Unit) {
 var level by rememberSaveable { mutableStateOf(s.level) }
 val transition=rememberInfiniteTransition(label="logo")
 val glow by transition.animateFloat(.65f,1f,infiniteRepeatable(tween(1700),RepeatMode.Reverse),label="glow")
 Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(24.dp)) {
  Spacer(Modifier.height(24.dp)); Box(Modifier.size(130.dp).background(Brush.linearGradient(listOf(Purple.copy(alpha=glow),Teal.copy(alpha=glow))),RoundedCornerShape(36.dp)),contentAlignment=Alignment.Center) { Icon(Icons.Rounded.PlayArrow,null,Modifier.size(80.dp),tint=Navy) }
  Text("LinguaFlix",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold)
  Text("هر دیالوگ، یک قدم به انگلیسی روان‌تر",style=MaterialTheme.typography.titleLarge,textAlign=TextAlign.Center)
  Text("با دیالوگ‌های آموزشی و زیرنویس‌های خودت، لغت، گرامر و مکالمه را در کنار هم یاد بگیر.",textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Panel { Text("سطح زبانت را انتخاب کن",fontWeight=FontWeight.Bold); FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { levels.forEach { FilterChip(level==it,{level=it},label={Text(it)}) } }; Text("هر زمان بخواهی در تنظیمات قابل تغییر است.",style=MaterialTheme.typography.bodySmall) }
  Button(onClick={finish(s.copy(level=level))},modifier=Modifier.fillMaxWidth().height(54.dp)) { Text("شروع یادگیری"); Icon(Icons.AutoMirrored.Rounded.ArrowForward,null) }
  TextButton(onClick={finish(s)}) { Text("فعلاً رد کن") }
 }
}
@Composable internal fun LessonCard(lesson: Lesson, vm: AppViewModel, remove: Boolean=false) {
 Card(onClick={vm.openLesson(lesson)},shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),modifier=Modifier.fillMaxWidth()) {
  Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
   Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=.13f),RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center) { Icon(Icons.Rounded.Movie,null,tint=MaterialTheme.colorScheme.primary) }
   Column(Modifier.weight(1f).padding(horizontal=14.dp)) { English(lesson.title); Text("${lesson.category} · ${lesson.level}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant); if(lesson.completed) Text("تکمیل‌شده",color=MaterialTheme.colorScheme.secondary,style=MaterialTheme.typography.labelSmall) }
   IconButton(onClick={vm.favorite(lesson)}) { Icon(if(lesson.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"علاقه‌مندی",tint=MaterialTheme.colorScheme.primary) }
   if(remove) IconButton(onClick={vm.toggleSaved(lesson)}) { Icon(Icons.Rounded.BookmarkRemove,"حذف از کتابخانه") }
  }
 }
}
@Composable internal fun HomeScreen(s: Settings,lessons: List<Lesson>,words: List<Word>,sessions: List<StudySession>,vm: AppViewModel,navigate: (Int)->Unit,importFile: ()->Unit) {
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
  item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("LINGUA / FLIX",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold,letterSpacing=3.sp); Heading("سلام، وقت یادگیریه 👋","امروز با یک دیالوگ تازه شروع کن") }; AssistChip(onClick={navigate(5)},label={Text(s.level)}) } }
  item { Card(onClick={navigate(1)},shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) { Icon(Icons.Rounded.Search,null); Text("جستجوی فیلم، سریال یا درس…") } } }
  item { Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF5C3DBB),Color(0xFF263B73))),RoundedCornerShape(28.dp)).padding(24.dp)) { Column(verticalArrangement=Arrangement.spacedBy(12.dp)) { Text("انگلیسی، در دل داستان",color=Color.White,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text("یک جمله بشنو. معنایش را کشف کن. در زندگی استفاده‌اش کن.",color=Color(0xFFE1DCFA)); Button(onClick={lessons.firstOrNull()?.let(vm::openLesson)},enabled=lessons.isNotEmpty(),colors=ButtonDefaults.buttonColors(containerColor=Teal,contentColor=Navy)) { Text("کشف درس‌های نمونه"); Icon(Icons.Rounded.PlayArrow,null) }; Text("۴ درس نمونه ساختگی • بدون نیاز به اینترنت",color=Color(0xFFD8D1F4),style=MaterialTheme.typography.labelSmall) } } }
  item { val minutes=sessions.filter { it.timestamp>=todayStart() }.sumOf { it.seconds }/60; Panel { Row { Text("هدف امروز",Modifier.weight(1f),fontWeight=FontWeight.Bold); Text("$minutes / ${s.dailyGoal} دقیقه",color=MaterialTheme.colorScheme.secondary) }; LinearProgressIndicator(progress={ (minutes.toFloat()/s.dailyGoal).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth()); Text("قدم‌های کوچک، پیشرفت ماندگار",style=MaterialTheme.typography.bodySmall) } }
  val recent=lessons.firstOrNull { it.lastDialogue!=0L && !it.completed }
  if(recent!=null) { item { Text("ادامه یادگیری",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }; item { LessonCard(recent,vm) } }
  item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("برای تو",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold); TextButton(onClick={navigate(1)}) { Text("همه درس‌ها") } } }
  items(lessons.filter { it.category!="واردشده" }.take(4),key={it.id}) { LessonCard(it,vm) }
  item { Text("مسیرهای یادگیری",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold); FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("مکالمه روزمره","اصطلاحات","گرامر","محیط کار").forEach { AssistChip(onClick={navigate(1)},label={Text(it)}) } } }
  item { Panel { Text("دفترچه لغاتت",fontWeight=FontWeight.Bold); Text("${words.size} عبارت ذخیره‌شده برای مرور بعدی"); OutlinedButton(onClick={navigate(3)}) { Text("مرور لغات") }; OutlinedButton(onClick=importFile) { Icon(Icons.Rounded.UploadFile,null); Text("واردکردن زیرنویس SRT") } } }
 }
}
internal fun todayStart() = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()