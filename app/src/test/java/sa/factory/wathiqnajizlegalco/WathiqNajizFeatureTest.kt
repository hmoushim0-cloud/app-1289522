package sa.factory.wathiqnajizlegalco

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import sa.factory.wathiqnajizlegalco.contract.*
import sa.factory.wathiqnajizlegalco.hardware.FipsHsmSecurityTokenDriver
import sa.factory.wathiqnajizlegalco.hardware.HsmSecurityState

class WathiqNajizFeatureTest {

    private lateinit var contractEngine: SaudiCommercialContractEngine
    private lateinit var hsmDriver: FipsHsmSecurityTokenDriver

    @Before
    fun setUp() {
        contractEngine = SaudiCommercialContractEngine()
        hsmDriver = FipsHsmSecurityTokenDriver()
    }

    @Test
    fun testCrAndNationalIdValidation() {
        assertTrue(contractEngine.validateCrOrNationalId("1010784920")) // Valid Riyadh CR
        assertTrue(contractEngine.validateCrOrNationalId("7001234567")) // Valid 700 unified number
        assertTrue(contractEngine.validateCrOrNationalId("1087654321")) // Valid Saudi National ID
        assertTrue(contractEngine.validateCrOrNationalId("2456789012")) // Valid Iqama

        assertFalse(contractEngine.validateCrOrNationalId("10107849")) // Too short
        assertFalse(contractEngine.validateCrOrNationalId("3010784920")) // Invalid start digit
        assertFalse(contractEngine.validateCrOrNationalId("1010ABC920")) // Non-digits
    }

    @Test
    fun testContractFinancialBreakdownAndSeverance() {
        // Commercial contract: 100,000 SAR + 15% VAT = 115,000 SAR
        val financial = contractEngine.calculateFinancialBreakdown(
            contractValueSar = 100000.0,
            durationMonths = 12,
            monthlyWageSar = 10000.0,
            completedServiceYears = 8.0,
            vatApplicable = true
        )

        assertEquals(100000.0, financial.baseAmountSar, 0.01)
        assertEquals(15000.0, financial.vatAmountSar, 0.01)
        assertEquals(115000.0, financial.totalAmountSar, 0.01)

        // Article 84 Severance: 5 years * (0.5 * 10k) = 25k + 3 years * (1.0 * 10k) = 30k -> 55,000 SAR
        assertEquals(55000.0, financial.statutorySeveranceSar, 0.01)

        // Under 5 years (3 years): 3 * (0.5 * 10k) = 15,000 SAR
        val shortService = contractEngine.calculateFinancialBreakdown(
            contractValueSar = 50000.0,
            durationMonths = 12,
            monthlyWageSar = 10000.0,
            completedServiceYears = 3.0
        )
        assertEquals(15000.0, shortService.statutorySeveranceSar, 0.01)
    }

    @Test
    fun testSaudiEmploymentContractLaborLawCompliance() {
        val compliantDraft = SaudiContractDraft(
            contractId = "CON-2026-001",
            type = ContractType.EMPLOYMENT_CONTRACT,
            firstPartyCrNumber = "1010123456",
            secondPartyIdOrCr = "1087654321",
            contractValueSar = 144000.0,
            contractDurationMonths = 12,
            probationPeriodDays = 90,
            nonCompeteMonths = 12,
            specifiedCity = "الرياض",
            jurisdiction = JurisdictionForum.LABOR_DISPUTE_COMMITTEE
        )

        val passAudit = contractEngine.auditContractCompliance(compliantDraft)
        assertTrue(passAudit.isCompliantWithSaudiLaw)
        assertEquals(0, passAudit.laborLawViolations.size)
        assertTrue(passAudit.nafithAttestationReady)
        assertTrue(passAudit.statusArabic.contains("مطابق للأنظمة"))

        // Non-compliant Draft: Article 53 (probation 200 days > 180) and Article 83 (non-compete 30 months > 24)
        val nonCompliantDraft = compliantDraft.copy(
            probationPeriodDays = 200,
            nonCompeteMonths = 30
        )

        val failAudit = contractEngine.auditContractCompliance(nonCompliantDraft)
        assertFalse(failAudit.isCompliantWithSaudiLaw)
        assertEquals(2, failAudit.laborLawViolations.size)
        assertTrue(failAudit.laborLawViolations.any { it.contains("المادة 53") })
        assertTrue(failAudit.laborLawViolations.any { it.contains("المادة 83") })
        assertFalse(failAudit.nafithAttestationReady)
    }

