package com.altaureum.covid.tracking.di.module

import com.altaureum.covid.tracking.ui.fragments.FragmentContacts
import dagger.Module
import dagger.android.ContributesAndroidInjector


@Module
abstract class FragmentBuildersModule {

    @ContributesAndroidInjector
    internal abstract fun contributeFragmentContacts(): FragmentContacts

}
