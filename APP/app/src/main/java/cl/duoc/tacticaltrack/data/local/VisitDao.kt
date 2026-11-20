package cl.duoc.tacticaltrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Query("SELECT * FROM visits ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<VisitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: VisitEntity): Long

    @Query("DELETE FROM visits")
    suspend fun clear()

    @Query("DELETE FROM visits WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(visits: List<VisitEntity>)

    @Transaction
    suspend fun replaceSnapshot(visits: List<VisitEntity>) {
        clear()
        insertAll(visits)
    }
}
