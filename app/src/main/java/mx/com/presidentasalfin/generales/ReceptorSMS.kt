package mx.com.presidentasalfin.generales

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class ReceptorSMS : BroadcastReceiver() {
    private var otpReceiver: OTPReceiveListener? = null

    fun initOTPListener(receiver: OTPReceiveListener) {
        this.otpReceiver = receiver
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            val extras = intent.extras
            val status = extras!!.get(SmsRetriever.EXTRA_STATUS) as Status

            when (status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    // Get SMS message contents
                    var otp: String = extras.get(SmsRetriever.EXTRA_SMS_MESSAGE) as String
                    Log.d("OTP_Message", otp)
                    // EXTRAE EL CÓDIGO DE UN SOLO USO DEL MENSAJE Y COMPLETA LA VERIFICACIÓN
                    if (otpReceiver != null) {
                        otp = otp.replace("<#> ", "").split("\n".toRegex())
                            .dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                        otpReceiver!!.onOTPReceived(otp)
                    }
                }

                CommonStatusCodes.TIMEOUT ->
                    // ESPERA EL 5 MINUTOS
                    // SI NO MUESTRA EL ERROR
                    if (otpReceiver != null)
                        otpReceiver!!.onOTPTimeOut()
            }
        }
    }

    interface OTPReceiveListener {
        fun onOTPReceived(otp: String)
        fun onOTPTimeOut()
    }
}