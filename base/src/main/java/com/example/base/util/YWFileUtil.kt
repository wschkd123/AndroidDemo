package com.example.base.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import androidx.annotation.WorkerThread
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.Flushable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

object YWFileUtil {
    /**
     * 压缩得到的文件的后缀名
     */
    const val ZIP_SUFFIX = ".zip"

    /**
     * 缓冲器大小
     */
    const val BUFFER = 4 * 1024
    const val TAG = "YWFileUtil"
    val isSDCardEnable: Boolean
        /**
         * 判断 SD 卡是否可用，这个判断包含了判断 writable 和 Readable
         */
        get() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()

    /**
     * 获取SD卡应用专属文件目录/storage/emulated/0/Android/data/app_package_name/files
     * 这个目录在android 4.4及以上系统不需要申请SD卡读写权限
     * 因此也不用考虑6.0系统动态申请SD卡读写权限问题，切随应用被卸载后自动清空 不会污染用户存储空间
     *
     * @param context 上下文
     * @return 缓存文件夹 如果没有SD卡或SD卡有问题则返回内部存储目录，/data/data/app_package_name/files
     * 否则优先返回SD卡缓存目录
     */
    fun getStorageFileDir(context: Context?): File? {
        return getStorageFileDir(context, null)
    }

    /**
     * 获取SD卡应用专属文件目录/storage/emulated/0/Android/data/app_package_name/files
     * 这个目录在android 4.4及以上系统不需要申请SD卡读写权限
     * 因此也不用考虑6.0系统动态申请SD卡读写权限问题，切随应用被卸载后自动清空 不会污染用户存储空间
     *
     * @param context 上下文
     * @param type 文件夹类型 可以为空，为空则返回API得到的一级目录
     * @return 缓存文件夹 如果没有SD卡或SD卡有问题则返回内部存储目录，/data/data/app_package_name/files
     * 否则优先返回SD卡缓存目录
     */
    fun getStorageFileDir(context: Context?, type: String?): File? {
        var appCacheDir = getExternalFileDirectory(context, type)
        if (appCacheDir == null) {
            appCacheDir = getInternalFileDirectory(context, type)
        }
        if (appCacheDir == null) {
            Log.e(TAG, "getStorageFileDir fail , ExternalFile and InternalFile both unavailable ")
        } else {
            if (!appCacheDir.exists() && !appCacheDir.mkdirs()) {
                Log.e(TAG, "getStorageFileDir fail ,the reason is make directory fail !")
            }
        }
        return appCacheDir
    }

    /**
     * 获取SD卡缓存目录
     *
     * @param context 上下文
     * @param type 文件夹类型 如果为空则返回 /storage/emulated/0/Android/data/app_package_name/files
     * 否则返回对应类型的文件夹如Environment.DIRECTORY_PICTURES 对应的文件夹为 ..
     * ./data/app_package_name/files/Pictures
     * [Environment.DIRECTORY_MUSIC],
     * [Environment.DIRECTORY_PODCASTS],
     * [Environment.DIRECTORY_RINGTONES],
     * [Environment.DIRECTORY_ALARMS],
     * [Environment.DIRECTORY_NOTIFICATIONS],
     * [Environment.DIRECTORY_PICTURES], or
     * [Environment.DIRECTORY_MOVIES].or 自定义文件夹名称
     * @return 缓存目录文件夹 或 null（无SD卡或SD卡挂载失败）
     */
    fun getExternalFileDirectory(context: Context?, type: String?): File? {
        if (context == null) {
            return null
        }
        var appFileDir: File? = null
        if (isSDCardEnable) {
            appFileDir = if (TextUtils.isEmpty(type)) {
                context.getExternalFilesDir(null)
            } else {
                context.getExternalFilesDir(type)
            }
            if (appFileDir == null) { // 有些手机需要通过自定义目录
                appFileDir = File(
                    Environment.getExternalStorageDirectory(),
                    "Android/data/" + context.packageName + "/files/" + type
                )
            }
            if (!appFileDir.exists() && !appFileDir.mkdirs()) {
                Log.e(TAG, "getExternalFileDirectory fail ,the reason is make directory fail ")
            }
        } else {
            Log.e(TAG, "getExternalFileDirectory fail ,the reason is sdCard unMounted ")
        }
        return appFileDir
    }

