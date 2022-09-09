package com.sally.pincode

import android.content.res.Resources

object Extensions {
    fun Float.dpToPx() = this * Resources.getSystem().displayMetrics.density
}