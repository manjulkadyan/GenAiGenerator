#!/bin/bash

# Quick deploy script for account deletion website
# This will deploy the website and function to Firebase

echo "=========================================="
echo "Deploying Account Deletion Website"
echo "=========================================="
echo ""

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI not found!"
    echo "Install it with: npm install -g firebase-tools"
    exit 1
fi

# Check if logged in
if ! firebase projects:list &> /dev/null; then
    echo "❌ Not logged into Firebase!"
    echo "Login with: firebase login"
    exit 1
fi

echo "✅ Firebase CLI found and logged in"
echo ""

# Set the project
echo "Setting Firebase project to: genaivideogenerator"
firebase use genaivideogenerator

if [ $? -ne 0 ]; then
    echo "⚠️  Warning: Could not set project. Make sure you have access to it."
    echo "You can set it manually with: firebase use genaivideogenerator"
fi

echo ""
echo "Deploying Firestore rules..."
firebase deploy --only firestore:rules

echo ""
echo "Deploying Functions..."
firebase deploy --only functions:requestAccountDeletion

echo ""
echo "Deploying Hosting..."
firebase deploy --only hosting

echo ""
echo "=========================================="
echo "✅ Deployment Complete!"
echo "=========================================="
echo ""
echo "Your website URLs:"
echo "  - https://genaivideogenerator.web.app"
echo "  - https://genaivideogenerator.web.app/delete-account.html"
echo "  - https://genaivideogenerator.firebaseapp.com/delete-account.html"
echo ""
echo "Add this URL to Google Play Console Data Safety section:"
echo "  https://genaivideogenerator.web.app/delete-account.html"
echo ""
echo "To view deletion requests:"
echo "  1. Go to Firebase Console → Firestore"
echo "  2. Look for 'deletion_requests' collection"
echo ""



