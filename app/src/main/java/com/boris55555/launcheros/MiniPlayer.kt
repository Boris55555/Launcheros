package com.boris55555.launcheros

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MiniPlayer(
    mediaMetadata: MediaMetadata?,
    playbackState: PlaybackState?,
    mediaController: MediaController?,
    onClose: () -> Unit,
    onClick: () -> Unit
) {
    val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp) // Narrower and smaller vertical padding
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(BorderStroke(2.dp, Color.Black), shape = RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 16.dp) // Smaller internal padding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Music Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { mediaController?.transportControls?.skipToPrevious() }
                )
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            if (isPlaying) mediaController?.transportControls?.pause()
                            else mediaController?.transportControls?.play()
                        }
                )
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { mediaController?.transportControls?.skipToNext() }
                )
            }

            // Close Button
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color.Black,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onClose() }
            )
        }
    }
}
