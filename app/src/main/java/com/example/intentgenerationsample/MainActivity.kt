package com.example.intentgenerationsample

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.intent_generator.annotations.GenerateIntent
import com.example.intent_generator.annotations.Param
import java.lang.ref.WeakReference

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

@GenerateIntent(
    target = TestActivity::class,
    params = [
        Param("uriList", ArrayList::class, isNullable = false),
        Param("toolType", String::class),
        Param("fromScreen", String::class),
        Param("recordedIndexList", ArrayList::class),
        Param("isFromPDFView", Boolean::class)
    ]
)
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

        DemiActivityIntent(activity = WeakReference(this),
            hasResultCode = false,
            resultCode = 0,
            animate = false,
            finish = false,
            clearTop = false,
            uriList = arrayListOf<String>()).startActivity()
    }
}

@GenerateIntent(
    target = DemiActivity::class,
    params = [
        Param("uriList", ArrayList::class, isNullable = false),
        Param("isFromPDFView", Boolean::class)
    ]
)
class DemiActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val data = DemiActivityIntent(activity = WeakReference(this),
            hasResultCode = false,
            resultCode = 0,
            animate = false,
            finish = false,
            clearTop = false,
            uriList = arrayListOf<String>()).getDataHandler()

        data.isFromPDFView
    }
}