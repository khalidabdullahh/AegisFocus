#!/usr/bin/env bash
# ==============================================================================
# 🔐 Aegis Focus Production Keystore Generator
# Generates a standard 2048-bit RSA upload keystore for signing APKs and AABs.
# ==============================================================================

set -e

KEYSTORE_PATH="release-keystore.jks"
KEY_ALIAS="aegis_key"
VALIDITY_DAYS=10000

if [ -f "$KEYSTORE_PATH" ]; then
    echo "⚠️ Keystore already exists at: $KEYSTORE_PATH"
    exit 0
fi

echo "Generating release keystore at: $KEYSTORE_PATH..."
keytool -genkey -v \
    -keystore "$KEYSTORE_PATH" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity "$VALIDITY_DAYS" \
    -storepass "aegis_secure_pass" \
    -keypass "aegis_secure_pass" \
    -dname "CN=Aegis Focus, OU=Engineering, O=AegisFocus, L=Dhaka, ST=Dhaka, C=BD"

echo "✅ Successfully generated: $KEYSTORE_PATH (Alias: $KEY_ALIAS)"
echo "Keep this keystore safe! You will need it to sign all future updates."
