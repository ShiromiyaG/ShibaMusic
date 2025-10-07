package com.shirou.shibamusic.util

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.InsetDrawable
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.shirou.shibamusic.App
import com.shirou.shibamusic.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.LinkedHashMap

object UIUtil {
    fun getSpanCount(itemCount: Int, maxSpan: Int): Int {
        val itemSize = if (itemCount == 0) 1 else itemCount
        return if (itemSize >= maxSpan) maxSpan else itemSize
    }

    fun getDividerItemDecoration(context: Context): DividerItemDecoration {
        val ATTRS = intArrayOf(android.R.attr.listDivider)

        val insetDivider = context.obtainStyledAttributes(ATTRS).use { a: TypedArray ->
            val divider = a.getDrawable(0)
            // The original Java code does not check for null. If divider is null, InsetDrawable constructor
            // would likely throw an IllegalArgumentException or NPE when its methods are called.
            // Using `!!` here mirrors that behavior for semantic equivalence.
            InsetDrawable(divider!!, 42, 0, 42, 42)
        }

        return DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
            setDrawable(insetDivider)
        }
    }

    private fun getLocalesFromResources(context: Context): LocaleListCompat {
        val tagsList = mutableListOf<String>()
        val xpp = context.resources.getXml(R.xml.locale_config)

        try {
            while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = xpp.name

                if (xpp.eventType == XmlPullParser.START_TAG) {
                    if (tagName == "locale" && xpp.attributeCount > 0 && xpp.getAttributeName(0) == "name") {
                        tagsList.add(xpp.getAttributeValue(0))
                    }
                }
                xpp.next()
            }
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return LocaleListCompat.forLanguageTags(tagsList.joinToString(","))
    }

    fun getLangPreferenceDropdownEntries(context: Context): LinkedHashMap<String, String> {
        val localeList = getLocalesFromResources(context)

        val localeEntries = mutableListOf<Pair<String, String>>()

        val systemDefaultLabel = App.getContext().getString(R.string.settings_system_language)
        val systemDefaultValue = "default"

        for (i in 0 until localeList.size()) {
            localeList[i]?.let { locale ->
                val label = Util.toPascalCase(locale.displayName) ?: locale.displayName
                localeEntries.add(label to locale.toLanguageTag())
            }
        }

        localeEntries.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.first })

        return LinkedHashMap<String, String>().apply {
            put(systemDefaultLabel, systemDefaultValue)
            localeEntries.forEach { (label, value) ->
                put(label, value)
            }
        }
    }

    fun getReadableDate(date: Date): String {
        val formatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        return formatter.format(date)
    }
}
