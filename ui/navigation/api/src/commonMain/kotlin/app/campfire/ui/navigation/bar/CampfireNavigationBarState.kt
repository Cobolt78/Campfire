// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.bar

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.campfire.common.compose.util.withDensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
class CampfireNavigationBarState internal constructor(
  initialOffset: Float,
  initialOffsetLimit: Float,
  initialBottomBarOffset: Float,
  initialShouldHide: Boolean,
) {
  private var searchBarScrollBehavior by mutableStateOf<SearchBarScrollBehavior?>(null)
  private var topAppBarScrollBehavior by mutableStateOf<TopAppBarScrollBehavior?>(null)

  var scrollBehavior: SearchBarScrollBehavior?
    get() = searchBarScrollBehavior
    private set(value) { searchBarScrollBehavior = value }

  var bottomBarOffset by mutableFloatStateOf(initialBottomBarOffset)
  var scrollOffsetLimit by mutableFloatStateOf(initialOffsetLimit)

  private var _scrollOffset by mutableFloatStateOf(initialOffset)

  val currentFraction: Float?
    get() = when {
      topAppBarScrollBehavior != null -> {
        topAppBarScrollBehavior?.state?.collapsedFraction?.coerceIn(0f, 1f)
      }
      searchBarScrollBehavior != null -> {
        val b = searchBarScrollBehavior!!
        if (b.scrollOffsetLimit < 0f) {
          (b.scrollOffset / b.scrollOffsetLimit).coerceIn(0f, 1f)
        } else {
          0f
        }
      }
      else -> null
    }

  @get:FrequentlyChangingValue
  val scrollOffset by derivedStateOf {
    if (!shouldHide && !isTransitioning) {
      currentFraction?.let { fraction ->
        -fraction * scrollOffsetLimit
      } ?: _scrollOffset
    } else {
      _scrollOffset
    }
  }

  /**
   * Whether or not we should hide the navigation bar
   */
  private var shouldHide by mutableStateOf(initialShouldHide)
  private var isTransitioning by mutableStateOf(false)

  private var transitionJob: Job? = null

  fun CoroutineScope.updateShouldHide(shouldHide: Boolean) {
    val currentScrollOffset = scrollOffset
    this@CampfireNavigationBarState.shouldHide = shouldHide

    transitionJob = launch {
      if (shouldHide) {
        AnimationState(currentScrollOffset, initialVelocity = 0f).animateTo(
          targetValue = -scrollOffsetLimit,
          animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
          ),
        ) {
          _scrollOffset = value
          isTransitioning = isRunning
        }
      } else if (!shouldHide) {
        val target = currentFraction?.let { -it * scrollOffsetLimit } ?: 0f
        AnimationState(_scrollOffset, initialVelocity = 0f).animateTo(
          targetValue = target,
          animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
          ),
        ) {
          _scrollOffset = value
          isTransitioning = isRunning
        }
      }
    }
  }

  /**
   * Computes the offset for the PlaybackBar in the UI
   */
  fun playbackBarOffset(): Float {
    val computeInternalOffset: () -> Float = {
      val scrollOffset = (-_scrollOffset)
        .coerceIn(0f, scrollOffsetLimit)

      val remainingHeight = scrollOffsetLimit - scrollOffset
      remainingHeight.coerceAtLeast(bottomBarOffset)
    }

    if (isTransitioning || shouldHide) {
      return computeInternalOffset()
    }

    return currentFraction?.let { fraction ->
      val remainingHeight = (1f - fraction) * scrollOffsetLimit
      remainingHeight.coerceAtLeast(bottomBarOffset)
    } ?: computeInternalOffset()
  }

  fun CoroutineScope.installScrollBehavior(behavior: SearchBarScrollBehavior) {
    val hadPreviousBehavior = searchBarScrollBehavior != null || topAppBarScrollBehavior != null
    searchBarScrollBehavior = behavior
    topAppBarScrollBehavior = null
    val targetOffset = if (behavior.scrollOffsetLimit < 0f) {
      -(behavior.scrollOffset / behavior.scrollOffsetLimit).coerceIn(0f, 1f) * scrollOffsetLimit
    } else 0f

    if (
      isTransitioning &&
      behavior.scrollOffset < 0f &&
      transitionJob?.isActive == true
    ) {
      transitionJob?.cancel()
      isTransitioning = false
    } else if (
      targetOffset != _scrollOffset &&
      !hadPreviousBehavior &&
      !shouldHide
    ) {
      transitionJob = launch {
        AnimationState(_scrollOffset, initialVelocity = 0f).animateTo(
          targetValue = targetOffset,
          animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
          ),
        ) {
          _scrollOffset = value
          isTransitioning = isRunning
        }
      }
    } else if (hadPreviousBehavior) {
      _scrollOffset = targetOffset
    }
  }

  fun clearScrollBehavior(behavior: SearchBarScrollBehavior) {
    if (searchBarScrollBehavior === behavior) {
      searchBarScrollBehavior = null
    }
  }

  fun CoroutineScope.installScrollBehavior(behavior: TopAppBarScrollBehavior) {
    val hadPreviousBehavior = searchBarScrollBehavior != null || topAppBarScrollBehavior != null
    topAppBarScrollBehavior = behavior
    searchBarScrollBehavior = null
    val targetOffset = -behavior.state.collapsedFraction.coerceIn(0f, 1f) * scrollOffsetLimit

    if (
      isTransitioning &&
      behavior.state.collapsedFraction > 0f &&
      transitionJob?.isActive == true
    ) {
      transitionJob?.cancel()
      isTransitioning = false
    } else if (
      targetOffset != _scrollOffset &&
      !hadPreviousBehavior &&
      !shouldHide
    ) {
      transitionJob = launch {
        AnimationState(_scrollOffset, initialVelocity = 0f).animateTo(
          targetValue = targetOffset,
          animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
          ),
        ) {
          _scrollOffset = value
          isTransitioning = isRunning
        }
      }
    } else if (hadPreviousBehavior) {
      _scrollOffset = targetOffset
    }
  }

  fun clearScrollBehavior(behavior: TopAppBarScrollBehavior) {
    if (topAppBarScrollBehavior === behavior) {
      topAppBarScrollBehavior = null
    }
  }

  companion object {
    fun Saver(): Saver<CampfireNavigationBarState, *> =
      listSaver(
        save = {
          listOf(
            it._scrollOffset,
            it.scrollOffsetLimit,
            it.bottomBarOffset,
            it.shouldHide,
          )
        },
        restore = {
          CampfireNavigationBarState(
            initialOffset = it[0] as Float,
            initialOffsetLimit = it[1] as Float,
            initialBottomBarOffset = it[2] as Float,
            initialShouldHide = it[3] as Boolean,
          )
        },
      )
  }
}

