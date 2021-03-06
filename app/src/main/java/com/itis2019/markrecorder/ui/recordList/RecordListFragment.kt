package com.itis2019.markrecorder.ui.recordList

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itis2019.markrecorder.R
import com.itis2019.markrecorder.ui.adapters.RecordAdapter
import com.itis2019.markrecorder.ui.adapters.RecordDataItem
import com.itis2019.markrecorder.ui.base.BaseFragment
import com.itis2019.markrecorder.utils.MENU_DELETE
import com.itis2019.markrecorder.utils.MENU_RENAME
import com.itis2019.markrecorder.utils.dagger.FragmentInjectable
import com.itis2019.markrecorder.utils.dagger.injectViewModel
import com.itis2019.markrecorder.utils.deleteFile
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.explanation_content.*
import kotlinx.android.synthetic.main.fragment_lecture_list.*

class RecordListFragment : BaseFragment(), FragmentInjectable {

    override lateinit var viewModel: RecordListViewModel

    override fun initViewModel() {
        AndroidSupportInjection.inject(this)
        viewModel = injectViewModel(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_lecture_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecycler()

        tv_emoji.text = getString(R.string.empty_records_emoji)
        tv_explanation.text = getString(R.string.empty_records_explanation)

        (record_lecture_button).setOnClickListener {
            runWithPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ) {
                viewModel.openLectureRecorder(this)
            }
        }
    }

    override fun initObservers() {
        observeRecordList()
        observeLoading(progress_bar)
        observeRecordDelete()
        observeRecordRenaming()
    }

    private fun observeRecordRenaming() =
        viewModel.showRecordNameEditDialog.observe(this, Observer { record ->
            record?.let {
                fragmentManager?.let {
                    RecordRenameDialog.newInstance(record)
                        .show(childFragmentManager, "RecordCreationFragment")
                }
            }
        })

    private fun observeRecordDelete() =
        viewModel.recordDeleting.observe(this, Observer { it?.let { deleteFile(it) } })

    private fun observeRecordList() =
        viewModel.getRecords().observe(this, Observer {
            (rv_records.adapter as RecordAdapter).submitList(
                listOf(RecordDataItem.Header(getString(R.string.title_recent_records))) + it
            )
            explanation_content.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        })

    private fun initRecycler() {
        val manager = LinearLayoutManager(activity)
        rv_records.adapter =
            RecordAdapter({ id: Long -> viewModel.openLecture(this, id) }, menuItemClickListener)
        rv_records.layoutManager = manager
        registerForContextMenu(rv_records)
        rv_records.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    (record_lecture_button).shrink()
                    return
                }
                val firstItem = manager.findFirstCompletelyVisibleItemPosition()
                if (firstItem == 0) (record_lecture_button).extend()
            }
        })

        val paddingTop = rv_records.paddingTop
        rv_records.setOnApplyWindowInsetsListener { v, insets ->
            val top: Int = insets.systemWindowInsetTop + paddingTop
            v.setPadding(v.paddingStart, top, v.paddingEnd, v.paddingBottom)
            insets
        }
    }

    private val menuItemClickListener = MenuItem.OnMenuItemClickListener { menuItem ->
        val item = (rv_records.adapter as RecordAdapter)
            .currentList[menuItem.groupId] as RecordDataItem.RecordItem
        when (menuItem.itemId) {
            MENU_RENAME -> {
                viewModel.renameMenuItemClicked(item.record)
                true
            }
            MENU_DELETE -> {
                viewModel.deleteRecord(item.record)
                true
            }
            else -> false
        }
    }
}
