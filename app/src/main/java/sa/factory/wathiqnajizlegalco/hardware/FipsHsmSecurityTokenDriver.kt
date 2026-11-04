package sa.factory.wathiqnajizlegalco.hardware

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

enum class HsmSecurityState {
    UNAUTHENTICATED,
    PIN_VERIFIED,
    LOCKED_OUT,
    ZEROIZED
}

data class HsmApduResponse(
    val data: ByteArray,
    val sw1: Byte,
    val sw2: Byte,
    val isSuccess: Boolean
)

data class HsmAttestationReport(
    val fipsLevel: String = "FIPS 140-2 Level 3",
    val tokenSerial: String,
    val activeState: HsmSecurityState,
    val remainingPinAttempts: Int,
    val supportedCurves: List<String> = listOf("NIST P-256 (secp256r1)", "Brainpool P256r1"),
    val hardwareTamperTripped: Boolean = false
)

class FipsHsmSecurityTokenDriver {

    companion object {
        const val CLA_HSM: Byte = 0x80.toByte()

        const val INS_VERIFY_PIN: Byte = 0x20
        const val INS_SIGN_HASH: Byte = 0x2A
        const val INS_GET_ATTESTATION: Byte = 0x08
        const val INS_ZEROIZE: Byte = 0xEE.toByte()

        const val SW1_SUCCESS: Byte = 0x90.toByte()
        const val SW2_SUCCESS: Byte = 0x00

        const val SW1_AUTH_FAIL: Byte = 0x63
        const val SW1_AUTH_BLOCKED: Byte = 0x69.toByte()
        const val SW2_BLOCKED: Byte = 0x83.toByte()

        const val SW1_PARAM_ERROR: Byte = 0x6A
        const val SW2_PARAM_ERROR: Byte = 0x86.toByte()

        const val MAX_PIN_ATTEMPTS: Int = 3
    }

    private var securityState: HsmSecurityState = HsmSecurityState.UNAUTHENTICATED
    private var remainingAttempts: Int = MAX_PIN_ATTEMPTS
    private val masterPin: String = "849201"
    private val tokenSerial: String = "HSM-SA-2026-9941"

    // Key storage simulation
    private val keyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    fun computeCrc16Ccitt(data: ByteArray, offset: Int, length: Int): Short {
        var crc = 0xFFFF
        for (i in offset until (offset + length)) {
            val b = data[i].toInt() and 0xFF
            crc = crc xor (b shl 8)
            for (bit in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }
        return (crc and 0xFFFF).toShort()
    }

    fun buildCommandApdu(cla: Byte, ins: Byte, p1: Byte, p2: Byte, data: ByteArray): ByteArray {
        val lc = data.size
        val buffer = ByteBuffer.allocate(5 + lc).order(ByteOrder.BIG_ENDIAN)
        buffer.put(cla)
        buffer.put(ins)
        buffer.put(p1)
        buffer.put(p2)
        buffer.put(lc.toByte())
        buffer.put(data)
        return buffer.array()
    }

    fun transmitApdu(command: ByteArray): HsmApduResponse {
        if (command.size < 4) {
            return HsmApduResponse(ByteArray(0), SW1_PARAM_ERROR, SW2_PARAM_ERROR, false)
        }

        val cla = command[0]
        val ins = command[1]
        val data = if (command.size > 5) command.copyOfRange(5, command.size) else ByteArray(0)

        if (cla != CLA_HSM) {
            return HsmApduResponse(ByteArray(0), SW1_PARAM_ERROR, SW2_PARAM_ERROR, false)
        }

        if (securityState == HsmSecurityState.ZEROIZED) {
            return HsmApduResponse(ByteArray(0), 0x69.toByte(), 0x99.toByte(), false)
        }

        if (securityState == HsmSecurityState.LOCKED_OUT && ins != INS_ZEROIZE) {
            return HsmApduResponse(ByteArray(0), SW1_AUTH_BLOCKED, SW2_BLOCKED, false)
        }

        return when (ins) {
            INS_VERIFY_PIN -> handleVerifyPin(data)
            INS_SIGN_HASH -> handleSignHash(data)
            INS_GET_ATTESTATION -> handleGetAttestation()
            INS_ZEROIZE -> handleZeroize()
            else -> HsmApduResponse(ByteArray(0), 0x6D, 0x00, false)
        }
    }

    private fun handleVerifyPin(pinBytes: ByteArray): HsmApduResponse {
        val pinStr = String(pinBytes, Charsets.UTF_8)
        return if (pinStr == masterPin) {
            remainingAttempts = MAX_PIN_ATTEMPTS
            securityState = HsmSecurityState.PIN_VERIFIED
            HsmApduResponse(ByteArray(0), SW1_SUCCESS, SW2_SUCCESS, true)
        } else {
            remainingAttempts--
            if (remainingAttempts <= 0) {
                securityState = HsmSecurityState.LOCKED_OUT
                HsmApduResponse(ByteArray(0), SW1_AUTH_BLOCKED, SW2_BLOCKED, false)
            } else {
                val sw2 = (0xC0 or remainingAttempts).toByte()
                HsmApduResponse(ByteArray(0), SW1_AUTH_FAIL, sw2, false)
            }
        }
    }

    private fun handleSignHash(hashBytes: ByteArray): HsmApduResponse {
        if (securityState != HsmSecurityState.PIN_VERIFIED) {
            return HsmApduResponse(ByteArray(0), 0x69.toByte(), 0x82.toByte(), false) // Security status not satisfied
        }

        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(keyPair.private)
        signer.update(hashBytes)
        val derSignature = signer.sign()

        return HsmApduResponse(derSignature, SW1_SUCCESS, SW2_SUCCESS, true)
    }

    private fun handleGetAttestation(): HsmApduResponse {
        val report = "$tokenSerial|FIPS-140-2-L3|${securityState.name}|ATTEMPTS=$remainingAttempts"
        return HsmApduResponse(report.toByteArray(Charsets.UTF_8), SW1_SUCCESS, SW2_SUCCESS, true)
    }

    private fun handleZeroize(): HsmApduResponse {
        securityState = HsmSecurityState.ZEROIZED
        remainingAttempts = 0
        return HsmApduResponse(ByteArray(0), SW1_SUCCESS, SW2_SUCCESS, true)
    }

    fun getAttestationReport(): HsmAttestationReport {
        return HsmAttestationReport(
            tokenSerial = tokenSerial,
            activeState = securityState,
            remainingPinAttempts = remainingAttempts,
            hardwareTamperTripped = securityState == HsmSecurityState.ZEROIZED
        )
    }

    fun verifySignature(hashBytes: ByteArray, signatureBytes: ByteArray): Boolean {
        return try {
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(keyPair.public)
            verifier.update(hashBytes)
            verifier.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }
}
