package com.example.myimageloader

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import androidx.collection.LruCache
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.experimental.and


object MyImageLoader {
    private var mCache: LruCache<String, Bitmap>? = null
    private lateinit var activity: Activity
    private lateinit var url: String
    private var isWriteToLocal = false

    init {
        // 设置最大使用的内存空间
        val maxSize = (Runtime.getRuntime().freeMemory() / 4).toInt()
        if (maxSize > 0) {
            mCache = object : LruCache<String, Bitmap>(maxSize) {
                override fun sizeOf(key: String, value: Bitmap): Int {
                    return value.rowBytes * value.height
                }
            }
        }
    }

    fun with(activity: Activity): MyImageLoader {
        this.activity = activity
        return this
    }

    fun load(url: String): MyImageLoader {
        this.url = url
        return this
    }

    fun into(imageView: ImageView): MyImageLoader {
        //先到内存获取
        if (url != null) {
            var bitmap = mCache?.get(url)
            if (bitmap != null) {
                // 直接从内存中获取图片来显示
                imageView.setImageBitmap(bitmap)
                //取到后初始化
                isWriteToLocal = false
                Log.d("zz", "内存中获取到了")
                return this
            }
            //内存中没找到 去本地获取
            bitmap = loadBitmapFromLocal(url)
            if (bitmap != null) {
                // 从本地获取图片来显示
                imageView.setImageBitmap(bitmap)
                //取到后初始化
                isWriteToLocal = false
                Log.d("zz", "本地中获取到了")
                return this
            }
            //内存和本地都没有 通过网络获取图片并加载 且可选择是否储存到内存/本地
            loadBitmapFromNet(url, activity, imageView)
        }
        return this
    }

    /**
     * 第一次加载图片时 可选择是否储存图片到本地
     */

    fun isWriteToLocal(): MyImageLoader {
        isWriteToLocal = true
        return this
    }

    /**
     * 通过网络加载图片
     */
    private fun loadBitmapFromNet(url: String, activity: Activity, imageView: ImageView) {
        Thread {
            val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30 * 1000
            connection.readTimeout = 30 * 1000
            connection.connect()
            val code: Int = connection.responseCode
            if (200 == code) {
                val inputStream: InputStream = connection.inputStream
                var bitmap = BitmapFactory.decodeStream(inputStream)
                //储存到内存
                mCache?.put(url, bitmap)
                //判断是否需要储存图片到本地
                if (isWriteToLocal) {
                    writeToLocal(url, bitmap)
                    isWriteToLocal = false
                }
                //主线程加载图片
                activity.runOnUiThread {
                    imageView.setImageBitmap(bitmap)
                    Log.d("zz", "通过网络请求获取到了")
                }
            }
        }.start()
    }

    /**
     * 储存图片至本地
     */
    private fun writeToLocal(url: String, bitmap: Bitmap) {
        try {
            val name = MD5Encoder.encode(url)
            val cacheDir = getCacheDir(name)
            var bos: BufferedOutputStream? = BufferedOutputStream(FileOutputStream(cacheDir))
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            bos?.flush()
            bos?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 从本地读取图片
     */
    private fun loadBitmapFromLocal(url: String): Bitmap? {
        try {
            val name = MD5Encoder.encode(url)
            val cacheDir = getCacheDir(name)
            val bis: BufferedInputStream? = BufferedInputStream(FileInputStream(cacheDir))
            val bitmap = BitmapFactory.decodeStream(bis)
            bis?.close()
            //储存到内存
            mCache?.put(url, bitmap)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 获取图片储存位置
     */
    private fun getCacheDir(name: String): String? {
        return activity.cacheDir.toString() + "/$name.jpg"
    }
}


/**
 * 将图片url加密
 */
object MD5Encoder {
    fun encode(string: String): String {
        val hash: ByteArray = MessageDigest.getInstance("MD5").digest(
            string.toByteArray(charset("UTF-8"))
        )
        val hex = StringBuilder(hash.size * 2)
        for (b in hash) {
            if (b and 0xFF.toByte() < 0x10) {
                hex.append("0")
            }
            hex.append(Integer.toHexString((b and 0xFF.toByte()).toInt()))
        }
        return hex.toString()
    }
}