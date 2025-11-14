package com.jacktorscript.batterynotifier.ui.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.jacktorscript.batterynotifier.MainActivity
import com.jacktorscript.batterynotifier.R
import com.jacktorscript.batterynotifier.core.BatteryInfo
import com.jacktorscript.batterynotifier.databinding.FragmentBatteryBinding
import com.jacktorscript.batterynotifier.widget.ArcProgress

class BatteryFragment : Fragment() {

    private var batteryMeter: ArcProgress? = null
    private var lastTime: TextView? = null
    private var batteryCurrent: TextView? = null
    private var batteryHealth: TextView? = null
    private var batteryStatus: TextView? = null
    private var batteryTechnology: TextView? = null
    private var batteryTemperature: TextView? = null
    private var batteryTemperatureF: TextView? = null
    private var batteryVoltage: TextView? = null
    private var batteryCapacity: TextView? = null
    private var pluggedType: TextView? = null

    private var pluggedTypeIcon: AppCompatImageView? = null
    private var stateIcon: AppCompatImageView? = null
    private var temperatureIcon: AppCompatImageView? = null
    private var wattageIcon: AppCompatImageView? = null

    private var _binding: FragmentBatteryBinding? = null
    private val binding get() = _binding!!

    private val uiHandler = Handler(Looper.getMainLooper())
    private var batteryReceiverRegistered = false
    private var isLastTimeUpdating = false

