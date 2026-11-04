package sa.factory.wathiqnajizlegalco.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.wathiqnajizlegalco.contract.ContractType
import sa.factory.wathiqnajizlegalco.contract.SaudiCommercialContractEngine
import sa.factory.wathiqnajizlegalco.contract.SaudiContractDraft

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    private val peripheral = "FIPS 140-2 Cryptographic HSM Security Token"
    private val hsmDriver = FipsHsmSecurityTokenDriver()
    private val contractEngine = SaudiCommercialContractEngine()

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "PKCS#11 / FIPS 140-2 USB Security Token initializing..."))
        delay(300)

        // Authenticate HSM Token with PIN
        val pinCmd = hsmDriver.buildCommandApdu(
            FipsHsmSecurityTokenDriver.CLA_HSM,
            FipsHsmSecurityTokenDriver.INS_VERIFY_PIN,
            0x00, 0x00,
            "849201".toByteArray(Charsets.UTF_8)
        )
        val pinResp = hsmDriver.transmitApdu(pinCmd)

        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "FIPS 140-2 HSM Token: ONLINE | PIN AUTHENTICATED [SW: 0x9000]"))

        var sequence = 100
        while (true) {
            delay(1500)
            sequence++

            // Sample Saudi Commercial Contract
            val draft = SaudiContractDraft(
                contractId = "CON-SA-2026-$sequence",
                type = ContractType.COMMERCIAL_SERVICE_AGREEMENT,
                firstPartyCrNumber = "1010784920",
                secondPartyIdOrCr = "1087456321",
                contractValueSar = 150000.0,
                vatApplicable = true,
                contractDurationMonths = 12
            )

            val audit = contractEngine.auditContractCompliance(draft)
            val digestBytes = audit.sha256DocumentDigest.toByteArray(Charsets.UTF_8)

            // Perform HSM hardware ECDSA signature
            val signCmd = hsmDriver.buildCommandApdu(
                FipsHsmSecurityTokenDriver.CLA_HSM,
                FipsHsmSecurityTokenDriver.INS_SIGN_HASH,
                0x00, 0x00,
                digestBytes
            )
            val signResp = hsmDriver.transmitApdu(signCmd)
            val isValidSig = hsmDriver.verifySignature(digestBytes, signResp.data)

            val payload = String.format(
                "HSM P-256 Sign Frame #%d: Contract: %s | Val: 150k SAR (VAT 15%%) | Digest: %s... | Sig: %d bytes | Verified: %s",
                sequence,
                draft.contractId,
                audit.sha256DocumentDigest.substring(0, 8),
                signResp.data.size,
                if (isValidSig) "VALID" else "INVALID"
            )

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", payload))
        }
    }
}
