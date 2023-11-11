package com.app.drawio

import android.app.Dialog
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.get
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.provider.MediaStore
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException


class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var selectBrush: ImageView? = null
    private var mIBcurrentPaint : ImageButton? = null
    private var ibUndo : ImageView?  = null
    private var customProgressDialog:Dialog? = null

    private val openGallery : ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data != null){
                val imgBackgroud : ImageView = findViewById(R.id.iv_background)
                imgBackgroud.setImageURI(result.data?.data)
            }
        }

    private val requestPermissions : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()){
                permission ->
                    permission.entries.forEach{
                        val permission_name = it.key
                        val isGranted = it.value
                        if(isGranted){
                            val snackBar=Snackbar.make(findViewById(android.R.id.content),"$permission_name permission granted",Toast.LENGTH_LONG)
                            snackBar.show()
                            val pickIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            openGallery.launch(pickIntent)
                        }
                        else{
                            if(permission_name == Manifest.permission.READ_EXTERNAL_STORAGE) {
                                val snackBar = Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "$permission_name permission denied",
                                    Toast.LENGTH_LONG
                                )
                                snackBar.show()
                            }
                        }
                    }

            }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val galleryBtn : ImageView = findViewById(R.id.ib_gallary)
        galleryBtn.setOnClickListener {
            reqStoragePermission()
        }


        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeforBrush(10.toFloat())

        val llPaintColors  = findViewById<LinearLayout>(R.id.ll_colors)
        mIBcurrentPaint = llPaintColors[1] as ImageButton
        mIBcurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.color_pallet_pressed)
        )
        ibUndo = findViewById(R.id.ib_undo)
        ibUndo?.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibSave : ImageView =findViewById(R.id.ib_save)
        ibSave.setOnClickListener {
            showProgressDialog()
            if(isReadStorageAllowed()){
                lifecycleScope.launch {
                    val flDrawingView : FrameLayout = findViewById(R.id.fl_drawingview_container)
                    saveBitmapFile(getBitmap(flDrawingView))
                }
            }
        }

        selectBrush= findViewById(R.id.ib_brush)
        selectBrush?.setOnClickListener {
            brushSizeDialog()
        }

    }

    private fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun reqStoragePermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationaleDialog("Application Requires Storage Access","Cannot open Gallary because Storage access is denied")
        } else{
            requestPermissions.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }
    private fun showRationaleDialog(title:String,message:String){
        val builder :AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog,_->
                dialog.dismiss()
            }
        builder.create().show()
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
    fun paintClicked(view:View){
        if(view !== mIBcurrentPaint){
            val imgBtn = view as ImageButton
            val colorTag = imgBtn.tag.toString()
            drawingView?.setColor(colorTag)
            imgBtn.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.color_pallet_pressed)
            )
            mIBcurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.color_pallet)
            )
            mIBcurrentPaint = view
        }
    }
    private fun getBitmap(view:View):Bitmap{
        val returnedBitmap =Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDraw = view.background
        if(bgDraw == null){
            canvas.drawColor(Color.WHITE)
        } else{
            bgDraw.draw(canvas)
        }
        view.draw(canvas)
        return returnedBitmap
    }
    private suspend fun saveBitmapFile(mBitmap : Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90 ,bytes)
                    val f = File(externalCacheDir?.absoluteFile.toString()+File.separator+"Draw.io"+System.currentTimeMillis()/1000+".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result=f.absolutePath

                    runOnUiThread{
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"File Saved Successfully :$result",Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        } else{
                            Toast.makeText(this@MainActivity,"Something Went Wrong while saving the file !! ",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e:Exception){
                    result=""
                    e.printStackTrace()
                }
            }
        }
        return result
    }
    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        customProgressDialog?.show()
    }
    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result:String){
        MediaScannerConnection.scanFile(this , arrayOf(result),null ){
            path , uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))
        }
    }
}