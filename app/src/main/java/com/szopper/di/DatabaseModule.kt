package com.szopper.di

import com.szopper.data.local.RealmDatabase
import com.szopper.data.repository.ProductRepositoryImpl
import com.szopper.domain.repository.ProductRepository
import com.szopper.domain.usecase.ReorderProductsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: ProductRepositoryImpl
    ): ProductRepository

    companion object {
        @Provides
        @Singleton
        fun provideRealmDatabase(): RealmDatabase = RealmDatabase()

        @Provides
        @Singleton
        fun provideReorderProductsUseCase(productRepository: ProductRepository): ReorderProductsUseCase {
            return ReorderProductsUseCase(productRepository)
        }
    }
}