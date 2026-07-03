plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

// Nota: no se usa el plugin de Firebase/google-services. El Google Sign-In
// para Drive se configura directamente en Google Cloud Console creando un
// "OAuth Client ID" de tipo Android (applicationId + huella SHA-1 del
// certificado de firma), sin necesidad de google-services.json.
// Ver README-ANDROID.md, seccion "Google Drive".
