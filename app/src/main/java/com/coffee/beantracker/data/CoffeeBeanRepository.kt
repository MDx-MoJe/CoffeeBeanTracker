package com.coffee.beantracker.data

import kotlinx.coroutines.flow.Flow

class CoffeeBeanRepository(private val coffeeBeanDao: CoffeeBeanDao) {
    val allBeans: Flow<List<CoffeeBean>> = coffeeBeanDao.getAllBeans()

    suspend fun getBeanById(id: Long): CoffeeBean? {
        return coffeeBeanDao.getBeanById(id)
    }

    suspend fun insert(bean: CoffeeBean): Long {
        return coffeeBeanDao.insertBean(bean)
    }

    suspend fun update(bean: CoffeeBean) {
        coffeeBeanDao.updateBean(bean)
    }

    suspend fun delete(bean: CoffeeBean) {
        coffeeBeanDao.deleteBean(bean)
    }

    suspend fun deleteById(id: Long) {
        coffeeBeanDao.deleteBeanById(id)
    }
}
