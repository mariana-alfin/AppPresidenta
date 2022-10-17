package mx.com.presidentasalfin

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificacionesActivity : FirebaseMessagingService() {

    private val canalID = "canalID"
    private val notificacionId = 0

    /*Esta funcion es para poder manejar una notificacion de Firebase cuando la App esta en primer plano
    (por default la notificaciones de Firebase se muestran en automatico cuando la App no esta en primer plano)*/
    @SuppressLint("ResourceAsColor")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val notificacion = NotificationCompat.Builder(this, canalID).also {
            it.setContentTitle(remoteMessage.notification?.title)
            it.setContentText(remoteMessage.notification?.body)
            it.setSmallIcon(R.drawable.ic_notification)
            it.setColor(R.color.Azul3)
            remoteMessage.notification?.channelId?.let { canalID -> it.setChannelId(canalID) }
            it.setPriority(NotificationCompat.PRIORITY_HIGH)
            it.setFullScreenIntent(PendingIntent.getActivity(this, 0, Intent(), 0), true)
            //it.setContentIntent(pendingIntent) //Se usa si se quiere enviar a alguna actividad en especifico
            it.setContentIntent(PendingIntent.getActivity(this, 0, Intent(), 0))
            it.setAutoCancel(true)
        }.build()

        val notificationManager = NotificationManagerCompat.from(this)

        notificationManager.notify(notificacionId, notificacion)
    }
}
