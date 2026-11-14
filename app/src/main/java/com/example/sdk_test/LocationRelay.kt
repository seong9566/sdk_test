package com.example.sdk_test

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * 중계 서버와의 웹소켓 연결. 내 위치를 보내고, 다른 참가자의 위치·퇴장을 메인 스레드 콜백으로 넘긴다.
 * 메시지: {"type":"loc","id","lat","lng"} / {"type":"leave","id"}
 */
class LocationRelay(
    private val url: String,
    private val myId: String,
    private val onPeerLocation: (id: String, lat: Double, lng: Double) -> Unit,
    private val onPeerLeft: (id: String) -> Unit,
    private val onStatus: (String) -> Unit,
) {
    private val client = OkHttpClient()
    private val main = Handler(Looper.getMainLooper())
    private var socket: WebSocket? = null
    private var closed = false

    fun connect() {
        closed = false
        socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                main.post { onStatus("relay 연결됨") }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = JSONObject(text)
                val id = msg.getString("id")
                if (id == myId) return
                main.post {
                    when (msg.getString("type")) {
                        "loc" -> onPeerLocation(id, msg.getDouble("lat"), msg.getDouble("lng"))
                        "leave" -> onPeerLeft(id)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "relay failure: ${t.message}")
                main.post { onStatus("relay 끊김, 재연결 중…") }
                if (!closed) main.postDelayed({ if (!closed) connect() }, RECONNECT_DELAY_MS)
            }
        })
    }

    fun sendLocation(lat: Double, lng: Double) {
        socket?.send(JSONObject().put("type", "loc").put("id", myId).put("lat", lat).put("lng", lng).toString())
    }

    fun close() {
        closed = true
        main.removeCallbacksAndMessages(null)
        socket?.close(1000, null)
    }

    companion object {
        private const val TAG = "PleosMapPoC"
        private const val RECONNECT_DELAY_MS = 3000L
    }
}
