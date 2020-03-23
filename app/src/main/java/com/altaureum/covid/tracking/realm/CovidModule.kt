package com.altaureum.covid.tracking.realm

import com.altaureum.covid.tracking.realm.data.CovidContact
import io.realm.annotations.RealmModule

@RealmModule(library = true, classes = [
    CovidContact::class
])
class CovidModule
