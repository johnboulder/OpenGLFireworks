package com.wherethismove.openglfireworks;

/**
 * Created by stockweezie on 5/25/2016.
 */

public class CubePrototype {

    private int mTimeToLive = 0;
    private float mCenterPosition[] = {0.0f, 0.0f};
    private float mFinalPosition[] = {0.0f, 0.0f};
    private float mIntermediatePosition[] = {0.0f, 0.0f};
    private boolean isFunctionLinear = false;

    public CubePrototype(float startX, float startY, float endX, float endY, float intX, float intY, int ttl, boolean isLinear)
    {
        // Set pathing data
        mTimeToLive = ttl;

        mCenterPosition[0] = startX;
        mCenterPosition[1] = startY;

        mIntermediatePosition[0] = intX;
        mIntermediatePosition[1] = intY;

        mFinalPosition[0] = endX;
        mFinalPosition[1] = endY;

        isFunctionLinear = isLinear;
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

    public boolean isFunctionLinear(){
        return isFunctionLinear;
    }
}
