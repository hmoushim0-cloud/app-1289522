package sa.factory.wathiqnajizlegalco.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WathiqNajizEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun legal_consultingDao(): WathiqNajizDao
}
