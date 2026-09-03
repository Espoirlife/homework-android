package com.hwt.teacher.di

import android.content.Context
import com.hwt.teacher.data.AppDatabase
import com.hwt.teacher.data.HomeworkRepository
import com.hwt.teacher.util.PasswordStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides
    @Singleton
    fun providePasswordStore(@ApplicationContext context: Context): PasswordStore =
        PasswordStore(context)

    @Provides
    @Singleton
    fun provideRepository(db: AppDatabase, passwordStore: PasswordStore): HomeworkRepository =
        HomeworkRepository(db, passwordStore)
}
