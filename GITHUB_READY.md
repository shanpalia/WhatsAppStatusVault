# WhatsApp Status Vault — GitHub Ready

App name: WhatsApp Status Vault
Developer: ShanPalia
Application ID: com.shanpalia.whatsappstatusvault
Android namespace: com.example

The namespace intentionally remains `com.example` so existing Kotlin package
declarations and relative Manifest component names continue to resolve.
The applicationId is the public app identity.

Google Services / Secrets Gradle plugins were removed because the app does not
need Firebase/Google Services for its current architecture.

Before Codemagic, verify that `app/`, `gradle/`, `settings.gradle.kts`,
`build.gradle.kts`, and the Gradle wrapper are present at repository root.
