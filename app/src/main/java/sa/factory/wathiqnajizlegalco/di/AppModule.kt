package sa.factory.wathiqnajizlegalco.di

import sa.factory.wathiqnajizlegalco.data.repository.WathiqNajizRepositoryImpl
import sa.factory.wathiqnajizlegalco.domain.repository.WathiqNajizRepository
import sa.factory.wathiqnajizlegalco.domain.usecase.*
import sa.factory.wathiqnajizlegalco.hardware.HardwareSensorDriver
import sa.factory.wathiqnajizlegalco.ui.viewmodel.WathiqNajizViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: WathiqNajizRepository = WathiqNajizRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): WathiqNajizViewModel = WathiqNajizViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
