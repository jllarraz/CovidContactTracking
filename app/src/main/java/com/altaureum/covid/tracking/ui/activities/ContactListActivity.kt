package com.altaureum.covid.tracking.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.realm.data.CovidContact
import com.altaureum.covid.tracking.ui.fragments.FragmentContacts
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector

import javax.inject.Inject

class ContactListActivity : AppCompatActivity(), HasAndroidInjector, FragmentContacts.OnListFragmentInteractionListener {



    @Inject
    open lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any>? {
        return fragmentDispatchingAndroidInjector
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_list)
        AndroidInjection.inject(this)

        if(savedInstanceState==null){
            selectContactListFragment()
        }
    }

    override fun onDestroy() {

        super.onDestroy()
    }

    override fun onSelected(contact: CovidContact) {
    }



    protected fun selectContactListFragment() {
        val fragmentManager = supportFragmentManager
        val ft = fragmentManager.beginTransaction()

        val fragment = FragmentContacts.newInstance()
        ft.replace(R.id.fragment, fragment, FragmentContacts::class.java.simpleName)
        ft.commit()
    }

    companion object{
        val TAG =ContactListActivity::class.java.simpleName
    }



}