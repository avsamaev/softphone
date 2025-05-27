package org.sipout.phone.ui.binding

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("android:onClick")
fun setOnClick(view: View, clickListener: View.OnClickListener?) {
    view.setOnClickListener(clickListener)
}
