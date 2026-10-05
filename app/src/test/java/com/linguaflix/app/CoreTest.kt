package com.linguaflix.app
import com.linguaflix.app.domain.*
import org.junit.Assert.*
import org.junit.Test
class CoreTest {
 @Test fun parsesBomMultilineAndPreservesMilliseconds() { val result=SrtParser.parse("\uFEFF1\r\n00:00:01,250 --> 00:00:03,500\r\n<i>Hello</i>\r\nthere!\r\n\r\n2\r\n00:00:04,000 --> 00:00:05,000\r\nBye"); assertEquals(2,result.size); assertEquals(1250L,result[0].startMs); assertEquals(3500L,result[0].endMs); assertEquals("Hello there!",result[0].text) }
 @Test fun sortsChronologically() { val result=SrtParser.parse("2\n00:00:04,000 --> 00:00:05,000\nLater\n\n1\n00:00:01.000 --> 00:00:02.000\nEarlier"); assertEquals("Earlier",result.first().text) }
 @Test(expected=IllegalArgumentException::class) fun rejectsInvalidTimeRange() { SrtParser.parse("1\n00:00:03,000 --> 00:00:01,000\nHello") }
 @Test(expected=IllegalArgumentException::class) fun rejectsEmptySubtitle() { SrtParser.parse("this is not a subtitle") }
 @Test(expected=IllegalArgumentException::class) fun rejectsInvalidMinutes() { SrtParser.parse("1\n00:61:00,000 --> 01:02:00,000\nHello") }
 @Test fun ignoresMalformedBlocks() { assertEquals(1,SrtParser.parse("bad block\n\n1\n00:00:01,000 --> 00:00:02,000\nHello").size) }
 @Test fun sampleTeachingRoundTripAndExercises() { DemoContent.lines.forEach { line -> levels().forEach { level -> val t=Teaching.parse(DemoContent.lesson(line,level).json()); assertTrue(t.demo); assertEquals(5,t.exercises.size); assertTrue(t.translation.isNotBlank()); t.exercises.forEach { e -> assertTrue(e.options.isEmpty() || e.answer in e.options) } } } }
 @Test(expected=IllegalArgumentException::class) fun demoNeverInventsImportedTranslation() { DemoContent.lesson("An unknown imported sentence","B1") }
 @Test fun answersNormalizePunctuationCaseAndWhitespace() { assertTrue(AnswerChecker.correct("  I'm   HERE! ","I'm here.")); assertTrue(AnswerChecker.correct("I’d try.","I'd try")); assertFalse(AnswerChecker.correct("I am here","I was here")) }
 @Test(expected=Exception::class) fun rejectsIncompleteApiPayload() { Teaching.parse("{\"translation\":\"hello\"}") }
 private fun levels()=listOf("A1","A2","B1","B2","C1","C2")
}