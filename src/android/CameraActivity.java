package org.apache.cordova.camera;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ryl.phoneloan.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraActivity extends Activity {

    public static final String PIC_PATH = "path";
    public static final float RATIO = 1.6f;

    private SurfaceView surfaceView;
    private Camera camera;
    private TextView mPreTv;
    private TextView mBlackCoverTv;
    private Button mComfermBtn;
    private Button mReturnToTakeBtn;
    private ImageButton mTakeBtn;
    private ImageButton mCancleBtn;
    private LinearLayout mComfirmLinear;
    private SurfaceHolder holder;

    private Bitmap tagBitmap;
    private MCameraLisener mCameraLisener;
    private ImageView imageView;


    private Camera.Size tagsize = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 没有标题 必须在设置布局之前找到调用
        setContentView(R.layout.camera_main);


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, // 设置全屏显示
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        mCameraLisener = new MCameraLisener();

        mTakeBtn = (ImageButton) findViewById(R.id.take);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        mPreTv = ((TextView) CameraActivity.this.findViewById(R.id.tv));
        mComfermBtn = ((Button) findViewById(R.id.comfirm));
        mReturnToTakeBtn = ((Button) findViewById(R.id.camera_returntotake));
        imageView = ((ImageView) CameraActivity.this.findViewById(R.id.image));
        mBlackCoverTv = ((TextView) findViewById(R.id.camera_black_cover));
        mComfirmLinear = ((LinearLayout) findViewById(R.id.camera_comfirm_linear));
        mCancleBtn = ((ImageButton) findViewById(R.id.camera_cancle));
// SurfaceView只有当activity显示到了前台，该控件才会被创建 因此需要监听surfaceview的创建
//拍照按钮
        mTakeBtn.setOnClickListener(mCameraLisener);
        mCancleBtn.setOnClickListener(mCameraLisener);
        mComfermBtn.setOnClickListener(mCameraLisener);
        mReturnToTakeBtn.setOnClickListener(mCameraLisener);

        holder = surfaceView.getHolder();
        holder.addCallback(new MySurfaceCallback());
//

//            Log.e("asd", "setpresize");
        if (tagsize != null) {

            holder.setFixedSize(tagsize.width, tagsize.height);// 设置分辨率
        }

        holder.setKeepScreenOn(true);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {
                        camera.cancelAutoFocus();
                    }
                });
                return false;
            }
        });

    }


    //获取预览及照片尺寸
    public void parameters(Camera camera) {

        List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();
        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        Camera.Size csize;
        Camera.Size psize;

        Camera.Size firstSize = null;
        Camera.Size tempSize = null;

        boolean getTagSize = false;

//if ()
        Log.e("asd", "previewSizes: " + previewSizes.size());
        for (int i = previewSizes.size() - 1; i >= 0; i--) {
            psize = previewSizes.get(i);
            Log.e("previewSize", psize.width + " x " + psize.height);
            for (int j = pictureSizes.size() - 1; j >= 0; j--) {
                csize = pictureSizes.get(j);

//                Log.e("pictureSize", csize.width + " x " + csize.height);
                if (psize.equals(csize)) {
                    Log.e("getMach", psize.width + " x " + psize.height);
                    if (firstSize == null) {
                        Log.e("getFirstMatch", psize.width + " x " + psize.height);
                        getTagSize = true;
                        firstSize = psize;
                        break;
                    } else {
                        Log.e("getAnotherMatch", psize.width + " x " + psize.height);
                        tempSize = psize;
                        break;
                    }


                }
            }


        }


        if (getTagSize) {
            if (tempSize != null) {
                tagsize = tempSize.width > firstSize.width ? tempSize : firstSize;
            }
//            break;
        }
        if (tagsize != null) {
            Log.e("tagSize", tagsize.width + " x " + tagsize.height);
        }

    }


//    //点击事件
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
////对焦
//        if (camera!=null&&surfaceView.isShown()){
//            camera.autoFocus(new Camera.AutoFocusCallback() {
//                @Override
//                public void onAutoFocus(boolean b, Camera camera) {
//                    camera.cancelAutoFocus();
//                }
//            });
//        }
//
//        return super.onTouchEvent(event);
//    }

    /**
     * 监听surfaceview的创建
     *
     * @author Administrator
     *         Surfaceview只有当activity显示到前台，该空间才会被创建
     */
    private final class MySurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
