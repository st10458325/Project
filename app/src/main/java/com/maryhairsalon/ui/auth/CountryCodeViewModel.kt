package com.maryhairsalon.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maryhairsalon.data.repository.CountryRepository
import com.maryhairsalon.data.repository.DialCodeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CountryCodeViewModel(
    private val repository: CountryRepository = CountryRepository()
) : ViewModel() {
    private val _options = MutableStateFlow<List<DialCodeOption>>(emptyList())
    val options: StateFlow<List<DialCodeOption>> = _options.asStateFlow()

    init {
        viewModelScope.launch { _options.value = repository.getDialCodes() }
    }
}