package com.altaureum.covid.tracking.ui.fragments

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.common.Preferences
import com.altaureum.covid.tracking.di.Injectable
import com.altaureum.covid.tracking.realm.data.CovidContact
import com.altaureum.covid.tracking.realm.utils.LiveRealmData
import com.altaureum.covid.tracking.realm.utils.RealmUtils
import com.altaureum.covid.tracking.realm.utils.covidContacts
import com.altaureum.covid.tracking.ui.adapters.ContactsAdapter
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.fragment_covid_contacts_list.*
import kotlinx.android.synthetic.main.fragment_registry.*

class FragmentRegistry: Fragment(), Injectable {
    var mListener: OnListFragmentInteractionListener? = null



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_registry, container, false)


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        buttonContinue.setOnClickListener {
            val covidId = covidId.text.toString()
            if(covidId.isNotEmpty()){
                val defaultSharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(activity)
                val edit = defaultSharedPreferences.edit()
                edit.putString(Preferences.KEY_COVID_ID, covidId)
                edit.commit()
                mListener?.onContinue()
            } else{
                Toast.makeText(activity,getString(R.string.warning_bad_covid), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }



    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnListFragmentInteractionListener")
        }
    }


    override fun onDetach() {
        super.onDetach()
        mListener = null
    }


    override fun onDestroyView() {
        super.onDestroyView()
    }

    interface OnListFragmentInteractionListener {
        fun onContinue()
    }

    companion object {
        private val TAG = FragmentRegistry::class.java.simpleName
        fun newInstance(): FragmentRegistry {
            val fragment = FragmentRegistry()
            val arguments = Bundle()
            fragment.arguments = arguments
            return fragment
        }
    }
}