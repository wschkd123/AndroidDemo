package com.example.base.download

import java.io.File

interface FileDownloadListener {
    fun onProgress(url: String, bytesRead: Long, contentLength: Long, done: Boolean) {

    }
    fun onSuccess(url: String, fileName: String, file: File)
    fun onFail(url: String, errorMessage: String)
}