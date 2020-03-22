package com.altaureum.covid.tracking.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.ui.activities.client.ClientActivity
import com.altaureum.covid.tracking.ui.activities.server.ServerActivity

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        launch_server_button.setOnClickListener { v: View? ->
            startActivity(Intent(this@MainActivity,
                    ServerActivity::class.java))
        }
        launch_client_button.setOnClickListener { v: View? ->
            startActivity(Intent(this@MainActivity,
                    ClientActivity::class.java))
        }

        launch_server_background_button.setOnClickListener {

        }
    }
}