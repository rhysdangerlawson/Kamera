package com.rhys.cameraapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

public class CameraActivity extends Activity implements Callback, View.OnClickListener {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private Button flipCamera;
    private Button flashCameraButton;
    private Button captureImage;
    private Button x;
    private int cameraId;
    private boolean flashmode = false;
    private int rotation;
    public ImageView imageView;
    public ImageView filterView;
    private SimpleGestureFilter detector;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        // camera surface view created
        imageView = (ImageView) findViewById(R.id.camera_image_view);
        cameraId = CameraInfo.CAMERA_FACING_BACK;
        flipCamera = (Button) findViewById(R.id.flipCamera);
        flashCameraButton = (Button) findViewById(R.id.flash);
        x = (Button) findViewById(R.id.x);
        x.setVisibility(View.GONE);
        captureImage = (Button) findViewById(R.id.captureImage);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        flipCamera.setOnClickListener(this);
        captureImage.setOnClickListener(this);
        flashCameraButton.setOnClickListener(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        if (Camera.getNumberOfCameras() > 1) {
            flipCamera.setVisibility(View.VISIBLE);
        }
        if (!getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            flashCameraButton.setVisibility(View.GONE);
        }

        // Detect touched area
        detector = new SimpleGestureFilter(this,this);
        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        ImageAdapter adapter = new ImageAdapter(context);
        viewPager.setAdapter(adapter);
    }

    /*******************************************************************************/
    /********************************* F I L T E R S *******************************/
    /*******************************************************************************/

    protected class ImagePagerAdapter extends PagerAdapter {

        public ViewGroup container;

        public int[] mImages = new int[] {
                R.drawable.flash,
                R.drawable.flash_icon,
                R.drawable.image,
                R.drawable.switch_camera
        };

        @Override
        public int getCount() {
            return mImages.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((ImageView) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Context context = CameraActivity.this;
            ImageView imageView = new ImageView(context);
            int padding = context.getResources().getDimensionPixelSize(R.dimen.padding_medium);
            imageView.setPadding(padding, padding, padding, padding);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setImageResource(mImages[position]);
            ((ViewPager) container).addView(imageView, 0);
            return imageView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager) container).removeView((ImageView) object);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent me){
        // Call onTouchEvent of SimpleGestureFilter class
        this.detector.onTouchEvent(me);
        return super.dispatchTouchEvent(me);
    }

    public void onSwipe(int direction) {
        String str = "";
        CameraActivity outer = new CameraActivity();
        CameraActivity.ImagePagerAdapter inner = outer.new ImagePagerAdapter();
        View filterView;
        filterView = findViewById(R.id.imageView);
        for(int position = 0;position<=4;position++) {
            switch (direction) {

                case SimpleGestureFilter.SWIPE_RIGHT:
                    filterView = findViewById(inner.mImages[position]);
                    setContentView(filterView);
                    str = "Swipe Right";
                    break;
                case SimpleGestureFilter.SWIPE_LEFT:
                    filterView = findViewById(inner.mImages[position]);
                    setContentView(filterView);
                    str = "Swipe Left";
                    break;
                case SimpleGestureFilter.SWIPE_DOWN:
                    filterView = findViewById(inner.mImages[position]);
                    setContentView(filterView);
                    str = "Swipe Down";
                    break;
                case SimpleGestureFilter.SWIPE_UP:
                    filterView = findViewById(inner.mImages[position]);
                    setContentView(filterView);
                    str = "Swipe Up";
                    break;

            }
            Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
        }
    }

    public void onDoubleTap() {
        Toast.makeText(this, "Double Tap", Toast.LENGTH_SHORT).show();
        int id = (cameraId == CameraInfo.CAMERA_FACING_BACK ? CameraInfo.CAMERA_FACING_FRONT : CameraInfo.CAMERA_FACING_BACK);
        if (!openCamera(id)) {
            alertCameraDialog();
        }
    }

