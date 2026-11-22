package cl.duoc.tacticaltrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class SessionViewModelStore : ViewModel(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    fun clearSession() { viewModelStore.clear() }
    override fun onCleared() { clearSession() }
}
