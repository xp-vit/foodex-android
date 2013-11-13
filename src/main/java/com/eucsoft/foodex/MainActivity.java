package com.eucsoft.foodex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.eucsoft.foodex.db.FoodDAO;
import com.eucsoft.foodex.db.model.FoodPair;
import com.eucsoft.foodex.fragment.AuthFragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();

        //TODO: REMOVE THIS METHOD
        initDBForTesting();

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_screen, new AuthFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //TODO: REMOVE
    private void initDBForTesting() {
        FoodDAO foodDAO = new FoodDAO(context);
        if (foodDAO.getFoodPairsNumber() <= 30) {

            for (int i = 0; i < 5; i++) {
                List<FoodPair> foods = new ArrayList<FoodPair>();
                FoodPair foodPair = new FoodPair();
                foodPair.user.foodURL = "http://cool-projects.com/foodex/abcd/abcd24jjf4f4f4f.jpg";
                foodPair.user.foodDate = new Date();
                foodPair.user.mapURL = "http://cool-projects.com/foodex/map/cccc/cccciewf32wfa.png";
                foodPair.user.bonAppetit = 1;
                foodPair.stranger.foodURL = "http://cool-projects.com/foodex/abcd/abcdfdsjofjo3.jpg";
                foodPair.stranger.foodDate = new Date();
                foodPair.stranger.mapURL = "http://cool-projects.com/foodex/map/cccc/cccciewf32wfa.png";
                foodPair.stranger.bonAppetit = 0;
                foods.add(foodPair);

                foodPair = new FoodPair();
                foodPair.user.foodURL = "http://cool-projects.com/foodex/abcd/abcdfdsjofjo3.jpg";
                foodPair.user.foodDate = new Date();
                foodPair.user.mapURL = "http://cool-projects.com/foodex/map/cccc/cccciewf32wfa.png";
                foodPair.user.bonAppetit = 0;
                foodPair.stranger.foodURL = "http://cool-projects.com/foodex/abcd/abcd24jjf4f4f4f.jpg";
                foodPair.stranger.foodDate = new Date();
                foodPair.stranger.mapURL = "http://cool-projects.com/foodex/map/cccc/cccciewf32wfa.png";
                foodPair.stranger.bonAppetit = 1;
                foods.add(foodPair);

                foodPair = new FoodPair();
                foodPair.user.foodURL = "http://cool-projects.com/foodex/abcd/abcd3fiojdsijf03f.jpg";
                foodPair.user.foodDate = new Date();
                foodPair.user.mapURL = "http://cool-projects.com/foodex/map/cccc/cccciewf32wfa.png";
                foodPair.user.bonAppetit = 1;
                foodPair.stranger.foodURL = "http://cool-projects.com/foodex/abcd/abcdfjiowjf32.jpg";
                foodPair.stranger.foodDate = new Date();
                foodPair.stranger.mapURL = "http://cool-projects.com/foodex/map/cccc/cccciewf32wfa.png";
                foodPair.stranger.bonAppetit = 0;
                foods.add(foodPair);

                foodPair = new FoodPair();
                foodPair.user.foodURL = "http://cool-projects.com/foodex/abcd/abcdfjiowjf32.jpg";
                foodPair.user.foodDate = new Date();
                foodPair.user.mapURL = "http://cool-projects.com/foodex/map/cccc/cccciewf32wfa.png";
                foodPair.user.bonAppetit = 0;
                foodPair.stranger.foodURL = "http://cool-projects.com/foodex/abcd/abcd3fiojdsijf03f.jpg";
                foodPair.stranger.foodDate = new Date();
                foodPair.stranger.mapURL = "http://cool-projects.com/foodex/map/cccc/cccciewf32wfa.png";
                foodPair.stranger.bonAppetit = 1;
                foods.add(foodPair);

                foodDAO.insertFoodPairs(foods);
            }
        }
        foodDAO.close();
    }

}