package com.altaureum.covid.tracking.di.module

import com.altaureum.covid.tracking.ui.activities.ContactListActivity
import com.altaureum.covid.tracking.ui.activities.MainActivity
import com.altaureum.covid.tracking.ui.activities.PermissionsActivity
import com.altaureum.covid.tracking.ui.activities.RegistryActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector


@Module
abstract class ActivityBindingModule {
    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    internal abstract fun contributeMainActivity(): MainActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    internal abstract fun contributeContactListActivity(): ContactListActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    internal abstract fun contributeRegistryActivity(): RegistryActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    internal abstract fun contributePermissionsActivity(): PermissionsActivity
}
