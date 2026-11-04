package com.astroloop.game.owner

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.EditText
import android.widget.Toast

object OwnerMenu {
    private var menu: AlertDialog? = null
    fun dismiss() { menu?.dismiss(); menu = null }
    fun activation(activity: Activity) {
        val id = OwnerAccess.installationCode()
        if (id == null) {
            Toast.makeText(activity, "Secure installation key unavailable. Owner access stays locked.", Toast.LENGTH_LONG).show()
            return
        }
        val input = EditText(activity).apply {
            hint = "Paste your signed activation code"; maxLines = 4
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        val dialog = AlertDialog.Builder(activity).setTitle("Owner activation")
            .setMessage("Copy your installation code and send it in your original build chat. Your activation works only in this installation.\n\n$id")
            .setView(input).setNegativeButton("Close", null)
            .setNeutralButton("Copy installation code", null).setPositiveButton("Activate", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Astro Loop owner installation", id))
                Toast.makeText(activity, "Installation code copied", Toast.LENGTH_SHORT).show()
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (OwnerAccess.activate(activity, input.text.toString().trim())) {
                    Toast.makeText(activity, "Activated. Open Owner access again to unlock the menu.", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                } else input.error = "Invalid activation for this installation"
            }
        }
        dialog.show()
    }
    fun showAuthenticated(activity: Activity) {
        if (!OwnerAccess.authorized) return
        dismiss()
        val input = EditText(activity).apply {
            hint = "/money unlimited"; isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        menu = AlertDialog.Builder(activity).setTitle("Owner mod menu")
            .setMessage("Unlimited yen: ${if (OwnerAccess.moneyEnabled) "ON" else "OFF"}\n\nCommand: /money unlimited\nDisable: /money normal\n\nWhile enabled, purchases do not reduce your wallet. Your normal balance is preserved.")
            .setView(input).setNegativeButton("Close", null).setPositiveButton("Run command", null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val enabled = when (input.text.toString().trim()) {
                            "/money unlimited" -> true
                            "/money normal" -> false
                            else -> { input.error = "Use /money unlimited or /money normal"; return@setOnClickListener }
                        }
                        if (OwnerAccess.setMoney(activity, enabled)) {
                            Toast.makeText(activity, if (enabled) "Unlimited yen enabled" else "Normal balance restored", Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                        }
                    }
                }
                dialog.setOnDismissListener { if (menu === dialog) menu = null }
                dialog.show()
            }
    }
}
