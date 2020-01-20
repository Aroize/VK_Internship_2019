package ru.rain.ifmo.vkinternship2019.presentation.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.data.song.Song
import ru.rain.ifmo.vkinternship2019.data.song.SongSingleton
import ru.rain.ifmo.vkinternship2019.domain.OnSnapPositionChangedListener
import ru.rain.ifmo.vkinternship2019.domain.PlayerService
import ru.rain.ifmo.vkinternship2019.presentation.activity.MainActivity
import ru.rain.ifmo.vkinternship2019.toPlayerDuration

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 16.12.2019
 */
class MainPlayerFragment : AbstractPlayerFragment(), OnSnapPositionChangedListener {

    private val updateHandler = Handler(Handler.Callback {
        seekBar.progress += 1
        currentTrackPos.text = (seekBar.progress.toLong() * 1000).toPlayerDuration(false)
        true
    })

    private val messenger = Messenger(updateHandler)

    private val seekUpdater = object : Runnable {

        @Volatile var update = false

        override fun run() {
            try {
                while (!updateThread.isInterrupted) {
                    while (update) {
                        Thread.sleep(1_000)
                        if (update)
                            messenger.send(Message())
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private var updateThread: Thread = Thread(seekUpdater)

    override var parentId: String = "parent.id"

    lateinit var fragmentView: View

    private lateinit var authorName: TextView

    private lateinit var dropDown: ImageView

    private val listener = ViewTreeObserver.OnGlobalLayoutListener {
        val icon = fragmentView.findViewById<ImageView>(R.id.explicit_icon)
        trackName.maxWidth = fragmentView.findViewById<LinearLayout>(R.id.track_container).width - icon.width
        removeGlobalLayoutListener()
    }

    private fun removeGlobalLayoutListener() {
        fragmentView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }

    private lateinit var playBtn: ImageButton

    private lateinit var prevBtn: ImageButton

    private lateinit var nextBtn: ImageButton

    private lateinit var trackName: TextView

    private lateinit var currentTrackPos: TextView

    private lateinit var trackDuration: TextView

    private lateinit var seekBar: SeekBar

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {

        private var finalPosition = 0

        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            if (p2) {
                finalPosition = p1
            }
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {

        }

        override fun onStopTrackingTouch(p0: SeekBar?) {
            mediaController.transportControls.seekTo(finalPosition.toLong() * 1000)
            currentTrackPos.text = (finalPosition.toLong() * 1000).toPlayerDuration(false)
            finalPosition = 0
        }
    }

    private lateinit var recyclerView: RecyclerView

    private var dy = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView = inflater.inflate(R.layout.fragment_main_player, container, false)
        initViews()
        return fragmentView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateThread.interrupt()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews() {
        val localBroadcastManager = LocalBroadcastManager.getInstance(context as Context)
        localBroadcastManager.sendBroadcast(Intent(PlayerService.ACTION_GET_POSITION))
        trackName = fragmentView.findViewById(R.id.track_name)
        trackName.isSelected = true
        authorName = fragmentView.findViewById(R.id.author_name)
        currentTrackPos = fragmentView.findViewById(R.id.current_tack_pos)
        trackDuration = fragmentView.findViewById(R.id.track_duration)
        fragmentView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        dropDown = fragmentView.findViewById(R.id.drop_down)
        dropDown.setOnTouchListener { view, motionEvent ->
            var result = true
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d("ANIMATE_MOTION", "Down : y=${motionEvent.y}")
                    dy = view.y - motionEvent.rawY
                }
                MotionEvent.ACTION_UP -> {
                    Log.d("ANIMATE_MOTION", "Up : y=${motionEvent.y} rawY=${motionEvent.rawY}")
                    if (fragmentView.y < (activity as MainActivity).displayHeight().toFloat() / 3) {
                        fragmentView.also {
                            AnimatorSet().apply {
                                play(ObjectAnimator.ofFloat(it, "y", it.y, 0f).apply { duration = 250 })
                                interpolator = LinearInterpolator()
                                start()
                            }
                        }
                    } else {
                        (activity as MainActivity).swapPlayer()
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    Log.d("ANIMATE_MOTION", "Move : y=${motionEvent.y} rawY=${motionEvent.rawY}")
                    fragmentView.y = motionEvent.rawY + dy
                }
                else -> result = false
            }
            result
        }
        playBtn = fragmentView.findViewById(R.id.play_pause_btn)
        playBtn.setOnClickListener {
            if (mediaController.playbackState?.state == PlaybackState.STATE_PLAYING) {
                mediaController.transportControls.pause()
            } else {
                mediaController.transportControls.play()
            }
            swapImage()
        }
        prevBtn = fragmentView.findViewById(R.id.prev_btn)
        prevBtn.setOnClickListener {
            mediaController.transportControls.skipToPrevious()
        }
        nextBtn = fragmentView.findViewById(R.id.next_btn)
        nextBtn.setOnClickListener {
            mediaController.transportControls.skipToNext()
        }
        seekBar = fragmentView.findViewById(R.id.track_seek_bar)
        seekBar.setOnSeekBarChangeListener(seekBarListener)
        recyclerView = fragmentView.findViewById(R.id.album_images)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = SongAdapter()
        val snapHelper = SingleSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            private var snapPosition = RecyclerView.NO_POSITION

            private var fromUser = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> fromUser = true
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        val snapPosition = snapHelper.getSnapPosition(recyclerView)
                        if (this.snapPosition != snapPosition && fromUser) {
                            this@MainPlayerFragment.onSnapPositionChanged(snapPosition)
                            this.snapPosition = snapPosition
                        }
                        fromUser = false
                    }
                }
            }
        })
    }

    override fun onSnapPositionChanged(position: Int) {
        if (position < SongSingleton.instance.index) {
            mediaController.transportControls.skipToPrevious()
        } else {
            mediaController.transportControls.skipToNext()
        }
    }

    override fun updateInfo(mediaMetadata: MediaMetadata) {
        trackName.text = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val duration = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        trackDuration.text = duration.toPlayerDuration()
        authorName.text = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        seekBar.max = (duration / 1000).toInt()
        recyclerView.smoothScrollToPosition(SongSingleton.instance.index)
        val pos = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DISC_NUMBER)
        seekBar.progress = (pos / 1000).toInt()
        currentTrackPos.text = pos.toPlayerDuration(false)
    }

    override fun swapImage() {
        mediaController.playbackState?.let {
            if (it.state == PlaybackState.STATE_PLAYING) {
                seekUpdater.update = true
                playBtn.setImageResource(R.drawable.ic_pause_48)
            } else {
                seekUpdater.update = false
                playBtn.setImageResource(R.drawable.ic_play_48)
            }
        }
    }

    private inner class SongViewHolder(view: View): RecyclerView.ViewHolder(view) {

        private val imageView = itemView.findViewById<ImageView>(R.id.album_image)

        fun bind(song: Song) {
            if (song.albumImage == null) {
                imageView.setImageResource(R.drawable.itunes_no_artwork)
            } else {
                imageView.setImageBitmap(song.albumImage)
            }
        }
    }

    private inner class SongAdapter : RecyclerView.Adapter<SongViewHolder>() {

        val songSongSingleton = SongSingleton.instance

        override fun getItemCount(): Int = songSongSingleton.playList.size

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            holder.bind(songSongSingleton.playList[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
            return SongViewHolder(layoutInflater.inflate(R.layout.item_view, parent, false))
        }
    }

    private class SingleSnapHelper : LinearSnapHelper() {
        override fun findTargetSnapPosition(
            layoutManager: RecyclerView.LayoutManager?,
            velocityX: Int,
            velocityY: Int
        ): Int {
            layoutManager ?: return RecyclerView.NO_POSITION
            val currentView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
            return layoutManager.getPosition(currentView)
        }

        fun getSnapPosition(recyclerView: RecyclerView): Int {
            val lm = recyclerView.layoutManager ?: return RecyclerView.NO_POSITION
            val view = findSnapView(lm) ?: return RecyclerView.NO_POSITION
            return lm.getPosition(view)
        }
    }
}