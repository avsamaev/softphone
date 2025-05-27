package org.sipout.phone.ui.binding

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("android:visibility")
fun setVisibility(view: View, visibility: Int) {
    view.visibility = visibility
}
