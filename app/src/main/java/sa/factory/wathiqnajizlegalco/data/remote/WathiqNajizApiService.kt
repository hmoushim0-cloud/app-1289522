package sa.factory.wathiqnajizlegalco.data.remote

import sa.factory.wathiqnajizlegalco.model.WathiqNajizItem
import sa.factory.wathiqnajizlegalco.model.WathiqNajizOrder

interface WathiqNajizApiService {
    suspend fun fetchCatalog(): List<WathiqNajizItem>
    suspend fun submitOrder(order: WathiqNajizOrder): Boolean
}
