package com.example.plasmatrack

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

// Modèle de données pour les manuels techniques
data class TechManualItem(val id: Long = System.currentTimeMillis(), val title: String, val fileUrl: String, val description: String = "")
data class TechManualSection(val name: String, val items: MutableList<TechManualItem> = mutableListOf())

object TechManualStorage {
    private const val PREF = "tech_manuals_prefs"
    private const val KEY = "tech_manuals_v1"

    fun loadAll(context: Context): MutableList<TechManualSection> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<TechManualSection>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "")
                val itemsArr = obj.optJSONArray("items") ?: JSONArray()
                val items = mutableListOf<TechManualItem>()
                for (j in 0 until itemsArr.length()) {
                    val it = itemsArr.optJSONObject(j) ?: continue
                    val id = it.optLong("id", System.currentTimeMillis())
                    val title = it.optString("title", "")
                    val url = it.optString("fileUrl", "")
                    val desc = it.optString("description", "")
                    items.add(TechManualItem(id = id, title = title, fileUrl = url, description = desc))
                }
                list.add(TechManualSection(name = name, items = items))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun saveAll(context: Context, sections: List<TechManualSection>) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val arr = JSONArray()
        sections.forEach { sec ->
            val obj = JSONObject()
            obj.put("name", sec.name)
            val a = JSONArray()
            sec.items.forEach { it ->
                val o = JSONObject()
                o.put("id", it.id)
                o.put("title", it.title)
                o.put("fileUrl", it.fileUrl)
                o.put("description", it.description)
                a.put(o)
            }
            obj.put("items", a)
            arr.put(obj)
        }
        sp.edit { putString(KEY, arr.toString()) }
    }

    fun addSection(context: Context, name: String) {
        val list = loadAll(context)
        if (list.any { it.name.equals(name, ignoreCase = true) }) return
        list.add(TechManualSection(name))
        saveAll(context, list)
    }

    fun addItem(context: Context, sectionName: String, title: String, fileUrl: String, description: String = "") {
        val list = loadAll(context)
        val sec = list.firstOrNull { it.name.equals(sectionName, ignoreCase = true) } ?: run {
            val s = TechManualSection(sectionName)
            list.add(s)
            s
        }
        sec.items.add(TechManualItem(title = title, fileUrl = fileUrl, description = description))
        saveAll(context, list)
    }

    fun removeItem(context: Context, sectionName: String, itemId: Long) {
        val list = loadAll(context)
        val sec = list.firstOrNull { it.name.equals(sectionName, ignoreCase = true) } ?: return
        sec.items.removeAll { it.id == itemId }
        saveAll(context, list)
    }

    fun removeSection(context: Context, sectionName: String) {
        val list = loadAll(context)
        list.removeAll { it.name.equals(sectionName, ignoreCase = true) }
        saveAll(context, list)
    }
}

