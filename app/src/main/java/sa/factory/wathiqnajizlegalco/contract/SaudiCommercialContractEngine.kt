package sa.factory.wathiqnajizlegalco.contract

import java.security.MessageDigest
import kotlin.math.roundToInt

enum class ContractType {
    COMMERCIAL_SERVICE_AGREEMENT,
    EMPLOYMENT_CONTRACT,
    COMMERCIAL_LEASE,
    SUPPLY_PROCUREMENT
}

enum class JurisdictionForum {
    SAUDI_COMMERCIAL_COURT,
    SCCA_ARBITRATION,
    LABOR_DISPUTE_COMMITTEE
}

data class SaudiContractDraft(
    val contractId: String,
    val type: ContractType,
    val firstPartyCrNumber: String,
    val secondPartyIdOrCr: String,
    val contractValueSar: Double,
    val vatApplicable: Boolean = true,
    val contractDurationMonths: Int,
    val probationPeriodDays: Int = 90,
    val nonCompeteMonths: Int = 0,
    val specifiedCity: String = "الرياض",
    val jurisdiction: JurisdictionForum = JurisdictionForum.SAUDI_COMMERCIAL_COURT,
    val clausesArabic: List<String> = emptyList()
)

data class ContractFinancialBreakdown(
    val baseAmountSar: Double,
    val vatAmountSar: Double,
    val totalAmountSar: Double,
    val monthlyWageSar: Double,
    val serviceYears: Double,
    val statutorySeveranceSar: Double
)

data class ContractAuditCertification(
    val contractId: String,
    val isCompliantWithSaudiLaw: Boolean,
    val laborLawViolations: List<String>,
    val commercialLawViolations: List<String>,
    val sha256DocumentDigest: String,
    val nafithAttestationReady: Boolean,
    val statusArabic: String
)

class SaudiCommercialContractEngine {

    companion object {
        const val VAT_RATE: Double = 0.15
        const val MAX_STATUTORY_PROBATION_DAYS: Int = 180
        const val MAX_STATUTORY_NON_COMPETE_MONTHS: Int = 24
    }

    fun validateCrOrNationalId(id: String): Boolean {
        val cleaned = id.trim()
        if (cleaned.length != 10) return false
        if (!cleaned.all { it.isDigit() }) return false

        // National ID starts with 1, Iqama starts with 2, CR starts with 1010 or 700
        val firstChar = cleaned[0]
        return firstChar == '1' || firstChar == '2' || firstChar == '7'
    }

    fun calculateFinancialBreakdown(
        contractValueSar: Double,
        durationMonths: Int,
        monthlyWageSar: Double = 0.0,
        completedServiceYears: Double = 0.0,
        vatApplicable: Boolean = true
    ): ContractFinancialBreakdown {
        if (contractValueSar < 0.0 || durationMonths <= 0) {
            throw IllegalArgumentException("Contract value and duration must be positive")
        }

        val vat = if (vatApplicable) contractValueSar * VAT_RATE else 0.0
        val total = contractValueSar + vat

        // Article 84 Saudi Labor Law: End of Service Gratuity
        var severance = 0.0
        if (monthlyWageSar > 0.0 && completedServiceYears > 0.0) {
            if (completedServiceYears <= 5.0) {
                severance = 0.5 * monthlyWageSar * completedServiceYears
            } else {
                val firstFive = 0.5 * monthlyWageSar * 5.0
                val remaining = 1.0 * monthlyWageSar * (completedServiceYears - 5.0)
                severance = firstFive + remaining
            }
        }

        return ContractFinancialBreakdown(
            baseAmountSar = (contractValueSar * 100.0).roundToInt() / 100.0,
            vatAmountSar = (vat * 100.0).roundToInt() / 100.0,
            totalAmountSar = (total * 100.0).roundToInt() / 100.0,
            monthlyWageSar = (monthlyWageSar * 100.0).roundToInt() / 100.0,
            serviceYears = (completedServiceYears * 10.0).roundToInt() / 10.0,
            statutorySeveranceSar = (severance * 100.0).roundToInt() / 100.0
        )
    }

    fun computeDocumentDigest(draft: SaudiContractDraft): String {
        val md = MessageDigest.getInstance("SHA-256")
        val content = "${draft.contractId}|${draft.type.name}|${draft.firstPartyCrNumber}|${draft.secondPartyIdOrCr}|${draft.contractValueSar}|${draft.contractDurationMonths}|${draft.specifiedCity}"
        val digest = md.digest(content.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (b in digest) {
            sb.append(String.format("%02x", b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    fun auditContractCompliance(draft: SaudiContractDraft): ContractAuditCertification {
        val laborViolations = mutableListOf<String>()
        val commercialViolations = mutableListOf<String>()

        // 1. Identity Validation
        if (!validateCrOrNationalId(draft.firstPartyCrNumber)) {
            commercialViolations.add("رقم السجل التجاري للطرف الأول غير صالح وفق معايير وزارة التجارة (يجب أن يتكون من 10 أرقام)")
        }
        if (!validateCrOrNationalId(draft.secondPartyIdOrCr)) {
            commercialViolations.add("رقم هوية أو سجل الطرف الثاني غير صالح وفق الأنظمة السعودية")
        }

        // 2. Employment specific validations (Saudi Labor Law)
        if (draft.type == ContractType.EMPLOYMENT_CONTRACT) {
            // Article 53: Probation period
            if (draft.probationPeriodDays > MAX_STATUTORY_PROBATION_DAYS) {
                laborViolations.add("مخالفة المادة 53 من نظام العمل: تجاوزت فترة التجربة 180 يوما (الحالي: ${draft.probationPeriodDays} يوما)")
            }

            // Article 83: Non-compete clause
            if (draft.nonCompeteMonths > MAX_STATUTORY_NON_COMPETE_MONTHS) {
                laborViolations.add("مخالفة المادة 83 من نظام العمل: شرط عدم المنافسة يتجاوز الحد الأقصى النظامي البالغ سنتين (الحالي: ${draft.nonCompeteMonths} شهرا)")
            }
            if (draft.nonCompeteMonths > 0 && draft.specifiedCity.isBlank()) {
                laborViolations.add("مخالفة المادة 83 من نظام العمل: شرط عدم المنافسة يلزم تحديد النطاق الجغرافي والمدينة بوضوح")
            }
        }

        // 3. Contract value checks
        if (draft.contractValueSar <= 0.0) {
            commercialViolations.add("قيمة العقد المالية غير محددة أو غير صالحة نظاما")
        }

        val isCompliant = laborViolations.isEmpty() && commercialViolations.isEmpty()
        val digest = computeDocumentDigest(draft)
        val readyForNafith = isCompliant && draft.contractValueSar > 0.0

        val status = if (isCompliant) {
            "العقد معتمد ومطابق للأنظمة التجارية والعمالية السعودية وجاهز للتوثيق عبر منصة نافذ وناجز"
        } else {
            "العقد يتضمن بنودا تخالف الأنظمة السعودية ويلزم تعديل المواد المخالفة قبل التوثيق"
        }

        return ContractAuditCertification(
            contractId = draft.contractId,
            isCompliantWithSaudiLaw = isCompliant,
            laborLawViolations = laborViolations,
            commercialLawViolations = commercialViolations,
            sha256DocumentDigest = digest,
            nafithAttestationReady = readyForNafith,
            statusArabic = status
        )
    }
}
