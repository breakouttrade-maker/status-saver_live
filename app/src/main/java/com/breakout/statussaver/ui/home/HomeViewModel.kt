package com.breakout.statussaver.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.breakout.statussaver.data.model.Resource
import com.breakout.statussaver.data.model.StatusFile
import com.breakout.statussaver.data.repository.StatusRepository
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = StatusRepository(app)
    private val _images = MutableLiveData<Resource<List<StatusFile>>>()
    val images: LiveData<Resource<List<StatusFile>>> = _images
    private val _videos = MutableLiveData<Resource<List<StatusFile>>>()
    val videos: LiveData<Resource<List<StatusFile>>> = _videos
    private val _saved = MutableLiveData<Resource<List<StatusFile>>>()
    val saved: LiveData<Resource<List<StatusFile>>> = _saved

    private var imgLoaded = false; private var vidLoaded = false; private var savLoaded = false

    fun loadAll(force: Boolean = false) {
        if (force) { imgLoaded = false; vidLoaded = false; savLoaded = false }
        loadImg(); loadVid(); loadSav()
    }

    private fun loadImg() { if (imgLoaded) return; viewModelScope.launch { _images.value = Resource.Loading(); try { val l = repo.loadStatuses(StatusRepository.FilterType.IMAGE); imgLoaded = true; _images.value = if(l.isEmpty()) Resource.Empty else Resource.Success(l) } catch(e:Exception) { _images.value = Resource.Error(e.message?:"Error") } } }
    private fun loadVid() { if (vidLoaded) return; viewModelScope.launch { _videos.value = Resource.Loading(); try { val l = repo.loadStatuses(StatusRepository.FilterType.VIDEO); vidLoaded = true; _videos.value = if(l.isEmpty()) Resource.Empty else Resource.Success(l) } catch(e:Exception) { _videos.value = Resource.Error(e.message?:"Error") } } }
    fun loadSav() { if (savLoaded) return; viewModelScope.launch { _saved.value = Resource.Loading(false); try { val l = repo.loadSavedStatuses(); savLoaded = true; _saved.value = if(l.isEmpty()) Resource.Empty else Resource.Success(l) } catch(e:Exception) { _saved.value = Resource.Error(e.message?:"Error") } } }
    fun isSaved(f: StatusFile) = repo.isAlreadySaved(f)
}
