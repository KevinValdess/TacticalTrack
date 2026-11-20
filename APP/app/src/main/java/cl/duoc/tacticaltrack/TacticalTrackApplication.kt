package cl.duoc.tacticaltrack

import android.app.Application
import cl.duoc.tacticaltrack.data.local.AppDatabase
import cl.duoc.tacticaltrack.data.repository.UserPreferencesRepository
import cl.duoc.tacticaltrack.data.repository.VisitRepository

class TacticalTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        cl.duoc.tacticaltrack.push.PushRegistration.initialize(this)
    }
    val catalogRepository by lazy { cl.duoc.tacticaltrack.data.repository.CatalogRepository(cl.duoc.tacticaltrack.data.remote.ApiFactory.catalog) }
    val database by lazy { AppDatabase.getInstance(this) }
    val visitRepository by lazy { VisitRepository(database.visitDao(), cl.duoc.tacticaltrack.data.remote.ApiFactory.visits, cl.duoc.tacticaltrack.data.remote.AndroidEvidenceStore(this)) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
}