    /**
     * 获取内存缓存目录 /data/data/app_package_name/files
     *
     * @param type 子目录，可以为空，为空直接返回一级目录
     * @return 缓存目录文件夹 或 null（创建目录文件失败）
     * 注：该方法获取的目录是能供当前应用自己使用，外部应用没有读写权限，如 系统相机应用
     */
    fun getInternalFileDirectory(context: Context?, type: String?): File? {
        if (context == null) {
            return null
        }
        var appFileDir: File? = null
        appFileDir = if (TextUtils.isEmpty(type)) {
            context.filesDir // /data/data/app_package_name/files
        } else {
            File(
                context.filesDir,
                type
            ) // /data/data/app_package_name/files/type
        }
        if (!appFileDir!!.exists() && !appFileDir.mkdirs()) {
            Log.e(TAG, "getInternalFileDirectory fail ,the reason is make directory fail !")
        }
        return appFileDir
    }

    /**
     * 得到源文件路径的所有文件
     * eg:
     * 输入：
     * h1/h2/h3/34.txt
     * h1/h22
     * 结果：34.txt h22
     *
     * @param dirFile 源文件路径
     */
    fun getAllFile(dirFile: File): List<File> {
        val fileList: MutableList<File> = ArrayList()
        val files = dirFile.listFiles() ?: return fileList
        for (file in files) { //文件
            if (file.isFile) {
                fileList.add(file)
                println("add file:" + file.name)
            } else { //目录
                if (file.listFiles() != null && file.listFiles().size != 0) { //非空目录
                    fileList.addAll(getAllFile(file)) //把递归文件加到fileList中
                } else { //空目录
                    fileList.add(file)
                }
            }
        }
        return fileList
    }

