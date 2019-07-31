package com.noober.amsdemo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.noober.amsdemo.test.Test1

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        Toast.makeText(this, "toast", Toast.LENGTH_LONG).show()
        val context = this
        Test1().onCreate2(this)
    }
}
