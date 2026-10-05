package com.linguaflix.app.ui
import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.*
import com.linguaflix.app.BuildConfig
import com.linguaflix.app.data.*
import com.linguaflix.app.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AppState(val ready: Boolean = false, val busy: Boolean = false, val error: String? = null, val lesson: Lesson? = null, val dialogues: List<Dialogue> = emptyList(), val selected: Set<Long> = emptySet(), val index: Int? = null, val teaching: Teaching? = null, val teacherAnswer: String? = null, val exerciseIndex: Int? = null, val answered: Boolean = false, val correct: Boolean = false, val apiConnected: Boolean = false)
class AppViewModel(app: Application) : AndroidViewModel(app) {
 private val repo = Repository(LinguaDatabase.create(app),AiService())
 private val store = SettingsStore(app)
 val settings = store.settings.stateIn(viewModelScope,SharingStarted.Eagerly,Settings())
 val lessons = repo.dao.lessons().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val words = repo.dao.words().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val attempts = repo.dao.attempts().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val sessions = repo.dao.sessions().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 private val _state = MutableStateFlow(AppState()); val state = _state.asStateFlow()
 private var teachingJob: Job? = null
 init { viewModelScope.launch { try { store.settings.first(); repo.seed(); _state.update { it.copy(ready=true) } } catch(e: Exception) { _state.update { it.copy(ready=true,error="راه‌اندازی پایگاه داده انجام نشد.") } } } }
 private fun work(block: suspend () -> Unit) { viewModelScope.launch { _state.update { it.copy(busy=true,error=null) }; try { block() } catch(e: CancellationException) { throw e } catch(e: Exception) { _state.update { it.copy(error=e.message ?: "عملیات انجام نشد.") } } finally { _state.update { it.copy(busy=false) } } } }
 fun dismissError() { _state.update { it.copy(error=null) } }
 fun saveSettings(value: Settings) = work { store.save(value) }
 fun openLesson(lesson: Lesson) = work { teachingJob?.cancel(); val dialogues=repo.dao.dialogues(lesson.id); _state.update { it.copy(lesson=lesson,dialogues=dialogues,index=null,teaching=null,exerciseIndex=null,selected=emptySet(),teacherAnswer=null) } }
 fun closeLesson() { teachingJob?.cancel(); _state.update { it.copy(lesson=null,index=null,teaching=null,exerciseIndex=null,busy=false) } }
 fun import(uri: Uri) = work {
  val context=getApplication<Application>(); val result = withContext(Dispatchers.IO) {
   val title=context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { if(it.moveToFirst()) it.getString(0).substringBeforeLast('.') else "زیرنویس من" } ?: "زیرنویس من"
   val input=context.contentResolver.openInputStream(uri)?.use { stream -> val output=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192); var total=0; while(true) { val read=stream.read(buffer); if(read<0) break; total+=read; require(total<=4_000_000) { "حداکثر اندازه زیرنویس ۴ مگابایت است." }; output.write(buffer,0,read) }; output.toString("UTF-8") } ?: error("خواندن فایل ممکن نیست.")
   title to input
  }
  val id=repo.import(result.first,result.second); val lesson=Lesson(id=id,title=result.first,saved=true); val dialogues=repo.dao.dialogues(id); _state.update { it.copy(lesson=lesson,dialogues=dialogues,index=null,selected=emptySet()) }
 }
 fun select(id: Long) { _state.update { it.copy(selected=if(id in it.selected) it.selected-id else it.selected+id) } }
 fun edit(dialogue: Dialogue, text: String) = work { require(text.isNotBlank()) { "متن دیالوگ خالی است." }; val edited=dialogue.copy(text=text.trim(),teaching=null,teachingLevel=null); repo.dao.updateDialogue(edited); _state.update { it.copy(dialogues=it.dialogues.map { d -> if(d.id==edited.id) edited else d }) } }
 fun learn(index: Int) {
  teachingJob?.cancel()
  val snapshot=_state.value; val d=snapshot.dialogues.getOrNull(index) ?: return
  _state.update { it.copy(index=index,teaching=null,busy=true,error=null,exerciseIndex=null,teacherAnswer=null) }
  teachingJob=viewModelScope.launch {
   try { val teaching=repo.teach(d,settings.value.level); val lesson=snapshot.lesson!!.copy(lastDialogue=d.id,updatedAt=System.currentTimeMillis()); repo.dao.updateLesson(lesson); _state.update { it.copy(teaching=teaching,lesson=lesson) } }
   catch(e: CancellationException) { throw e }
   catch(e: Exception) { _state.update { it.copy(error=e.message ?: "آموزش دریافت نشد.") } }
   finally { _state.update { it.copy(busy=false) } }
  }
 }
 fun move(delta: Int) { val s=_state.value; val indices = s.dialogues.indices.filter { s.selected.isEmpty() || s.dialogues[it].id in s.selected }; val pos=indices.indexOf(s.index); indices.getOrNull(pos+delta)?.let { learn(it) } }
 fun toggleSaved(lesson: Lesson) = work { val updated=lesson.copy(saved=!lesson.saved); repo.dao.updateLesson(updated); _state.update { if(it.lesson?.id==updated.id) it.copy(lesson=updated) else it } }
 fun favorite(lesson: Lesson) = work { repo.dao.updateLesson(lesson.copy(favorite=!lesson.favorite)) }
 fun delete(lesson: Lesson) = work { repo.dao.deleteLesson(lesson.id); if(_state.value.lesson?.id==lesson.id) closeLesson() }
 fun complete() = work { val lesson=_state.value.lesson ?: return@work; val updated=lesson.copy(completed=true,saved=true); repo.dao.updateLesson(updated); _state.update { it.copy(lesson=updated) } }
 fun saveWord(v: Vocabulary) = work { if(words.value.none { it.term.equals(v.term,true) }) repo.dao.saveWord(Word(v.term,v.meaning,v.example,v.pronunciation,v.part,v.level)) }
 fun removeWord(w: Word) = work { repo.dao.deleteWord(w.term) }
 fun difficult(w: Word) = work { repo.dao.saveWord(w.copy(difficult=!w.difficult)) }
 fun review(w: Word, remembered: Boolean) = work { val repetitions=if(remembered) w.repetitions+1 else 0; val days=if(remembered) (1L shl repetitions.coerceAtMost(5)) else 0L; repo.dao.saveWord(w.copy(repetitions=repetitions,difficult=!remembered,nextReview=System.currentTimeMillis()+if(remembered) days*86400000 else 600000)) }
 fun startExercises() { _state.update { it.copy(exerciseIndex=0,answered=false) } }
 fun answer(answer: String) = work { val s=_state.value; if(s.answered) return@work; val e=s.teaching!!.exercises[s.exerciseIndex!!]; val correct=AnswerChecker.correct(answer,e.answer); repo.dao.attempt(Attempt(lessonId=s.lesson!!.id,question=e.question,answer=answer,correctAnswer=e.answer,correct=correct)); _state.update { it.copy(answered=true,correct=correct) } }
 fun nextExercise() { val s=_state.value; val next=(s.exerciseIndex ?: 0)+1; if(next >= (s.teaching?.exercises?.size ?: 0)) { _state.update { it.copy(exerciseIndex=null,answered=false) } } else _state.update { it.copy(exerciseIndex=next,answered=false) } }
 fun stopExercises() { _state.update { it.copy(exerciseIndex=null) } }
 fun ask(question: String) = work { require(question.isNotBlank()); val s=_state.value; val answer=repo.ai.ask(s.dialogues[s.index!!].text,question,settings.value.level); _state.update { it.copy(teacherAnswer=answer) } }
 fun configure(endpoint: String,key: String,model: String) = work { check(BuildConfig.DEBUG) { "تنظیم مستقیم API فقط در نسخه توسعه فعال است." }; require(endpoint.startsWith("https://") && key.isNotBlank() && model.isNotBlank()) { "نشانی HTTPS، کلید و نام مدل لازم است." }; repo.ai.config=ApiConfig(endpoint.trim(),key.trim(),model.trim()); repo.dao.clearTeaching(); _state.update { it.copy(apiConnected=true) } }
 fun disconnect() = work { repo.ai.config=null; repo.dao.clearTeaching(); _state.update { it.copy(apiConnected=false) } }
 fun clearCache() = work { repo.dao.clearTeaching() }
 fun reset() = work { repo.clearAll(); repo.seed(); closeLesson() }
 fun recordTime(seconds: Long) { if(seconds in 1..3600) viewModelScope.launch { try { repo.dao.session(StudySession(seconds=seconds)) } catch (_: Exception) {} } }
}