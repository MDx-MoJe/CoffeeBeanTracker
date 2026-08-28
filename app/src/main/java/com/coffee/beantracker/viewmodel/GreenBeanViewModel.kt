package com.coffee.beantracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.coffee.beantracker.data.CoffeeBeanDatabase
import com.coffee.beantracker.data.GreenBean
import com.coffee.beantracker.data.GreenBeanDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GreenBeanViewModel(application: Application) : AndroidViewModel(application) {
    private val dao: GreenBeanDao
    val allGreenBeans: LiveData<List<GreenBean>>

    init {
        dao = CoffeeBeanDatabase.getDatabase(application).greenBeanDao()
        allGreenBeans = dao.getAllGreenBeans().asLiveData()
    }

    fun insert(bean: GreenBean) = viewModelScope.launch(Dispatchers.IO) {
        dao.insert(bean)
    }

    fun update(bean: GreenBean) = viewModelScope.launch(Dispatchers.IO) {
        dao.update(bean)
    }

    fun delete(bean: GreenBean) = viewModelScope.launch(Dispatchers.IO) {
        dao.delete(bean)
    }

    suspend fun getById(id: Long): GreenBean? = dao.getById(id)
}
