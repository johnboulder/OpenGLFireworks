package com.wherethismove.openglfireworks;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * Created by stockweezie on 5/18/2016.
 */

class Cube {

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mColorBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    private int mTimeToLive = 0;
    private float mUnitsPerFrame;
    // TODO create variables for startPosition AND currentPosition
    private float mCenterPosition[] = {0.0f, 0.0f};
    private float mStartPosition[] = {0.0f, 0.0f};
    private float mFinalPosition[] = {0.0f, 0.0f};
    private float mIntermediatePosition[] = {0.0f, 0.0f};
    private boolean isFunctionLinear = false;

    private boolean matrixInitialPositionSet = false;

    static final int COORDS_PER_VERTEX = 3;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Values for the quadratic equation dictating this object's path
    private float ma = 0.0f;
    private float mb = 0.0f;
    private float mc = 0.0f;
    private float mm = 0.0f;

    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // The matrix must be included as a modifier of gl_Position.
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    public float[] mModelMatrix = new float[16];

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    // 8 corners of the cube
    private float vertices[] = {
            -0.01f, -0.01f, -0.01f, // 0
            0.01f, -0.01f, -0.01f,  // 1
            0.01f,  0.01f, -0.01f,  // 2
            -0.01f, 0.01f, -0.01f,  // 3
            -0.01f, -0.01f,  0.01f, // 4
            0.01f, -0.01f,  0.01f,  // 5
            0.01f,  0.01f,  0.01f,  // 6
            -0.01f,  0.01f,  0.01f  // 7
    };

    private final short drawOrder[] = {
            0, 1, 2,
            0, 2, 3,
            1, 5, 6,
            1, 6, 2,
            4, 5, 6,
            4, 6, 7,
            0, 4, 7,
            0, 7, 3};

    private float colors[] = {
            //0.25f,  0.76f, 0.44f,  1.0f,
            0.0f,  0.0f, 1.0f,  1.0f,
            0.86f,  0.79f, 0.45f,  1.0f,
            0.96f,  0.58f,  0.98f,  1.0f,
            0.43f,  0.93f,  0.47f,  1.0f,
            0.56f,  0.23f,  0.18f,  1.0f,
            0.86f,  0.66f,  0.09f,  1.0f,
            0.51f,  0.99f,  0.74f,  1.0f,
            0.28f,  0.66f,  0.38f,  1.0f
    };

    private short indices[] = {
            0, 4, 5, 0, 5, 1,
            1, 5, 6, 1, 6, 2,
            2, 6, 7, 2, 7, 3,
            3, 7, 4, 3, 4, 0,
            4, 7, 6, 4, 6, 5,
            3, 0, 1, 3, 1, 2
    };

    // TODO set this up so that all points necessary for rendering and calculating arc are initialized
    public Cube(float startX, float startY, float endX, float endY, float intX, float intY, int ttl, float unitsPerFrame, boolean linear)
    {
        // Set pathing data
        mUnitsPerFrame = unitsPerFrame;
        mTimeToLive = ttl;

        mCenterPosition[0] = startX;
        mCenterPosition[1] = startY;

        mStartPosition[0] = startX;
        mStartPosition[1] = startY;

        mIntermediatePosition[0] = intX;
        mIntermediatePosition[1] = intY;

        mFinalPosition[0] = endX;
        mFinalPosition[1] = endY;

        isFunctionLinear = linear;

        // Generate the remaining pathing data
        generatePathFunction();

        Matrix.setIdentityM(mModelMatrix, 0);

        ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mVertexBuffer = byteBuf.asFloatBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);

