// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.image

import app.campfire.core.image.CoverUrls
import coil3.intercept.Interceptor
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.pxOrElse

/**
 * Rewrites Audiobookshelf cover/author image requests to ask the server for a rendition sized to the
 * slot the image is being displayed in, instead of the server's 400px default for every surface.
 *
 * Coil resolves the target [coil3.size.Size] from the composable's measured bounds, so thumbnails in a
 * grid request a small width while the expanded player requests a large one — no call-site changes
 * needed. The rewritten URL becomes the memory/disk cache key, so each rendition is cached independently
 * and [CoverUrls.sized] is the single source of truth for anyone that needs to look an entry up directly.
 *
 * If the server fails to serve the requested sized/WebP rendition (e.g. sharp resize or cache file
 * corruption on the server for that specific width), it falls back gracefully to the un-sized default
 * request, and then to `raw=1` (bypassing server-side thumbnail generation completely).
 *
 * When network fails completely (e.g. offline, airplane mode, or data black spot), it performs a multi-tier
 * disk/memory cache fallback across all size buckets with network disabled, ensuring cached thumbnails
 * scale up rather than rendering blank covers.
 */
class CoverSizingInterceptor : Interceptor {

  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
    val url = chain.request.data as? String
    if (url == null || !CoverUrls.isServerImageUrl(url)) return chain.proceed()

    val widthPx = chain.size.width.pxOrElse { chain.size.height.pxOrElse { 0 } }.takeIf { it > 0 }
    val sizedUrl = CoverUrls.sized(url, widthPx)
    val headers = chain.request.httpHeaders.newBuilder()
      .set("Accept", ACCEPT_HEADER)
      .build()
    val request = chain.request.newBuilder()
      .data(sizedUrl)
      .httpHeaders(headers)
      .build()
    val result = chain.withRequest(request).proceed()
    if (result is SuccessResult) return result

    // 1st Fallback: try un-sized original URL (handles server WebP or specific dimension cache failures)
    val defaultResult = chain.proceed()
    if (defaultResult is SuccessResult) return defaultResult

    // 2nd Fallback: request raw=1 to bypass server thumbnail cache / sharp resize failures
    val rawUrl = CoverUrls.raw(url)
    if (rawUrl != url) {
      val rawRequest = chain.request.newBuilder()
        .data(rawUrl)
        .build()
      val rawResult = chain.withRequest(rawRequest).proceed()
      if (rawResult is SuccessResult) return rawResult
    }

    // 3rd Fallback (Offline / Disk Cache Resiliency):
    // When network fails (e.g. offline, airplane mode, or data black spot), check if any other
    // rendition of this cover exists in Coil's disk/memory cache. Setting networkCachePolicy(CachePolicy.DISABLED)
    // instructs Coil to only read from disk/memory without making network requests.
    val targetWidth = widthPx ?: CoverUrls.ARTWORK_WIDTH
    val candidateWidths = CoverUrls.WIDTH_BUCKETS
      .sortedWith(
        compareBy<Int> { if (it <= targetWidth) 0 else 1 }
          .thenByDescending { if (it <= targetWidth) it else -it },
      )

    for (bucket in candidateWidths) {
      val candidateUrl = CoverUrls.sized(url, bucket)
      if (candidateUrl == sizedUrl) continue
      val cacheOnlyRequest = chain.request.newBuilder()
        .data(candidateUrl)
        .networkCachePolicy(CachePolicy.DISABLED)
        .build()
      val cachedResult = chain.withRequest(cacheOnlyRequest).proceed()
      if (cachedResult is SuccessResult) return cachedResult
    }

    if (rawUrl != sizedUrl) {
      val cachedRawRequest = chain.request.newBuilder()
        .data(rawUrl)
        .networkCachePolicy(CachePolicy.DISABLED)
        .build()
      val cachedRawResult = chain.withRequest(cachedRawRequest).proceed()
      if (cachedRawResult is SuccessResult) return cachedRawResult
    }

    if (url != sizedUrl) {
      val cachedUnsizedRequest = chain.request.newBuilder()
        .data(url)
        .networkCachePolicy(CachePolicy.DISABLED)
        .build()
      val cachedUnsizedResult = chain.withRequest(cachedUnsizedRequest).proceed()
      if (cachedUnsizedResult is SuccessResult) return cachedUnsizedResult
    }

    return result
  }

  internal companion object {
    /** Lets the server pick WebP (smaller renditions) when it can; falls back to JPEG otherwise. */
    const val ACCEPT_HEADER = "image/webp,image/*;q=0.8,*/*;q=0.5"
  }
}
