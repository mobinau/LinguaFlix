package com.linguaflix.app
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.linguaflix.app.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class PersistenceTest {
 @Test fun roomPersistsLessonWordsAttemptsAndCascadesDialogues() = runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="test-${System.nanoTime()}.db"
  var db=Room.databaseBuilder(context,LinguaDatabase::class.java,name).build()
  try {
   val id=db.dao().addLesson(Lesson(title="Test",saved=true)); db.dao().addDialogues(listOf(Dialogue(lessonId=id,position=0,startMs=1000,endMs=2000,text="Hello")))
   db.dao().saveWord(Word("hello","سلام","Hello!")); db.dao().attempt(Attempt(lessonId=id,question="q",answer="a",correctAnswer="a",correct=true)); db.close()
   db=Room.databaseBuilder(context,LinguaDatabase::class.java,name).build()
   assertEquals("Test",db.dao().lessons().first().single().title); assertEquals(1000L,db.dao().dialogues(id).single().startMs); assertEquals(1,db.dao().words().first().size); assertTrue(db.dao().attempts().first().single().correct)
   db.dao().deleteLesson(id); assertTrue(db.dao().dialogues(id).isEmpty())
  } finally { db.close(); context.deleteDatabase(name) }
 }
 @Test fun datastoreRetainsSettings() = runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext; val store=SettingsStore(context); val original=store.settings.first()
  try { val expected=original.copy(level="C1",theme="dark",fontScale=1.2f,dailyGoal=25,accent="GB",audio=false); store.save(expected); assertEquals(expected,SettingsStore(context).settings.first()) } finally { store.save(original) }
 }
}