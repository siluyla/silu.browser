package id.silu.browser

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class UserScripts(private val prefs: SharedPreferences) {

    class Meta(val name: String, val includes: List<String>, val excludes: List<String>)

    fun list(): JSONArray = try {
        JSONArray(prefs.getString(KEY, "[]"))
    } catch (e: JSONException) {
        JSONArray()
    }

    fun find(id: String): JSONObject? {
        val all = list()
        for (i in 0 until all.length()) {
            val entry = all.getJSONObject(i)
            if (entry.getString("id") == id) return entry
        }
        return null
    }

    fun add(code: String): Boolean {
        if (code.isBlank()) return false
        val all = list()
        all.put(JSONObject().put("id", System.currentTimeMillis().toString()).put("name", meta(code).name).put("code", code).put("on", true))
        persist(all)
        return true
    }

    fun edit(id: String, code: String) = rewrite(id) { it.put("code", code).put("name", meta(code).name) }

    fun toggle(id: String) = rewrite(id) { it.put("on", !it.optBoolean("on", true)) }

    fun remove(id: String) = rewrite(id) { null }

    fun meta(code: String): Meta {
        var name = ""
        val includes = mutableListOf<String>()
        val excludes = mutableListOf<String>()
        val header = HEADER.find(code)?.groupValues?.get(1).orEmpty()
        DIRECTIVE.findAll(header).forEach { match ->
            val value = match.groupValues[2].trim()
            when (match.groupValues[1]) {
                "name" -> if (name.isEmpty()) name = value
                "match", "include" -> includes.add(value)
                "exclude", "exclude-match" -> excludes.add(value)
            }
        }
        return Meta(name.ifEmpty { "Untitled script" }, includes, excludes)
    }

    fun payloadsFor(url: String): List<String> {
        val all = list()
        val payloads = ArrayList<String>()
        for (i in 0 until all.length()) {
            val entry = all.getJSONObject(i)
            if (!entry.optBoolean("on", true)) continue
            val code = entry.getString("code")
            if (applies(meta(code), url)) payloads.add(wrap(entry.getString("id"), code))
        }
        return payloads
    }

    private fun rewrite(id: String, transform: (JSONObject) -> JSONObject?) {
        val source = list()
        val result = JSONArray()
        for (i in 0 until source.length()) {
            val entry = source.getJSONObject(i)
            if (entry.getString("id") == id) transform(entry)?.let { result.put(it) } else result.put(entry)
        }
        persist(result)
    }

    private fun persist(all: JSONArray) {
        prefs.edit().putString(KEY, all.toString()).apply()
    }

    private fun applies(meta: Meta, url: String): Boolean = runCatching {
        meta.excludes.none { toRegex(it).matches(url) } &&
            (meta.includes.isEmpty() || meta.includes.any { toRegex(it).matches(url) })
    }.getOrDefault(false)

    private fun toRegex(pattern: String): Regex {
        if (pattern == "*" || pattern == "<all_urls>") return Regex(".*")
        if (pattern.length > 2 && pattern.startsWith("/") && pattern.endsWith("/")) {
            return Regex(pattern.substring(1, pattern.length - 1))
        }
        val out = StringBuilder()
        var rest = pattern
        if (rest.startsWith("*://")) {
            out.append("https?://")
            rest = rest.substring(4)
        }
        if (rest.startsWith("*.")) {
            out.append("(?:[^/]*\\.)?")
            rest = rest.substring(2)
        }
        for (c in rest) if (c == '*') out.append(".*") else out.append(Regex.escape(c.toString()))
        return Regex(out.toString(), RegexOption.IGNORE_CASE)
    }

    private fun wrap(id: String, code: String): String =
        "(function(){if(window.__silu_$id)return;window.__silu_$id=1;${PageScripts.SHIM} try{\n$code\n}catch(e){console.error('[SiLu userscript]',e)}})();"

    private companion object {
        const val KEY = "us"
        val HEADER = Regex("==UserScript==([\\s\\S]*?)==/UserScript==")
        val DIRECTIVE = Regex("@(\\S+)[ \\t]+(.+)")
    }
}
