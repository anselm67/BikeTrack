package com.anselm.location

import java.text.SimpleDateFormat
import java.util.Locale

val Any.TAG: String
    get() {
        return if (!javaClass.isAnonymousClass) {
            val name = javaClass.simpleName
            if (name.length <= 23) name else name.substring(0, 23)// first 23 chars
        } else {
            val name = javaClass.name
            if (name.length <= 23) name else name.substring(name.length - 23, name.length)
        }
    }

fun Double.formatIf(ifFmt: String, elseFmt: String, tst: (Double) -> Boolean): String {
    return if ( tst(this) ) {
        ifFmt.format(this)
    } else {
        elseFmt.format(this)
    }
}

fun DoubleArray.shift(position: Int) {
    if ( position > 0 ) {
        // Shift right
        this.copyInto(this, 0, position, this.size - position)
    } else {
        // Shift left
        this.copyInto(this, -position, 0, this.size + position)
    }
}

val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' HH:mm", Locale.US)
