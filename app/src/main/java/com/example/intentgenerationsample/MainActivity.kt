package com.example.intentgenerationsample

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.intent_generator.annotations.GenerateIntent
import com.example.intent_generator.annotations.Param

@GenerateIntent(
    target = MainActivity::class,
    params = [
        Param("uriList", ArrayList::class, isNullable = false),
        Param("toolType", String::class),
        Param("fromScreen", String::class),
        Param("recordedIndexList", ArrayList::class),
        Param("isFromPDFView", Boolean::class)
    ]
)
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }
}

//@GenerateIntent(
//    target = TestActivity::class,
//    params = [
//        Param("uriList", ArrayList::class, isNullable = false),
//        Param("toolType", String::class),
//        Param("hasResultCode", Boolean::class),
//        Param("fromScreen", String::class),
//        Param("recordedIndexList", ArrayList::class),
//        Param("isFromPDFView", Boolean::class)
//    ]
//)
class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }
}