    private var lastBatteryLevel: Int? = null
    private var lastBatteryStrokeColor: Int? = null
    private var lastStateColor: Int? = null
    private var lastPluggedIconRes: Int? = null
    private var lastPluggedColor: Int? = null
    private var lastPluggedAlpha: Float? = null
    private var lastTemperatureColor: Int? = null
    private var lastWattageColor: Int? = null

    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_BATTERY_CHANGED == intent.action && isAdded) {
                updateBatteryStatus()
            }
        }
    }

    private val batteryLevelRunnable = object : Runnable {
        override fun run() {
            if (!isAdded) {
                return
            }
            updateBatteryLevel()
            uiHandler.postDelayed(this, 10000)
        }
    }

    private val batteryAmpRunnable = object : Runnable {
        override fun run() {
            if (!isAdded) {
                return
            }
            updateBatteryCurrent()
            uiHandler.postDelayed(this, 2000)
        }
    }

    private val lastTimeRunnable = object : Runnable {
        override fun run() {
            if (!isAdded) {
                isLastTimeUpdating = false
                return
            }
            val ctx = context ?: return
            updateTextView(lastTime, BatteryInfo.getLastTime(ctx))
            uiHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatteryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        batteryMeter = view.findViewById(R.id.battery_view)
        lastTime = view.findViewById(R.id.lastStateChangeTime)
        batteryCurrent = view.findViewById(R.id.txt_battery_current)
        batteryVoltage = view.findViewById(R.id.txt_battery_voltage)
        batteryTemperature = view.findViewById(R.id.txt_battery_temp)
        batteryTemperatureF = view.findViewById(R.id.txt_battery_temp_f)
        batteryTechnology = view.findViewById(R.id.txt_battery_tech)
        batteryStatus = view.findViewById(R.id.txt_battery_status)
        batteryCapacity = view.findViewById(R.id.txt_battery_capacity)
        batteryHealth = view.findViewById(R.id.txt_battery_health)
        pluggedType = view.findViewById(R.id.txt_plugged_type)
        pluggedTypeIcon = view.findViewById(R.id.ic_plugged_type)
        stateIcon = view.findViewById(R.id.ic_state)
        temperatureIcon = view.findViewById(R.id.ic_temperature)
        wattageIcon = view.findViewById(R.id.ic_wattage)

        registerBatteryReceiver()
        updateBatteryStatus()
        updateBatteryLevel()
        updateBatteryCurrent()
        startPeriodicUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (batteryReceiverRegistered) {
            activity?.unregisterReceiver(batteryReceiver)
            batteryReceiverRegistered = false
        }
        uiHandler.removeCallbacks(batteryLevelRunnable)
        uiHandler.removeCallbacks(batteryAmpRunnable)
        uiHandler.removeCallbacks(lastTimeRunnable)
        isLastTimeUpdating = false

        batteryMeter = null
        lastTime = null
        batteryCurrent = null
        batteryHealth = null
        batteryStatus = null
        batteryTechnology = null
        batteryTemperature = null
        batteryTemperatureF = null
        batteryVoltage = null
        batteryCapacity = null
        pluggedType = null
        pluggedTypeIcon = null
        stateIcon = null
        temperatureIcon = null
        wattageIcon = null

        lastBatteryLevel = null
        lastBatteryStrokeColor = null
        lastStateColor = null
        lastPluggedIconRes = null
        lastPluggedColor = null
        lastPluggedAlpha = null
        lastTemperatureColor = null
        lastWattageColor = null

        _binding = null
    }

    private fun registerBatteryReceiver() {
        if (!batteryReceiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            activity?.registerReceiver(batteryReceiver, filter)
            batteryReceiverRegistered = true
        }
    }

    private fun startPeriodicUpdates() {
        uiHandler.post(batteryLevelRunnable)
        uiHandler.post(batteryAmpRunnable)
        startLastTimeUpdates()
    }

    private fun startLastTimeUpdates() {
        if (!isLastTimeUpdating) {
            uiHandler.post(lastTimeRunnable)
            isLastTimeUpdating = true
        }
    }

    private fun updateBatteryLevel() {
        val ctx = context ?: return
        val level = BatteryInfo.getBatteryLevel(ctx)
        if (lastBatteryLevel != level) {
            val colorRes = if (level <= 20) R.color.red else R.color.white
            val color = ContextCompat.getColor(ctx, colorRes)
            if (lastBatteryStrokeColor != color) {
                batteryMeter?.setFinishedStrokeColor(color)
                lastBatteryStrokeColor = color
            }
            batteryMeter?.setProgress(level.toFloat())
            lastBatteryLevel = level
        }
    }

    private fun updateBatteryCurrent() {
        val ctx = context ?: return
        val currentText = BatteryInfo.getCurrentNow(ctx).toString() + " mA"
        updateTextView(batteryCurrent, currentText)
    }

    private fun updateBatteryStatus() {
        val ctx = context ?: return
        val mainActivity = activity as? MainActivity ?: return

        val health = BatteryInfo.getHealth(ctx)
        val status = BatteryInfo.getStatus(ctx)
        val temperature = BatteryInfo.getTemperature(ctx)
        val plugged = BatteryInfo.getPlugged(ctx)
        val voltage = BatteryInfo.getVoltage(ctx)

        updateTextView(lastTime, BatteryInfo.getLastTime(ctx))
        startLastTimeUpdates()

        updateTextView(batteryVoltage, BatteryInfo.getVoltageString(ctx))
        updateTextView(batteryTechnology, BatteryInfo.getTechnology(ctx))
        updateTextView(batteryHealth, BatteryInfo.getHealthString(ctx))
        updateTextView(batteryTemperature, BatteryInfo.getTemperatureString(ctx, false))
        updateTextView(batteryTemperatureF, BatteryInfo.getTemperatureString(ctx, true))
        updateTextView(batteryCapacity, BatteryInfo.getBatteryDesignCapacityString(ctx))
        updateTextView(batteryStatus, BatteryInfo.getStatusString(ctx))
        updateTextView(pluggedType, BatteryInfo.getPluggedString(ctx))

        val stateColorRes = if (health == 4 || health == 5 || health == 6) {
            R.color.battery_warning
        } else {
            when (status) {
                2 -> R.color.battery_charge
                3 -> R.color.battery_discharge
                else -> R.color.battery_unknown
            }
        }
        lastStateColor = updateIconColor(stateIcon, stateColorRes, lastStateColor)

        when (plugged) {
            0 -> {
                val lastType = mainActivity.prefsConfig?.getInt("last_plugged", 1) ?: 1
                val iconRes = when (lastType) {
                    2 -> R.drawable.ic_usb_unplugged_24
                    3 -> R.drawable.ic_ac_unplugged_24
                    else -> R.drawable.ic_ac_unplugged_24
                }
                updatePluggedIcon(iconRes, R.color.battery_off, 0.66f)
            }
            1 -> {
                updatePluggedIcon(R.drawable.ic_ac_plugged_24, R.color.battery_charge, 1.0f)
                mainActivity.prefsConfig?.setInt("last_plugged", 1)
            }
            2 -> {
                updatePluggedIcon(R.drawable.ic_usb_plugged_24, R.color.battery_charge, 1.0f)
                mainActivity.prefsConfig?.setInt("last_plugged", 2)
            }
            3 -> {
                updatePluggedIcon(R.drawable.ic_wireless_charging_24, R.color.battery_charge, 1.0f)
                mainActivity.prefsConfig?.setInt("last_plugged", 3)
            }
        }

        val temperatureColorRes = when {
            temperature >= 5000 -> R.color.battery_temp_very_hot
            temperature >= 4000 -> R.color.battery_temp_hot
            temperature >= 3000 -> R.color.battery_temp_warm
            temperature >= 2000 -> R.color.battery_temp_normal
            else -> R.color.battery_temp_cold
        }
        lastTemperatureColor = updateIconColor(temperatureIcon, temperatureColorRes, lastTemperatureColor)

        val wattColorRes = if (voltage.toInt() >= 7) {
            R.color.battery_alert
        } else {
            R.color.battery_power
        }
        lastWattageColor = updateIconColor(wattageIcon, wattColorRes, lastWattageColor)
    }

    private fun updatePluggedIcon(iconRes: Int, colorRes: Int, alpha: Float) {
        val ctx = context ?: return
        if (lastPluggedIconRes != iconRes) {
            pluggedTypeIcon?.setImageResource(iconRes)
            lastPluggedIconRes = iconRes
        }
        if (lastPluggedAlpha != alpha) {
            pluggedTypeIcon?.alpha = alpha
            lastPluggedAlpha = alpha
        }
        val color = ContextCompat.getColor(ctx, colorRes)
        if (lastPluggedColor != color) {
            pluggedTypeIcon?.setColorFilter(color)
            lastPluggedColor = color
        }
    }

    private fun updateIconColor(
        icon: AppCompatImageView?,
        colorRes: Int,
        previousColor: Int?
    ): Int? {
        val ctx = context ?: return previousColor
        val color = ContextCompat.getColor(ctx, colorRes)
        if (previousColor != color) {
            icon?.setColorFilter(color)
            return color
        }
        return previousColor
    }

    private fun updateTextView(textView: TextView?, newText: String) {
        if (textView != null && textView.text.toString() != newText) {
            textView.text = newText
        }
    }
}
