package com.app.drawio

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var selectBrush: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeforBrush(10.toFloat())

        selectBrush= findViewById(R.id.ib_brush)
        selectBrush?.setOnClickListener {
            brushSizeDialog()
        }

    }
    private fun brushSizeDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size :")
        val smallBtn : ImageButton = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
            drawingView?.setSizeforBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val medBtn : ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        medBtn.setOnClickListener{
            drawingView?.setSizeforBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn:ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener{
            drawingView?.setSizeforBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }
}