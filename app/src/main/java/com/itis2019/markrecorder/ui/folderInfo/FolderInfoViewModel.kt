package com.itis2019.markrecorder.ui.folderInfo

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.itis2019.markrecorder.entities.Folder
import com.itis2019.markrecorder.repository.FolderRepository
import com.itis2019.markrecorder.repository.RecordRepository
import com.itis2019.markrecorder.router.Router
import com.itis2019.markrecorder.ui.adapters.RecordDataItem
import com.itis2019.markrecorder.ui.base.BaseRecordListViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class FolderInfoViewModel @Inject constructor(
    private val lectureRepository: RecordRepository,
    private val folderRepository: FolderRepository,
    private val router: Router
) : BaseRecordListViewModel(lectureRepository) {

    private val folder = MutableLiveData<Folder>()
    private lateinit var header: RecordDataItem.Header

    fun getFolder(): LiveData<Folder> = folder

    fun openRecordFromFolder(fragment: Fragment, id: Long) =
        router.openRecordFromFolder(fragment, id)

    fun getFolder(folderId: Long) {
        disposables.add(folderRepository.
            getFolder(folderId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { loadingData.setValue(true) }
                .doAfterTerminate { loadingData.setValue(false) }
                .subscribe(
                    {
                        folder.value = it
                        header =
                            RecordDataItem.Header(getFolder().value?.name ?: "", isWhite = true)
                        records.value = listOf(header)
                        getRecords(folderId)
                    },
                    { errorData.value = it }
                ))
    }

    private fun getRecords(folderId: Long) =
        disposables.add(lectureRepository.getRecordsFromFolder(folderId)
            .map { it.map { record -> RecordDataItem.RecordItem(record, true) } }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { loadingData.setValue(true) }
            .doAfterNext { loadingData.setValue(false) }
            .subscribe(
                { records.value = listOf(header.copy(recordsCount = it.count())) + it },
                { errorData.value = it }
            ))
}
