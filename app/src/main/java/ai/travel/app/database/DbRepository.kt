package ai.travel.app.database

import ai.travel.app.database.travel.TripsDao
import ai.travel.app.database.travel.TripsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DatabaseRepo(private val tripsDao: TripsDao) {
    fun getTrips(day: String, timeOfDay: String): Flow<List<TripsEntity?>> =
        tripsDao.getTrips(day, timeOfDay)

    val allTrips: Flow<List<TripsEntity?>> = tripsDao.getAllTrips()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun insertTrip(tourDetails: TripsEntity) {
        coroutineScope.launch {
            tripsDao.insertTrip(tourDetails)
        }
    }


}