    @Test
    fun testHsmPinAuthenticationAndState() {
        // Valid PIN verification
        val validPinCmd = hsmDriver.buildCommandApdu(
            FipsHsmSecurityTokenDriver.CLA_HSM,
            FipsHsmSecurityTokenDriver.INS_VERIFY_PIN,
            0x00, 0x00,
            "849201".toByteArray(Charsets.UTF_8)
        )
        val validResp = hsmDriver.transmitApdu(validPinCmd)
        assertTrue(validResp.isSuccess)
        assertEquals(FipsHsmSecurityTokenDriver.SW1_SUCCESS, validResp.sw1)
        assertEquals(HsmSecurityState.PIN_VERIFIED, hsmDriver.getAttestationReport().activeState)

        // Reset with a new driver to test lockouts
        val freshDriver = FipsHsmSecurityTokenDriver()
        val wrongPinCmd = freshDriver.buildCommandApdu(
            FipsHsmSecurityTokenDriver.CLA_HSM,
            FipsHsmSecurityTokenDriver.INS_VERIFY_PIN,
            0x00, 0x00,
            "000000".toByteArray(Charsets.UTF_8)
        )

        // Attempt 1
        val r1 = freshDriver.transmitApdu(wrongPinCmd)
        assertFalse(r1.isSuccess)
        assertEquals(2, freshDriver.getAttestationReport().remainingPinAttempts)

        // Attempt 2
        freshDriver.transmitApdu(wrongPinCmd)
        assertEquals(1, freshDriver.getAttestationReport().remainingPinAttempts)

        // Attempt 3 -> Lockout
        val r3 = freshDriver.transmitApdu(wrongPinCmd)
        assertFalse(r3.isSuccess)
        assertEquals(FipsHsmSecurityTokenDriver.SW1_AUTH_BLOCKED, r3.sw1)
        assertEquals(HsmSecurityState.LOCKED_OUT, freshDriver.getAttestationReport().activeState)
    }

    @Test
    fun testHsmEcdsaP256HardwareSignature() {
        // Authenticate first
        val pinCmd = hsmDriver.buildCommandApdu(
            FipsHsmSecurityTokenDriver.CLA_HSM,
            FipsHsmSecurityTokenDriver.INS_VERIFY_PIN,
            0x00, 0x00,
            "849201".toByteArray(Charsets.UTF_8)
        )
        hsmDriver.transmitApdu(pinCmd)

        // Sign contract hash
        val testDigest = "a3f5c789e0123456789abcdef0123456789abcdef0123456789abcdef0123456".toByteArray(Charsets.UTF_8)
        val signCmd = hsmDriver.buildCommandApdu(
            FipsHsmSecurityTokenDriver.CLA_HSM,
            FipsHsmSecurityTokenDriver.INS_SIGN_HASH,
            0x00, 0x00,
            testDigest
        )
        val signResp = hsmDriver.transmitApdu(signCmd)
        assertTrue(signResp.isSuccess)
        assertTrue(signResp.data.isNotEmpty())

        // Verify ECDSA signature with public key
        val isValid = hsmDriver.verifySignature(testDigest, signResp.data)
        assertTrue("ECDSA P-256 signature should be cryptographically verified", isValid)

        // Corrupted hash verification should fail
        val corruptedDigest = "b3f5c789e0123456789abcdef0123456789abcdef0123456789abcdef0123456".toByteArray(Charsets.UTF_8)
        assertFalse(hsmDriver.verifySignature(corruptedDigest, signResp.data))
    }

    @Test
    fun testHsmEmergencyZeroization() {
        val zeroizeCmd = hsmDriver.buildCommandApdu(
            FipsHsmSecurityTokenDriver.CLA_HSM,
            FipsHsmSecurityTokenDriver.INS_ZEROIZE,
            0x00, 0x00,
            ByteArray(0)
        )
        val zeroizeResp = hsmDriver.transmitApdu(zeroizeCmd)
        assertTrue(zeroizeResp.isSuccess)

        val report = hsmDriver.getAttestationReport()
        assertEquals(HsmSecurityState.ZEROIZED, report.activeState)
        assertTrue(report.hardwareTamperTripped)

        // Further commands must be rejected
        val pinCmd = hsmDriver.buildCommandApdu(
            FipsHsmSecurityTokenDriver.CLA_HSM,
            FipsHsmSecurityTokenDriver.INS_VERIFY_PIN,
            0x00, 0x00,
            "849201".toByteArray(Charsets.UTF_8)
        )
        val postZeroizeResp = hsmDriver.transmitApdu(pinCmd)
        assertFalse(postZeroizeResp.isSuccess)
    }

    @Test
    fun testSha256DigestDeterminism() {
        val draft = SaudiContractDraft(
            contractId = "CON-2026-TEST",
            type = ContractType.COMMERCIAL_SERVICE_AGREEMENT,
            firstPartyCrNumber = "1010123456",
            secondPartyIdOrCr = "7009876543",
            contractValueSar = 50000.0,
            contractDurationMonths = 6,
            specifiedCity = "جدة"
        )

        val digest1 = contractEngine.computeDocumentDigest(draft)
        val digest2 = contractEngine.computeDocumentDigest(draft)

        assertEquals(digest1, digest2)
        assertEquals(64, digest1.length)
        assertTrue(digest1.all { it.isDigit() || it in 'a'..'f' })
    }
}
