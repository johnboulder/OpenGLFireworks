package com.wherethismove.openglfireworks;

import android.content.Context;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class FragmentOpenGL extends Fragment {

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;

    private GLSurfaceView mGLView;
    //private static final String ARG_PARAM1 = "param1";

    //private String mParam1;
    private OnFragmentInteractionListener mListener;

    public FragmentOpenGL() {
        // Required empty public constructor
    }

    public static FragmentOpenGL newInstance(String param1, String param2) {
        FragmentOpenGL fragment = new FragmentOpenGL();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_open_gl, container, false);
        mGLView = (GLSurfaceView) view.findViewById(R.id.gl_surface_view);

        mGLView.setEGLContextClientVersion(2);
        final TheRenderer renderer = new TheRenderer();
        mGLView.setRenderer(renderer);
        //mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mGLView.setOnTouchListener(new View.OnTouchListener(){

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                // MotionEvent reports input details from the touch screen
                // and other input controls. In this case, you are only
                // interested in events where the touch position changed.

                switch (e.getAction()) {
                    case MotionEvent.ACTION_UP:

                        // Get the values for consecutiveExplosions and cubesPerExplosion
                        MainActivity activity = (MainActivity) getActivity();
                        int explosions = activity.getConsecutiveExplosions();
                        int cubes = activity.getCubesPerExplosion();

                        // Get the position
                        float x = e.getX();
                        float y = e.getY();

                        renderer.createCubeFirework(x, y, cubes, explosions);
                        mGLView.requestRender();
                }
                return true;
            }
        });

        return view;
        //return inflater.inflate(R.layout.fragment_open_gl, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGLView != null) { mGLView.onPause(); }
        mGLView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGLView != null) { mGLView.onResume(); }
        mGLView.onResume();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
