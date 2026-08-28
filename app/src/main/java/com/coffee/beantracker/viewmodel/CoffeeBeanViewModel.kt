package com.coffee.beantracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.coffee.beantracker.data.CoffeeBean
import com.coffee.beantracker.data.CoffeeBeanDatabase
import com.coffee.beantracker.data.CoffeeBeanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CoffeeBeanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CoffeeBeanRepository
    val allBeans: LiveData<List<CoffeeBean>>

    init {
        val dao = CoffeeBeanDatabase.getDatabase(application).coffeeBeanDao()
        repository = CoffeeBeanRepository(dao)
        allBeans = repository.allBeans.asLiveData()
    }

    fun insert(bean: CoffeeBean) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(bean)
    }

    fun update(bean: CoffeeBean) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(bean)
    }

    fun delete(bean: CoffeeBean) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(bean)
    }

    fun deleteById(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteById(id)
    }

    suspend fun getBeanById(id: Long): CoffeeBean? {
        return repository.getBeanById(id)
    }
}
