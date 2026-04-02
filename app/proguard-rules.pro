# ViewModels
-keep class com.btestrs.android.TestViewModel { *; }
-keep class com.btestrs.android.HistoryViewModel { *; }

# Data classes used in JSON serialization
-keep class com.btestrs.android.BtestConfig { *; }
-keep class com.btestrs.android.BtestConfig$* { *; }
-keep class com.btestrs.android.BtestResult { *; }
-keep class com.btestrs.android.BtestSummary { *; }
-keep class com.btestrs.android.SavedCredential { *; }
-keep class com.btestrs.android.RendererConfig { *; }
-keep class com.btestrs.android.UploadResult { *; }

# Room entities and DAO
-keep class com.btestrs.android.data.** { *; }
