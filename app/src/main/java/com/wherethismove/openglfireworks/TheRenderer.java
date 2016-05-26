/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wherethismove.openglfireworks;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class TheRenderer implements GLSurfaceView.Renderer {

    // TODO create a second cube object that contains the non-opengl data
    private ArrayList<CubePrototype> objectsPendingCreation = new ArrayList<>();

    private static final String TAG = "MyGLRenderer";
    private float mHeight;
    private float mWidth;
    private final float VERTICAL_SCALE_FACTOR = 1.6f;
    private int mConsecutiveExplosions = 0;
    private int mCubesPerExplosion = 0;
    private final float[] DEFAULT_START_POINT = {-0.3f, -2.3f};
    private final float[] DEFAULT_INTERMEDIATE_POINT = {0.0f, 0.0f};

    // The distance at which the object should move each frame, "speed"
    private final float UNITS_PER_FRAME = 0.005f;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    //private Cube[] renderObjectArray;
    private ArrayList<Cube> renderObjectList = new ArrayList<>();

    private float mAngle;
    //private float mNextPosition[] = {-1.0f, -1.0f};

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        List<CubePrototype> objectsPendingCreation_synced = Collections.synchronizedList(objectsPendingCreation);
        List<Cube> renderObjectList_synced = Collections.synchronizedList(renderObjectList);

        ArrayList<Cube> tempList = new  ArrayList<>();
        while(objectsPendingCreation_synced.size()>0)
        {
            CubePrototype cubeP = objectsPendingCreation_synced.get(0);
            float[] endP = cubeP.getFinalPosition();
            float[] intP = cubeP.getIntermediatePosition();
            float[] curP = cubeP.getPosition();
            int ttl = cubeP.getTimeToLive();

            Cube cub = new Cube(curP[0], curP[1], endP[0], endP[1], intP[0], intP[1], ttl, UNITS_PER_FRAME, cubeP.isFunctionLinear());

            objectsPendingCreation_synced.remove(cubeP);
            tempList.add(cub);
        }

        if(tempList.size()>0)
            renderObjectList_synced.addAll(tempList);

        if(renderObjectList_synced.size() > 0)
        {
            // Render the objects in the render object array
            for(Iterator<Cube> iter = renderObjectList_synced.iterator(); iter.hasNext(); )
            {
                Cube object = iter.next();
                // If the object hasn't been drawn on the screen yet, draw it at its starting position
                if(!object.initialPositionSet())
                {
                    float[] temp = new float[16];

                    Matrix.translateM(object.mModelMatrix, 0, object.getPosition()[0], object.getPosition()[1], 0f);
                    Matrix.multiplyMM(temp, 0, mMVPMatrix, 0, object.mModelMatrix, 0);

                    object.draw(temp);
                }
                else
                    drawObject(object, mMVPMatrix, iter);
            }
        }
    }

    /**
     * Adds objects to be rendered to the object array. Intended to be used for handling the
     * consecutive explosions of cubes for each cube...
     */
    public void addConsecutiveObjectsToObjectArray()
    {
        //Cube cub = new Cube(curP[0], curP[1], endP[0], endP[1], intP[0], intP[1], ttl, UNITS_PER_FRAME);
        //renderObjectList.add(cub);
    }

    private void drawObject(Cube object, float[] MVPmatrix, Iterator<Cube> iter)
    {
        // When we draw an object we must do these things
        /*
        * 1. Check if the object has reached its final destination
        *   - if it has do not render it, instead check which iteration it was part of and if we need to spawn more cubes
        *
        * 2. Render the object if it hasn't reached its final destination
        *   2.1 Get the object's current position, increment the x value, and pass it to its path function
        */
        float[] scratch = new float[16];

        float currentPosition[] = object.getPosition();
        float finalPosition[] = object.getFinalPosition();

        // if object has reached its final destination
        if(currentPosition[0] == finalPosition[0])
        {
            int TTL = object.getTimeToLive();
            // Check the time to live field, if it's greater than 0, decrement it and create objects with that TTL
            if(TTL>0){
                TTL--;
                List<CubePrototype> objectsPendingCreation_synced = Collections.synchronizedList(objectsPendingCreation);
                // A circle of radius r centered on a point (h, k)
                // (x-h)^2 + (y-k)^2 = r^2
                // find mCubesPerExplosion points around the circle and add them to the objectsPendingCreation list
                boolean xModified = false;
                boolean yModified = false;
                float[] centerP = object.getPosition();
                // TODO setup a member of cube to use so that this value scales down with each consecutive explosion
                float r = 0.3f;
                float h = centerP[0];
                float k = centerP[1];

                final float X_MODIFIER = abs(h) + r;
                final float Y_MODIFIER = abs(k) + r;

                if(h<0)
                {
                    h+=X_MODIFIER;
                    xModified=true;
                }
                if(k<0)
                {
                    k+=Y_MODIFIER;
                    yModified=true;
                }

                float maxX = h+r;
                float minX = h-r;
                float maxY = k+r;
                float minY = k-r;

                float width = maxX-minX;

                // Multiply by 2 since there's 2 corresponding points for every value of x
                float increment = (width/2)/(mCubesPerExplosion/2);

                // Ensure that we start at a point that isn't on the circle's horizontal axis
                maxX-=increment;

                // Find the bottom right most values and
                for(int i = 0; i<mCubesPerExplosion/4; i++)
                {

                    float[] upperRightPoint = new float[2];
                    float[] upperLeftPoint = new float[2];
                    float[] lowerLeftPoint = new float[2];
                    float[] lowerRightPoint = new float[2];

                    float x = maxX - increment*i;
                    float r2 = r*r;
                    float x_h = x-h;
                    float x_h2 = x_h*x_h;
                    double y_d = -(sqrt(r2 - x_h2)/2) + k;
                    float y = (float)y_d;

                    lowerRightPoint[0] = x;
                    lowerRightPoint[1] = y;

                    lowerLeftPoint[0] = maxX - x;
                    lowerLeftPoint[1] = y;

                    upperRightPoint[0] = x;
                    upperRightPoint[1] = maxY - y;

                    upperLeftPoint[0] = maxX - x;
                    upperLeftPoint[1] = maxY - y;

                    if(xModified){
                        // Modify all the points back to their original position

                    }

                    CubePrototype lrCP = new CubePrototype(
                            // Start point
                            (xModified)? h-X_MODIFIER : h,
                            (yModified)? k-Y_MODIFIER : k,
                            // End point
                            (xModified)? lowerRightPoint[0]-X_MODIFIER : lowerRightPoint[0],
                            (yModified)? lowerRightPoint[1]-Y_MODIFIER : lowerRightPoint[1],
                            // Intermediate point
                            (xModified)? r/2 + h - X_MODIFIER : r/2 + h,
                            // (lower values I want a smaller arc so r/4 instead of r/2)
                            (yModified)? r/4 + k - Y_MODIFIER : r/4 + k,
                            TTL,
                            true);
                    objectsPendingCreation_synced.add(lrCP);

                    CubePrototype urCP = new CubePrototype(
                            // Same
                            (xModified)?  h -X_MODIFIER :  h ,
                            (yModified)?  k -Y_MODIFIER :  k ,
                            // Same
                            (xModified)? upperRightPoint[0]-X_MODIFIER : upperRightPoint[0],
                            (yModified)? upperRightPoint[1]-Y_MODIFIER : upperRightPoint[1],
                            // Same
                            (xModified)? r/2 + h - X_MODIFIER : r/2 +  h ,
                            (yModified)? r/2 + k - Y_MODIFIER : r/2 +  k ,
                            TTL,
                            true);
                    objectsPendingCreation_synced.add(urCP);

                    CubePrototype llCP = new CubePrototype(
                            // Same
                            (xModified)?  h -X_MODIFIER :  h ,
                            (yModified)?  k -Y_MODIFIER :  k ,
                            // Same
                            (xModified)? lowerLeftPoint[0]-X_MODIFIER : lowerLeftPoint[0],
                            (yModified)? lowerLeftPoint[1]-Y_MODIFIER : lowerLeftPoint[1],
                            // Subtract by  h  instead
                            (xModified)? r/2 -  h  - X_MODIFIER : r/2 -  h ,
                            (yModified)? r/4 +  k  - Y_MODIFIER : r/4 +  k ,
                            TTL,
                            true);
                    objectsPendingCreation_synced.add(llCP);

                    CubePrototype ulCP = new CubePrototype(
                            // Same
                            (xModified)?  h -X_MODIFIER :  h ,
                            (yModified)?  k -Y_MODIFIER :  k ,
                            // Same
                            (xModified)? upperLeftPoint[0]-X_MODIFIER : upperLeftPoint[0],
                            (yModified)? upperLeftPoint[1]-Y_MODIFIER : upperLeftPoint[1],
                            // Subtract by  h  instead
                            (xModified)? r/2 -  h  - X_MODIFIER : r/2 -  h ,
                            (yModified)? r/2 +  k  - Y_MODIFIER : r/2 +  k ,
                            TTL,
                            true);
                    objectsPendingCreation_synced.add(ulCP);

                }
            }
            iter.remove();
            return;
        }

//        Matrix.setRotateM(mCube.mModelMatrix, 0, 0, 1.0f, 0.2f, 0.0f);
//        Matrix.multiplyMM(temp, 0, scratch, 0, mCube.mModelMatrix, 0);
//        mCube.draw(temp);

        // Find the values for how much the x and y are going to translate relative to the current position
        float nextPosition[] = object.getNextPosition();
        float trans_x, trans_y;
        trans_x = currentPosition[0] - nextPosition[0];
        trans_x = -trans_x;
        trans_y = currentPosition[1] - nextPosition[1];
        trans_y = -trans_y;

        Matrix.translateM(object.mModelMatrix, 0, trans_x, trans_y, 0f);
        Matrix.multiplyMM(scratch, 0, MVPmatrix, 0, object.mModelMatrix, 0);


        // Save the next currentPosition of the object
        object.setPosition(nextPosition[0], nextPosition[1]);

        // Draw triangle
        object.draw(scratch);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        setWidthAndHeight(width, height);

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

    }

    // Translates the positions on the screen into quadrants on the matrix
    private float[] translatePositions(float x, float y){
        // x is 1080
        // y is 1920
        // if(orientation is portrait)
        float verticalAxis = mWidth/2; // 540
        float horizontalAxis = mHeight/2; // 960
        float new_x = 0.0f;
        float new_y = 0.0f;

        if(y<horizontalAxis){
            y = horizontalAxis - y;
            new_y = y/horizontalAxis;
        }
        else if(y>horizontalAxis){
            y = y - horizontalAxis;
            // Negate the value because android's measurements go from top left corner of screen
            // i.e. The higher the value, the closer it is to the bottom
            new_y = -(y/horizontalAxis);
        }

        if(x<verticalAxis){
            x = verticalAxis - x;
            new_x = -(x/verticalAxis);
        }
        else if(x>verticalAxis){
            x = x - verticalAxis;
            new_x = x/verticalAxis;
        }

        // Through trial and error I found that the view bounds extend beyond the coordinate system of -1.0 <= y <= 1.0
        // There's probably an easy way to calculate these bounds based on the frustum, but this works for the time being.
        float translation[] = {new_x, new_y*VERTICAL_SCALE_FACTOR};
        return translation;
    }



    /**
     * Utility method for compiling a OpenGL shader.
     *
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);
        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    /**
     * Returns the rotation angle of the triangle shape (mTriangle).
     *
     * @return - A float representing the rotation angle.
     */
    public float getAngle() {
        return mAngle;
    }

    /**
     * Sets the rotation angle of the triangle shape (mTriangle).
     */
    public void setAngle(float angle) {
        mAngle = angle;
    }

    /**
     * @param x     The x position on the screen where the firework will explode
     * @param y     The y position on the screen where the firework will explode
     * @param cubesPerExplosion     Number of cubes to appear after the explosion
     * @param consecutiveExplosions     Number of consecutive explosions after the first explosion
     */
    public void createCubeFirework(float x, float y, int cubesPerExplosion, int consecutiveExplosions)
    {

        mCubesPerExplosion = cubesPerExplosion;
        mConsecutiveExplosions = consecutiveExplosions;

        float translation[];
        translation = translatePositions(x, y);
        float trans_x = translation[0];
        float trans_y = translation[1];

        CubePrototype startCube;
        if(trans_x>0)
            startCube = new CubePrototype(DEFAULT_START_POINT[0], DEFAULT_START_POINT[1], trans_x, trans_y, DEFAULT_INTERMEDIATE_POINT[0], DEFAULT_INTERMEDIATE_POINT[1], consecutiveExplosions, false);
        else
            startCube = new CubePrototype(-DEFAULT_START_POINT[0], DEFAULT_START_POINT[1], trans_x, trans_y, DEFAULT_INTERMEDIATE_POINT[0], DEFAULT_INTERMEDIATE_POINT[1], consecutiveExplosions, false);

        List<CubePrototype> objectsPendingCreation_synced = Collections.synchronizedList(objectsPendingCreation);
        objectsPendingCreation_synced.add(startCube);

    }

    public void setWidthAndHeight(float width, float height){
        mWidth = width;
        mHeight = height;
    }

}