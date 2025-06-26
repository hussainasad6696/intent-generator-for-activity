package com.example.intentgenerationsample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.intent_generator.annotations.GenerateIntent
import com.example.intent_generator.annotations.Param
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.lang.ref.WeakReference

@Serializable
@Parcelize
data class Demi2(val p1: String, val p2: Int) : Parcelable

@Serializable
@Parcelize
data class Demi(val p1: String, val p2: Demi2) : Parcelable

@GenerateIntent(
    target = MainActivity::class,
    resultCode = 1002,
    params = [
        Param("intList", ArrayList::class, Int::class),
        Param("stringList", ArrayList::class, String::class),
        Param("charSeqList", ArrayList::class, CharSequence::class),
        Param("uriList", ArrayList::class, Uri::class, isNullable = false),
        Param("customParcelableList", ArrayList::class, Demi::class),
        Param("customParcelable", Demi::class),
        Param("intValue", Int::class),
        Param("longValue", Long::class),
        Param("floatValue", Float::class),
        Param("doubleValue", Double::class),
        Param("hussain", Boolean::class),
        Param("shortValue", Short::class),
        Param("byteValue", Byte::class),
        Param("charValue", Char::class)
    ]
)
class MainActivity : AppCompatActivity() {

    private var mIntentData: MainActivityIntent = MainActivityIntent.default(this)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        mIntentData = MainActivityIntent.default(this).getDataHandler()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        mIntentData = MainActivityIntent.default(this).getDataHandler()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        TestActivityIntent(
            activity = WeakReference(this),
            uriList = arrayListOf()
        ).startActivity()
    }
}

@GenerateIntent(
    target = TestActivity::class,
    params = [
        Param("uriList", ArrayList::class, isNullable = false, typeArg = Uri::class),
        Param("toolType", String::class),
        Param("fromScreen", String::class),
        Param("recordedIndexList", ArrayList::class, typeArg = Int::class),
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

    }
}

@GenerateIntent(
    target = DemiActivity::class,
    params = [
        Param("uriList", ArrayList::class, isNullable = false, typeArg = Uri::class),
        Param("isFromPDFView", Boolean::class)
    ]
)
class DemiActivity : AppCompatActivity() {
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