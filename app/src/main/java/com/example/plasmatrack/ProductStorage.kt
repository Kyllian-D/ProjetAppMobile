package com.example.plasmatrack

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ProductStorage {
    private const val PREFS = "shop_prefs"
    private const val KEY_PRODUCTS = "products_json"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_CONN_FAVORITES = "conn_favorites"
    private const val KEY_LABELS = "labels_json"

    fun saveProducts(context: Context, products: List<Product>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray()
        products.forEach { p ->
            val obj = org.json.JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("description", p.description)
            obj.put("section", p.section)
            obj.put("imagePath", p.imagePath)
            arr.put(obj)
        }
        prefs.edit { putString(KEY_PRODUCTS, arr.toString()) }
    }

    fun loadProducts(context: Context): MutableList<Product> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PRODUCTS, null) ?: return mutableListOf()
        val arr = try {
            JSONArray(json)
        } catch (_: Exception) {
            return mutableListOf()
        }

        val list = mutableListOf<Product>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optLong("id", System.currentTimeMillis())
            val name = obj.optString("name", "")
            val description = obj.optString("description", "")
            val section = obj.optString("section", "")
            val path = obj.optString("imagePath", "")
            val product = Product(id = id, name = name, description = description, section = section, imagePath = path.ifBlank { null })
            list.add(product)
        }
        return list
    }

    // --- Favorites persistence (public) ---
    fun saveFavorites(context: Context, favorites: Set<Long>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_FAVORITES, favorites.joinToString(",")) }
    }

    fun loadFavorites(context: Context): MutableSet<Long> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val joined = prefs.getString(KEY_FAVORITES, "") ?: ""
        return if (joined.isBlank()) mutableSetOf() else joined.split(",").mapNotNull { it.toLongOrNull() }.toMutableSet()
    }

    // --- Connection cards favorites (by unique string id) ---
    fun saveConnFavorites(context: Context, favorites: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_CONN_FAVORITES, favorites.joinToString("||")) }
    }

    fun loadConnFavorites(context: Context): MutableSet<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val joined = prefs.getString(KEY_CONN_FAVORITES, "") ?: ""
        return if (joined.isBlank()) mutableSetOf() else joined.split("||").map { it }.toMutableSet()
    }

    // --- Labels (captured from camera) persistence ---
    data class LabelRecord(
        val id: Long = System.currentTimeMillis(),
        val endoscope: String,
        val serial: String,
        val dateStr: String,
        val operator: String,
        val photoPath: String?, // internal file path
        val savedAt: Long = System.currentTimeMillis()
    )

    fun saveLabels(context: Context, labels: List<LabelRecord>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray()
        labels.forEach { l ->
            val o = JSONObject()
            o.put("id", l.id)
            o.put("endoscope", l.endoscope)
            o.put("serial", l.serial)
            o.put("dateStr", l.dateStr)
            o.put("operator", l.operator)
            o.put("photoPath", l.photoPath)
            o.put("savedAt", l.savedAt)
            arr.put(o)
        }
        prefs.edit { putString(KEY_LABELS, arr.toString()) }
    }

    fun loadLabels(context: Context): MutableList<LabelRecord> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LABELS, null) ?: return mutableListOf()
        val arr = try { JSONArray(json) } catch (_: Exception) { return mutableListOf() }
        val list = mutableListOf<LabelRecord>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optLong("id", System.currentTimeMillis())
            val endo = obj.optString("endoscope", "")
            val serial = obj.optString("serial", "")
            val dateStr = obj.optString("dateStr", "")
            val operator = obj.optString("operator", "")
            val photo = obj.optString("photoPath", "").ifBlank { null }
            val savedAt = obj.optLong("savedAt", System.currentTimeMillis())
            list.add(LabelRecord(id = id, endoscope = endo, serial = serial, dateStr = dateStr, operator = operator, photoPath = photo, savedAt = savedAt))
        }
        return list
    }
}
