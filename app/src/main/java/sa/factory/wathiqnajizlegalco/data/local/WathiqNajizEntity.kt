package sa.factory.wathiqnajizlegalco.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.wathiqnajizlegalco.model.WathiqNajizItem

@Entity(tableName = "legal_consulting_items_cache")
data class WathiqNajizEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): WathiqNajizItem = WathiqNajizItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
