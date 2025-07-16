package dev.anilbeesetti.nextplayer.feature.player.buffering

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.DefaultAllocator

/**
 * Optimized LoadControl for better video scrubbing and caching performance.
 * This configuration prioritizes larger buffers and smoother seeking operations.
 */
@OptIn(UnstableApi::class)
class OptimizedLoadControl private constructor(
    private val allocator: DefaultAllocator,
    private val minBufferMs: Int,
    private val maxBufferMs: Int,
    private val bufferForPlaybackMs: Int,
    private val bufferForPlaybackAfterRebufferMs: Int,
    private val targetBufferBytes: Int,
    private val prioritizeTimeOverSizeThresholds: Boolean,
    private val backBufferDurationMs: Int,
    private val retainBackBufferFromKeyframe: Boolean
) : LoadControl {

    private val defaultLoadControl = DefaultLoadControl.Builder()
        .setAllocator(allocator)
        .setBufferDurationsMs(
            minBufferMs,
            maxBufferMs,
            bufferForPlaybackMs,
            bufferForPlaybackAfterRebufferMs
        )
        .setTargetBufferBytes(targetBufferBytes)
        .setPrioritizeTimeOverSizeThresholds(prioritizeTimeOverSizeThresholds)
        .setBackBuffer(backBufferDurationMs, retainBackBufferFromKeyframe)
        .build()

    override fun onPrepared() = defaultLoadControl.onPrepared()

    override fun onTracksSelected(
        renderers: Array<out Renderer>,
        trackGroups: TrackGroupArray,
        trackSelections: Array<out ExoTrackSelection>
    ) = defaultLoadControl.onTracksSelected(renderers, trackGroups, trackSelections)

    override fun onStopped() = defaultLoadControl.onStopped()

    override fun onReleased() = defaultLoadControl.onReleased()

    override fun getAllocator(): Allocator = defaultLoadControl.allocator

    override fun getBackBufferDurationUs(): Long = defaultLoadControl.backBufferDurationUs

    override fun retainBackBufferFromKeyframe(): Boolean = defaultLoadControl.retainBackBufferFromKeyframe()

    override fun shouldContinueLoading(
        playbackPositionUs: Long,
        bufferedDurationUs: Long,
        playbackSpeed: Float
    ): Boolean = defaultLoadControl.shouldContinueLoading(playbackPositionUs, bufferedDurationUs, playbackSpeed)

    override fun shouldStartPlayback(
        bufferedDurationUs: Long,
        playbackSpeed: Float,
        rebuffering: Boolean,
        targetLiveOffsetUs: Long
    ): Boolean = defaultLoadControl.shouldStartPlayback(bufferedDurationUs, playbackSpeed, rebuffering, targetLiveOffsetUs)

    class Builder {
        private var allocator: DefaultAllocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)
        
        // Optimized buffer settings for better scrubbing performance
        private var minBufferMs = DEFAULT_MIN_BUFFER_MS * 2 // Increase min buffer
        private var maxBufferMs = DEFAULT_MAX_BUFFER_MS * 2 // Increase max buffer
        private var bufferForPlaybackMs = DEFAULT_BUFFER_FOR_PLAYBACK_MS
        private var bufferForPlaybackAfterRebufferMs = DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
        private var targetBufferBytes = DEFAULT_TARGET_BUFFER_BYTES * 4 // Quadruple target buffer
        private var prioritizeTimeOverSizeThresholds = false
        
        // Enhanced back buffer for better seeking performance
        private var backBufferDurationMs = DEFAULT_BACK_BUFFER_DURATION_MS * 4 // Keep more back buffer
        private var retainBackBufferFromKeyframe = true // Retain keyframes for faster seeking

        fun setAllocator(allocator: DefaultAllocator) = apply { this.allocator = allocator }

        fun setBufferDurationsMs(
            minBufferMs: Int,
            maxBufferMs: Int,
            bufferForPlaybackMs: Int,
            bufferForPlaybackAfterRebufferMs: Int
        ) = apply {
            this.minBufferMs = minBufferMs
            this.maxBufferMs = maxBufferMs
            this.bufferForPlaybackMs = bufferForPlaybackMs
            this.bufferForPlaybackAfterRebufferMs = bufferForPlaybackAfterRebufferMs
        }

        fun setTargetBufferBytes(targetBufferBytes: Int) = apply { 
            this.targetBufferBytes = targetBufferBytes 
        }

        fun setBackBuffer(backBufferDurationMs: Int, retainBackBufferFromKeyframe: Boolean) = apply {
            this.backBufferDurationMs = backBufferDurationMs
            this.retainBackBufferFromKeyframe = retainBackBufferFromKeyframe
        }

        fun build(): OptimizedLoadControl {
            return OptimizedLoadControl(
                allocator = allocator,
                minBufferMs = minBufferMs,
                maxBufferMs = maxBufferMs,
                bufferForPlaybackMs = bufferForPlaybackMs,
                bufferForPlaybackAfterRebufferMs = bufferForPlaybackAfterRebufferMs,
                targetBufferBytes = targetBufferBytes,
                prioritizeTimeOverSizeThresholds = prioritizeTimeOverSizeThresholds,
                backBufferDurationMs = backBufferDurationMs,
                retainBackBufferFromKeyframe = retainBackBufferFromKeyframe
            )
        }
    }

    companion object {
        // Default values from ExoPlayer's DefaultLoadControl
        const val DEFAULT_MIN_BUFFER_MS = 50_000 // 50 seconds
        const val DEFAULT_MAX_BUFFER_MS = 50_000 // 50 seconds
        const val DEFAULT_BUFFER_FOR_PLAYBACK_MS = 2_500 // 2.5 seconds
        const val DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000 // 5 seconds
        const val DEFAULT_TARGET_BUFFER_BYTES = -1 // No limit
        const val DEFAULT_BACK_BUFFER_DURATION_MS = 0 // No back buffer by default
        
        // Our optimized defaults
        const val OPTIMIZED_MIN_BUFFER_MS = 60_000 // 1 minute
        const val OPTIMIZED_MAX_BUFFER_MS = 180_000 // 3 minutes  
        const val OPTIMIZED_TARGET_BUFFER_BYTES = 64 * 1024 * 1024 // 64MB
        const val OPTIMIZED_BACK_BUFFER_DURATION_MS = 30_000 // 30 seconds back buffer
    }
}
