package sa.factory.wathiqnajizlegalco.data.repository

import sa.factory.wathiqnajizlegalco.domain.repository.WathiqNajizRepository
import sa.factory.wathiqnajizlegalco.hardware.HardwareSensorDriver
import sa.factory.wathiqnajizlegalco.model.WathiqNajizItem
import sa.factory.wathiqnajizlegalco.model.WathiqNajizOrder
import sa.factory.wathiqnajizlegalco.model.WathiqNajizTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WathiqNajizRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : WathiqNajizRepository {

    private val _catalog = MutableStateFlow<List<WathiqNajizItem>>(listOf(
        WathiqNajizItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع FIPS 140-2 Cryptographic HSM Security Token", 180.0, "معتمد", "الخدمات الرئيسية"),
        WathiqNajizItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        WathiqNajizItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<WathiqNajizItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<WathiqNajizItem>>(emptyList())
    override val activeCart: StateFlow<List<WathiqNajizItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<WathiqNajizTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            WathiqNajizTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: WathiqNajizItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: WathiqNajizItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): WathiqNajizOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = WathiqNajizOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
