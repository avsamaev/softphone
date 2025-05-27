package org.sipout.phone.ui.binding

import android.view.View
import androidx.databinding.BindingAdapter
import org.sipout.phone.ui.call.view.RoundCornersTextureView

@BindingAdapter("android:visibility")
fun setVisibility(view: RoundCornersTextureView, visibility: Int) {
    view.visibility = visibility
}

@BindingAdapter("android:onClick")
fun setOnClick(view: RoundCornersTextureView, clickListener: View.OnClickListener?) {
    view.setOnClickListener(clickListener)
}
