package com.example.sdk_test

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 3분할 화면(차량 상태 | Pleos 내비 | 이 앱)의 오른쪽 칸 자리표시 화면.
 */
class PlaceholderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Placeholder"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setBackgroundColor(Color.parseColor("#F2F2F2"))
        })
    }
}
