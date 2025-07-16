package dev.anilbeesetti.nextplayer.feature.player.scrubbing

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.TimeBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Optimized scrubbing handler that reduces excessive seeking operations during scrubbing.
 * This improves performance by batching seek operations and using intelligent buffering.
 */
@OptIn(UnstableApi::class)
class OptimizedScrubHandler(
    private val player: Player,
    private val coroutineScope: CoroutineScope,
    private val onScrubStart: (position: Long) -> Unit = {},
    private val onScrubMove: (position: Long) -> Unit = {},
    private val onScrubStop: (position: Long) -> Unit = {}
) : TimeBar.OnScrubListener {

    private var isPlayingOnScrubStart = false
    private var scrubStartPosition = -1L
    private var lastSeekPosition = -1L
    private var seekJob: Job? = null
    private var isActivelyScrubbing = false
    
    // Optimization parameters
    private val seekDelayMs = 100L // Delay between seeks to batch operations
    private val minSeekDistance = 1000L // Minimum distance to trigger seek (1 second)

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        isActivelyScrubbing = true
        scrubStartPosition = player.currentPosition
        lastSeekPosition = position
        
        // Store playing state and pause if needed
        if (player.isPlaying) {
            isPlayingOnScrubStart = true
            player.pause()
        }
        
        // Use precise seeking for initial scrub if ExoPlayer
        if (player is ExoPlayer) {
            player.setSeekParameters(SeekParameters.EXACT)
        }
        
        onScrubStart(position)
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        if (!isActivelyScrubbing) return
        
        lastSeekPosition = position
        
        // Cancel previous seek operation
        seekJob?.cancel()
        
        // Only seek if the distance is significant or after a delay
        val seekDistance = kotlin.math.abs(position - (player.currentPosition))
        
        if (seekDistance > minSeekDistance) {
            // For significant jumps, seek immediately but use keyframe seeking for better performance
            if (player is ExoPlayer) {
                player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            }
            performSeek(position)
        } else {
            // For small movements, delay the seek to batch operations
            seekJob = coroutineScope.launch {
                delay(seekDelayMs)
                if (isActivelyScrubbing && lastSeekPosition == position) {
                    if (player is ExoPlayer) {
                        player.setSeekParameters(SeekParameters.EXACT)
                    }
                    performSeek(position)
                }
            }
        }
        
        onScrubMove(position)
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        isActivelyScrubbing = false
        
        // Cancel any pending seek operations
        seekJob?.cancel()
        
        if (!canceled) {
            // Perform final precise seek
            if (player is ExoPlayer) {
                player.setSeekParameters(SeekParameters.EXACT)
            }
            performSeek(position)
        }
        
        // Reset seek parameters to default for normal playback
        if (player is ExoPlayer) {
            player.setSeekParameters(SeekParameters.DEFAULT)
        }
        
        // Resume playback if it was playing before scrubbing
        if (isPlayingOnScrubStart) {
            player.play()
        }
        
        // Reset state
        scrubStartPosition = -1L
        isPlayingOnScrubStart = false
        
        onScrubStop(position)
    }
    
    private fun performSeek(position: Long) {
        try {
            player.seekTo(position)
        } catch (e: Exception) {
            // Handle any seeking errors gracefully
            e.printStackTrace()
        }
    }
    
    /**
     * Cleanup method to cancel any pending operations
     */
    fun cleanup() {
        seekJob?.cancel()
        isActivelyScrubbing = false
    }
}
