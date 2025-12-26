#!/bin/bash

# Script to create a keystore for Android app signing
# This will guide you through creating a keystore and keystore.properties file

echo "=========================================="
echo "Android Keystore Creation Script"
echo "=========================================="
echo ""

# Default values
KEYSTORE_NAME="genai-video-keystore.jks"
KEY_ALIAS="genai-video-key"
KEYSTORE_PATH="../$KEYSTORE_NAME"

# Get keystore name
read -p "Enter keystore filename (default: $KEYSTORE_NAME): " input
KEYSTORE_NAME=${input:-$KEYSTORE_NAME}
KEYSTORE_PATH="../$KEYSTORE_NAME"

# Get key alias
read -p "Enter key alias (default: $KEY_ALIAS): " input
KEY_ALIAS=${input:-$KEY_ALIAS}

# Get keystore password
read -sp "Enter keystore password (will be hidden): " KEYSTORE_PASSWORD
echo ""
read -sp "Re-enter keystore password: " KEYSTORE_PASSWORD_CONFIRM
echo ""

if [ "$KEYSTORE_PASSWORD" != "$KEYSTORE_PASSWORD_CONFIRM" ]; then
    echo "❌ Passwords don't match!"
    exit 1
fi

# Get key password
read -sp "Enter key password (can be same as keystore password): " KEY_PASSWORD
echo ""
read -sp "Re-enter key password: " KEY_PASSWORD_CONFIRM
echo ""

if [ "$KEY_PASSWORD" != "$KEY_PASSWORD_CONFIRM" ]; then
    echo "❌ Passwords don't match!"
    exit 1
fi

# Get certificate info
echo ""
echo "Enter certificate information:"
read -p "Your name: " NAME
read -p "Organizational unit: " ORG_UNIT
read -p "Organization: " ORGANIZATION
read -p "City: " CITY
read -p "State/Province: " STATE
read -p "Country code (2 letters, e.g., US): " COUNTRY

# Create keystore
echo ""
echo "Creating keystore..."
keytool -genkey -v -keystore "$KEYSTORE_PATH" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -dname "CN=$NAME, OU=$ORG_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE, C=$COUNTRY"

if [ $? -eq 0 ]; then
    echo "✅ Keystore created successfully at: $KEYSTORE_PATH"
    
    # Create keystore.properties file
    echo ""
    echo "Creating keystore.properties file..."
    cat > keystore.properties << EOF
# Keystore Configuration
# Generated automatically - DO NOT commit to git!

storeFile=$KEYSTORE_PATH
storePassword=$KEYSTORE_PASSWORD
keyAlias=$KEY_ALIAS
keyPassword=$KEY_PASSWORD
EOF
    
    echo "✅ keystore.properties created successfully!"
    echo ""
    echo "=========================================="
    echo "✅ Setup Complete!"
    echo "=========================================="
    echo ""
    echo "Keystore location: $KEYSTORE_PATH"
    echo "Key alias: $KEY_ALIAS"
    echo ""
    echo "⚠️  IMPORTANT:"
    echo "   - Keep your keystore file and passwords safe!"
    echo "   - You'll need them to update your app on Google Play"
    echo "   - The keystore.properties file is already in .gitignore"
    echo ""
    echo "Next steps:"
    echo "   1. Build signed APK: ./gradlew assembleRelease"
    echo "   2. Build signed AAB: ./gradlew bundleRelease"
    echo "   3. Upload to Google Play Console"
    echo ""
else
    echo "❌ Failed to create keystore!"
    exit 1
fi