    /**
     * 遍历所有文件/文件夹
     * 该工具与 getAllFile 有重合的地方
     *
     * @param file 源文件路径
     * @param files 返回集合
     * @param withDir 是否包含目录
     * eg:
     * 输入：
     * h1/h2/h3/34.txt
     * h1/h22
     * 结果：h1 h2 h3 34.txt h22
     */
    fun flatFileWithDir(file: File?, files: MutableList<File?>, withDir: Boolean) {
        try {
            if (file == null || !file.exists()) {
                return
            }
            if (withDir) {
                files.add(file)
            } else if (file.isFile) {
                files.add(file)
            }
            if (!file.isFile) {
                val children = file.listFiles()
                if (children != null) {
                    for (aChildren in children) {
                        flatFileWithDir(aChildren, files, withDir)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 复制 srcDir 到 destDir
     *
     * @param srcDir 源文件夹
     * @param destDir 目标文件夹
     * @return 是否成功
     */
    fun copyDir(srcDir: File, destDir: File?, overwrite: Boolean): Boolean {
        if (!srcDir.exists()) {
            return false
        }

        // 先创建目标文件夹
        if (!mkdirsIfNotExit(destDir)) {
            return false
        }

        // 遍历源文件夹下每一个文件
        var result = true
        val files = srcDir.listFiles() ?: return false
        for (file in files) {
            result = if (file.isDirectory) {
                // 如果是文件夹递归
                copyDir(file, File(destDir, file.name), overwrite)
            } else if (!file.exists()) {
                Log.e(TAG, "copyDir: file not exists (" + file.absolutePath + ")")
                false
            } else if (!file.isFile) {
                Log.e(TAG, "copyDir: file not file (" + file.absolutePath + ")")
                false
            } else if (!file.canRead()) {
                Log.e(TAG, "copyDir: file cannot read (" + file.absolutePath + ")")
                false
            } else {
                copyFile(file, File(destDir, file.name), overwrite)
            }
        }
        return result
    }
    /**
     * 复制 srcFile 到 destFile
     *
     * @param srcFile 来源文件
     * @param destFile 目标文件
     * @param overwrite 是否覆盖
     * @return 是否成功
     */
    /**
     * 复制 srcFile 到 destFile
     *
     * @param srcFile 来源文件
     * @param destFile 目标文件
     * @return 是否成功
     */
    @JvmOverloads
    fun copyFile(srcFile: File, destFile: File, overwrite: Boolean = false): Boolean {
        try {
            if (!srcFile.exists()) {
                return false
            }
            if (destFile.exists()) {
                if (overwrite) {
                    destFile.delete()
                } else {
                    return true
                }
            } else {
                if (mkdirsIfNotExit(destFile.parentFile)) {
                    destFile.createNewFile()
                }
            }
            var input: FileInputStream? = null
            var fos: FileOutputStream? = null
            try {
                input = FileInputStream(srcFile)
                fos = FileOutputStream(destFile)
                val block = ByteArray(1024 * 50)
                var readNumber = -1
                while (input.read(block).also { readNumber = it } != -1) {
                    fos.write(block, 0, readNumber)
                }
                fos.flush()
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            } finally {
                close(input)
                close(fos)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * 不存在的情况下创建 File
     *
     * @param file 文件
     * @return 是否成功
     */
    fun mkdirsIfNotExit(file: File?): Boolean {
        if (file == null) {
            return false
        }
        if (!file.exists()) {
            synchronized(YWFileUtil::class.java) { return file.mkdirs() }
        }
        return true
    }

    /**
     * 强制删除文件，若失败则延迟 200ms 重试，重试次数 10 次
     * 【注】务必在子线程中调用
     *
     * @param file 待删除文件
     * @return 是否成功
     */
    @WorkerThread
    fun forceDeleteFile(file: File): Boolean {
        if (!file.exists()) {
            return true
        }
        var result = false
        var tryCount = 0
        while (!result && tryCount++ < 10) {
            result = file.delete()
            if (!result) {
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    Log.e("forceDeleteFile", e.message!!)
                }
            }
        }
        return result
    }

    /**
     * 清空文件（夹）
     * 【注】务必在子线程中调用
     *
     * @param file 待删除文件
     * @return 是否成功
     */
    @WorkerThread
    fun clear(file: File?): Boolean {
        if (file == null) {
            return false
        }
        if (!file.exists()) {
            return false
        }
        return if (!file.isDirectory) {
            forceDeleteFile(file)
        } else {
            var ret = true
            try {
                val files = file.listFiles()
                for (i in files.indices) {
                    Thread.sleep(1)
                    if (files[i].isDirectory) {
                        if (!clear(files[i])) {
                            // 只要失败就return false
                            return false
                        }
                    } else {
                        if (!forceDeleteFile(files[i])) {
                            ret = false
                            break
                        }
                    }
                }
                val to = File(
                    file.absolutePath
                            + System.currentTimeMillis()
                )
                file.renameTo(to)
                forceDeleteFile(to) // 删除空文件夹
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            ret
        }
    }

    fun writeStream(file: File?, `is`: InputStream?): Boolean {
        var os: FileOutputStream? = null
        try {
            if (file == null || `is` == null) {
                return false
            }
            val parentFile = file.parentFile
            if (!parentFile.exists()) {
                val mkdirs = parentFile.mkdirs()
                if (!mkdirs) {
                    return false
                }
            }
            if (!file.exists()) {
                val newFile = file.createNewFile()
                if (!newFile) {
                    return false
                }
            }
            os = FileOutputStream(file)
            var byteCount: Int
            val bytes = ByteArray(1024)
            while (`is`.read(bytes).also { byteCount = it } != -1) {
                os.write(bytes, 0, byteCount)
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            close(`is`)
            close(os)
        }
        return false
    }

    /**
     * 读取文件内容
     *
     * @param path 文件路径
     * @return 内容
     */
    fun readFile(path: String?): String? {
        if (path == null) {
            return null
        }
        var bufferedReader: BufferedReader? = null
        val stringBuilder = StringBuilder()
        try {
            bufferedReader = BufferedReader(FileReader(path))
            var strTemp: String?
            while (bufferedReader.readLine().also { strTemp = it } != null) {
                stringBuilder.append(strTemp)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            close(bufferedReader)
        }
        return stringBuilder.toString()
    }

    /**
     * 从Asset读取文件内容
     *
     * @param fileName 文件名称
     * @return 内容
     */
    fun readAsset(context: Context?, fileName: String?): String? {
        if (fileName == null || context == null) {
            return null
        }
        var bufferedReader: BufferedReader? = null
        var inputStream: InputStream? = null
        val stringBuilder = StringBuilder()
        try {
            inputStream = context.assets.open(fileName)
            bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var strTemp: String?
            while (bufferedReader.readLine().also { strTemp = it } != null) {
                stringBuilder.append(strTemp)
                stringBuilder.append('\n')
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            close(bufferedReader)
            close(inputStream)
        }
        return stringBuilder.toString()
    }

    /**
     * 内容保存到文件
     *
     * @param destFile 目标文件
     * @param content 内容
     */
    fun save2File(destFile: File?, content: String): Boolean {
        if (destFile == null || TextUtils.isEmpty(content)) {
            return false
        }
        var outputStream: OutputStream? = null
        try {
            destFile.delete()
            if (!destFile.createNewFile()) {
                return false
            }
            outputStream = FileOutputStream(destFile)
            outputStream.write(content.toByteArray(StandardCharsets.UTF_8))
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            close(outputStream)
        }
        return true
    }

    /**
     * 保存bitmap 到文件
     *
     * @param bitmap bitmap
     * @param filepath filepath
     * @return 保存成功
     */
    fun saveBitmap(bitmap: Bitmap, filepath: String): Boolean {
        Log.i(TAG, "saveBitmap [filepath] = $filepath")
        val format = CompressFormat.JPEG
        val quality = 100
        var stream: OutputStream? = null
        try {
            val file = File(filepath)
            val parentFile = file.parentFile
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs()
            }
            if (!file.exists()) {
                val createResult = file.createNewFile()
                if (!createResult) {
                    return false
                }
            }
            stream = FileOutputStream(filepath)
            return bitmap.compress(format, quality, stream)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            flush(stream)
            close(stream)
        }
        return false
    }

    /**
     * 关闭输入流
     *
     * @param closeable IO stream
     */
    fun close(closeable: Closeable?) {
        if (closeable == null) {
            return
        }
        try {
            closeable.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Flush输入流
     *
     * @param flushable IO stream
     */
    fun flush(flushable: Flushable?) {
        if (flushable == null) {
            return
        }
        try {
            flushable.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 解压文件
     *
     * @param zipFilePath
     * @param unzipPath
     * @throws IOException
     * @throws ZipException
     */
    @Throws(ZipException::class, IOException::class)
    fun unzipFile(zipFilePath: String?, unzipPath: String) {
        val file = File(zipFilePath)
        val zipFile = ZipFile(file)
        val desFile = File(unzipPath)
        if (!desFile.exists()) {
            desFile.mkdirs()
        }
        try {
            val entries: Enumeration<*> = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement() as ZipEntry
                val zipEntryName = entry.name
                val `in` = zipFile.getInputStream(entry)
                val outPath = (unzipPath + zipEntryName).replace("\\*".toRegex(), "/")
                // 判断路径是否存在,不存在则创建文件路径
                val temp = File(outPath.substring(0, outPath.lastIndexOf('/')))
                if (!temp.exists()) {
                    temp.mkdirs()
                }
                // 判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
                if (File(outPath).isDirectory) {
                    continue
                }
                // 输出文件路径信息
                var out: OutputStream? = null
                try {
                    out = FileOutputStream(outPath)
                    val buf1 = ByteArray(4 * 1024)
                    var len: Int
                    while (`in`.read(buf1).also { len = it } > 0) {
                        out.write(buf1, 0, len)
                    }
                    `in`.close()
                    out.close()
                } catch (e: IOException) {
                    throw e
                } finally {
                    try {
                        out?.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: ZipException) {
            throw e
        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取指定路径下的文件
     */
    fun getAccessFileOrCreate(access: String?): File? {
        try {
            val f = File(access)
            return if (!f.exists()) {
                if (mkdirsIfNotExit(f.parentFile)) {
                    f.createNewFile()
                }
                f
            } else {
                f
            }
        } catch (e: IOException) {
            Log.e("Utility getAccessFile", e.toString())
        }
        return null
    }

    fun getAccessFileOrNull(access: String?): File? {
        val f = File(access)
        return if (f.exists()) {
            f
        } else {
            null
        }
    }

    fun saveByteArrayToFile(byteArray: ByteArray?, filepath: String): Boolean {
        Log.i(TAG, "saveByteArrayToFile [filepath] = $filepath")
        var fos: FileOutputStream? = null
        try {
            val file = createNewFile(filepath) ?: return false
            fos = FileOutputStream(file)
            fos.write(byteArray)
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "save $filepath fail" + e.message)
            return false
        } finally {
            flush(fos)
            close(fos)
        }
        return true
    }

    /**
     * 判断是否是本地文件路径
     */
    fun isLocalPath(uri: String?): Boolean {
        return uri != null && (uri.startsWith("/") || uri.startsWith("file://"))
    }

    fun createNewFile(path: String): File? {
        val file = File(path)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            val createResult = file.createNewFile()
            if (!createResult) {
                Log.e(TAG, "create ${file.path} fail")
                return null
            }
        } else {
            Log.e(TAG, "$file is exists")
        }
        return file
    }

    fun getFormatFromUrl(url: String): String {
        return url.substring(url.lastIndexOf('.') + 1)
    }

    /**
     * 替换path的后缀（格式）
     */
    fun replacePathSuffix(path: String, newSuffix: String): String {
        // 获取最后一个斜杠的索引
        val lastSlashIndex = path.lastIndexOf("/")
        // 获取最后一个点的索引
        val lastDotIndex = path.lastIndexOf(".")
        // 如果最后一个点在最后一个斜杠之后，则将后缀替换为新的后缀
        if (lastDotIndex > lastSlashIndex) {
            val oldSuffix = path.substring(lastDotIndex + 1)
            return path.replace(oldSuffix, newSuffix)
        }
        // 如果没有找到最后一个点或者最后一个点在最后一个斜杠之前，则直接返回原始URL
        return path
    }


}