// TODO Auto-generated method stub
            ;

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
// TODO Auto-generated method stub

            initCamera();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
// TODO Auto-generated method stub

        }
    }


    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    private void initCamera() {
try{

//}catch (Exception e){
//
//}
        // 当surfaceview创建就去打开相机
        camera = Camera.open();
        parameters(camera);
        Camera.Parameters params = camera.getParameters();
// Log.i("i", params.flatten());
//                params.setJpegQuality(80); // 设置照片的质量
        params.setPictureSize(tagsize.width, tagsize.height);
        params.setPreviewFrameRate(5);
        // 预览帧率
        camera.setParameters(params); // 将参数设置给相机
//右旋90度，将预览调正
        camera.setDisplayOrientation(90);
// 设置预览显示
        camera.setPreviewDisplay(surfaceView.getHolder());
// 开启预览
        camera.startPreview();
        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean b, Camera camera) {
                camera.cancelAutoFocus();
            }
        });

        parameters(camera);


    } catch (IOException e) {
// TODO Auto-generated catch block
        e.printStackTrace();
    }

//        try {
//// 当surfaceview创建就去打开相机
//            camera = Camera.open();
//            parameters(camera);
//            Camera.Parameters params = camera.getParameters();
//// Log.i("i", params.flatten());
////                params.setJpegQuality(80); // 设置照片的质量
//            if (tagsize != null) {
//
//                params.setPictureSize(tagsize.width, tagsize.height);
//
//                surfaceView.getHolder().setFixedSize(tagsize.width, tagsize.height);// 设置分辨率
//
//                params.setPreviewSize(tagsize.width, tagsize.height);
//            }
////                params.setPreviewFrameRate(5);
//            // 预览帧率
//
////            Camera.Parameters params = camera.getParameters();//获取camera的parameter实例
////            List<Camera.Size> sizeList = params.getSupportedPreviewSizes();//获取所有支持的camera尺寸
////            Camera.Size optionSize = getOptimalPreviewSize(sizeList, surfaceView.getWidth(), surfaceView.getHeight());//获取一个最为适配的camera.size
////            params.setPreviewSize(optionSize.width,optionSize.height);//把camera.size赋值到parameters
////            camera.setParameters(params);//把parameters设置给camera
////
//            camera.setParameters(params); // 将参数设置给相机
////右旋90度，将预览调正
//            camera.setDisplayOrientation(90);
//// 设置预览显示
//            camera.setPreviewDisplay(surfaceView.getHolder());
//// 开启预览
//            camera.startPreview();
//            camera.autoFocus(new Camera.AutoFocusCallback() {
//                @Override
//                public void onAutoFocus(boolean b, Camera camera) {
//                    camera.cancelAutoFocus();
//                }
//            });
//
//
//        } catch (IOException e) {
//// TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    //拍照的函数
    public void takepicture() {
/*
* shutter:快门被按下
* raw:相机所捕获的原始数据
* jpeg:相机处理的数据
*/
        camera.takePicture(null, null, new MyPictureCallback());
    }

    //byte转Bitmap
    public Bitmap Bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    //bitmap转byte
    public byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    //照片回调函数，其实是处理照片的
    private final class MyPictureCallback implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
// TODO Auto-generated method stub
            try {
                Bitmap bitmap = Bytes2Bimap(data);
                Matrix m = new Matrix();
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                m.setRotate(90);
//将照片右旋90度

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, m,
                        true);
                Log.d("TAG", "width " + width);
                Log.d("TAG", "height " + height);
                width = bitmap.getWidth();
                height = bitmap.getHeight();
//截取透明框内照片

//取景框在父控件中的位置
                int left = mPreTv.getLeft();
                int top = mPreTv.getTop();
                int right = mPreTv.getRight();
                int bottom = mPreTv.getBottom();

                //父控件大小
                RelativeLayout relativeLayout = (RelativeLayout) CameraActivity.this.findViewById((R.id.activity_main));
                int pWith = relativeLayout.getWidth();
                int phight = relativeLayout.getHeight();

                //在父控件中的位置（比例）
                float leftPosition = (float) left / pWith;
                float rightPosition = (float) right / pWith;
                float topPosition = (float) top / phight;
                float bottomPosition = (float) bottom / phight;

                //取景框取到图片中的多少（比例）
                float preWith = rightPosition - leftPosition;
                float preHeight = bottomPosition - topPosition;

