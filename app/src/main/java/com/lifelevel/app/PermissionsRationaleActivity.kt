package com.lifelevel.app

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val message = TextView(this).apply {
            text = "LIFE LEVEL использует данные о количестве шагов только для отображения дневной активности и прогресса по цели. Данные читаются из Health Connect после вашего разрешения и не отправляются на сервер."
            textSize = 18f
            setPadding(48, 48, 48, 48)
        }
        setContentView(message)
    }
}
