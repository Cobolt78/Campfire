@file:OptIn(
  ExperimentalSharedTransitionApi::class,
  ExperimentalMaterial3ExpressiveApi::class,
)

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.LibraryItemSharedTransitionKey
import app.campfire.common.compose.widgets.MediaProgressBar
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.placeholder_book
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.painterResource

class CoverImageSlot(
  private val imageUrl: String?,
  private val contentDescription: String?,
  private val sharedTransitionKey: String,
  private val progress: MediaProgress? = null,
) : ContentSlot {

  override val id: String = "cover_image"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) = SharedElementTransitionScope {
    BoxWithConstraints(
      modifier = modifier.fillMaxWidth(),
    ) {
      val size = minWidth * 0.75f
      val shape = MaterialTheme.shapes.largeIncreased
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier
            .size(size)
            .clip(shape),
        ) {
          CoverImage(
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            size = size,
            placeholder = painterResource(Res.drawable.placeholder_book),
            shape = shape,
            modifier = Modifier.fillMaxSize(),
            sharedElementModifier = Modifier
              .sharedElement(
                sharedContentState = rememberSharedContentState(
                  LibraryItemSharedTransitionKey(
                    id = sharedTransitionKey,
                    type = LibraryItemSharedTransitionKey.ElementType.Image,
                  ),
                ),
                animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
              ),
          )

          progress?.let { mediaProgress ->
            if (mediaProgress.isFinished || mediaProgress.actualProgress > 0f) {
              MediaProgressBar(
                mediaProgress = mediaProgress,
                trackHeight = 6.dp,
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .fillMaxWidth(),
              )
            }
          }
        }
      }
    }
  }
}
