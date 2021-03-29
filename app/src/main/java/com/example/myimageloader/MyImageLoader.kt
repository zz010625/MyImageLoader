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
    private var mBitmap: Bitmap? = null
    private var imageView: ImageView? = null

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
        if (this.url != null) {
            //先到内存获取
            mBitmap = getOnMemory()
            if (mBitmap != null) {
                return this
            }
            //内存中没找到 去本地获取
            mBitmap = getOnLocal()
            if (mBitmap != null) {
                return this
            }
            //内存和本地都没有 通过网络获取图片并加载 且可选择是否储存到内存/本地
            loadBitmapFromNet(MyImageLoader.url, activity)
        }
        return this
    }

    fun into(imageView: ImageView?) {
        this.imageView = imageView
        if (mBitmap != null && this.imageView != null) {
            imageView?.setImageBitmap(mBitmap)
            //清空数据
            empty()
        }
    }

    /**
     * 从内存获取
     */
    private fun getOnMemory(): Bitmap? {
        val bitmap = mCache?.get(url)
        if (bitmap != null) {
            return bitmap
        }
        return null
    }

    /**
     * 从本地获取
     */
    private fun getOnLocal(): Bitmap? {
        val bitmap: Bitmap? = loadBitmapFromLocal(url)
        if (bitmap != null) {
            return bitmap
        }
        return null
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
    private fun loadBitmapFromNet(url: String, activity: Activity) {
        var bitmap: Bitmap
        Thread {
            val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30 * 1000
            connection.readTimeout = 30 * 1000
            connection.connect()
            val code: Int = connection.responseCode
            if (200 == code) {
                val inputStream: InputStream = connection.inputStream
                bitmap = BitmapFactory.decodeStream(inputStream)
                //储存到内存
                mCache?.put(url, bitmap)
                //判断是否需要储存图片到本地
                if (isWriteToLocal) {
                    writeToLocal(url, bitmap)
                }
                //主线程加载图片
                activity.runOnUiThread {
                    mBitmap = bitmap
                    into(imageView)
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
            val bos: BufferedOutputStream? = BufferedOutputStream(FileOutputStream(cacheDir))
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

    /**
     * 清空/初始化相关变量 保证下一次使用时不受原数据影响
     */
    private fun empty() {
        isWriteToLocal = false
        mBitmap = null
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