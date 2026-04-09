package com.suprajit.uvcluster.ui.features.settings.filemanager


import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suprajit.uvcluster.R
import java.io.File

class FileManagerFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var fab: FloatingActionButton

    // Navigation stack
    private val pathStack = mutableListOf<String>()

    // Current list
    private var currentList = mutableListOf<FileNode>()

    private var isGrid = false

    data class FileNode(
        val name: String,
        val fullPath: String,
        val isDirectory: Boolean,
        var isSelected: Boolean = false
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_file_manager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        recyclerView = view.findViewById(R.id.recyclerView)
        toolbar = view.findViewById(R.id.topBar)
        fab = view.findViewById(R.id.fab)

        setupLayout()
        setupToolbar()
        setupFab()

        loadDirectory("/")

        // 🔙 Back navigation
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (pathStack.size > 1) {
               pathStack.removeAt(pathStack.lastIndex)
                loadDirectory(pathStack.last())
            } else {
                requireActivity().finish()
            }
        }
    }

    // =========================================================
    //  MAIN LOADER (UNCHANGED + IMPROVED)
    // =========================================================
    private fun loadDirectory(path: String) {

        Log.d("FILE_MANAGER", "📂 Loading: $path")

        Thread {

            var files = listFilesWithSu(path)

            Log.d("FILE_MANAGER", "SU count: ${files.size}")

            if (files.isEmpty()) {
                files = listFilesNormal(path)
                Log.d("FILE_MANAGER", "SH count: ${files.size}")
            }

            if (files.isEmpty()) {

                val fallback = "/storage/emulated/0"

                requireActivity().runOnUiThread {
                    Toast.makeText(context, "Root not accessible", Toast.LENGTH_SHORT).show()
                }

                files = listFilesNormal(fallback)

                pathStack.clear()
                pathStack.add(fallback)
            }

            if (pathStack.isEmpty() || pathStack.last() != path) {
                pathStack.add(path)
            }

            currentList = files.toMutableList()

            requireActivity().runOnUiThread {
                refresh()
            }

        }.start()
    }

    private fun refresh() {
        recyclerView.adapter = FileAdapter(currentList)
    }

    // =========================================================
    // SORT
    // =========================================================
    private fun sortAZ() {
        currentList.sortBy { it.name.lowercase() }
        Toast.makeText(context, "Sorted A-Z", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun sortDate() {
        currentList.sortByDescending { File(it.fullPath).lastModified() }
        Toast.makeText(context, "Sorted by Date", Toast.LENGTH_SHORT).show()
        refresh()
    }

    // =========================================================
    // 🔍 SEARCH
    // =========================================================
    private fun search(query: String) {
        val filtered = currentList.filter {
            it.name.contains(query, true)
        }
        recyclerView.adapter = FileAdapter(filtered.toMutableList())
    }

    // =========================================================
    // SELECT ALL
    // =========================================================
    private fun selectAll() {
        currentList.forEach { it.isSelected = true }
        refresh()
    }

    // =========================================================
    // 🗑 DELETE
    // =========================================================
    private fun deleteSelected() {
        currentList.removeAll {
            if (it.isSelected) {
                File(it.fullPath).deleteRecursively()
                true
            } else false
        }
        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
        refresh()
    }

    // =========================================================
    //  RENAME
    // =========================================================
    private fun rename(node: FileNode) {

        val input = EditText(requireContext())
        input.setText(node.name)

        AlertDialog.Builder(requireContext())
            .setTitle("Rename")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val file = File(node.fullPath)
                val newFile = File(file.parent, input.text.toString())

                if (file.renameTo(newFile)) {
                    Toast.makeText(context, "Renamed", Toast.LENGTH_SHORT).show()
                    loadDirectory(pathStack.last())
                }
            }
            .show()
    }

    // =========================================================
    // SHARE
    // =========================================================
    private fun share(path: String) {
        val file = File(path)

        val uri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(Intent.createChooser(intent, "Share"))
    }

    // =========================================================
    // OPEN FILE
    // =========================================================
    private fun openFile(path: String) {
        val file = File(path)

        val uri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            file
        )

        val mime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mime ?: "*/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(intent)
    }

    // =========================================================
    // TOOLBAR
    // =========================================================
    private fun setupToolbar() {

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.sort_az -> { sortAZ(); true }
                R.id.sort_date -> { sortDate(); true }
                R.id.grid_toggle -> {
                    isGrid = !isGrid
                    setupLayout()
                    true
                }
                R.id.select_all -> { selectAll(); true }
                else -> false
            }
        }

        toolbar.setOnClickListener {
            val input = EditText(requireContext())

            AlertDialog.Builder(requireContext())
                .setTitle("Search")
                .setView(input)
                .setPositiveButton("Go") { _, _ ->
                    search(input.text.toString())
                }
                .show()
        }
    }

    // =========================================================
    // FAB
    // =========================================================
    private fun setupFab() {
        fab.setOnClickListener {

            val options = arrayOf("Delete", "Share", "Rename")

            AlertDialog.Builder(requireContext())
                .setItems(options) { _, which ->

                    val selected = currentList.find { it.isSelected }

                    when (which) {
                        0 -> deleteSelected()
                        1 -> selected?.let { share(it.fullPath) }
                        2 -> selected?.let { rename(it) }
                    }
                }
                .show()
        }
    }

    // =========================================================
    // GRID / LIST
    // =========================================================
    private fun setupLayout() {
        recyclerView.layoutManager = if (isGrid) {
            GridLayoutManager(context, 3)
        } else {
            LinearLayoutManager(context)
        }
    }

    // =========================================================
    // SHELL EXECUTION (UNCHANGED)
    // =========================================================
    private fun listFilesWithSu(path: String): List<FileNode> {
        return executeLs(arrayOf("su", "-c", "ls -p \"$path\""), path)
    }

    private fun listFilesNormal(path: String): List<FileNode> {
        return executeLs(arrayOf("sh", "-c", "ls -p \"$path\""), path)
    }

    private fun executeLs(cmd: Array<String>, path: String): List<FileNode> {

        val result = mutableListOf<FileNode>()

        try {
            val process = Runtime.getRuntime().exec(cmd)

            val reader = process.inputStream.bufferedReader()

            reader.forEachLine { line ->
                val isDir = line.endsWith("/")
                val cleanName = line.removeSuffix("/")

                val fullPath = if (path == "/") "/$cleanName" else "$path/$cleanName"

                result.add(FileNode(cleanName, fullPath, isDir))
            }

            process.waitFor()

        } catch (e: Exception) {
            Log.e("FILE_MANAGER", "Error", e)
        }

        return result
    }

    // =========================================================
    // ADAPTER
    // =========================================================
    inner class FileAdapter(private val files: MutableList<FileNode>) :
        RecyclerView.Adapter<FileAdapter.VH>() {

        inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context)
            tv.textSize = 16f
            tv.setPadding(40, 30, 20, 30)
            return VH(tv)
        }

        override fun getItemCount() = files.size

        override fun onBindViewHolder(holder: VH, position: Int) {

            val node = files[position]

            holder.tv.text =
                (if (node.isDirectory) "📁 " else "📄 ") + node.name

            holder.tv.setBackgroundColor(
                if (node.isSelected) 0xFFE0E0E0.toInt()
                else 0xFFFFFFFF.toInt()
            )

            holder.tv.setOnClickListener {
                if (node.isDirectory) {
                    loadDirectory(node.fullPath)
                } else {
                    openFile(node.fullPath)
                }
            }

            holder.tv.setOnLongClickListener {
                node.isSelected = !node.isSelected
                notifyItemChanged(position)
                true
            }
        }
    }
}
