package com.example.sdk_test

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Pleos Connect 확인용 PoC.
 * WebView에 Kakao Maps JS API를 띄우고, AAOS 표준 LocationManager 좌표를 JS로 넘겨 마커를 갱신한다.
 */
class MapActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var locationManager: LocationManager
    private var pageLoaded = false

    private val locationListener = LocationListener { location -> pushLocation(location) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = getSystemService(LocationManager::class.java)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    pageLoaded = true
                    if (hasLocationPermission()) startLocationUpdates()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    Log.i(TAG, "console ${message.messageLevel()}: ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                    return true
                }
            }
        }
        setContentView(webView)

        Log.i(TAG, "WebView package=${WebView.getCurrentWebViewPackage()?.let { "${it.packageName} ${it.versionName}" }}")

        val html = assets.open("kakao_map.html").bufferedReader().use { it.readText() }
            .replace("__KAKAO_JS_KEY__", getString(R.string.kakao_js_key))
        // 카카오는 요청 출처(도메인)를 검사하므로, 콘솔 Web 플랫폼에 등록한 도메인을 baseUrl로 쓴다.
        webView.loadDataWithBaseURL(BASE_URL, html, "text/html", "utf-8", null)

        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION && hasLocationPermission() && pageLoaded) startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationManager.removeUpdates(locationListener)
        val providers = locationManager.getProviders(true)
        Log.i(TAG, "enabled providers=$providers")
        val provider = when {
            LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
            LocationManager.FUSED_PROVIDER in providers -> LocationManager.FUSED_PROVIDER
            else -> providers.firstOrNull()
        }
        if (provider == null) {
            webView.evaluateJavascript("setStatus('사용 가능한 위치 provider 없음')", null)
            return
        }
        locationManager.getLastKnownLocation(provider)?.let { pushLocation(it) }
        locationManager.requestLocationUpdates(provider, 1000L, 0f, locationListener)
    }

    private fun pushLocation(location: Location) {
        if (!pageLoaded) return
        webView.evaluateJavascript(
            "updateLocation(${location.latitude}, ${location.longitude}, ${location.accuracy}, '${location.provider}')",
            null
        )
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        locationManager.removeUpdates(locationListener)
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PleosMapPoC"
        private const val REQ_LOCATION = 1
        // https여야 SDK 내부의 프로토콜 상대 경로(//t1.kakaocdn.net/...)가 https로 풀린다(http면 cleartext 차단).
        private const val BASE_URL = "https://localhost"
    }
}
