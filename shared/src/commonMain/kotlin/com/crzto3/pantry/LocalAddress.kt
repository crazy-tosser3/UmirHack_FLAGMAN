package com.crzto3.pantry

internal fun isLocalNetworkUrl(url: String): Boolean {
    val host = url.substringAfter("://")
        .substringBefore('/')
        .substringAfterLast('@')
        .let { if (it.startsWith("[")) it.substringAfter('[').substringBefore(']') else it.substringBefore(':') }
        .lowercase()
    if (host.isEmpty() || host == "localhost") return false
    if (host.endsWith(".local") || host.endsWith(".lan") || host.endsWith(".home.arpa") || !host.contains('.') && !host.contains(':')) {
        return true
    }
    if (host.contains(':')) {
        return host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe8") ||
            host.startsWith("fe9") || host.startsWith("fea") || host.startsWith("feb")
    }
    val octets = host.split('.').map { it.toIntOrNull() ?: return false }
    if (octets.size != 4) return false
    val (a, b) = octets
    return a == 10 ||
        a == 192 && b == 168 ||
        a == 172 && b in 16..31 ||
        a == 169 && b == 254 ||
        a == 100 && b in 64..127
}
