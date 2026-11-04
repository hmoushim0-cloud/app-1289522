# تطبيق وثيق القانونية — تصميم بوستات وإنتاج فيديوهات (`wathiq-najiz-legal-consulting-1289522`)
> منظومة legal_consulting المتكاملة مع ربط FIPS 140-2 Cryptographic HSM Security Token

- **Project ID**: `#1289522`
- **Commercial Archetype**: `legal_consulting`
- **Bound Hardware Peripheral**: `FIPS 140-2 Cryptographic HSM Security Token`
- **Package Name**: `sa.factory.wathiqnajizlegalco`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)


---

## [Sidekick Autonomous Update]

### 1. Overview
Sidekick autonomous companion enhancement (Iteration 23). Implemented Saudi commercial and labor contract compliance validation conforming to Ministry of Justice / Najiz / Nafith standards, alongside a FIPS 140-2 Level 3 Cryptographic Hardware Security Module (HSM) APDU driver with ECDSA NIST P-256 signing.

### 2. Features Implemented

#### Feature A: Saudi Commercial & Labor Contract Engine (`SaudiCommercialContractEngine.kt`)
- **Commercial & Labor Statutory Auditing**: Enforces compliance with Saudi Labor Law (نظام العمل السعودي) Article 53 (probation period $\le 180$ days), Article 83 (non-compete period $\le 24$ months with explicit geographical scope), and Commercial Courts Law (نظام المحاكم التجارية).
- **ZATCA Tax Invoice Calculation**: Standard 15% VAT calculation and commercial financial breakdown.
- **Article 84 End-of-Service Severance Gratuity**: Calculates statutory severance pay ($0.5 \times \text{monthlyWage} \times \text{years}$ for the first 5 years, plus $1.0 \times \text{monthlyWage} \times \text{excessYears}$ thereafter).
- **SHA-256 Cryptographic Document Digest**: Generates deterministic document fingerprints for Nafith and Najiz platform attestation.
- **Unified Identity Verification**: Validates 10-digit Saudi Commercial Register (CR) numbers, unified 700 series numbers, and National IDs/Iqamas.

#### Feature B: FIPS 140-2 Level 3 Cryptographic HSM Security Token Driver (`FipsHsmSecurityTokenDriver.kt`)
- **ISO 7816 / FIPS APDU Command & Response Framing**: APDU protocol handler supporting `VERIFY_PIN` (`0x20`), `SIGN_HASH` (`0x2A`), `GET_ATTESTATION` (`0x08`), and emergency `ZEROIZE` (`0xEE`).
- **NIST P-256 (secp256r1) ECDSA Signatures**: Cryptographic signing of contract digests with public-key verification.
- **Hardware Security State Machine**: Enforces PIN authentication, track remaining attempts (3 attempts before lockout), and zeroization on security violation.
- **Hardware Telemetry Binding**: Bound directly to `HardwareSensorDriver.kt` for live Android state flow telemetry.

### 3. Verification & Unit Testing
Comprehensive JUnit 4 test suite in `WathiqNajizFeatureTest.kt`:
- `testCrAndNationalIdValidation`: PASSED
- `testContractFinancialBreakdownAndSeverance`: PASSED
- `testSaudiEmploymentContractLaborLawCompliance`: PASSED
- `testHsmPinAuthenticationAndState`: PASSED
- `testHsmEcdsaP256HardwareSignature`: PASSED
- `testHsmEmergencyZeroization`: PASSED
- `testSha256DigestDeterminism`: PASSED

Total: 7/7 tests passing (100% success rate).

### 4. How to Run Tests
```bash
export JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-arm64
export PATH=$JAVA_HOME/bin:$PATH
CP="/usr/share/java/junit4-4.13.2.jar:/usr/share/java/hamcrest-2.2.jar:/usr/share/java/kotlin-stdlib.jar"

mkdir -p /tmp/iter23_bin
kotlinc -Xskip-metadata-version-check -cp "$CP" -d /tmp/iter23_bin \
  app/src/main/java/sa/factory/wathiqnajizlegalco/contract/SaudiCommercialContractEngine.kt \
  app/src/main/java/sa/factory/wathiqnajizlegalco/hardware/FipsHsmSecurityTokenDriver.kt \
  app/src/test/java/sa/factory/wathiqnajizlegalco/WathiqNajizFeatureTest.kt

java -cp "/tmp/iter23_bin:$CP" org.junit.runner.JUnitCore sa.factory.wathiqnajizlegalco.WathiqNajizFeatureTest
```
