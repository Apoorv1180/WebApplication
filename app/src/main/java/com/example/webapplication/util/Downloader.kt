package com.example.webapplication.util

interface Downloader {
    fun downloadDocFile(url: String): Long
    fun downloadImageFile(url:String): Long
    fun downloadPdfFile(url: String): Long
    fun downloadApkFile(url: String): Long
}