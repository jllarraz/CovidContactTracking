package com.altaureum.covid.tracking.ui.fragments

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.altaureum.covid.tracking.R
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

class FragmentContacts: Fragment(), Injectable {
    var mListener: OnListFragmentInteractionListener? = null

    /**
     * List to show the vehicles recognised
     */

    lateinit var loadingLayout: ConstraintLayout
    lateinit var emptyLayout: ConstraintLayout

    var realm: Realm?=null
    internal var response: RealmResults<CovidContact>? = null
    private var liveRealmData: LiveRealmData<CovidContact>?=null
    var disposable = CompositeDisposable()


    val handler: Handler = Handler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_covid_contacts_list, container, false)


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingLayout = view.findViewById(R.id.layout_loading)
        emptyLayout = view.findViewById(R.id.layout_empty)

        list!!.addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(context,
            androidx.recyclerview.widget.DividerItemDecoration.VERTICAL))

        list!!.setOnTouchListener { view, event ->
            val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            false
        }

        realm = RealmUtils.getInstance(activity!!)
        showProgressBar(true)
        liveRealmData = realm?.covidContacts()?.getAsyncAsLiveData(covidIds = null)

        liveRealmData?.observe(viewLifecycleOwner,
            androidx.lifecycle.Observer<RealmResults<CovidContact>> {
                try {
                    if(response == null) {
                        response = it
                        list?.adapter = ContactsAdapter(
                            response,
                            true,
                            object: ContactsAdapter.AdapterCallback{
                                override fun onSelected(contact: CovidContact){
                                    mListener?.onSelected(realm?.copyFromRealm(contact)!!)
                                }

                                override fun onEmptyList() {
                                    showBlankSlateIfEmpty()
                                }

                                override fun onLoading(isLoading: Boolean) {
                                    showProgressBar(isLoading)
                                }
                            })

                    }
                    showProgressBar(false)
                    showBlankSlateIfEmpty()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun showBlankSlateIfEmpty() {
        if (list!!.adapter!!.itemCount == 0) {
            list!!.visibility = View.GONE
            layout_empty!!.visibility = View.VISIBLE
            loadingLayout.visibility = View.INVISIBLE
            //empty_view!!.visibility = View.VISIBLE
        } else {
            layout_empty!!.visibility = View.GONE
            loadingLayout.visibility = View.VISIBLE
            //empty_view!!.visibility = View.GONE
            list!!.visibility = View.VISIBLE
        }
    }

    private fun showProgressBar(isShown:Boolean){
        if(isShown){
            list!!.visibility = View.GONE
            layout_empty!!.visibility = View.VISIBLE
            loadingLayout.visibility = View.VISIBLE
        } else {
            layout_empty!!.visibility = View.GONE
            loadingLayout.visibility = View.INVISIBLE
            list!!.visibility = View.VISIBLE
        }
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
        if(liveRealmData!=null) {
            liveRealmData!!.removeObservers(viewLifecycleOwner)
        }
        response = null
        realm?.close()
        if (!disposable.isDisposed) {
            disposable.dispose()
        }
        super.onDestroyView()
    }

    interface OnListFragmentInteractionListener {
        fun onSelected(contact: CovidContact)
    }

    companion object {
        private val TAG = FragmentContacts::class.java.simpleName
        fun newInstance(): FragmentContacts {
            val fragment = FragmentContacts()
            val arguments = Bundle()
            fragment.arguments = arguments
            return fragment
        }
    }
}