//目标图片的起始点
                float leftLocation = leftPosition * width;
                float topLocation = topPosition * height;

                float tagWith = (preWith * width);
                float tagHight = (preHeight * height);

                tagBitmap = Bitmap.createBitmap(bitmap, (int) leftLocation, (int) topLocation,
                        (int) (preWith * width), (int) (preHeight * height));
//                tagBitmap = Bitmap.createBitmap(bitmap, (int) leftLocation, (int) topLocation,
//                        (int) (preWith * width), (int) (tagWith / RATIO));
//                tagBitmap = Bitmap.createBitmap(bitmap, (int) leftLocation, (int) topLocation,
//                        (int) (tagHight * RATIO), (int) tagHight);

                bitmap.recycle();


// 在拍照的时候相机是被占用的,拍照之后需要重新预览
//                camera.startPreview();


                toComfirm();

            } catch (Exception e) {


// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    private class MCameraLisener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.take:
                    takepicture();
                    break;


                case R.id.camera_cancle:
//                    if (tagBitmap == null) {

                    onCancle();
//                    } else {
//                        returnToPreview(
//
//                        );
//                    }
                    break;
                case R.id.comfirm:
                    onComfirm();
                    break;

                case R.id.camera_returntotake:
                    returnToPreview();
                    break;

            }
        }


    }

    private void toComfirm() {
        mTakeBtn.setVisibility(View.GONE);
        mCancleBtn.setVisibility(View.GONE);

//        mPreTv.setVisibility(View.GONE);


        imageView.setVisibility(View.VISIBLE);
//                mPreTv.setBackground(new BitmapDrawable(bitmap));
//        Matrix matrix=new Matrix();
//        imageView.getWidth();
//        matrix.setScale();
//        Bitmap tempBitmap=Bitmap.createBitmap(tagBitmap,0,0,tagBitmap.getWidth(),tagBitmap.getHeight(),)
        imageView.setImageBitmap(tagBitmap);

        mBlackCoverTv.setVisibility(View.VISIBLE);
        mComfirmLinear.setVisibility(View.VISIBLE);
    }

    private void returnToPreview() {
// 在拍照的时候相机是被占用的,拍照之后需要重新预览
        setCamera();

        mPreTv.setVisibility(View.VISIBLE);


        imageView.setVisibility(View.INVISIBLE);

        mBlackCoverTv.setVisibility(View.GONE);
        tagBitmap = null;
        mComfirmLinear.setVisibility(View.GONE);
        mTakeBtn.setVisibility(View.VISIBLE);
        mCancleBtn.setVisibility(View.VISIBLE);
    }

    /**
     * 设置相机及预览
     */
    private void setCamera() {
//
        try {
            if (camera == null) {
                camera = Camera.open();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (camera != null) {

            camera.startPreview();
        }

    }

    //mTakeBtn finish and success
    private void onComfirm() {
        try {
            if (tagBitmap == null) {
                return;
            }
            saveBitmapOnSd(getIntent().getStringExtra(PIC_PATH));
            setActResult(true);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.savepic_fail, Toast.LENGTH_SHORT).show();
            setActResult(false);
        }
        this.finish();
    }

    private void onCancle() {

        setActResult(false);
//        if (camera != null) {
//            try {
//                camera.release();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        this.finish();
    }

    @Override
    protected void onStop() {
        super.onPause();
        if (camera != null) {
            try {
                camera.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        returnToPreview();
    }

    private void setActResult(boolean isComfirm) {
        if (isComfirm) {

            setResult(Activity.RESULT_OK, getIntent());
        } else {
            setResult(Activity.RESULT_CANCELED);
        }
    }


    private void saveBitmapOnSd(String picPath) throws Exception {

        byte[] data = Bitmap2Bytes(tagBitmap);
        File file = new File(picPath);
        FileOutputStream fos = null;

        fos = new FileOutputStream(file);
        fos.write(data);
        fos.flush();
        fos.close();

        camera.release();
    }
}