// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class SeriesProviderEntryDto(
  val id: String? = null,
  val libraryId: String? = null,
  val seriesId: String? = null,
  val seriesName: String? = null,
  val title: String,
  val position: String? = null,
  val releaseDate: String? = null,
  val asin: String? = null,
  val coverUrl: String? = null,
  val providerUrl: String? = null,
  val summary: String? = null,
  val entryType: String? = null,
  val isReleased: Boolean = false,
  val lastScannedAt: Long? = null,
)

@Serializable
data class UpcomingReleasesResponse(
  val libraryId: String? = null,
  val upcomingReleases: List<SeriesProviderEntryDto> = emptyList(),
  val total: Int = 0,
  val scanStatus: SeriesProviderScanStatus? = null,
)

@Serializable
data class MissingBooksResponse(
  val libraryId: String? = null,
  val missingBooks: List<SeriesProviderEntryDto> = emptyList(),
  val total: Int = 0,
  val scanStatus: SeriesProviderScanStatus? = null,
)

@Serializable
data class SeriesProviderEntriesResponse(
  val seriesId: String? = null,
  val seriesName: String? = null,
  val entries: List<SeriesProviderEntryDto> = emptyList(),
)

@Serializable
data class SeriesProviderScanStatus(
  val isScanning: Boolean = false,
  val progress: SeriesScanProgress? = null,
  val lastScan: SeriesScanSummary? = null,
)

@Serializable
data class SeriesScanProgress(
  val current: Int = 0,
  val total: Int = 0,
)

@Serializable
data class SeriesScanSummary(
  val scannedAt: Long? = null,
  val status: String? = null,
  val seriesChecked: Int = 0,
  val upcomingFound: Int = 0,
  val missingFound: Int = 0,
  val skippedCount: Int = 0,
  val errorMessage: String? = null,
)

@Serializable
data class SeriesProviderScanActionResponse(
  val success: Boolean = false,
  val message: String? = null,
)
