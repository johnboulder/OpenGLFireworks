package com.wherethismove.openglfireworks;

import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements FragmentOpenGL.OnFragmentInteractionListener{

    private final String OPENGL_FRAGMENT_BSTACK_NAME = "open_gl_fragment";
    private int cubesPerExplosion = 4;
    private int consecutiveExplosions = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        FloatingActionButton fabCubesUp = (FloatingActionButton) findViewById(R.id.cubes_per_explosion_up);
        FloatingActionButton fabCubesDown = (FloatingActionButton) findViewById(R.id.cubes_per_explosion_down);
        FloatingActionButton fabExplosionsUp = (FloatingActionButton) findViewById(R.id.consecutive_explosions_up);
        FloatingActionButton fabExplosionsDown = (FloatingActionButton) findViewById(R.id.consecutive_explosions_down);

        final TextView cubesCounter = (TextView) findViewById(R.id.cubesCounter);
        final TextView explosionsCounter = (TextView) findViewById(R.id.explosionsCounter);

        cubesCounter.setText(Integer.toString(cubesPerExplosion));
        explosionsCounter.setText(Integer.toString(consecutiveExplosions));

        fabCubesUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Increment
                cubesPerExplosion+=4;
                cubesCounter.setText(Integer.toString(cubesPerExplosion));
            }
        });

        fabCubesDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Decrement
                if(cubesPerExplosion>=8) {
                    cubesPerExplosion-=4;
                    cubesCounter.setText(Integer.toString(cubesPerExplosion));
                }
            }
        });

        fabExplosionsUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Increment
                consecutiveExplosions++;
                explosionsCounter.setText(Integer.toString(consecutiveExplosions));
            }
        });

        fabExplosionsDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Increment
                if(consecutiveExplosions>=1){
                    consecutiveExplosions--;
                    explosionsCounter.setText(Integer.toString(consecutiveExplosions));
                }
            }
        });

        initializeView();
    }

    private void initializeView()
    {
//        Bundle args = new Bundle();
//        args.putString(IssueFragment.ARG_URL, "https://api.github.com/repos/rails/rails/issues");
//        IssueFragment issuesFragment = new IssueFragment();
//        issuesFragment.setArguments(args);

        FragmentOpenGL openGLFragment = new FragmentOpenGL();
        // Set the list fragment up
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().add(R.id.fragment_container, openGLFragment, OPENGL_FRAGMENT_BSTACK_NAME).commit();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public int getCubesPerExplosion() {
        return cubesPerExplosion;
    }

    public int getConsecutiveExplosions() {
        return consecutiveExplosions;
    }
}
