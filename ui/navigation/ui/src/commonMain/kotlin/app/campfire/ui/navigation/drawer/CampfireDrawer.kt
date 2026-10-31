// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.drawer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Brightness6
import app.campfire.common.compose.icons.rounded.DarkMode
import app.campfire.common.compose.icons.rounded.LightMode
import app.campfire.common.compose.icons.rounded.Palette
import app.campfire.common.compose.layout.NavigationType
import app.campfire.common.compose.layout.isLandscapePhone
import app.campfire.common.compose.layout.navigationType
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.UserScope
import app.campfire.core.reflect.instanceOf
import app.campfire.settings.api.ThemeMode.DARK
import app.campfire.settings.api.ThemeMode.LIGHT
import app.campfire.settings.api.ThemeMode.SYSTEM
import app.campfire.ui.navigation.HomeNavigationItem
import app.campfire.ui.navigation.NavigationPresenterFactory
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.campfire.common.compose.icons.rounded.CheckCircle
import app.campfire.common.compose.icons.rounded.Sync
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.updates.AppUpdateWidget
import app.campfire.updates.source.AppUpdateSource
import app.campfire.updates.source.CheckUpdateResult
import app.campfire.whatsnew.api.WhatsNewWidgetProvider
import app.campfire.whatsnew.api.screen.ChangelogScreen
import campfire.ui.navigation.ui.generated.resources.Res
import campfire.ui.navigation.ui.generated.resources.action_change_theme
import campfire.ui.navigation.ui.generated.resources.action_change_theme_mode
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.ContributesTo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@ContributesTo(UserScope::class)
interface CampfireDrawerComponent {
  val whatsNewWidget: WhatsNewWidgetProvider
  val appUpdateWidget: AppUpdateWidget
  val appUpdateSource: AppUpdateSource
  val navigationPresenterFactory: NavigationPresenterFactory
}

