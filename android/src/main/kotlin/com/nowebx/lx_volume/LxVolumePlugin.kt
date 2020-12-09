package com.nowebx.lx_volume

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.view.KeyEvent
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min


interface VolumeKeyListener {
    fun onVolumeKeyDown(keyCode: Int, event: KeyEvent): Boolean
}

interface CanListenVolumeKey {
    fun setVolumeKeyListener(listener: VolumeKeyListener?)
}

/** LxVolumePlugin */
class LxVolumePlugin : FlutterPlugin, MethodCallHandler, ActivityAware, VolumeKeyListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var lxRegistrar: ActivityPluginBinding

    private lateinit var lxBind: FlutterPlugin.FlutterPluginBinding
    private var lxApplication: Application? = null
    private val lxEventSink = QueuingEventSink()
    public var type: Int = AudioManager.STREAM_MUSIC
    private var lxEnableUI: Boolean = true
    private var lxWatching: Boolean = false
    private var lxVolumeReceiver: VolumeReceiver? = null
    private var lxEventChannel: EventChannel? = null
    private val volumeChangedAction = "android.media.VOLUME_CHANGED_ACTION"

    private var volStep = 1.0f / 16.0f

    private var minStep: Float = 0.0f

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        lxBind = flutterPluginBinding
        lxApplication = flutterPluginBinding.getApplicationContext() as Application?
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.nowebx.lx_volume")
        channel?.setMethodCallHandler(this)
    }

//    init {
//        val max = audioManager().getStreamMaxVolume(type)
//        minStep = 1.0f / max.toFloat()
//        volStep = max(minStep, volStep)
//    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "init" -> {
                type = AudioManager.STREAM_MUSIC;
                if (call.hasArgument("type")) {
                    type = (call.argument<Int>("type"))!!.toInt()
                }
                result.success(true);
            }
            "get" -> {
                result.success(getVolume())
            }
            "set" -> {
                var vol = getVolume()
                if (call.hasArgument("vol")) {
                    val v = call.argument<Double>("vol")
                    vol = setVolume(v!!.toFloat())
                }
                result.success(vol)
            }
            "enable_watch" -> {
                enableWatch()
                result.success(null)
            }
            "disable_watch" -> {
                disableWatch()
                result.success(null)
            }
            "enable_ui" -> {
                lxEnableUI = true
                result.success(null)
            }
            "disable_ui" -> {
                lxEnableUI = false
                result.success(null)
            }
            "get_enable_ui" -> {
                result.success(lxEnableUI)
            }
            else -> result.notImplemented()
        }
    }

    private fun audioManager(): AudioManager {
        var activity = lxRegistrar.activity
        return activity?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }


    private val flag: Int
        get() {
            return if (lxEnableUI) AudioManager.FLAG_SHOW_UI else 0
        }

    fun getVolume(): Float {
        val audioManager = audioManager()
        val max = audioManager?.getStreamMaxVolume(type).toFloat()
        val vol = audioManager?.getStreamVolume(type).toFloat()
        return vol / max
    }

    private fun setVolume(vol: Float): Float {
        val audioManager = audioManager()
        val max = audioManager.getStreamMaxVolume(type)
        var volIndex = (vol * max).toInt()
        volIndex = min(volIndex, max)
        volIndex = max(volIndex, 0)
        audioManager.setStreamVolume(type, volIndex, flag)
        return volIndex.toFloat() / max.toFloat()
    }

    private fun volumeUp(step: Float): Float {
        var vol = getVolume() + step
        vol = setVolume(vol)
        return vol
    }

    private fun enableWatch() {
        if (!lxWatching) {
            lxWatching = true
            lxEventChannel = EventChannel(lxBind.binaryMessenger, "com.nowebx.lx_volume/event")
            lxEventChannel!!.setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(o: Any?, eventSink: EventChannel.EventSink) {
                    lxEventSink.setDelegate(eventSink)
                }
                override fun onCancel(o: Any?) {
                    lxEventSink.setDelegate(null)
                }
            })

            val activity = lxRegistrar.activity
            if (activity is CanListenVolumeKey) {
                activity.setVolumeKeyListener(this)
            }
            lxVolumeReceiver = VolumeReceiver(this)
            val filter = IntentFilter()
            filter.addAction(volumeChangedAction)
            lxApplication!!.baseContext.registerReceiver(lxVolumeReceiver, filter)
        }

    }

    private fun disableWatch() {
        if (lxWatching) {
            lxWatching = false
            lxEventChannel!!.setStreamHandler(null)
            lxEventChannel = null

            var activity = lxRegistrar.getActivity()
            if (activity is CanListenVolumeKey) {
                activity.setVolumeKeyListener(null)
            }

            lxApplication!!.baseContext.unregisterReceiver(lxVolumeReceiver)
            lxVolumeReceiver = null
        }
    }

    fun sink(event: Any) {
        lxEventSink.success(event)
    }

    override fun onVolumeKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_MUTE -> setVolume(0.0f)
            KeyEvent.KEYCODE_VOLUME_UP -> volumeUp(minStep)
            KeyEvent.KEYCODE_VOLUME_DOWN -> volumeUp(-minStep)
        }
        return true
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(@NonNull activityPluginBinding: ActivityPluginBinding) {
        lxRegistrar = activityPluginBinding
    }

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(@NonNull binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {}


}

private class VolumeReceiver(plugin: LxVolumePlugin) : BroadcastReceiver() {
    private var lxPlugin: WeakReference<LxVolumePlugin> = WeakReference(plugin)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
            val plugin = lxPlugin.get()
            if (plugin != null) {
                val volume = plugin.getVolume()
                val type = plugin.type
                val event: MutableMap<String, Any> = mutableMapOf()
                event["event"] = "vol"
                event["v"] = volume
                event["t"] = type
                plugin.sink(event)
            }
        }
    }
}