        vertexBuffer = byteBuf.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);

        byteBuf = ByteBuffer.allocateDirect(colors.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mColorBuffer = byteBuf.asFloatBuffer();
        mColorBuffer.put(colors);
        mColorBuffer.position(0);

        int vertexShader = TheRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = TheRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
    }

    public void draw(float[] mvpMatrix)
    {
        // Flag saying that the object has been drawn once already
        if(!matrixInitialPositionSet)
            matrixInitialPositionSet = true;

        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");


        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, colors, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        TheRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        TheRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the cube
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, indices.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    public void generatePathFunction()
    {
        generatePathFunction(mCenterPosition, mFinalPosition, mIntermediatePosition);
    }

    /**
     *
     */
    public void generatePathFunction(float[] start, float[] end, float[] intermediate)
    {
        if(isFunctionLinear){
            float[] x = new float[2];
            float[] y = new float[2];
            x[0] = start[0];
            y[0] = start[1];

            x[1] = end[0];
            y[1] = end[1];

            // y = mx + b
            mm = (y[0] - y[1])/(x[0] - x[1]);
            mb = -(mm*x[0]) + y[0];
        }
        else{
            // Check that none of the x values are equal, if any are, adjust them
            // Only ever adjust the start and intermediate points...
            if(start[0] == end[0] || start[0] == intermediate[0])
            {
                if(end[0]<intermediate[0])
                    start[0] = intermediate[0] + 0.2f;
                else
                    start[0] = intermediate[0] - 0.2f;
            }
            if(intermediate[0] == end[0])
            {
                intermediate[0] = (start[0]+end[0])/2;
            }

            float[] x = new float[3];
            float[] y = new float[3];
            x[0] = start[0];
            y[0] = start[1];

            x[1] = end[0];
            y[1] = end[1];

            x[2] = intermediate[0];
            y[2] = intermediate[1];

            float A1 = -(x[0]*x[0])+(x[1]*x[1]);
            float B1 = -x[0]+x[1];
            float D1 = -y[0]+y[1];
            float A2 = -(x[1]*x[1])+(x[2]*x[2]);
            float B2 = -x[1]+x[2];
            float D2 = -y[1]+y[2];
            float Bmult = -(B2/B1);
            float A3 = (Bmult*A1)+A2;
            float D3 = (Bmult*D1)+D2;

            float a = D3/A3;
            float b = (D1-(A1*a))/B1;
            float c = y[0] - (a*(x[0]*x[0])) - (b*x[0]);

            ma = a;
            mb = b;
            mc = c;
        }

    }

    /**
     *
     */
    public float[] getNextPosition()
    {
        // ax^2 + bx + c
        float currentX = mCenterPosition[0];
        float nextX, y;

        if(mFinalPosition[0]>mStartPosition[0])
            nextX = currentX + mUnitsPerFrame;
        else
            nextX = currentX - mUnitsPerFrame;

        if(mFinalPosition[0] > mStartPosition[0] && nextX > mFinalPosition[0])
            nextX = mFinalPosition[0];
        else if(mFinalPosition[0] < mStartPosition[0] && nextX < mFinalPosition[0])
            nextX = mFinalPosition[0];

        if(isFunctionLinear){
            y = mm*nextX + mb;
        }
        else{
            float ax2 = ma * (nextX * nextX);
            float bx = mb * nextX;
            y = ax2 + bx + mc;
        }
        return new float[] {nextX, y};
    }

    public void setIntermediatePosition(float x, float y)
    {
        this.mIntermediatePosition[0] = x;
        this.mIntermediatePosition[1] = y;
    }

    public float[] getIntermediatePosition()
    {
        return mIntermediatePosition;
    }

    public void setFinalPosition(float x, float y)
    {
        this.mFinalPosition[0] = x;
        this.mFinalPosition[1] = y;
    }

    public float[] getFinalPosition()
    {
        return mFinalPosition;
    }

    public void setPosition(float x, float y)
    {
        mCenterPosition[0] = x;
        mCenterPosition[1] = y;
    }

    public float[] getPosition()
    {
        return mCenterPosition;
    }

    public int getTimeToLive() {
        return mTimeToLive;
    }

    public void setTimeToLive(int timeToLive) {
        mTimeToLive = timeToLive;
    }

    public boolean initialPositionSet(){
        return this.matrixInitialPositionSet;
    }
}
