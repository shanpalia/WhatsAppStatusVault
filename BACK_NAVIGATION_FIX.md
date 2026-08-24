# Back Navigation Fix

System Android Back now behaves as follows:

- Status / Messages / Direct / Vault / Reports / Settings -> Home
- Home -> exits the app
- The in-app top-left Back button -> Home

The implementation uses AndroidX Compose `BackHandler` so system navigation
does not immediately exit from child screens.
