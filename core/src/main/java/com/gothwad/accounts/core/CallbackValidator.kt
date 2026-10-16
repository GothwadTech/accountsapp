package com.gothwad.accounts.core

import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest

internal object CallbackValidator {
    fun parameters(callback: String, redirect: String, state: String): Map<String, String> {
        require(callback.length <= 8192)
        val uri = URI(callback)
        val expected = URI(redirect)
        require(uri.rawFragment == null && !uri.isOpaque)
        require(uri.scheme == expected.scheme && uri.rawAuthority == expected.rawAuthority && uri.rawPath == expected.rawPath)
        val entries = (uri.rawQuery ?: "").split('&').filter { it.isNotEmpty() }.map {
            val parts = it.split('=', limit = 2)
            URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
        }
        require(entries.map { it.first }.distinct().size == entries.size)
        val params = entries.toMap()
        require(MessageDigest.isEqual(state.toByteArray(), params["state"].orEmpty().toByteArray()))
        require(params.containsKey("code") xor params.containsKey("error"))
        require(!(params["code"] ?: params["error"]).isNullOrBlank())
        return params
    }
}
