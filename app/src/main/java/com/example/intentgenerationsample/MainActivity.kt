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

@Serializable
@Parcelize
data class Demi2(val p1: String, val p2: Int) : Parcelable
@Serializable
@Parcelize
data class Demi(val p1: String, val p2: Demi2): Parcelable
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
        Param("booleanValue", Boolean::class),
        Param("shortValue", Short::class),
        Param("byteValue", Byte::class),
        Param("charValue", Char::class)
    ]
)
class MainActivity : AppCompatActivity() {

    private val mIntentData: MainActivityIntent = MainActivityIntent.default(this)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)


    }

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

//        DemiActivityIntent(activity = WeakReference(this),
//            hasResultCode = false,
//            resultCode = 0,
//            animate = false,
//            finish = false,
//            clearTop = false,
//            uriList = arrayListOf<String>()).startActivity()
    }
}

//@GenerateIntent(
//    target = DemiActivity::class,
//    params = [
//        Param("uriList", ArrayList::class, isNullable = false),
//        Param("isFromPDFView", Boolean::class)
//    ]
//)
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

//        val data = DemiActivityIntent(activity = WeakReference(this),
//            hasResultCode = false,
//            resultCode = 0,
//            animate = false,
//            finish = false,
//            clearTop = false,
//            uriList = arrayListOf<String>()).getDataHandler()
//
//        data.isFromPDFView
    }
}