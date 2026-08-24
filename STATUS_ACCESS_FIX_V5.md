# Status Access V5 Fix

Fixed the Codemagic compile error:
`StatusSaverScreen.kt:278 Unresolved reference: folderUri`

The status access banner now uses the existing `storageGranted` state from
`Environment.isExternalStorageManager()` instead of the removed SAF `folderUri`.