@Composable
fun CampfireDrawer(
  currentScreen: Screen,
  drawerState: DrawerState,
  navigator: Navigator,
  accountSwitcher: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  component: CampfireDrawerComponent = rememberComponent(),
) {
  val scope = rememberCoroutineScope()
  var isCheckingForUpdates by remember { mutableStateOf(false) }
  var updateFeedback by remember { mutableStateOf<DrawerUpdateFeedback?>(null) }
  var isUpdateAvailable by remember { mutableStateOf(false) }

  LaunchedEffect(component.appUpdateSource) {
    isUpdateAvailable = component.appUpdateSource.isUpdateAvailable()
    component.appUpdateSource.changes().collect {
      isUpdateAvailable = component.appUpdateSource.isUpdateAvailable()
    }
  }
  val presenter = remember(component) {
    component.navigationPresenterFactory()
  }
  val state = presenter.presentDrawer()
  val windowSizeClass = LocalWindowSizeClass.current
  val isPermanentDrawer = windowSizeClass.navigationType == NavigationType.Drawer

  // Prefer an exact match so sibling items sharing a screen class (e.g. Settings and
  // Downloads) don't both light up; fall back to the class for parameterised screens.
  val selectedScreen = state.navigationItems.firstOrNull { it.screen == currentScreen }?.screen
    ?: state.navigationItems.firstOrNull { it.screen.instanceOf(currentScreen::class) }?.screen

  DrawerSheet(
    modifier = modifier,
    windowInsets = if (windowSizeClass.isLandscapePhone) {
      DrawerDefaults.windowInsets
        .only(WindowInsetsSides.Horizontal)
    } else {
      DrawerDefaults.windowInsets
    },
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
    ) {
      if (windowSizeClass.isLandscapePhone) {
        Spacer(Modifier.height(24.dp))
      }

      accountSwitcher()

      component.whatsNewWidget.Content(
        onClick = {
          navigator.goTo(ChangelogScreen)
          scope.launch {
            drawerState.close()
          }
        },
        modifier = Modifier.padding(
          horizontal = 16.dp,
        ),
      )

      Spacer(Modifier.height(8.dp))

      state.navigationItems.forEach { item ->
        if (item.screen == ChangelogScreen) {
          NavigationDrawerItem(
            icon = {
              if (isCheckingForUpdates) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  strokeWidth = 2.dp,
                )
              } else {
                BadgedBox(
                  badge = {
                    if (isUpdateAvailable) {
                      Badge()
                    }
                  },
                ) {
                  Icon(
                    imageVector = CampfireIcons.Rounded.Sync,
                    contentDescription = "Check for updates",
                  )
                }
              }
            },
            label = { Text(text = "Check for updates") },
            selected = false,
            onClick = {
              if (!isCheckingForUpdates) {
                isCheckingForUpdates = true
                updateFeedback = null
                scope.launch {
                  try {
                    val result = component.appUpdateSource.checkForUpdates(force = true)
                    when (result) {
                      is CheckUpdateResult.Available -> {
                        updateFeedback = null
                      }
                      is CheckUpdateResult.UpToDate -> {
                        updateFeedback = DrawerUpdateFeedback.UpToDate("Campfire is up to date (v${state.appVersion})")
                        delay(4000)
                        if (updateFeedback is DrawerUpdateFeedback.UpToDate) {
                          updateFeedback = null
                        }
                      }
                      is CheckUpdateResult.Error -> {
                        updateFeedback = DrawerUpdateFeedback.Error("Couldn't connect to GitHub")
                        delay(4000)
                        if (updateFeedback is DrawerUpdateFeedback.Error) {
                          updateFeedback = null
                        }
                      }
                    }
                  } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    updateFeedback = DrawerUpdateFeedback.Error("Couldn't connect to GitHub")
                    delay(4000)
                    if (updateFeedback is DrawerUpdateFeedback.Error) {
                      updateFeedback = null
                    }
                  } finally {
                    isCheckingForUpdates = false
                  }
                }
              }
            },
            modifier = Modifier.padding(
              horizontal = 16.dp,
            ),
          )

          component.appUpdateWidget.Content(
            modifier = Modifier.padding(
              horizontal = 16.dp,
              vertical = 4.dp,
            ),
          )

          AnimatedVisibility(
            visible = updateFeedback != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
          ) {
            val feedback = updateFeedback
            if (feedback != null) {
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  val (icon, tint) = when (feedback) {
                    is DrawerUpdateFeedback.UpToDate ->
                      CampfireIcons.Rounded.CheckCircle to MaterialTheme.colorScheme.primary
                    is DrawerUpdateFeedback.Error ->
                      CampfireIcons.Rounded.Warning to MaterialTheme.colorScheme.error
                  }
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = feedback.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }

        DestinationListItem(
          item = item,
          selected = item.screen == selectedScreen,
          onClick = {
            if (isPermanentDrawer) {
              // A permanent drawer is the top-level switcher, like the bottom bar and rail:
              // swap the root instead of stacking destinations on top of each other.
              navigator.resetRoot(item.screen)
            } else {
              navigator.goTo(item.screen)
              scope.launch {
                drawerState.close()
              }
            }
          },
        )
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          horizontal = 16.dp,
          vertical = 8.dp,
        ),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "v${state.appVersion}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp),
      )

      Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val changeThemeModeLabel = stringResource(Res.string.action_change_theme_mode)
        IconButtonTooltip(
          text = changeThemeModeLabel,
        ) {
          OutlinedIconButton(
            shapes = IconButtonDefaults.shapes(
              shape = RoundedCornerShape(
                topStart = CornerSize(50),
                bottomStart = CornerSize(50),
                topEnd = CornerSize(4.dp),
                bottomEnd = CornerSize(4.dp),
              ),
            ),
            onClick = {
              state.eventSink(DrawerUiEvent.CycleThemeMode)
            },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .size(
                IconButtonDefaults.smallContainerSize(
                  IconButtonDefaults.IconButtonWidthOption.Wide,
                ),
              ),
          ) {
            AnimatedContent(
              targetState = state.themeMode,
              transitionSpec = {
                val inSpec: FiniteAnimationSpec<Float> = tween(220, delayMillis = 90)
                (
                  fadeIn(animationSpec = inSpec) + scaleIn(
                    initialScale = 0f,
                    animationSpec = inSpec,
                  )
                  ) togetherWith (
                  scaleOut(
                    animationSpec = tween(90),
                  ) + fadeOut(tween(90))
                  )
              },
            ) { mode ->
              Icon(
                when (mode) {
                  LIGHT -> CampfireIcons.Rounded.LightMode
                  DARK -> CampfireIcons.Rounded.DarkMode
                  SYSTEM -> CampfireIcons.Rounded.Brightness6
                },
                contentDescription = changeThemeModeLabel,
              )
            }
          }
        }

        val changeThemeLabel = stringResource(Res.string.action_change_theme)
        IconButtonTooltip(
          text = changeThemeLabel,
        ) {
          FilledTonalIconButton(
            shapes = IconButtonDefaults.shapes(
              shape = RoundedCornerShape(
                topStart = CornerSize(4.dp),
                bottomStart = CornerSize(4.dp),
                topEnd = CornerSize(50),
                bottomEnd = CornerSize(50),
              ),
            ),
            colors = IconButtonDefaults.filledTonalIconButtonColors(),
            onClick = {
              navigator.goTo(ThemePickerScreen)
              scope.launch {
                drawerState.close()
              }
            },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .size(
                IconButtonDefaults.smallContainerSize(
                  IconButtonDefaults.IconButtonWidthOption.Wide,
                ),
              ),
          ) {
            Icon(CampfireIcons.Rounded.Palette, contentDescription = changeThemeLabel)
          }
        }
      }
    }
  }
}

@Composable
private fun DestinationListItem(
  item: HomeNavigationItem,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationDrawerItem(
    icon = {
      Icon(
        imageVector = item.iconImageVector,
        contentDescription = item.contentDescription,
      )
    },
    label = { Text(text = item.label) },
    selected = selected,
    onClick = onClick,
    modifier = modifier
      .padding(
        horizontal = 16.dp,
      ),
  )
}

@Composable
private fun DrawerSheet(
  modifier: Modifier = Modifier,
  windowInsets: WindowInsets = DrawerDefaults.windowInsets,
  content: @Composable ColumnScope.() -> Unit,
) {
  val navigationType = LocalWindowSizeClass.current.navigationType
  if (navigationType == NavigationType.Drawer) {
    PermanentDrawerSheet(
      content = content,
      modifier = modifier,
      windowInsets = windowInsets,
    )
  } else {
    ModalDrawerSheet(
      content = content,
      modifier = modifier,
      windowInsets = windowInsets,
    )
  }
}

private sealed interface DrawerUpdateFeedback {
  val message: String

  data class UpToDate(override val message: String) : DrawerUpdateFeedback
  data class Error(override val message: String) : DrawerUpdateFeedback
}
