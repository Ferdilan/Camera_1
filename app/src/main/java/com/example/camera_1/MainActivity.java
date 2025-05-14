package com.example.camera_1;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.os.Environment;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private Camera mCamera = null;
    private static Preview preview;
    private FrameLayout frameLayoutpreview;
    private Camera.PictureCallback mPicture;
    private ImageButton button_shutter;
    private static String previewState = "PREVIEW";

    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        frameLayoutpreview = findViewById(R.id.FrameLayout_Preview);
        button_shutter = findViewById(R.id.button_shuter);


        button_shutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (previewState){
                    case "BUSY":
                        //menampilkan layar preview
                        mCamera.startPreview();
                        previewState = "PREVIEW";
                        break;
                    case "PREVIEW":
                        //mengambil gambar
                        mCamera.takePicture(null,null,mPicture);
                        previewState = "BUSY";
                        break;
                }
            }
        });
    }


    private boolean cekIzinDitolak(){
        for (String permission : PERMISSIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cekIzinDitolak()){
            requestPermissions(PERMISSIONS, 1);
            return;
        }
        mCamera = getCamera();
        preview = new Preview(this,mCamera);
        frameLayoutpreview.addView(preview);

        mPicture = new Camera.PictureCallback(){
            @Override
            public void onPictureTaken(byte[] data, Camera camera){
                //mengambil file foto kosong yang telah dibuat menggunakan method getOutputMediaFile()
                File pictureFile = getOutputMediaFile();
                //mengecek File
                if (pictureFile == null){
                    Toast.makeText(MainActivity.this, "Error creating media file, check storage permissions", Toast.LENGTH_SHORT).show();
                    return;
                }
                //memasukkan foto yang telah diambil dan menyimpannya
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                }catch (FileNotFoundException e){
                    Toast.makeText(MainActivity.this,"File not found: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                }catch (IOException e){
                    Toast.makeText(MainActivity.this, "Error Accessing File: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

    }

    public android.hardware.Camera getCamera(){
        Camera c = null;
        try {
            c = android.hardware.Camera.open();
        }
        catch (Exception e){
            Toast.makeText(this,"Kamera Tidak Tersedia", Toast.LENGTH_SHORT).show();
        }
        return c;
    }

    private class Preview extends SurfaceView implements SurfaceHolder.Callback{

        SurfaceHolder mHolder;
        private Camera PrevCamera;
            public Preview(Context context, Camera camera) {
            super(context);
            PrevCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                mCamera.setDisplayOrientation(90);
            }catch (Exception e){
                Toast.makeText(MainActivity.this,"Failed to Open Camera "+ e, Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            //digunakan untuk mengatur tampilan kamera
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            if (mCamera != null){
                mCamera.stopPreview();;
                mCamera.release();
                mCamera = null;
            }
        }
    }

    private File getOutputMediaFile() {
        //menentukan penyimpanan foto pada folder pictures
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() + "/Aplikasi Kamera ";
        File mediaStorageDir = new File(path);
        //membuat folder baru jika belum ada
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Toast.makeText(this, "failed to create directory", Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        //scan file agar file yang dibuat dideteksi oleh aplikasi lain (ex: agar foto yg diambil terlihat di gallerry)
        sendBroadcast(new Intent((Intent.ACTION_MEDIA_SCANNER_SCAN_FILE), Uri.parse("file://"+path)));
        // menentukan nama file foto yang dibuat
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath()+File.separator+
                "IMG_"+timestamp+".jpg");
        return mediaFile;

    }


}