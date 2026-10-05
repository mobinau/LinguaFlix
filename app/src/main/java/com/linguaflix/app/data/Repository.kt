package com.linguaflix.app.data
import androidx.room.withTransaction
import com.linguaflix.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class Repository(val db: LinguaDatabase, val ai: AiService) {
 val dao = db.dao()
 suspend fun seed() = db.withTransaction {
  if (dao.lessonCount() == 0) {
   val categories = listOf("مکالمه روزمره","کمک و درخواست","محیط کار","تصمیم‌گیری")
   DemoContent.lines.forEachIndexed { index, line -> val id = dao.addLesson(Lesson(title=listOf("A little anticipation","A helping hand","The end of a good day","Take your chance")[index],category=categories[index],level=listOf("A2","A2","B1","B2")[index])); dao.addDialogues(listOf(Dialogue(lessonId=id,position=0,startMs=0,endMs=4000,text=line))) }
  }
 }
 suspend fun import(title: String, input: String): Long {
  val lines = withContext(Dispatchers.Default) { SrtParser.parse(input) }
  return db.withTransaction { val id = dao.addLesson(Lesson(title=title.ifBlank { "زیرنویس من" },saved=true)); dao.addDialogues(lines.mapIndexed { i,s -> Dialogue(lessonId=id,position=i,startMs=s.startMs,endMs=s.endMs,text=s.text) }); id }
 }
 suspend fun teach(dialogue: Dialogue, level: String): Teaching {
  val current=dao.dialogue(dialogue.id) ?: error("دیالوگ حذف شده است."); if (current.teaching != null && current.teachingLevel == level) return withContext(Dispatchers.Default) { Teaching.parse(current.teaching) }
  val result = ai.teach(dialogue.text,level)
  dao.updateDialogue(dialogue.copy(teaching=result.json(),teachingLevel=level))
  return result
 }
 suspend fun clearAll() = db.withTransaction { dao.clearAttempts(); dao.clearSessions(); dao.clearWords(); dao.clearLessons() }
}