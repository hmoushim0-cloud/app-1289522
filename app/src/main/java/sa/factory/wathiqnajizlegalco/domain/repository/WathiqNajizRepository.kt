package sa.factory.wathiqnajizlegalco.domain.repository

import sa.factory.wathiqnajizlegalco.model.WathiqNajizItem
import sa.factory.wathiqnajizlegalco.model.WathiqNajizOrder
import sa.factory.wathiqnajizlegalco.model.WathiqNajizTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WathiqNajizRepository {
    val catalogFlow: Flow<List<WathiqNajizItem>>
    val activeCart: StateFlow<List<WathiqNajizItem>>
    val telemetryFlow: Flow<WathiqNajizTelemetry>
    
    fun addToCart(item: WathiqNajizItem)
    fun removeFromCart(item: WathiqNajizItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): WathiqNajizOrder
    suspend fun refreshCatalog()
}
