package com.huynn109.otp_consent

import android.app.Activity
import android.content.*
import android.content.ContentValues.TAG
import android.content.Intent.*
import android.os.Build
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class SMSBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val SMS_CONSENT_REQUEST = 0x1009
    }

    private var listener: Listener? = null
    private var activity: Activity? = null

    fun injectListener(activity: Activity?, listener: Listener?) {
        this.listener = listener
        this.activity = activity
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            val extras = intent.extras
            val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as Status

            when (smsRetrieverStatus.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    // Get consent intent
                    val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                    try {
                        // Start activity to show consent dialog to user, activity must be started in
                        // 5 minutes, otherwise you'll receive another TIMEOUT intent
                        val intentName = consentIntent.resolveActivity(context.packageManager)

                        Log.e(TAG, "onReceive: " + intentName.packageName + " " + intentName.className)

                        if (intentName.packageName.equals("com.google.android.gms", ignoreCase = true) &&
                                intentName.className.equals("com.google.android.gms.auth.api.phone.ui.UserConsentPromptActivity", ignoreCase = true)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                consentIntent.removeFlags(FLAG_GRANT_READ_URI_PERMISSION)
                                consentIntent.removeFlags(FLAG_GRANT_WRITE_URI_PERMISSION)
                                consentIntent.removeFlags(FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                                consentIntent.removeFlags(FLAG_GRANT_PREFIX_URI_PERMISSION)
                            }
                            activity?.startActivityForResult(consentIntent, SMS_CONSENT_REQUEST)
                            listener?.onShowPermissionDialog()
                        }
                    } catch (e: ActivityNotFoundException) {
                        // Handle the exception ...
                    }
                }
                CommonStatusCodes.TIMEOUT -> listener?.onTimeout()
            }
        }
    }

    interface Listener {
        fun onShowPermissionDialog()
        fun onTimeout()
    }
}
