package com.example.plasmatrack

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

// Réutiliser les classes de données QuickGuide et QuickGuideSection définies dans QuickGuidesStorage.kt
// Stockage séparé pour les guides rapides Plasma (clé prefs partagée différente)

object PlasmaQuickGuidesStorage {
    private const val PREF = "plasma_quick_guides_prefs"
    private const val KEY = "plasma_quick_guides_v1"

    fun loadAll(context: Context): MutableList<QuickGuideSection> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<QuickGuideSection>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "")
                val guidesArr = obj.optJSONArray("guides") ?: JSONArray()
                val guides = mutableListOf<QuickGuide>()
                for (j in 0 until guidesArr.length()) {
                    val g = guidesArr.optJSONObject(j) ?: continue
                    val id = g.optLong("id", System.currentTimeMillis())
                    val title = g.optString("title", "")
                    val url = g.optString("youtubeUrl", "")
                    val desc = g.optString("description", "")
                    guides.add(QuickGuide(id = id, title = title, youtubeUrl = url, description = desc))
                }
                list.add(QuickGuideSection(name = name, guides = guides))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun saveAll(context: Context, sections: List<QuickGuideSection>) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val arr = JSONArray()
        sections.forEach { sec ->
            val obj = JSONObject()
            obj.put("name", sec.name)
            val gArr = JSONArray()
            sec.guides.forEach { g ->
                val go = JSONObject()
                go.put("id", g.id)
                go.put("title", g.title)
                go.put("youtubeUrl", g.youtubeUrl)
                go.put("description", g.description)
                gArr.put(go)
            }
            obj.put("guides", gArr)
            arr.put(obj)
        }
        sp.edit { putString(KEY, arr.toString()) }
    }

    fun addSection(context: Context, name: String) {
        val list = loadAll(context)
        if (list.any { it.name.equals(name, ignoreCase = true) }) return
        list.add(QuickGuideSection(name))
        saveAll(context, list)
    }

    fun addGuide(context: Context, sectionName: String, title: String, youtubeUrl: String, description: String = "") {
        val list = loadAll(context)
        val sec = list.firstOrNull { it.name.equals(sectionName, ignoreCase = true) } ?: run {
            val s = QuickGuideSection(sectionName)
            list.add(s)
            s
        }
        sec.guides.add(QuickGuide(title = title, youtubeUrl = youtubeUrl, description = description))
        saveAll(context, list)
    }

    fun removeGuide(context: Context, sectionName: String, guideId: Long) {
        val list = loadAll(context)
        val sec = list.firstOrNull { it.name.equals(sectionName, ignoreCase = true) } ?: return
        sec.guides.removeAll { it.id == guideId }
        saveAll(context, list)
    }

    fun removeSection(context: Context, sectionName: String) {
        val list = loadAll(context)
        list.removeAll { it.name.equals(sectionName, ignoreCase = true) }
        saveAll(context, list)
    }
}

