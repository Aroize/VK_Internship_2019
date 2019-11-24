package ru.rain.ifmo.vkinternship2019.presentation.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_folder_list.*
import ru.rain.ifmo.vkinternship2019.data.filesystem.MusicFolder
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.data.song.SongSingleton

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 22.11.2019
 */
class FolderListActivity : AppCompatActivity() {
    companion object {
        fun createIntent(packageContext: Context, folders: ArrayList<MusicFolder>): Intent {
            Companion.folders = folders
            return Intent(packageContext, FolderListActivity::class.java)
        }

        private var folders: ArrayList<MusicFolder>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_list)
        supportActionBar?.hide()
        val recyclerView = folders_list
        recyclerView.layoutManager = LinearLayoutManager(this)
        if (folders != null) {
            recyclerView.adapter = FolderAdapter(folders as ArrayList<MusicFolder>)
        }
    }

    private fun onPickFolder(folder: MusicFolder) {
        SongSingleton.instance.storage = folder
        setResult(Activity.RESULT_OK, Intent())
        finish()
    }

    inner class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private lateinit var data: MusicFolder

        init {
            view.setOnClickListener {
                onPickFolder(data)
            }
        }

        private val songCount = view.findViewById<TextView>(R.id.songs_count)
        private val folderPath = view.findViewById<TextView>(R.id.folder_path)

        fun bind(data: MusicFolder) {
            this.data = data
            songCount.text = "${data.songs.size}"
            folderPath.text = data.path
        }
    }

    inner class FolderAdapter(private val arrayList: ArrayList<MusicFolder>)
        : RecyclerView.Adapter<FolderViewHolder>() {
        override fun getItemCount(): Int = arrayList.size

        override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
            holder.bind(arrayList[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
            val view = layoutInflater.inflate(R.layout.item_folder, parent, false)
            return FolderViewHolder(view)
        }
    }
}