    /*******************************************************************************/
    /**************************** E N D  F I L T E R S *****************************/
    /*******************************************************************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!openCamera(CameraInfo.CAMERA_FACING_BACK)) {
            alertCameraDialog();
        }

    }

    private boolean openCamera(int id) {
        boolean result = false;
        cameraId = id;
        releaseCamera();
        try {
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (camera != null) {
            try {
                setUpCamera(camera);
                camera.setErrorCallback(new ErrorCallback() {

                    @Override
                    public void onError(int error, Camera camera) {

                    }
                });
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                result = true;
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
                releaseCamera();
            }
        }
        return result;
    }

    private void setUpCamera(Camera c) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;

            default:
                break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            // frontFacing
            rotation = (info.orientation + degree) % 330;
            rotation = (360 - rotation) % 360;
        } else {
            // Back-facing
            rotation = (info.orientation - degree + 360) % 360;
        }
        c.setDisplayOrientation(rotation);
        Parameters params = c.getParameters();

        List<String> focusModes = params.getSupportedFlashModes();
        if (focusModes != null) {
            if (focusModes
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFlashMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
        }

        params.setRotation(rotation);
    }

    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.setErrorCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("error", e.toString());
            camera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.flash:
                x.setVisibility(View.GONE);
                flashOnButton();
                break;
            case R.id.flipCamera:
                x.setVisibility(View.GONE);
                flashCameraButton.setVisibility(View.GONE);
                flipCamera();
                break;
            case R.id.captureImage:
                x.setVisibility(View.VISIBLE);
                flashCameraButton.setVisibility(View.GONE);
                flipCamera.setVisibility(View.GONE);
                captureImage.setVisibility(View.GONE);
                takeImage();
                break;
            case R.id.x:
                x.setVisibility(View.GONE);
                flashCameraButton.setVisibility(View.VISIBLE);
                flipCamera.setVisibility(View.VISIBLE);
                captureImage.setVisibility(View.VISIBLE);
                flipCamera();
            default:
                break;
        }
    }



    private void takeImage() {

        camera.takePicture(null, null, new PictureCallback() {
            public void FadeIn(final ImageView v, int time) {

                final int begin_alpha = 0;
                final int end_alpha = 0;
                final boolean toggleVisibility = true;

                if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                    v.setImageAlpha(begin_alpha);
                else
                    v.setAlpha(begin_alpha);

                if (toggleVisibility) {
                    if (v.getVisibility() == View.GONE)
                        v.setVisibility(View.VISIBLE);
                    else
                        v.setVisibility(View.GONE);
                }

                Animation a = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        if (interpolatedTime == 1) {
                            if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                                v.setImageAlpha(end_alpha);
                            else
                                v.setAlpha(end_alpha);

                            if (toggleVisibility) {
                                if (v.getVisibility() == View.GONE)
                                    v.setVisibility(View.VISIBLE);
                                else
                                    v.setVisibility(View.GONE);
                            }
                        } else {
                            int new_alpha = (int) (begin_alpha + (interpolatedTime * (end_alpha - begin_alpha)));
                            if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                                v.setImageAlpha(new_alpha);
                            else
                                v.setAlpha(new_alpha);
                            v.requestLayout();
                        }
                    }

                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }
                };

                a.setDuration(time);
                v.startAnimation(a);
            }
            private File imageFile;

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    // convert byte array into bitmap
                    Bitmap loadedImage = null;
                    Bitmap rotatedBitmap = null;
                    loadedImage = BitmapFactory.decodeByteArray(data, 0, data.length);

                    // rotate Image
                    Matrix rotateMatrix = new Matrix();
                    rotateMatrix.postRotate(rotation);
                    rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0, loadedImage.getWidth(), loadedImage.getHeight(), rotateMatrix, false);

                    String state = Environment.getExternalStorageState();
                    File folder = null;

                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    imageView.setImageBitmap(bitmap);
                    camera.stopPreview();
                    surfaceView.setVisibility(View.INVISIBLE);
                    imageView.setVisibility(View.VISIBLE);

                    if (state.contains(Environment.MEDIA_MOUNTED)) {
                        folder = new File(Environment
                                .getExternalStorageDirectory() + "/Camera");
                    } else {
                        folder = new File(Environment
                                .getExternalStorageDirectory() + "/Camera");
                    }

                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }
                    if (success) {
                        java.util.Date date = new java.util.Date();
                        imageFile = new File(folder.getAbsolutePath()
                                + File.separator
                                + new Timestamp(date.getTime()).toString()
                                + "Image.jpg");

                        imageFile.createNewFile();
                    } else {
                        Toast.makeText(getBaseContext(), "Image Not saved",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

                    // save image into gallery
                    rotatedBitmap.compress(CompressFormat.JPEG, 100, ostream);

                    FileOutputStream fout = new FileOutputStream(imageFile);
                    fout.write(ostream.toByteArray());
                    fout.close();
                    ContentValues values = new ContentValues();

                    values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
                    values.put(Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.DATA, imageFile.getAbsolutePath());

                    CameraActivity.this.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                camera.stopPreview();
            }
        });
    }

    protected void flipCamera() {
        int id = (cameraId == CameraInfo.CAMERA_FACING_BACK ? CameraInfo.CAMERA_FACING_FRONT
                : CameraInfo.CAMERA_FACING_BACK);
        if (!openCamera(id)) {
            alertCameraDialog();
        }
    }

    private void alertCameraDialog() {
        AlertDialog.Builder dialog = createAlert(CameraActivity.this, "Camera info", "error to open camera");
        dialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });
        dialog.show();
    }

    private Builder createAlert(Context context, String title, String message) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog));
        dialog.setIcon(R.drawable.image);
        if (title != null)
            dialog.setTitle(title);
        else
            dialog.setTitle("Information");
        dialog.setMessage(message);
        dialog.setCancelable(false);
        return dialog;

    }

    private void flashOnButton() {
        if (camera != null) {
            try {
                Parameters param = camera.getParameters();
                param.setFlashMode(!flashmode ? Parameters.FLASH_MODE_TORCH
                        : Parameters.FLASH_MODE_OFF);
                camera.setParameters(param);
                flashmode = !flashmode;
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
    }
}