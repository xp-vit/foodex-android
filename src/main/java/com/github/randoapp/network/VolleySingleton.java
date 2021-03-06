package com.github.randoapp.network;

import android.content.Context;

import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.github.randoapp.cache.LruMemCache;

import java.io.File;

import static com.github.randoapp.Constants.DEFAULT_CACHE_DIR;
import static com.github.randoapp.Constants.DEFAULT_CACHE_SIZE;

public class VolleySingleton {

    private static VolleySingleton instance = null;

    private RequestQueue requestQueue;
    private ImageLoader imageLoader;

    private VolleySingleton(Context context) {
        requestQueue = createRequestQueue(context);
        imageLoader = new ImageLoader(this.requestQueue, new LruMemCache());
    }

    public static VolleySingleton getInstance(Context context) {
        if (instance == null) {
            instance = new VolleySingleton(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    private RequestQueue createRequestQueue(Context context) {
        if (context == null) {
            return null;
        }

        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        Network network = new BasicNetwork(new HurlStack());
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir, DEFAULT_CACHE_SIZE), network);
        queue.start();
        return queue;
    }

}
