@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.linguaflix.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linguaflix.app.data.*
import com.linguaflix.app.domain.*
@Composable internal fun SearchScreen(lessons: List<Lesson>,vm: AppViewModel,importFile: ()->Unit) {
 var query by rememberSaveable { mutableStateOf("") }; var category by rememberSaveable { mutableStateOf("همه") }
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  item { Heading("داستان بعدی تو","جستجو در درس‌های نمونه و زیرنویس‌های واردشده") }
  item { OutlinedTextField(query,{query=it},label={Text("نام فیلم، سریال یا درس")},leadingIcon={Icon(Icons.Rounded.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth()) }
  item { Button(onClick=importFile,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Rounded.UploadFile,null); Text("واردکردن زیرنویس انگلیسی") }; Text("فایل UTF-8 با فرمت SRT؛ حداکثر ۴ مگابایت",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp)) }
  item { FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { (listOf("همه")+lessons.map { it.category }.distinct()).forEach { FilterChip(category==it,{category=it},label={Text(it)}) } } }
  val filtered=lessons.filter { (category=="همه"||it.category==category) && (it.title.contains(query,true)||it.category.contains(query,true)) }
  if(filtered.isEmpty()) item { Empty("درسی پیدا نشد. زیرنویس خودت را وارد کن.") }
  items(filtered,key={it.id}) { LessonCard(it,vm) }
 }
}
@Composable internal fun LibraryScreen(lessons: List<Lesson>,vm: AppViewModel) {
 var query by rememberSaveable { mutableStateOf("") }; var filter by rememberSaveable { mutableStateOf("همه") }; var deleting by remember { mutableStateOf<Lesson?>(null) }
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  item { Heading("کتابخانه من","داستان‌هایی که برای یادگیری نگه داشته‌ای") }
  item { OutlinedTextField(query,{query=it},label={Text("جستجو در کتابخانه")},modifier=Modifier.fillMaxWidth(),singleLine=true) }
  item { FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("همه","نیمه‌تمام","تکمیل‌شده","علاقه‌مندی").forEach { FilterChip(filter==it,{filter=it},label={Text(it)}) } } }
  val filtered=lessons.filter { (if(filter=="علاقه‌مندی") it.favorite else it.saved) && it.title.contains(query,true) && (filter!="نیمه‌تمام"||!it.completed) && (filter!="تکمیل‌شده"||it.completed) }
  if(filtered.isEmpty()) item { Empty("هنوز درسی در این بخش نیست. در صفحه آموزش، ذخیره درس را بزن.") }
  items(filtered,key={it.id}) { lesson -> Column { LessonCard(lesson,vm,remove=lesson.saved); if(lesson.category=="واردشده") TextButton(onClick={deleting=lesson}) { Text("حذف زیرنویس از گوشی") } } }
 }
 deleting?.let { lesson -> AlertDialog(onDismissRequest={deleting=null},title={Text("حذف زیرنویس؟")},text={Text("دیالوگ‌ها و آموزش ذخیره‌شده این زیرنویس حذف می‌شوند.")},confirmButton={TextButton(onClick={vm.delete(lesson);deleting=null}){Text("حذف")}},dismissButton={TextButton(onClick={deleting=null}){Text("انصراف")}}) }
}
@Composable internal fun DialogueScreen(state: AppState,vm: AppViewModel) {
 var editing by remember { mutableStateOf<Dialogue?>(null) }; var draft by remember { mutableStateOf("") }
 val lesson=state.lesson!!
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item { TextButton(onClick=vm::closeLesson) { Icon(Icons.AutoMirrored.Rounded.ArrowBack,null); Text("بازگشت") }; Heading(lesson.title,"${state.dialogues.size} دیالوگ • انتخاب، ویرایش و یادگیری") }
  item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick={vm.toggleSaved(lesson)}) { Text(if(lesson.saved) "حذف از کتابخانه" else "ذخیره درس") }; if(lesson.lastDialogue!=0L) TextButton(onClick={vm.learn(state.dialogues.indexOfFirst { it.id==lesson.lastDialogue }.coerceAtLeast(0))}) { Text("ادامه درس") } } }
  if(state.selected.isNotEmpty()) item { Button(onClick={vm.learn(state.dialogues.indexOfFirst { it.id in state.selected })},modifier=Modifier.fillMaxWidth()) { Text("یادگیری ${state.selected.size} دیالوگ انتخاب‌شده") } }
  items(state.dialogues,key={it.id}) { d -> Panel { Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(d.id in state.selected,{vm.select(d.id)}); Text("${SrtParser.timestamp(d.startMs)} — ${SrtParser.timestamp(d.endMs)}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary); Spacer(Modifier.weight(1f)); IconButton(onClick={editing=d;draft=d.text}) { Icon(Icons.Rounded.Edit,"ویرایش") } }; English(d.text); TextButton(onClick={vm.learn(state.dialogues.indexOf(d))}) { Text("یادگیری این دیالوگ"); Icon(Icons.Rounded.PlayArrow,null) } } }
 }
 editing?.let { d -> AlertDialog(onDismissRequest={editing=null},title={Text("ویرایش دیالوگ")},text={OutlinedTextField(draft,{draft=it},modifier=Modifier.fillMaxWidth())},confirmButton={TextButton(onClick={vm.edit(d,draft);editing=null},enabled=draft.isNotBlank()){Text("ذخیره")}},dismissButton={TextButton(onClick={editing=null}){Text("انصراف")}}) }
}
@Composable internal fun WordsScreen(words: List<Word>,vm: AppViewModel) {
 var query by rememberSaveable { mutableStateOf("") }; var difficult by rememberSaveable { mutableStateOf(false) }; var reviewing by rememberSaveable { mutableStateOf(false) }; var reveal by rememberSaveable { mutableStateOf(false) }
 val due=words.filter { it.nextReview<=System.currentTimeMillis() }; val current=due.firstOrNull()
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  item { Heading("واژه‌های من","${words.size} عبارت در مسیر یادگیری تو") }
  item { OutlinedTextField(query,{query=it},label={Text("جستجوی لغت یا معنی")},modifier=Modifier.fillMaxWidth()); FilterChip(difficult,{difficult=!difficult},label={Text("فقط لغات دشوار")}) }
  item { Button(onClick={reviewing=!reviewing;reveal=false},enabled=due.isNotEmpty(),modifier=Modifier.fillMaxWidth()) { Text(if(reviewing) "بستن مرور" else "مرور ${due.size} لغت آماده") } }
  if(reviewing && current!=null) item { Panel { English(current.term,large=true); if(reveal) { Text(current.meaning); English(current.example); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Button(onClick={vm.review(current,true);reveal=false}) { Text("بلد بودم") }; OutlinedButton(onClick={vm.review(current,false);reveal=false}) { Text("دوباره تمرین کنم") } } } else TextButton(onClick={reveal=true}) { Text("نمایش معنی") } } }
  val filtered=words.filter { (!difficult||it.difficult)&&(it.term.contains(query,true)||it.meaning.contains(query)) }
  if(filtered.isEmpty()) item { Empty("لغتی پیدا نشد. عبارت‌های مهم را از صفحه آموزش ذخیره کن.") }
  items(filtered,key={it.term}) { w -> Panel { Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { English(w.term,large=true); Text(w.meaning) }; IconButton(onClick={vm.difficult(w)}) { Icon(if(w.difficult) Icons.Rounded.Star else Icons.Rounded.StarBorder,"علامت دشوار",tint=MaterialTheme.colorScheme.primary) }; IconButton(onClick={vm.removeWord(w)}) { Icon(Icons.Rounded.DeleteOutline,"حذف لغت") } }; English(w.pronunciation); English(w.example); Text("سطح ${w.level} • ${w.repetitions} مرور موفق",style=MaterialTheme.typography.bodySmall) } }
 }
}
@Composable internal fun ProgressScreen(s: Settings,lessons: List<Lesson>,words: List<Word>,attempts: List<Attempt>,sessions: List<StudySession>) {
 val correct=attempts.count { it.correct }; val percent=if(attempts.isEmpty()) 0 else correct*100/attempts.size
 val today=sessions.filter { it.timestamp>=todayStart() }.sumOf { it.seconds }/60
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
  item { Heading("داستان پیشرفت تو","هر قدم، از یادگیری واقعی خودت") }
  item { Panel { Icon(Icons.Rounded.Insights,null,tint=MaterialTheme.colorScheme.secondary,modifier=Modifier.size(36.dp)); Text("$percent٪",style=MaterialTheme.typography.displayMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary); Text("پاسخ صحیح در ${attempts.size} تمرین"); LinearProgressIndicator(progress={percent/100f},modifier=Modifier.fillMaxWidth()) } }
  item { listOf("درس تکمیل‌شده" to lessons.count { it.completed }.toString(),"لغت ذخیره‌شده" to words.size.toString(),"دقیقه یادگیری" to (sessions.sumOf { it.seconds }/60).toString(),"تمرین انجام‌شده" to attempts.size.toString()).forEach { (label,value) -> Panel(Modifier.padding(bottom=12.dp)) { Row { Text(value,Modifier.weight(1f),style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.primary); Text(label) } } } }
  item { Panel { Text("هدف روزانه",fontWeight=FontWeight.Bold); Text("$today از ${s.dailyGoal} دقیقه"); LinearProgressIndicator(progress={ (today.toFloat()/s.dailyGoal).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth()); Text("زمان فقط هنگام باز بودن صفحه آموزش و فعال بودن برنامه ثبت می‌شود.",style=MaterialTheme.typography.bodySmall) } }
  item { Text("مرور اشتباهات اخیر",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
  val mistakes=attempts.filter { !it.correct }.take(10)
  if(mistakes.isEmpty()) item { Empty("هنوز اشتباهی ثبت نشده است.") }
  items(mistakes,key={it.id}) { a -> Panel { Text(a.question); Text("پاسخ تو: ${a.answer}",color=MaterialTheme.colorScheme.error); Text("پاسخ نمونه: ${a.correctAnswer}",color=MaterialTheme.colorScheme.secondary) } }
 }
}