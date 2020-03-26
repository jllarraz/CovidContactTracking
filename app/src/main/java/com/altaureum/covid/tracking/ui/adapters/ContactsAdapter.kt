package com.altaureum.covid.tracking.ui.adapters

import androidx.appcompat.widget.AppCompatTextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import com.altaureum.covid.tracking.MyApplication.Companion.context
import com.altaureum.covid.tracking.realm.data.CovidContact
import com.altaureum.covid.tracking.realm.utils.RealmUtils
import com.altaureum.covid.tracking.realm.utils.covidContacts
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.realm.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class ContactsAdapter(internal var data: OrderedRealmCollection<CovidContact>?, autoUpdate: Boolean, internal var adapterCallback: AdapterCallback?) : RealmRecyclerViewAdapter<CovidContact, RecyclerView.ViewHolder>(data, autoUpdate, true), Filterable, LifecycleOwner {

    private val disposable = CompositeDisposable()
    private var realm: Realm?=null
    private val lifecycleRegistry = LifecycleRegistry(this)
    val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)

    init {
        realm = RealmUtils.getInstance(context!!)
        lifecycleRegistry.markState(Lifecycle.State.INITIALIZED)
    }


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        lifecycleRegistry.markState(Lifecycle.State.STARTED)
        if (i == ITEM) {
            val v = LayoutInflater.from(viewGroup.context).inflate(com.altaureum.covid.tracking.R.layout.row_covid_contact, viewGroup, false)
            return ViewHolder(v)
        } else
            throw RuntimeException("Could not inflate layout")
    }

    override fun getItemViewType(position: Int): Int {
        return ITEM
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        if (!disposable.isDisposed) {
            disposable.dispose()
        }
        realm?.close()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun getItemCount(): Int {
        return if (getData() != null) {
            getData()?.size!!
        } else 0
    }

    fun getPosition(gender: CovidContact): Int {
        return getData()?.indexOf(gender)!!
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        val contact = getItem(holder.adapterPosition)
        val mHolder = holder as ViewHolder
        mHolder.bind(contact)
    }

    fun filterResults(text: String?) {
        var search = text
        search = search?.toLowerCase()?.trim { it <= ' ' }
        if (search == null || search.isEmpty()) {
            adapterCallback?.onLoading(true)
            realm?.covidContacts()?.getAsyncAsLiveData(covidIds = null)?.observe(this,
                    androidx.lifecycle.Observer<RealmResults<CovidContact>> {
                        updateData(it)
                        adapterCallback?.onLoading(false)
                        if(data?.isEmpty()!!){
                            adapterCallback?.onEmptyList()
                        }
                    })

        } else {

            val subscribe = realm?.covidContacts()?.search(search)?.asChangesetObservable()
                    ?.doOnSubscribe({
                        adapterCallback?.onLoading(true)
                    })?.subscribe(Consumer {
                        updateData(it.collection)
                        adapterCallback?.onLoading(false)
                        if (data?.isEmpty()!!) {
                            adapterCallback?.onEmptyList()
                        }
                    })
            disposable.add(subscribe!!)
        }
    }

    override fun getFilter(): Filter {
        return CovidContactFilter(this)
    }

    override fun getItem(position: Int): CovidContact {
        return getData()?.get(position)!!
    }



    private inner class CovidContactFilter internal constructor(private val adapter: ContactsAdapter) : Filter() {

        override fun performFiltering(constraint: CharSequence): Filter.FilterResults {
            return Filter.FilterResults()
        }

        override fun publishResults(constraint: CharSequence, results: Filter.FilterResults) {
            adapter.filterResults(constraint.toString())
        }
    }

    private inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        lateinit var valueId: AppCompatTextView
        lateinit var valueTime: AppCompatTextView
        lateinit var contactTime: AppCompatTextView
        lateinit var contactDistance: AppCompatTextView
        lateinit var layout: ConstraintLayout

        init {
            valueId = itemView.findViewById(com.altaureum.covid.tracking.R.id.valueId)
            valueTime = itemView.findViewById(com.altaureum.covid.tracking.R.id.valueTime)
            contactTime = itemView.findViewById(com.altaureum.covid.tracking.R.id.contactTime)
            contactDistance = itemView.findViewById(com.altaureum.covid.tracking.R.id.contactDistance)
            layout = itemView.findViewById(com.altaureum.covid.tracking.R.id.layoutContainer)
            layout.setOnClickListener {
                val position = adapterPosition
                val item = getItem(position)
                if (adapterCallback != null && item!=null) {
                    adapterCallback!!.onSelected(item)
                }
            }

        }

        fun bind(covidContact: CovidContact) {
            valueId.text = covidContact.covidId
            if(covidContact.firstContactDate!=null){
                valueTime.text = simpleDateFormat.format(covidContact.firstContactDate!!)
            }
            var secondsInContact:Long=0
            if(covidContact.firstContactDate!=null && covidContact.lastContactDate!=null){
                secondsInContact =
                    TimeUnit.MILLISECONDS.toSeconds(covidContact.lastContactDate!!.time - covidContact.firstContactDate!!.time)

            }
            contactTime.text = context?.getString(com.altaureum.covid.tracking.R.string.contact_time, secondsInContact)

            val last = covidContact.locations?.last()!!
            contactDistance.text = context?.getString(com.altaureum.covid.tracking.R.string.last_distance, last.calculatedDistance)
        }

    }

    interface AdapterCallback {
        fun onSelected(contact: CovidContact)
        fun onLoading(isLoading:Boolean)
        fun onEmptyList()

    }

    companion object {
        private val ITEM = 1
    }
}

