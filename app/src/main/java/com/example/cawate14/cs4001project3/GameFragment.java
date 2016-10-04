package com.example.cawate14.cs4001project3;

import android.animation.ValueAnimator;
import android.app.Fragment;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.GridLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class GameFragment extends Fragment implements GameViewControl {

    private static final int IMAGE_COUNT = 8;

    private static final Integer[] resimg = {
            R.drawable.badaxx,
            R.drawable.colorcube,
            R.drawable.colors,
            R.drawable.cute,
            R.drawable.firefox,
            R.drawable.fog,
            R.drawable.gone_fishing,
            R.drawable.illustris_box_dmdens_gasvel,
            R.drawable.sunset,
            R.drawable.torus,
    };

    private GameModel model = null;
    private ArrayList<Tile> tiles = new ArrayList<>();
    private ArrayList<Integer> images = new ArrayList<>(Arrays.asList(resimg));
    private Handler handler = null;
    private Timer fliptimer = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        model = new GameModel(this, IMAGE_COUNT);
        handler = new Handler();
        fliptimer = new Timer();
        //Collections.shuffle(images);
    }

    // Based on http://stackoverflow.com/a/28328782/4526277
    private static int getClosestFactors(int in){
        int test = (int)Math.floor(Math.sqrt(in));
        while(in % test != 0){
            test--;
        }
        return test;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.fragment_game, container, false);

        // Setup grid
        GridLayout grid = (GridLayout) root.findViewById(R.id.gamegrid);
        grid.setColumnCount(getClosestFactors(model.getNumTiles()));

        final float density = getResources().getDisplayMetrics().density;
        Log.d("DBG", "Density " + density + "," +
                getResources().getDisplayMetrics().densityDpi + "," +
                getResources().getDisplayMetrics().scaledDensity + ","
        );

        // Init image views
        tiles.clear();
        for(int i = 0; i < model.getNumTiles(); ++i){
            final int ti = i;
            final GameModel gmodel = model;

            // Create tile layout container
            // A GridLayout is used here because GONE children automatically take up no space
            GridLayout tilelayout = new GridLayout(getContext());
            ViewGroup.LayoutParams tileParam = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tilelayout.setLayoutParams(tileParam);
            tilelayout.setPadding(10, 10, 10, 10);

            ViewGroup.LayoutParams imageParam = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            Drawable fg = new ColorDrawable(ContextCompat.getColor(getContext(), R.color.colorAccent));
            Drawable bg = new ColorDrawable(ContextCompat.getColor(getContext(), R.color.colorPrimary));

            // Front of tile
            final ImageView front = new ImageView(getContext());
            front.setLayoutParams(imageParam);
            front.setBackground(fg);

            // Back of tile
            final ImageView back = new ImageView(getContext());
            back.setLayoutParams(imageParam);
            back.setBackground(bg);
            //back.setAdjustViewBounds(true);
            //back.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // Setup layout callback for measurement
            ViewTreeObserver vto = front.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    front.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    //int x = (int)Math.floor(front.getMeasuredWidth() / density);
                    //int y = (int)Math.floor(front.getMeasuredHeight() / density);
                    int x = front.getMeasuredWidth();
                    int y = front.getMeasuredHeight();
                    Log.d("DBG", "Resize to " + x + "," + y);

                    // Load image in background
                    ImageResourceWorkerTask imageWorker = new ImageResourceWorkerTask(getResources(), back, x, y);
                    imageWorker.execute(images.get(model.getImageId(ti)));
                }
            });

            // Flip animator
            ValueAnimator flipAnimator = ValueAnimator.ofFloat(0f, 1f);
            flipAnimator.addUpdateListener(new FlipListener(front, back, model.getFlipped(i)));

            // Click listener
            front.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Update game model
                    gmodel.clickTile(ti);
                }
            });

            // Add image views
            tilelayout.addView(front);
            tilelayout.addView(back);

            // Add tile to grid
            GridLayout.LayoutParams gridParam = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1, 1f));
            //gridParam.setMargins(10, 10, 10, 10);
            grid.addView(tilelayout, gridParam);

            // Store the views and animators
            Tile tile = new Tile();
            tile.layout = tilelayout;
            tile.front = front;
            tile.back = back;
            tile.anim = flipAnimator;
            tiles.add(tile);
        }

        return root;
    }

    @Override
    public void flipTile(final int i) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                tiles.get(i).anim.start();
            }
        });
    }

    @Override
    public void unFlipTile(int i) {
        fliptimer.schedule(new FlipTimer(i), 1000);
    }

    @Override
    public void tilesMatched(int i, int j) {
        //tiles.get(i).back.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.tileMatch));
        //tiles.get(j).back.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.tileMatch));
    }

    @Override
    public void tilesNotMatched(int i, int j) {
        //tiles.get(i).back.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.tileNoMatch));
        //tiles.get(j).back.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.tileNoMatch));
    }

    class Tile {
        public GridLayout layout;
        public ImageView front;
        public ImageView back;
        public ValueAnimator anim;
    }

    class FlipTimer extends TimerTask {

        private int tile;

        public FlipTimer(int i){
            tile = i;
        }

        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // Reverse flip
                    tiles.get(tile).anim.reverse();
                }
            });
        }
    }

}
