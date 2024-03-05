package com.example.webapplication.util

interface Downloader {
    fun downloadFile(url: String): Long
}