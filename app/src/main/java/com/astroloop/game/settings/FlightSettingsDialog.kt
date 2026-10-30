package com.astroloop.game.settings

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.widget.*

/** Native, scrollable controls remain readable with Android font scaling and TalkBack. */
object FlightSettingsDialog {
    fun create(context: Context, inFlight: Boolean, onCheckUpdates: () -> Unit = {}, apply: (PilotOptions) -> Unit): AlertDialog {
        val themed=android.view.ContextThemeWrapper(context,android.R.style.Theme_Material_Dialog_Alert)
        val density=context.resources.displayMetrics.density
        fun dp(value:Int)=(value*density).toInt()
        val body=LinearLayout(themed).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(24),dp(8),dp(24),dp(12)) }
        fun label(text:String)=TextView(themed).apply { this.text=text;textSize=16f;setTextColor(Color.WHITE);setPadding(0,dp(12),0,dp(6));body.addView(this) }
        label("Use the fixed thumb pad to steer. Release it to brake. Weapons fire automatically. This menu pauses the action.")
        val handedness=Switch(themed).apply { text="Joystick on right";isChecked=PilotSettings.current.rightHandedJoystick;setPadding(0,dp(12),0,dp(12)) };body.addView(handedness)
        label("Frame rate")
        val modes=RadioGroup(themed).apply { orientation=RadioGroup.VERTICAL }
        val balanced=RadioButton(themed).apply { id=android.view.View.generateViewId();text="Balanced · up to 60 FPS" }
        val smooth=RadioButton(themed).apply { id=android.view.View.generateViewId();text="Smooth · up to 120 FPS" }
        modes.addView(balanced);modes.addView(smooth);body.addView(modes)
        modes.check(if(PilotSettings.current.fps==60) balanced.id else smooth.id)
        val sensitivityLabel=label("Steering response · %.1f×".format(PilotSettings.current.sensitivity))
        val steering=SeekBar(themed).apply { max=9;progress=((PilotSettings.current.sensitivity-.7f)*10 + .5f).toInt().coerceIn(0,9);contentDescription="Steering response" }
        steering.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar:SeekBar?,progress:Int,fromUser:Boolean) { sensitivityLabel.text="Steering response · %.1f×".format(.7f+progress/10f) }
            override fun onStartTrackingTouch(bar:SeekBar?) {}
            override fun onStopTrackingTouch(bar:SeekBar?) {}
        });body.addView(steering)
        val detail=Switch(themed).apply { text="Nebula backdrop";isChecked=PilotSettings.current.detailedBackground;setPadding(0,dp(12),0,dp(12)) };body.addView(detail)
        val motion=Switch(themed).apply { text="Moving starfield";isChecked=PilotSettings.current.backgroundMotion;setPadding(0,dp(12),0,dp(12)) };body.addView(motion)
        val rocks=Switch(themed).apply { text="3D tumbling asteroids";isChecked=PilotSettings.current.asteroid3d;setPadding(0,dp(12),0,dp(12)) };body.addView(rocks)
        label("Balanced mode and a simple backdrop reduce rendering work. Samsung battery saver may limit the display rate. Settings are saved on this phone.")
        val scroll=ScrollView(themed).apply { addView(body) }
        return AlertDialog.Builder(themed).setTitle(if(inFlight) "Flight paused" else "Pilot settings")
            .setView(scroll)
            .setPositiveButton(if(inFlight) "Apply & resume" else "Apply") { _,_->
                apply(PilotOptions(if(modes.checkedRadioButtonId==balanced.id)60 else 120,.7f+steering.progress/10f,detail.isChecked,motion.isChecked,handedness.isChecked,rocks.isChecked))
            }
            .setNegativeButton(if(inFlight) "Resume" else "Close",null)
            .setNeutralButton("Updates",null).create().also { dialog ->
                // Keep the underlying pause panel open during checks and installation.
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { onCheckUpdates() }
                }
            }
    }
}
