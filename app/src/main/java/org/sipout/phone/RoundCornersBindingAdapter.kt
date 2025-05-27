package org.sipout.phone.ui.binding

import android.view.View
import androidx.databinding.BindingAdapter
import org.sipout.phone.ui.call.view.RoundCornersTextureView

@BindingAdapter("roundCornersRadius")
fun setRoundCornersRadius(view: View, radius: Float) {
    if (view is RoundCornersTextureView) {
        view.setRadius(radius)
    }
}
