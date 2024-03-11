package com.example.beyond.demo.appwidget
 
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.beyond.demo.R
import org.json.JSONArray
 
class ListDemoService : RemoteViewsService() {
 
    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return ListDemoRemoteViewsFactory(applicationContext)
    }
 
    private class ListDemoRemoteViewsFactory(private val context: Context) : RemoteViewsFactory {
        private var datum: MutableList<Pair<Boolean?, String?>> = ArrayList()
        override fun onCreate() {
            datum.clear()
            val sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            val json = sharedPreferences.getString("todo", null) ?: return
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val done = jsonObject.optBoolean("done", false)
                val content = jsonObject.optString("content", "")
                datum.add(Pair(done, content))
            }
        }
 
        override fun onDataSetChanged() {
            datum.clear()
            val sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            val json = sharedPreferences.getString("todo", null) ?: return
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val done = jsonObject.optBoolean("done", false)
                val content = jsonObject.optString("content", "")
                datum.add(Pair(done, content))
            }
        }
 
        override fun onDestroy() {
            datum.clear()
        }
 
        override fun getCount(): Int {
            return datum.size
        }
 
        override fun getViewAt(position: Int): RemoteViews {
            val item = datum[position]
            // 创建在当前索引位置要显示的View
            val remoteViews = RemoteViews(context.packageName, R.layout.item_lv_demo)
            // 设置要显示的内容
            remoteViews.setImageViewResource(
                R.id.iv_done, if (item.first != null && item.first!!) android.R.drawable.checkbox_on_background
            else android.R.drawable.checkbox_off_background)
            remoteViews.setTextViewText(R.id.tv_content, item.second)
            return remoteViews
        }
 
        override fun getLoadingView(): RemoteViews? {
            return null
        }
 
        override fun getViewTypeCount(): Int {
            return 1
        }
 
        override fun getItemId(position: Int): Long {
            return position.toLong()
        }
 
        override fun hasStableIds(): Boolean {
            return true
        }
    }
}