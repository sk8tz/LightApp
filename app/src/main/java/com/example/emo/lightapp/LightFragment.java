package com.example.emo.lightapp;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
// import android.support.v4.app.Fragment;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Collections;

public class LightFragment extends Fragment {

    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private Switch mSwitch;

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    public LightFragment() {
    }

    public static LightFragment newInstance() {
        return new LightFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_light, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwitch = (Switch) view.findViewById(R.id.switch_light);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                turnFlashMode();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        openCamera();
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void openCamera() {
        Activity activity = getActivity();

        int permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {
            String cameraId = null;
            for (final String id : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);

                Integer direction = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (direction != CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }

                Boolean availability = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(availability)) {
                    cameraId = id;
                    break;
                }
            }

            if (cameraId != null) {
                manager.openCamera(cameraId, mStateCallback, mBackgroundHandler);
            } else {
                Toast.makeText(activity, "Camera flash is not available", Toast.LENGTH_SHORT).show();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;

            try {
                mSurfaceTexture = new SurfaceTexture(1);
                mSurface = new Surface(mSurfaceTexture);
                mPreviewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewRequestBuilder.addTarget(mSurface);

                camera.createCaptureSession(
                        Collections.singletonList(mSurface),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mCaptureSession = session;
                                turnFlashMode();
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                                Toast.makeText(getActivity(), "Camera configuration failed", Toast.LENGTH_SHORT).show();
                            }
                        },
                        null
                );
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private void closeCamera() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void turnFlashMode() {
        if ((mPreviewRequestBuilder == null) || (mCaptureSession == null)) {
            return;
        }

        int metadata = mSwitch.isChecked()
                ? CameraMetadata.FLASH_MODE_TORCH
                : CameraMetadata.FLASH_MODE_OFF;

        mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, metadata);

        try {
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void requestCameraPermission() {
        if (FragmentCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(getActivity(), "Permission to camera has been denied", Toast.LENGTH_LONG).show();
            Interact(null);
        } else {
            FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
            } else {
                // permission denied
                Toast.makeText(getActivity(), "Permission to camera is denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(@Nullable String[] arguments);
    }

    private OnFragmentInteractionListener mListener;

    public void Interact(@Nullable String[] arguments) {
        if (mListener != null) {
            mListener.onFragmentInteraction(arguments);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
