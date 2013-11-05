package com.eucsoft.foodex.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.eucsoft.foodex.Constants;
import com.eucsoft.foodex.MainActivity;
import com.eucsoft.foodex.db.model.Food;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class API {

    public static HttpClient client = new DefaultHttpClient();

    static {
        try {
            SharedPreferences sharedPref = MainActivity.context.getSharedPreferences(Constants.SEESSION_COOKIE_NAME, Context.MODE_PRIVATE);
            BasicClientCookie cookie = new BasicClientCookie(Constants.SEESSION_COOKIE_NAME, sharedPref.getString(Constants.SEESSION_COOKIE_NAME, ""));
            ((DefaultHttpClient) client).getCookieStore().addCookie(cookie);
        } catch (Exception e) {
            //Why is the world so cruel?
        }
    }

    public static void signup(String email, String password) throws Exception {
        try {
            HttpPost request = new HttpPost(Constants.SIGNUP_URL);
            addParamsToRequest(request, Constants.SIGNUP_EMAIL_PARAM, email, Constants.SIGNUP_PASSWORD_PARAM, password);
            HttpResponse response = client.execute(request);
            JSONObject jsonObject = readJSON(response);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw processError(jsonObject);
            }

            storeSession(((DefaultHttpClient) client).getCookieStore());
        } catch (IOException e) {
            throw processError(e);
        }
    }

    public static List<Food> fetchUser() throws Exception {
        try {
            HttpGet request = new HttpGet(Constants.FETCH_USER_URL);
            HttpResponse response = client.execute(request);


            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject json = readJSON(response);
                JSONArray jsonFoods = json.getJSONArray(Constants.FOODS_PARAM);

                List<Food> foods = new ArrayList<Food>(jsonFoods.length());

                for (int i = 0; i < jsonFoods.length(); i++) {
                    Food food = new Food();
                    JSONObject jsonFood = jsonFoods.getJSONObject(i);
                    JSONObject user = jsonFood.getJSONObject(Constants.USER_PARAM);
                    JSONObject stranger = jsonFood.getJSONObject(Constants.STRANGER_PARAM);
                    food.setUserPhotoURL(user.getString(Constants.FOOD_URL_PARAM));
                    food.setUserMap(user.getString(Constants.MAP_URL_PARAM));
                    food.setUserLiked(user.getInt(Constants.BON_APPETIT_PARAM));

                    food.setStrangerPhotoURL(stranger.getString(Constants.FOOD_URL_PARAM));
                    food.setStrangerMap(stranger.getString(Constants.MAP_URL_PARAM));
                    food.setStrangerLiked(stranger.getInt(Constants.BON_APPETIT_PARAM));

                    food.creation = new Date(user.getLong(Constants.CREATION_PARAM));

                    foods.add(food);
                }
                return foods;
            } else {
                throw processError(readJSON(response));
            }
        } catch (IOException e) {
            throw processError(e);
        }
    }

    public static byte[] downloadFood(String url) throws Exception {
        try {
            HttpGet request = new HttpGet(Constants.DOWNLOAD_FOOD_URL + url);
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toByteArray(entity);
            } else {
                throw processError(readJSON(response));
            }
        } catch (IOException e) {
            throw processError(e);
        }
    }

    public static Food uploadFood(File foodFile, Location location) throws Exception {
        try {
            HttpPost request = new HttpPost(Constants.ULOAD_FOOD_URL);
            FileEntity fileEntity = new FileEntity(foodFile, Constants.IMAGE_MIME_TYPE);
            request.setEntity(fileEntity);
            addParamsToRequest(request, Constants.LATITUDE_PARAM, String.valueOf(location.getLatitude()));
            addParamsToRequest(request, Constants.LONGITUDE_PARAM, String.valueOf(location.getLongitude()));

            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject json = readJSON(response);
                Food food = new Food();
                food.setUserPhotoURL(json.getString(Constants.FOOD_URL_PARAM));
                food.creation = new Date(json.getLong(Constants.CREATION_PARAM));
                return food;
            } else {
                throw processError(readJSON(response));
            }
        } catch (IOException e) {
            throw processError(e);
        }
    }

    public static void report(String id) throws Exception {
        try {
            HttpPost request = new HttpPost(Constants.REPORT_URL);
            addParamsToRequest(request, Constants.FOOD_ID_PARAM, id);

            HttpResponse response = client.execute(request);
            JSONObject json = readJSON(response);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw processError(json);
            }
        } catch (UnsupportedEncodingException e) {
            throw processError(e);
        } catch (ClientProtocolException e) {
            throw processError(e);
        } catch (IOException e) {
            throw processError(e);
        }
    }

    public static void bonAppetit(String id) throws Exception {
        try {
            HttpPost request = new HttpPost(Constants.REPORT_URL);
            addParamsToRequest(request, Constants.BON_APPETIT_URL, id);
            HttpResponse response = client.execute(request);
            JSONObject json = readJSON(response);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw processError(json);
            }
        } catch (UnsupportedEncodingException e) {
            throw processError(e);
        } catch (ClientProtocolException e) {
            throw processError(e);
        } catch (IOException e) {
            throw processError(e);
        }
    }

    private static void addParamsToRequest(HttpPost request, String... args) throws UnsupportedEncodingException {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
        for (int i = 0; i < args.length; i+=2) {
            nameValuePairs.add(new BasicNameValuePair(args[i], args[i+1]));
        }
        request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
    }

    private static void storeSession(CookieStore cookieStore) {
        SharedPreferences sharedPref = MainActivity.context.getSharedPreferences(Constants.SEESSION_COOKIE_NAME, Context.MODE_PRIVATE);
        sharedPref.edit().putString(Constants.SEESSION_COOKIE_NAME, cookieStore.getCookies().get(0).getValue());
    }

    private static JSONObject readJSON(HttpResponse response) throws Exception {
        try {
            String line = "";
            StringBuilder json = new StringBuilder();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            while ((line = buffer.readLine()) != null) {
                json.append(line);
            }

            JSONObject jsonObject = new JSONObject(json.toString());
            return jsonObject;
        } catch (JSONException e) {
            throw processError(e);
        } catch (IOException e) {
            throw processError(e);
        }
    }

    private static Exception processError(Object json) {
        if (json instanceof JSONObject) {
            try {
                return new Exception(((JSONObject) json).getString("message"));
            } catch (JSONException e) {
            }
        }
        if (json instanceof Exception) {
            return new Exception("NOT IMPLEMENTED");
        }
        return new Exception("NOT IMPLEMENTED");
    }

}
