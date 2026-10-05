package com.linguaflix.app.domain
import org.json.JSONArray
import org.json.JSONObject

data class Vocabulary(val term: String, val meaning: String, val pronunciation: String, val part: String, val example: String, val synonyms: String, val level: String)
data class Exercise(val type: String, val question: String, val options: List<String>, val answer: String, val explanation: String)
data class Teaching(val translation: String, val literal: String, val context: String, val words: List<Vocabulary>, val grammar: String, val idioms: String, val conversation: String, val exercises: List<Exercise>, val demo: Boolean) {
 fun json(): String = JSONObject().put("translation", translation).put("literal", literal).put("context", context).put("grammar", grammar).put("idioms", idioms).put("conversation", conversation).put("demo", demo).put("words", JSONArray().apply { words.forEach { put(JSONObject().put("term",it.term).put("meaning",it.meaning).put("pronunciation",it.pronunciation).put("part",it.part).put("example",it.example).put("synonyms",it.synonyms).put("level",it.level)) } }).put("exercises", JSONArray().apply { exercises.forEach { put(JSONObject().put("type",it.type).put("question",it.question).put("options",JSONArray(it.options)).put("answer",it.answer).put("explanation",it.explanation)) } }).toString()
 companion object {
  fun parse(raw: String): Teaching {
   val o = JSONObject(raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
   fun required(key: String) = o.getString(key).also { require(it.isNotBlank()) { "پاسخ آموزشی ناقص است." } }
   val wa = o.getJSONArray("words"); val ea = o.getJSONArray("exercises")
   val words = (0 until wa.length()).map { wa.getJSONObject(it).run { Vocabulary(getString("term"),getString("meaning"),optString("pronunciation"),optString("part"),getString("example"),optString("synonyms"),optString("level","B1")) } }
   val exercises = (0 until ea.length()).map { ea.getJSONObject(it).run { val a = getJSONArray("options"); Exercise(getString("type"),getString("question"),(0 until a.length()).map { n -> a.getString(n) },getString("answer"),getString("explanation")) } }
   require(exercises.isNotEmpty() && words.isNotEmpty()) { "پاسخ آموزشی فاقد لغات یا تمرین است." }
   exercises.forEach { require(it.answer.isNotBlank() && (it.options.isEmpty() || it.answer in it.options)) { "پاسخ تمرین نامعتبر است." } }
   return Teaching(required("translation"),o.optString("literal"),required("context"),words,required("grammar"),required("idioms"),required("conversation"),exercises,o.optBoolean("demo",false))
  }
 }
}
object DemoContent {
 val lines = listOf("I'm looking forward to seeing you.", "Could you give me a hand?", "Let's call it a day.", "If I were you, I'd take a chance.")
 fun lesson(text: String, level: String): Teaching {
  val index = lines.indexOf(text)
  require(index >= 0) { "آموزش آفلاین فقط برای دیالوگ‌های نمونه موجود است. برای دیالوگ شخصی API را در تنظیمات توسعه متصل کنید." }
  val translations = listOf("مشتاق دیدنت هستم.","می‌توانی کمکم کنی؟","بیایید کار امروز را تمام کنیم.","اگر جای تو بودم، شانسم را امتحان می‌کردم.")
  val terms = listOf("look forward to","give me a hand","call it a day","take a chance")
  val meanings = listOf("مشتاق چیزی بودن","به من کمک کردن","کار را برای امروز تمام کردن","ریسک کردن / شانس را امتحان کردن")
  val examples = listOf("I look forward to meeting your family.","Can you give me a hand with this box?","We have worked enough. Let's call it a day.","Take a chance and apply for that job.")
  val grammar = listOf("پس از look forward to، فعل به شکل ing می‌آید، چون to در این عبارت حرف اضافه است. مثال: I look forward to learning. اشتباه رایج: to see به جای to seeing.","Could you + فعل ساده برای درخواست مؤدبانه است. مثال: Could you open the window? از can مؤدبانه‌تر است. اشتباه رایج: افزودن to بعد از could.","Let's مخفف let us است و با فعل ساده برای پیشنهاد مشترک می‌آید. مثال: Let's go home. اشتباه رایج: Let's going.","شرطی نوع دوم: If + گذشته ساده، would + فعل ساده. برای موقعیت فرضی استفاده می‌شود. I'd = I would. مثال: If I had time, I'd travel. اشتباه رایج: would در بخش if.")
  val idioms = listOf("look forward to یعنی انتظار همراه با اشتیاق؛ معنای آن نگاه به جلو نیست. در نامه رسمی نیز رایج است.","give someone a hand یعنی کمک کردن؛ در این جمله منظور دست دادن نیست. عبارت محاوره‌ای و دوستانه است. شکل رسمی: assist someone.","call it a day یعنی توقف کار برای امروز، نه نام‌گذاری یک روز. در محیط کار دوستانه رایج است. شکل رسمی: finish work for today.","take a chance یعنی پذیرفتن احتمال شکست و امتحان کردن. شکل رسمی‌تر: take a risk.")
  val literal = listOf("برای دیدنت با اشتیاق منتظر هستم.","می‌توانی دستی به من بدهی؟ (معنی اصطلاحی: کمک)","بیایید آن را یک روز بنامیم. (ترجمه لفظی معنی اصلی را نمی‌رساند)","اگر من تو بودم، یک شانس می‌گرفتم.")
  val answers = listOf("seeing","give","call","were")
  val blanks = listOf("I'm looking forward to ___ you.","Could you ___ me a hand?","Let's ___ it a day.","If I ___ you, I'd take a chance.")
  val natural = lines[index]
  val exercises = listOf(
   Exercise("معنی", "معنی عبارت ${terms[index]} چیست؟",listOf(meanings[index],"عجله کردن","فراموش کردن"),meanings[index],idioms[index]),
   Exercise("جای خالی",blanks[index],listOf(answers[index],"going","to do").distinct(),answers[index],grammar[index]),
   Exercise("گرامر","کدام توضیح درباره این جمله درست است؟",listOf(grammar[index].substringBefore('.'),"همیشه باید فعل را جمع بست.","این جمله گذشته کامل است."),grammar[index].substringBefore('.'),grammar[index]),
   Exercise("عبارت طبیعی","کدام عبارت طبیعی‌تر است؟",listOf(natural,"I am do a help yesterday.","You to can is go."),natural,"جمله صحیح با ترتیب طبیعی واژه‌ها و ساختار مناسب بیان شده است."),
   Exercise("ترجمه", "به انگلیسی بنویس: ${translations[index]}",emptyList(),natural,"یک پاسخ نمونه: $natural")
  )
  val detail = if (level in listOf("A1","A2")) "\nساده‌تر: ابتدا عبارت را کامل یاد بگیر و با صدای بلند تکرار کن." else "\nتمرین تکمیلی: جمله را برای موقعیتی در زندگی خودت بازنویسی کن."
  return Teaching(translations[index],literal[index],"این دیالوگ ساختگی و آموزشی است و از فیلم دارای حق نشر نقل نشده است. سطح $level.",listOf(Vocabulary(terms[index],meanings[index],listOf("/lʊk ˈfɔːrwərd tuː/","/ɡɪv mi ə hænd/","/kɔːl ɪt ə deɪ/","/teɪk ə tʃæns/")[index],"عبارت",examples[index],listOf("anticipate","help / assist","finish","take a risk")[index],level)),grammar[index]+detail,idioms[index],"A: $natural\nB: ${listOf("Me too! See you soon.","Sure. What do you need?","Good idea. See you tomorrow.","You're right. I'll try.")[index]}\nدر موقعیت واقعی: ${examples[index]}",exercises,true)
 }
}
object AnswerChecker {
 fun normalize(value: String) = value.lowercase().trim().replace('’','\'').replace(Regex("[.!?،؟]+$"), "").replace(Regex("\\s+"), " ")
 fun correct(answer: String, expected: String) = normalize(answer) == normalize(expected)
}