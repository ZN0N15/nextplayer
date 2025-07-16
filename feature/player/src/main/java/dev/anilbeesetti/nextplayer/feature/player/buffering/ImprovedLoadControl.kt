package dev.anilbeesetti.nextplayer.feature.player.buffering

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.DefaultAllocator

/**
 * Simplified LoadControl focused on improving backward scrubbing performance
 * by retaining more back buffer and using larger buffer sizes.
 */
@OptIn(UnstableApi::class)
class ImprovedLoadControl private constructor(
    private val delegate: DefaultLoadControl
) : LoadControl by delegate {

    class Builder {
        // Buffer settings optimized for backward seeking
        private var minBufferMs = 60_000 // 1 minute minimum buffer
        private var maxBufferMs = 180_000 // 3 minutes maximum buffer  
        private var bufferForPlaybackMs = 2_500 // Start playback after 2.5s
        private var bufferForPlaybackAfterRebufferMs = 5_000 // Resume after 5s rebuffer
        private var backBufferDurationMs = 60_000 // Keep 1 minute of back buffer
        private var retainBackBufferFromKeyframe = true
        private var targetBufferBytes = 64 * 1024 * 1024 // 64MB target buffer
        
        fun build(): ImprovedLoadControl {
            val allocator = DefaultAllocator(
                /* trimOnReset= */ true,
                /* individualAllocationSize= */ C.DEFAULT_BUFFER_SEGMENT_SIZE
            )
            
            val defaultLoadControl = DefaultLoadControl.Builder()
                .setAllocator(allocator)
                .setBufferDurationsMs(
                    minBufferMs,
                    maxBufferMs,
                    bufferForPlaybackMs,
                    bufferForPlaybackAfterRebufferMs
                )
                .setBackBuffer(backBufferDurationMs, retainBackBufferFromKeyframe)
                .setTargetBufferBytes(targetBufferBytes)
                .build()
                
            return ImprovedLoadControl(defaultLoadControl)
        }
    }
    
    companion object {
        fun builder(): Builder = Builder()
    }
}
