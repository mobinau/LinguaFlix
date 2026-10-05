package com.linguaflix.app.domain

data class Subtitle(val startMs: Long, val endMs: Long, val text: String)
object SrtParser {
 private val timing = Regex("(\\d{1,3}):(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*(\\d{1,3}):(\\d{2}):(\\d{2})[,.](\\d{3})(?:\\s+.*)?")
 fun parse(input: String): List<Subtitle> {
  require(input.length <= 4_000_000) { "فایل زیرنویس بیش از حد بزرگ است (حداکثر ۴ مگابایت)." }
  val normalized = input.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
  val result = normalized.split(Regex("\n[ \\t]*\n")).mapNotNull { block ->
   val lines = block.trim().lines(); val index = lines.indexOfFirst { timing.matches(it.trim()) }
   if (index < 0) return@mapNotNull null
   val match = timing.matchEntire(lines[index].trim())!!
   fun time(offset: Int): Long { val h = match.groupValues[offset].toLong(); val m = match.groupValues[offset+1].toLong(); val s = match.groupValues[offset+2].toLong(); require(m < 60 && s < 60) { "زمان زیرنویس نامعتبر است." }; return ((h*60+m)*60+s)*1000+match.groupValues[offset+3].toLong() }
   val start = time(1); val end = time(5)
   require(end >= start) { "زمان پایان پیش از شروع است." }
   val text = lines.drop(index+1).joinToString(" ").replace(Regex("<[^>]*>"), "").replace(Regex("\\{[^}]*}"), "").trim()
   if (text.isBlank()) null else Subtitle(start, end, text)
  }.sortedBy { it.startMs }
  require(result.isNotEmpty()) { "هیچ دیالوگ معتبر SRT در فایل پیدا نشد." }
  require(result.size <= 15000) { "تعداد دیالوگ‌ها بیش از حد مجاز است." }
  return result
 }
 fun timestamp(ms: Long) = "%02d:%02d:%02d".format(ms/3600000, ms/60000%60, ms/1000%60)
}