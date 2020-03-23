package com.altaureum.covid.tracking.di.module

import androidx.lifecycle.ViewModelProvider
import com.altaureum.covid.tracking.ui.viewmodel.ProjectViewModelFactory

import dagger.Binds
import dagger.Module


@Module
abstract class ViewModelModule {
    @Binds
    internal abstract fun bindViewModelFactory(projectViewModelFactory: ProjectViewModelFactory): ViewModelProvider.Factory
}
