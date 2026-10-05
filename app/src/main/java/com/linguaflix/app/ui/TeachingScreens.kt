@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.linguaflix.app.ui
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.linguaflix.app.data.*
import java.util.Locale
@Composable internal fun Accordion(title: String,content: @Composable ColumnScope.()->Unit) {
 var expanded by rememberSaveable(title) { mutableStateOf(title=="ترجمه و مفهوم") }
 Panel { Row(Modifier.fillMaxWidth().clickable { expanded=!expanded },verticalAlignment=Alignment.CenterVertically) { Text(title,Modifier.weight(1f),fontWeight=FontWeight.Bold); Icon(if(expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,if(expanded) "بستن" else "بازکردن") }; AnimatedVisibility(expanded) { Column(verticalArrangement=Arrangement.spacedBy(10.dp),content=content) } }
}
@Composable internal fun TeachingScreen(state: AppState,s: Settings,vm: AppViewModel) {
 val dialogue=state.dialogues[state.index!!]; val teaching=state.teaching
 var selectedWord by remember(dialogue.id) { mutableStateOf<String?>(null) }; var question by rememberSaveable(dialogue.id) { mutableStateOf("") }
 val context=LocalContext.current
 var tts by remember { mutableStateOf<TextToSpeech?>(null) }; var ttsReady by remember { mutableStateOf(false) }; var audioError by remember { mutableStateOf(false) }
 DisposableEffect(context,s.accent) {
  var engine: TextToSpeech?=null
  engine=TextToSpeech(context) { status -> if(status==TextToSpeech.SUCCESS) { val available=engine?.setLanguage(if(s.accent=="GB") Locale.UK else Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED; ttsReady=available>=0 } else ttsReady=false }
  tts=engine
  onDispose { ttsReady=false; engine?.stop(); engine?.shutdown() }
 }
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item { TextButton(onClick={vm.openLesson(state.lesson!!)}) { Icon(Icons.AutoMirrored.Rounded.ArrowBack,null); Text("دیالوگ‌ها") }; Text("درس ${state.index+1} از ${state.dialogues.size} • ${s.level}",color=MaterialTheme.colorScheme.secondary) }
  item { Panel { Text("دیالوگ امروز",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge); CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { FlowRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) { dialogue.text.split(Regex("\\s+")).forEach { word -> Text(word,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.clickable { selectedWord=word.trim(' ',',','.', '?','!','"') }.padding(vertical=4.dp)) } } }; Text("برای بررسی معنی روی کلمه بزن",style=MaterialTheme.typography.bodySmall); if(s.audio) OutlinedButton(onClick={audioError=tts?.speak(dialogue.text,TextToSpeech.QUEUE_FLUSH,null,"dialogue") == TextToSpeech.ERROR},enabled=ttsReady) { Icon(Icons.AutoMirrored.Rounded.VolumeUp,null); Text(if(ttsReady) "شنیدن تلفظ" else "صدای انگلیسی در دسترس نیست") }; if(audioError) Text("پخش صوت انجام نشد.",color=MaterialTheme.colorScheme.error) } }
  if(teaching==null && !state.busy) item { Empty("آموزش این دیالوگ هنوز دریافت نشده است."); Button(onClick={vm.learn(state.index)}){Text("تلاش دوباره")} }
  if(teaching!=null) {
   item { Text(if(teaching.demo) "محتوای آموزشی نمونه • آفلاین" else "آموزش تولیدشده با هوش مصنوعی",color=MaterialTheme.colorScheme.secondary,style=MaterialTheme.typography.labelMedium) }
   item { Accordion("ترجمه و مفهوم") { Text(teaching.translation,style=MaterialTheme.typography.titleLarge); Text("ترجمه لفظی: ${teaching.literal}"); Text(teaching.context,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
   item { Accordion("واژه‌ها و عبارت‌ها") { teaching.words.forEach { w -> English(w.term,large=true); Text(w.meaning); English("${w.pronunciation} · ${w.part} · ${w.level}"); English(w.example); if(w.synonyms.isNotBlank()) English("Synonyms: ${w.synonyms}"); OutlinedButton(onClick={vm.saveWord(w)}) { Icon(Icons.Rounded.BookmarkAdd,null); Text("ذخیره در دفترچه لغات") }; HorizontalDivider() } } }
   item { Accordion("گرامر به زبان ساده") { Text(teaching.grammar) } }
   item { Accordion("اصطلاحات و زبان نیتیو") { Text(teaching.idioms) } }
   item { Accordion("در مکالمه استفاده کن") { Text(teaching.conversation) } }
   item { Panel { Text("نوبت توست",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold); Text("با ۵ تمرین، یادگیری‌ات را محک بزن."); Button(onClick=vm::startExercises,modifier=Modifier.fillMaxWidth()) { Text("شروع تمرین") } } }
   item { Accordion("از معلم بپرس") { OutlinedTextField(question,{question=it},label={Text("پرسش درباره این جمله")},modifier=Modifier.fillMaxWidth()); Button(onClick={vm.ask(question)},enabled=question.isNotBlank()&&!state.busy){Text("پرسیدن")}; state.teacherAnswer?.let { Text(it) } } }
  }
  item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick={vm.toggleSaved(state.lesson!!)},modifier=Modifier.weight(1f)) { Text(if(state.lesson!!.saved) "ذخیره‌شده ✓" else "ذخیره درس") }; Button(onClick=vm::complete,modifier=Modifier.weight(1f),enabled=teaching!=null) { Text(if(state.lesson!!.completed) "تکمیل‌شده ✓" else "تکمیل درس") } } }
  item { val indices=state.dialogues.indices.filter { state.selected.isEmpty()||state.dialogues[it].id in state.selected }; val position=indices.indexOf(state.index); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { OutlinedButton(onClick={vm.move(-1)},enabled=position>0&&!state.busy) { Text("قبلی") }; Button(onClick={vm.move(1)},enabled=position>=0&&position<indices.lastIndex&&!state.busy) { Text("دیالوگ بعدی") } } }
 }
 selectedWord?.let { word -> val vocabulary=teaching?.words?.firstOrNull { it.term.equals(word,true)||it.term.split(' ').any { token->token.equals(word,true) } }; AlertDialog(onDismissRequest={selectedWord=null},title={English(word,large=true)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { Text(vocabulary?.meaning ?: "این کلمه در واژه‌نامه این درس نیست. برای معنی دقیق در بافت جمله، از معلم بپرس."); vocabulary?.let { English(it.example) } }},confirmButton={TextButton(onClick={if(vocabulary!=null) vm.saveWord(vocabulary) else { question="معنی کلمه $word در این جمله چیست؟";vm.ask(question) }; selectedWord=null},enabled=vocabulary!=null||teaching!=null){Text(if(vocabulary!=null) "ذخیره عبارت" else "پرسش از معلم")}},dismissButton={TextButton(onClick={selectedWord=null}){Text("بستن")}}) }
}
@Composable internal fun ExerciseScreen(state: AppState,vm: AppViewModel) {
 val index=state.exerciseIndex!!; val e=state.teaching!!.exercises[index]; var typed by rememberSaveable(index) { mutableStateOf("") }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  TextButton(onClick=vm::stopExercises) { Text("بازگشت به درس") }; Heading("وقت تمرین","تمرین ${index+1} از ${state.teaching.exercises.size} • ${e.type}")
  LinearProgressIndicator(progress={ (index+1f)/state.teaching.exercises.size },modifier=Modifier.fillMaxWidth())
  Panel { Text(e.question,style=MaterialTheme.typography.titleLarge) }
  if(e.options.isEmpty()) { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { OutlinedTextField(typed,{typed=it},label={Text("Your translation")},enabled=!state.answered,modifier=Modifier.fillMaxWidth()) }; Button(onClick={vm.answer(typed)},enabled=typed.isNotBlank()&&!state.answered&&!state.busy,modifier=Modifier.fillMaxWidth()) { Text("بررسی پاسخ") } }
  else e.options.forEach { option -> OutlinedButton(onClick={vm.answer(option)},enabled=!state.answered&&!state.busy,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)) { Text(option,Modifier.padding(8.dp)) } }
  if(state.answered) { Panel { Text(if(state.correct) "آفرین، درست بود ✓" else "این بار درست نبود؛ با هم یاد می‌گیریم",color=if(state.correct) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold); Text("پاسخ نمونه: ${e.answer}"); Text(e.explanation); if(e.options.isEmpty()) Text("بررسی آفلاین با پاسخ نمونه مقایسه می‌کند؛ ترجمه‌های هم‌معنی ممکن است متفاوت باشند.",style=MaterialTheme.typography.bodySmall) }; Button(onClick=vm::nextExercise,modifier=Modifier.fillMaxWidth()) { Text(if(index==state.teaching.exercises.lastIndex) "پایان تمرین" else "تمرین بعدی") } }
 }
}