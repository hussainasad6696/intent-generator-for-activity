package com.example.intentgenerationsample

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty1

interface IntentFactory {
    val activity: WeakReference<Activity>
    val hasResultCode: Boolean
    val resultCode: Int
    val animate: Boolean
    val finish: Boolean
    val clearTop: Boolean?
    val intent: Intent
}

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

    fun returnWithResult(resultCode: Int) {
        Log.i(TAG, "safeIntent: ${toString()}")

        try {
            val activity = activity.get() ?: return
            activity.setResult(resultCode, intent)

            if (finish) activity.finish()
//            if (animate) activity.animateActivity()
        } catch (e: ActivityReferenceEmptyException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startActivity() {
        Log.i(TAG, "safeIntent: ${toString()}")

        try {
            val activity = activity.get() ?: return

            if (finish) activity.finish()

            val modifiedIntent = intent

            clearTop?.let {
                modifiedIntent.flags =
                    if (it) Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    else Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            if (hasResultCode)
                activity.startActivityForResult(modifiedIntent, resultCode)
            else activity.startActivity(modifiedIntent)

//            if (animate)
//                activity.animateActivity(false)
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
        val properties = kClass.members.filterIsInstance<KProperty1<Any, *>>()

        sb.append("\n===================================================================")
        sb.append("\n===========================IntentHandler===========================")
        sb.append("\n===============================Start===============================")
        sb.append("${kClass.simpleName}:\n")
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