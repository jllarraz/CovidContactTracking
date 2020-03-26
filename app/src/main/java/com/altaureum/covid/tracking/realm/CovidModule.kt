package com.altaureum.covid.tracking.realm

import com.altaureum.covid.tracking.realm.data.CovidContact
import com.altaureum.covid.tracking.realm.data.LocationCovidContact
import io.realm.annotations.RealmModule

@RealmModule(library = true, classes = [
    CovidContact::class,
    LocationCovidContact::class
])
class CovidModule
