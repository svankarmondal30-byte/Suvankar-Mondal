package com.example.device

import android.app.ActivityManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import com.example.model.ActionResult
import com.example.model.DeviceDiagnostics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PhoneController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private var isTorchOn: Boolean = false
    private var torchCameraId: String? = null
    private var sosJob: Job? = null

    init {
        initTorch()
    }

    private fun initTorch() {
        try {
            cameraManager?.let { cm ->
                for (id in cm.cameraIdList) {
                    val characteristics = cm.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        torchCameraId = id
                        break
                    }
                }
            }
        } catch (_: Exception) {
            torchCameraId = null
        }
    }

    fun isFlashlightActive(): Boolean = isTorchOn
    fun isSosActive(): Boolean = sosJob?.isActive == true

    fun setFlashlight(enable: Boolean): ActionResult {
        stopSosStrobe()
        val hasFlash = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        if (!hasFlash) {
            return ActionResult.Failure("This device does not have a hardware flashlight.")
        }
        val camId = torchCameraId ?: "0"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager?.setTorchMode(camId, enable)
                isTorchOn = enable
                ActionResult.Success(if (enable) "Flashlight on." else "Flashlight off.")
            } else {
                ActionResult.Failure("Flashlight control requires Android 6.0 or higher.")
            }
        } catch (e: CameraAccessException) {
            ActionResult.Failure("Camera hardware unavailable: ${e.message}")
        } catch (e: Exception) {
            ActionResult.Failure("Unable to toggle flashlight: ${e.message}")
        }
    }

    fun startSosStrobe(scope: CoroutineScope): ActionResult {
        val hasFlash = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        if (!hasFlash) {
            return ActionResult.Failure("Device lacks flashlight hardware.")
        }
        val camId = torchCameraId ?: "0"

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return ActionResult.Failure("Strobe requires Android 6.0+")
        }

        stopSosStrobe()
        sosJob = scope.launch {
            try {
                // SOS pattern: 3 short, 3 long, 3 short
                val timings = listOf(
                    200L, 200L, 200L, 200L, 200L, 600L, // 3 short
                    600L, 300L, 600L, 300L, 600L, 600L, // 3 long
                    200L, 200L, 200L, 200L, 200L, 1200L // 3 short + pause
                )
                while (isActive) {
                    for (i in timings.indices step 2) {
                        if (!isActive) break
                        cameraManager?.setTorchMode(camId, true)
                        isTorchOn = true
                        delay(timings[i])
                        cameraManager?.setTorchMode(camId, false)
                        isTorchOn = false
                        delay(timings.getOrElse(i + 1) { 200L })
                    }
                }
            } catch (_: Exception) {
                setFlashlight(false)
            }
        }
        return ActionResult.Success("Emergency SOS Strobe Beacon activated.")
    }

    fun stopSosStrobe() {
        sosJob?.cancel()
        sosJob = null
        if (isTorchOn) {
            val camId = torchCameraId ?: "0"
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager?.setTorchMode(camId, false)
                    isTorchOn = false
                }
            } catch (_: Exception) {}
        }
    }

    fun setRingerMode(mode: String): ActionResult {
        audioManager?.let { am ->
            return try {
                when (mode.lowercase()) {
                    "silent", "mute", "सैलेंट", "সাইলেন্ট" -> {
                        am.ringerMode = AudioManager.RINGER_MODE_SILENT
                        ActionResult.Success("Ringer mode set to Silent.")
                    }
                    "vibrate", "vibration", "ভাইব্রেট", "वाइब्रेट" -> {
                        am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        ActionResult.Success("Ringer mode set to Vibrate.")
                    }
                    else -> {
                        am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        ActionResult.Success("Ringer mode set to Normal.")
                    }
                }
            } catch (e: Exception) {
                ActionResult.Failure("Unable to change ringer mode: ${e.message}")
            }
        }
        return ActionResult.Failure("Audio service not available.")
    }

    fun adjustVolume(direction: Int): ActionResult {
        audioManager?.let { am ->
            try {
                am.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    direction,
                    AudioManager.FLAG_SHOW_UI
                )
                val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                return ActionResult.Success("Volume adjusted ($current/$max).")
            } catch (e: Exception) {
                return ActionResult.Failure("Could not adjust volume: ${e.message}")
            }
        }
        return ActionResult.Failure("Audio service not available.")
    }

    fun muteVolume(): ActionResult {
        audioManager?.let { am ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                } else {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                }
                return ActionResult.Success("Media volume muted.")
            } catch (e: Exception) {
                return ActionResult.Failure("Unable to mute: ${e.message}")
            }
        }
        return ActionResult.Failure("Audio service not available.")
    }

    fun toggleMediaPlayback(): ActionResult {
        audioManager?.let { am ->
            try {
                val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                am.dispatchMediaKeyEvent(downEvent)
                am.dispatchMediaKeyEvent(upEvent)
                return ActionResult.Success("Media play/pause command sent.")
            } catch (e: Exception) {
                return ActionResult.Failure("Media control failed: ${e.message}")
            }
        }
        return ActionResult.Failure("Audio service not available.")
    }

    fun nextMediaTrack(): ActionResult {
        audioManager?.let { am ->
            try {
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                return ActionResult.Success("Skipping to next track.")
            } catch (e: Exception) {
                return ActionResult.Failure("Media control failed: ${e.message}")
            }
        }
        return ActionResult.Failure("Audio service not available.")
    }

    fun previousMediaTrack(): ActionResult {
        audioManager?.let { am ->
            try {
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                return ActionResult.Success("Skipping to previous track.")
            } catch (e: Exception) {
                return ActionResult.Failure("Media control failed: ${e.message}")
            }
        }
        return ActionResult.Failure("Audio service not available.")
    }

    fun openApp(appName: String): ActionResult {
        val target = appName.trim().lowercase()
        return when {
            target.contains("youtube") -> launchPackageOrWeb("com.google.android.youtube", "https://www.youtube.com", "YouTube")
            target.contains("camera") || target.contains("ক্যামেরা") || target.contains("कैमरा") -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                safelyStartActivity(intent, "Camera")
            }
            target.contains("map") || target.contains("maps") || target.contains("ম্যাপ") || target.contains("नक्शा") -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                safelyStartActivity(intent, "Google Maps")
            }
            target.contains("chrome") || target.contains("browser") || target.contains("ব্রাউজার") -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                safelyStartActivity(intent, "Browser")
            }
            target.contains("whatsapp") || target.contains("হোয়াটসঅ্যাপ") -> {
                launchPackageOrWeb("com.whatsapp", "https://web.whatsapp.com", "WhatsApp")
            }
            target.contains("calculator") || target.contains("ক্যালকুলেটর") || target.contains("कैलकुलेटर") -> {
                val intent = Intent().apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_APP_CALCULATOR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                safelyStartActivity(intent, "Calculator")
            }
            target.contains("clock") || target.contains("alarm") || target.contains("ঘড়ি") || target.contains("घड़ी") -> {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                safelyStartActivity(intent, "Clock")
            }
            target.contains("setting") || target.contains("সেটিংস") || target.contains("सेटिंग") -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                safelyStartActivity(intent, "Settings")
            }
            target.contains("gmail") || target.contains("mail") || target.contains("ইমেইল") -> {
                launchPackageOrWeb("com.google.android.gm", "https://mail.google.com", "Gmail")
            }
            target.contains("spotify") || target.contains("স্পটিফাই") -> {
                launchPackageOrWeb("com.spotify.music", "https://open.spotify.com", "Spotify")
            }
            else -> {
                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage(target)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    safelyStartActivity(launchIntent, appName)
                } else {
                    ActionResult.Failure("Application '$appName' not found on device.")
                }
            }
        }
    }

    fun openSettingsPage(page: String): ActionResult {
        val intent = when (page.lowercase()) {
            "wifi", "wi-fi", "ওয়াইফাই", "वाईफाई" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth", "ব্লুটুথ", "ब्लूटूथ" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "display", "brightness", "ডিসপ্লে" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "sound", "audio", "সাউন্ড" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            "battery", "ব্যাটারি", "बैटरी" -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safelyStartActivity(intent, "Settings ($page)")
    }

    fun setAlarm(hour: Int, minute: Int, message: String?): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message ?: "MAX Assistant Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val displayTime = String.format("%02d:%02d", hour, minute)
        return safelyStartActivity(intent, "Alarm for $displayTime")
    }

    fun setTimer(seconds: Int, message: String?): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message ?: "MAX Assistant Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val mins = seconds / 60
        val secs = seconds % 60
        val timeLabel = if (mins > 0) "$mins min $secs sec" else "$secs sec"
        return safelyStartActivity(intent, "Timer for $timeLabel")
    }

    fun createReminder(title: String, beginTimeMs: Long): ActionResult {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTimeMs)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safelyStartActivity(intent, "Reminder '$title'")
    }

    fun executePhoneCall(recipient: String): ActionResult {
        val cleanNumber = recipient.filter { it.isDigit() || it == '+' }
        val uri = if (cleanNumber.isNotEmpty()) {
            Uri.parse("tel:$cleanNumber")
        } else {
            Uri.parse("tel:${Uri.encode(recipient)}")
        }
        val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safelyStartActivity(dialIntent, "Calling $recipient")
    }

    fun executeSendMessage(recipient: String, messageText: String): ActionResult {
        val cleanNumber = recipient.filter { it.isDigit() || it == '+' }
        val uri = if (cleanNumber.isNotEmpty()) {
            Uri.parse("smsto:$cleanNumber")
        } else {
            Uri.parse("smsto:${Uri.encode(recipient)}")
        }
        val sendIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", messageText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safelyStartActivity(sendIntent, "Message to $recipient")
    }

    fun startNavigation(destination: String): ActionResult {
        val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            return safelyStartActivity(mapIntent, "Navigation to $destination")
        }
        val webNav = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safelyStartActivity(webNav, "Navigation to $destination")
    }

    fun searchWeb(query: String): ActionResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            return safelyStartActivity(intent, "Web search for '$query'")
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safelyStartActivity(browserIntent, "Web search for '$query'")
    }

    fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    }

    fun getDeviceDiagnostics(): DeviceDiagnostics {
        // Battery
        val batteryPct = getBatteryLevel().coerceAtLeast(0)
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // RAM
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0)

        // Storage
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val bytesTotal = stat.blockCountLong * stat.blockSizeLong
        val freeStorageGb = Math.round((bytesAvailable.toDouble() / (1024 * 1024 * 1024)) * 10.0) / 10.0
        val totalStorageGb = Math.round((bytesTotal.toDouble() / (1024 * 1024 * 1024)) * 10.0) / 10.0

        // Network
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = connManager?.activeNetwork
        val caps = connManager?.getNetworkCapabilities(activeNet)
        val networkType = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Connected"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular 4G/5G"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }

        return DeviceDiagnostics(
            batteryPct = batteryPct,
            isCharging = isCharging,
            ramUsedMb = usedRamMb,
            ramTotalMb = totalRamMb,
            storageFreeGb = freeStorageGb,
            storageTotalGb = totalStorageGb,
            networkType = networkType,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        )
    }

    private fun launchPackageOrWeb(packageName: String, webFallback: String, name: String): ActionResult {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            safelyStartActivity(launchIntent, name)
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webFallback)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            safelyStartActivity(webIntent, name)
        }
    }

    private fun safelyStartActivity(intent: Intent, actionName: String): ActionResult {
        return try {
            context.startActivity(intent)
            ActionResult.Success("Opening $actionName.")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to launch $actionName: ${e.message}")
        }
    }
}
