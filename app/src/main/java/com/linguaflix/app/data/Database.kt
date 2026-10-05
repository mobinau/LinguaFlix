package com.linguaflix.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lessons")
data class Lesson(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val category: String = "واردشده", val level: String = "B1", val saved: Boolean = false, val favorite: Boolean = false, val completed: Boolean = false, val lastDialogue: Long = 0, val updatedAt: Long = System.currentTimeMillis())
@Entity(tableName = "dialogues", foreignKeys = [ForeignKey(entity = Lesson::class, parentColumns = ["id"], childColumns = ["lessonId"], onDelete = ForeignKey.CASCADE)], indices = [Index("lessonId")])
data class Dialogue(@PrimaryKey(autoGenerate = true) val id: Long = 0, val lessonId: Long, val position: Int, val startMs: Long, val endMs: Long, val text: String, val teaching: String? = null, val teachingLevel: String? = null)
@Entity(tableName = "words")
data class Word(@PrimaryKey val term: String, val meaning: String, val example: String, val pronunciation: String = "", val part: String = "", val level: String = "B1", val difficult: Boolean = false, val nextReview: Long = 0, val repetitions: Int = 0)
@Entity(tableName = "attempts", indices = [Index("lessonId")])
data class Attempt(@PrimaryKey(autoGenerate = true) val id: Long = 0, val lessonId: Long, val question: String, val answer: String, val correctAnswer: String, val correct: Boolean, val timestamp: Long = System.currentTimeMillis())
@Entity(tableName = "sessions")
data class StudySession(@PrimaryKey(autoGenerate = true) val id: Long = 0, val seconds: Long, val timestamp: Long = System.currentTimeMillis())
@Dao
interface LinguaDao {
 @Query("SELECT * FROM lessons ORDER BY updatedAt DESC") fun lessons(): Flow<List<Lesson>>
 @Query("SELECT * FROM words ORDER BY nextReview, term") fun words(): Flow<List<Word>>
 @Query("SELECT * FROM attempts ORDER BY timestamp DESC") fun attempts(): Flow<List<Attempt>>
 @Query("SELECT * FROM sessions") fun sessions(): Flow<List<StudySession>>
 @Query("SELECT * FROM dialogues WHERE lessonId=:lessonId ORDER BY position") suspend fun dialogues(lessonId: Long): List<Dialogue>
 @Query("SELECT * FROM dialogues WHERE id=:id") suspend fun dialogue(id: Long): Dialogue?
 @Query("SELECT COUNT(*) FROM lessons") suspend fun lessonCount(): Int
 @Insert suspend fun addLesson(lesson: Lesson): Long
 @Insert suspend fun addDialogues(dialogues: List<Dialogue>)
 @Update suspend fun updateLesson(lesson: Lesson)
 @Update suspend fun updateDialogue(dialogue: Dialogue)
 @Upsert suspend fun saveWord(word: Word)
 @Insert suspend fun attempt(attempt: Attempt)
 @Insert suspend fun session(session: StudySession)
 @Query("DELETE FROM lessons WHERE id=:id") suspend fun deleteLesson(id: Long)
 @Query("DELETE FROM words WHERE term=:term") suspend fun deleteWord(term: String)
 @Query("DELETE FROM attempts") suspend fun clearAttempts()
 @Query("DELETE FROM sessions") suspend fun clearSessions()
 @Query("DELETE FROM words") suspend fun clearWords()
 @Query("DELETE FROM lessons") suspend fun clearLessons()
 @Query("UPDATE dialogues SET teaching=NULL, teachingLevel=NULL") suspend fun clearTeaching()
}
@Database(entities = [Lesson::class, Dialogue::class, Word::class, Attempt::class, StudySession::class], version = 1, exportSchema = true)
abstract class LinguaDatabase : RoomDatabase() {
 abstract fun dao(): LinguaDao
 companion object { fun create(context: Context) = Room.databaseBuilder(context.applicationContext, LinguaDatabase::class.java, "linguaflix.db").build() }
}