@Composable
fun rememberCampfireNavigationBarState(): CampfireNavigationBarState {
  return rememberSaveable(saver = CampfireNavigationBarState.Saver()) {
    CampfireNavigationBarState(
      initialOffset = 0f,
      initialOffsetLimit = 0f,
      initialBottomBarOffset = 0f,
      initialShouldHide = false,
    )
  }
}

val LocalNavigationBarState = compositionLocalOf<CampfireNavigationBarState?> { null }

@Composable
fun AttachScrollBehaviorToLocalNavigationBar(scrollBehavior: SearchBarScrollBehavior) {
  val scope = rememberCoroutineScope()
  val navigationBarState = LocalNavigationBarState.current

  if (navigationBarState != null) {
    DisposableEffect(scrollBehavior) {
      with(navigationBarState) {
        scope.installScrollBehavior(scrollBehavior)
      }

      onDispose {
        navigationBarState.clearScrollBehavior(scrollBehavior)
      }
    }
  }
}

@Composable
fun AttachScrollBehaviorToLocalNavigationBar(scrollBehavior: TopAppBarScrollBehavior) {
  val scope = rememberCoroutineScope()
  val navigationBarState = LocalNavigationBarState.current

  if (navigationBarState != null) {
    DisposableEffect(scrollBehavior) {
      with(navigationBarState) {
        scope.installScrollBehavior(scrollBehavior)
      }

      onDispose {
        navigationBarState.clearScrollBehavior(scrollBehavior)
      }
    }
  }
}

val CampfireNavigationBarWindowInsets: WindowInsets
  @Composable get() {
    val navigationBarState = LocalNavigationBarState.current
    return if (navigationBarState != null) {
      val playbackBarOffset = navigationBarState.playbackBarOffset()
      WindowInsets(
        bottom = withDensity { playbackBarOffset.toDp() },
      )
    } else {
      WindowInsets()
    }
  }
