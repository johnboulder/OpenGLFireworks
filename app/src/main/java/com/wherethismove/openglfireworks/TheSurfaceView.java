package com.wherethismove.openglfireworks;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
public class TheSurfaceView extends GLSurfaceView {

    //private final TheRenderer mRenderer;

    public TheSurfaceView(Context context) {
        super(context);
    }
}
