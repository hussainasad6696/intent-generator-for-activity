package com.example.intentgenerationsample

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.Keep
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty1

@Keep
interface IntentFactory {
    val activity: WeakReference<Activity>
    val hasResultCode: Boolean
    val resultCode: Int
    val animate: Boolean
    val finish: Boolean
    val clearTop: Boolean
    val newTask: Boolean
    val intent: Intent
}

@Keep
abstract class IntentHandler : IntentFactory {
    protected val TAG = this::class.simpleName

    companion object {
        const val HAS_RESULT_CODE = "hasResultCode"
    }

    interface IntentResultHandler {
        fun onSingleIntent()
        fun onMultiIntent()
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    }

    open fun getDataHandler(intent: Intent? = null): IntentHandler = this
    open fun setIntentResultListener(intentResultHandler: IntentResultHandler) {}

    fun safeIntent(): Intent? = runCatching {
        Log.i(TAG, "safeIntent: ${toString()}")
        intent
    }.getOrNull()

    fun returnWithResult(code: Int? = null) {
        val intentResultCode = code ?: this.resultCode

        Log.i(TAG, "code == $intentResultCode :: returnWithResult: ${toString()}")

        try {
            val activity = activity.get() ?: return
            activity.setResult(intentResultCode, intent)

            if (finish) activity.finish()
            if (animate) activity.animateActivity()
        } catch (e: ActivityReferenceEmptyException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startActivity() {
        Log.i(TAG, "startActivity: finish $finish == ${toString()}")

        try {
            val activity = activity.get() ?: return

            val modifiedIntent = intent

            if (clearTop)
                modifiedIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            if (newTask)
                modifiedIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            if (hasResultCode)
                activity.startActivityForResult(modifiedIntent, resultCode)
            else activity.startActivity(modifiedIntent.also { Log.i(TAG, "startActivity intent: ${it.extras}") })

            if (finish) activity.finish()

            if (animate)
                activity.animateActivity(false)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun toString(): String {
        if (BuildConfig.DEBUG.not()) return ""

        val sb = StringBuilder()

        val kClass = this::class
        val properties = kClass.members.filterIsInstance<kotlin.reflect.KProperty1<Any, *>>()

        sb.append("\n===================================================================")
        sb.append("\n===========================IntentHandler===========================")
        sb.append("\n===============================Start===============================")
        sb.append("\n\n${kClass.simpleName}:\n\n")
        for (prop in properties) {
            try {
                sb.append("${prop.name}: == ${prop.get(this)}\n")
            } catch (e: Exception) {
                sb.append("${prop.name}: <error getting value>\n")
            }
        }
        sb.append("\n==============================End=================================")
        sb.append("\n===========================IntentHandler===========================")
        sb.append("\n===================================================================")

        return sb.toString()
    }
}

class ActivityReferenceEmptyException : Exception("Activity reference is empty from weakReference")

fun Activity.animateActivity(
    isClosing: Boolean = true,
    startAnimation: Int = R.anim.slide_in_right,
    endAnimation: Int = R.anim.slide_out_left,
) {
    if (isVersionLessThanEqualTo(Build.VERSION_CODES.S_V2)) {
        if (isClosing.not()) {
            overridePendingTransition(startAnimation, endAnimation)
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}

@ChecksSdkIntAtLeast(parameter = 0)
fun isVersionLessThanEqualTo(version: Int): Boolean {
    return Build.VERSION.SDK_INT <= version
}