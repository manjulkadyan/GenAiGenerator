#!/bin/bash

# Quick script to get SHA-1 fingerprint for Firebase

echo "=========================================="
echo "Get SHA-1 Fingerprint for Firebase"
echo "=========================================="
echo ""

echo "Choose keystore type:"
echo "1. Debug keystore (for testing)"
echo "2. Release keystore (for production)"
read -p "Enter choice (1 or 2): " choice

if [ "$choice" == "1" ]; then
    echo ""
    echo "Getting SHA-1 from debug keystore..."
    echo ""
    keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
    
    echo ""
    echo "=========================================="
    echo "✅ SHA-1 Fingerprint (copy this):"
    echo "=========================================="
    keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1 | awk '{print $2}'
    
elif [ "$choice" == "2" ]; then
    read -p "Enter path to keystore file: " keystore_path
    read -p "Enter keystore alias: " alias
    read -sp "Enter keystore password: " password
    echo ""
    
    echo ""
    echo "Getting SHA-1 from release keystore..."
    echo ""
    keytool -list -v -keystore "$keystore_path" -alias "$alias" -storepass "$password" | grep SHA1
    
    echo ""
    echo "=========================================="
    echo "✅ SHA-1 Fingerprint (copy this):"
    echo "=========================================="
    keytool -list -v -keystore "$keystore_path" -alias "$alias" -storepass "$password" | grep SHA1 | awk '{print $2}'
else
    echo "Invalid choice"
    exit 1
fi

echo ""
echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Go to Firebase Console → Project Settings"
echo "2. Find your Android app"
echo "3. Click 'Add fingerprint'"
echo "4. Paste the SHA-1 above"
echo "5. Download new google-services.json"
echo "6. Replace app/google-services.json"
echo "7. Rebuild the app"
echo ""







