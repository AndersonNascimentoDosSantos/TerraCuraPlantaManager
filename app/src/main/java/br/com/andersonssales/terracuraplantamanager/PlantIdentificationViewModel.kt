package br.com.andersonssales.terracuraplantamanager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class PlantIdentificationViewModel : ViewModel() {
    private val _plantInfo = MutableLiveData<JSONObject?>()
    val plantInfo: LiveData<JSONObject?> = _plantInfo

    fun updatePlantInfo(info: JSONObject?) {
        viewModelScope.launch(Dispatchers.Main) {
            _plantInfo.value = info
        }
    }
}



