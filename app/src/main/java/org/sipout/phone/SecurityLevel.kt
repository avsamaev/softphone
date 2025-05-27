package org.sipout.phone.core

enum class SecurityLevel {
    NONE,
    ENCRYPTED,
    EndToEndEncrypted,
    EndToEndEncryptedAndVerified,
    Unsafe
}
