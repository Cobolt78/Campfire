// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.User
import app.campfire.core.session.UserSession
import app.campfire.core.session.user
import app.campfire.user.api.UserRepository
import app.campfire.user.store.UserStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.impl.extensions.get

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreUserRepository(
  private val userSession: UserSession,
  private val userStoreFactory: UserStore.Factory,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : UserRepository {

  private val userStore by lazy { userStoreFactory.create() }

  override val userFlow: StateFlow<User> by lazy {
    if (userSession !is UserSession.LoggedIn) {
      MutableStateFlow(userSession.user ?: User.EMPTY).asStateFlow()
    } else {
      // Refreshed from the server once per session: the server only pushes `user_updated` to the
      // admin who changed an account, never to the account itself, so this is how a user's own
      // permission changes (e.g. download) reach the app. Offline, the fetch fails and the cached
      // user stands.
      userStore.stream(StoreReadRequest.cached(Unit, refresh = true))
        .mapNotNull { it.dataOrNull() }
        .distinctUntilChanged()
        .stateIn(
          // Cache this in our UserScope coroutine scope
          scope = coroutineScopeHolder.get(),
          started = SharingStarted.Eagerly,
          initialValue = userSession.user,
        )
    }
  }

  override fun observeCurrentUser(): Flow<User> {
    if (userSession !is UserSession.LoggedIn) {
      return userSession.user?.let { flowOf(it) } ?: emptyFlow()
    }
    return userStore.stream(StoreReadRequest.cached(Unit, refresh = false))
      .mapNotNull { it.dataOrNull() }
      .distinctUntilChanged()
  }

  override suspend fun getCurrentUser(): User {
    if (userSession !is UserSession.LoggedIn) {
      return userSession.user ?: User.EMPTY
    }
    return try {
      userStore.get(Unit)
    } catch (_: Exception) {
      userSession.user
    }
  }
}
