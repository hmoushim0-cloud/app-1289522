package sa.factory.wathiqnajizlegalco.domain.usecase

import sa.factory.wathiqnajizlegalco.domain.repository.WathiqNajizRepository
import sa.factory.wathiqnajizlegalco.model.WathiqNajizItem
import sa.factory.wathiqnajizlegalco.model.WathiqNajizOrder
import sa.factory.wathiqnajizlegalco.model.WathiqNajizTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: WathiqNajizRepository) {
    operator fun invoke(): Flow<List<WathiqNajizItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: WathiqNajizRepository) {
    val cart = repository.activeCart
    fun add(item: WathiqNajizItem) = repository.addToCart(item)
    fun remove(item: WathiqNajizItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: WathiqNajizRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): WathiqNajizOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: WathiqNajizRepository) {
    operator fun invoke(): Flow<WathiqNajizTelemetry> = repository.telemetryFlow
}
