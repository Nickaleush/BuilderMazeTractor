package com.nickaleush.tractormaze.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.ToneGenerator

/**
 * Central audio controller for the tractor maze game.
 *
 * The project can work without bundled audio files: missing sounds fall back to
 * short synthesized tones, while missing music is simply skipped. To add real
 * audio later, place files in `res/raw` using the names from [SoundEffect] and
 * [MusicTrack].
 */
class SoundManager(
    private val context: Context
) {

    enum class SoundEffect(val rawResName: String, private val toneType: Int) {
        Turn("sfx_turn", ToneGenerator.TONE_PROP_BEEP),
        Collect("sfx_collect", ToneGenerator.TONE_PROP_ACK),
        Crash("sfx_crash", ToneGenerator.TONE_CDMA_LOW_L),
        Win("sfx_win", ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD),
        Lose("sfx_lose", ToneGenerator.TONE_CDMA_ABBR_ALERT);

        fun toneType(): Int = toneType
    }

    enum class MusicTrack(val rawResName: String) {
        Menu("music_menu"),
        Game("music_game"),
    }

    private var soundEnabled = true
    private var musicEnabled = true
    private var soundVolume = 0.9f
    private var musicVolume = 0.8f

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val effectIds = mutableMapOf<SoundEffect, Int>()

    private var toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    }.getOrNull()

    private var musicPlayer: MediaPlayer? = null
    private var currentTrack: MusicTrack? = null
    private var loadedTrack: MusicTrack? = null

    init {
        SoundEffect.entries.forEach { effect ->
            val resId = rawResId(effect.rawResName)
            if (resId != 0) {
                effectIds[effect] = soundPool.load(context, resId, 1)
            }
        }
    }

    fun applySettings(
        soundEnabled: Boolean,
        musicEnabled: Boolean,
        soundVolume: Float = this.soundVolume,
        musicVolume: Float = this.musicVolume
    ) {
        this.soundEnabled = soundEnabled
        this.musicEnabled = musicEnabled
        this.soundVolume = soundVolume.coerceIn(0f, 1f)
        this.musicVolume = musicVolume.coerceIn(0f, 1f)
        musicPlayer?.setVolume(this.musicVolume, this.musicVolume)
        if (!musicEnabled) {
            pauseMusic()
        } else {
            currentTrack?.let { resumeOrStart(it) }
        }
    }

    fun playEffect(effect: SoundEffect) {
        if (!soundEnabled) return
        val soundId = effectIds[effect]
        if (soundId != null) {
            soundPool.play(soundId, soundVolume, soundVolume, 1, 0, 1f)
        } else {
            if (soundVolume > 0f) runCatching { toneGenerator?.startTone(effect.toneType(), 160) }
        }
    }

    fun playMusic(track: MusicTrack) {
        currentTrack = track
        if (!musicEnabled) return
        resumeOrStart(track)
    }

    private fun resumeOrStart(track: MusicTrack) {
        val resId = rawResId(track.rawResName)
        if (resId == 0) return
        if (musicPlayer != null && loadedTrack == track) {
            runCatching { if (musicPlayer?.isPlaying == false) musicPlayer?.start() }
            return
        }
        releaseMusicPlayer()
        musicPlayer = MediaPlayer.create(context, resId)?.apply {
            isLooping = true
            setVolume(musicVolume, musicVolume)
            runCatching { start() }
        }
        loadedTrack = if (musicPlayer != null) track else null
    }

    fun pauseMusic() {
        runCatching { if (musicPlayer?.isPlaying == true) musicPlayer?.pause() }
    }

    fun resumeMusic() {
        if (!musicEnabled) return
        currentTrack?.let { resumeOrStart(it) }
    }

    fun stopMusic() {
        currentTrack = null
        releaseMusicPlayer()
    }

    private fun releaseMusicPlayer() {
        runCatching {
            musicPlayer?.stop()
            musicPlayer?.release()
        }
        musicPlayer = null
        loadedTrack = null
    }

    fun release() {
        releaseMusicPlayer()
        runCatching { soundPool.release() }
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }

    private fun rawResId(name: String): Int =
        context.resources.getIdentifier(name, "raw", context.packageName)
}
