package se.kth.dogtracker.utils

import android.content.Context
import android.os.Vibrator

class VibrationUtil {

    companion object{
        fun vibrate(context: Context){
            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(40L)
        }
    }


}