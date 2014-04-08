package com.eucsoft.foodex.adapter;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.support.v4.view.ViewPager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.webkit.URLUtil;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eucsoft.foodex.App;
import com.eucsoft.foodex.Constants;
import com.eucsoft.foodex.R;
import com.eucsoft.foodex.animation.AnimationFactory;
import com.eucsoft.foodex.api.API;
import com.eucsoft.foodex.network.FoodImageRequest;
import com.eucsoft.foodex.network.VolleySingleton;
import com.eucsoft.foodex.db.FoodDAO;
import com.eucsoft.foodex.db.model.FoodPair;
import com.eucsoft.foodex.log.Log;
import com.eucsoft.foodex.menu.ReportMenu;
import com.eucsoft.foodex.service.SyncService;
import com.eucsoft.foodex.task.BonAppetitTask;
import com.eucsoft.foodex.task.callback.OnError;

import org.apache.http.auth.AuthenticationException;

import java.util.List;
import java.util.Map;

import static com.android.volley.Request.Priority;

public class FoodPairsAdapter extends BaseAdapter {

    private List<FoodPair> foodPairs;
    private int foodImageSize;

    private int size;

    @Override
    public int getCount() {
        return size;
    }

    @Override
    public Object getItem(int position) {
        return foodPairs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public FoodPairsAdapter(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int displayWidth = display.getWidth();
        int orientation = context.getResources().getConfiguration().orientation;
        foodImageSize = getFoodImageSize(orientation, displayWidth);
        initData();
    }

    private void initData() {
        FoodDAO foodDAO = new FoodDAO(App.context);
        foodPairs = foodDAO.getAllFoodPairs();
        foodDAO.close();
        size = foodPairs.size();
    }

    @Override
    public void notifyDataSetChanged() {
        initData();
        super.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        final FoodPair foodPair = foodPairs.get(position);
        final ViewHolder holder;

        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            LayoutInflater inflater = (LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.food_pair_item, container, false);
            holder = createHolder(convertView);
            addListenersToHolder(holder);
        }

        recycle(holder, foodPair);
        loadImages(holder, foodPair);
        setAnimations(holder);
        return convertView;
    }

    private ViewHolder createHolder(View convertView) {
        ViewHolder holder = new ViewHolder();

        holder.user = new ViewHolder.UserHolder();
        holder.stranger = new ViewHolder.UserHolder();

        holder.bonAppetitButton = (ImageButton) convertView.findViewWithTag("bon_appetit_button");
        holder.viewSwitcher = (ViewSwitcher) convertView.findViewWithTag("viewSwitcher");

        holder.stranger.foodPager = (ViewPager) convertView.findViewWithTag("stranger");
        holder.user.foodPager = (ViewPager) convertView.findViewWithTag("user");

        ViewSwitcher.LayoutParams foodImagesLayout = new ViewSwitcher.LayoutParams(foodImageSize, foodImageSize);
        holder.stranger.foodPager.setLayoutParams(foodImagesLayout);
        holder.user.foodPager.setLayoutParams(foodImagesLayout);

        holder.user.foodMapPagerAdatper = new FoodMapSwitcherAdapter(holder.user);
        holder.user.foodPager.setAdapter(holder.user.foodMapPagerAdatper);

        holder.stranger.foodMapPagerAdatper = new FoodMapSwitcherAdapter(holder.stranger);
        holder.stranger.foodPager.setAdapter(holder.stranger.foodMapPagerAdatper);

        createReportDialog(convertView, holder);

        convertView.setTag(holder);
        return holder;
    }

    private void createReportDialog(View convertView, final ViewHolder holder) {
        holder.reportDialog = (LinearLayout) convertView.findViewWithTag("report_dialog");
        Button reportButton = (Button) holder.reportDialog.getChildAt(0);
        int reportButtonWidth = convertView.getResources().getDimensionPixelSize(R.dimen.report_button_width);
        int reportButtonHeight = convertView.getResources().getDimensionPixelSize(R.dimen.report_button_height);

        LinearLayout.LayoutParams reportButtonParams = new LinearLayout.LayoutParams(reportButtonWidth, reportButtonHeight);
        int marginCenter = foodImageSize / 2;
        reportButtonParams.topMargin =  marginCenter - reportButtonHeight / 2;
        reportButtonParams.leftMargin = marginCenter - reportButtonWidth / 2;
        reportButton.setLayoutParams(reportButtonParams);
        holder.reportDialog.setLayoutParams(new RelativeLayout.LayoutParams(foodImageSize, foodImageSize));
    }

    private void addListenersToHolder(final ViewHolder holder) {
        View.OnClickListener foodOnClickListener = createFoodOnClickListener(holder);
        holder.user.foodMapPagerAdatper.setOnClickListener(foodOnClickListener);
        holder.stranger.foodMapPagerAdatper.setOnClickListener(foodOnClickListener);

        holder.bonAppetitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.stranger.foodPager.isShown() && !holder.foodPair.stranger.isBonAppetit()) {
                    holder.bonAppetitButton.setImageResource(R.drawable.bonappetit2);
                    new BonAppetitTask(holder.foodPair)
                        .onError(new OnError() {
                            @Override
                            public void onError(Map<String, Object> data) {
                                if (holder.stranger.foodPager.isShown()) {
                                    holder.bonAppetitButton.setImageResource(R.drawable.bonappetit);
                                    if (data.get(Constants.ERROR) != null) {
                                        Toast.makeText(App.context, (CharSequence) data.get(Constants.ERROR), Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(App.context, R.string.failed_to_set_bon_appetit_for_food, Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        })
                        .execute();
                }
            }
        });

        holder.reportDialog.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        holder.reportDialog.getChildAt(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    API.report(holder.foodPair.stranger.foodId);
                    ReportMenu.off();
                    SyncService.run();
                } catch (AuthenticationException exc) {
                    exc.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private View.OnClickListener createFoodOnClickListener(final ViewHolder holder) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!holder.animationInProgress) {
                    ViewPager oldFoodMapView = (ViewPager) holder.viewSwitcher.getCurrentView();
                    holder.viewSwitcher.showNext();
                    ViewPager newFoodMapView = (ViewPager) holder.viewSwitcher.getCurrentView();
                    newFoodMapView.setCurrentItem(oldFoodMapView.getCurrentItem());

                    if (holder.stranger.foodPager.isShown()) {
                        if (holder.foodPair.stranger.isBonAppetit()) {
                            holder.bonAppetitButton.setImageResource(R.drawable.bonappetit2);
                        } else {
                            holder.bonAppetitButton.setImageResource(R.drawable.bonappetit);
                        }
                    } else {
                        if (holder.foodPair.user.isBonAppetit()) {
                            holder.bonAppetitButton.setImageResource(R.drawable.bonappetit2);
                        } else {
                            holder.bonAppetitButton.setImageResource(R.drawable.bonappetit);
                        }
                    }
                }
            }
        };
    }

    private void recycle(ViewHolder holder, FoodPair foodPair) {
        holder.foodPair = foodPair;
        holder.animationInProgress = false;

        cancelRequests(holder);

        setViewSwitcherToDefault(holder);
        setPagesToDefault(holder);

        if (ReportMenu.isReport) {
            holder.reportDialog.setVisibility(View.VISIBLE);
            holder.bonAppetitButton.setEnabled(false);
            holder.bonAppetitButton.setVisibility(View.INVISIBLE);
            return;
        }

        holder.bonAppetitButton.setImageResource(foodPair.stranger.isBonAppetit() ? R.drawable.bonappetit2 : R.drawable.bonappetit);

        if (holder.stranger.foodImage != null && holder.user.foodImage != null
                && holder.stranger.mapImage != null && holder.user.mapImage != null) {
            holder.user.foodMapPagerAdatper.recycle(holder.user.foodImage, holder.user.mapImage);
            holder.stranger.foodMapPagerAdatper.recycle(holder.stranger.foodImage, holder.stranger.mapImage);
        }

        holder.reportDialog.setVisibility(View.GONE);
        holder.bonAppetitButton.setVisibility(View.VISIBLE);
    }

    private void cancelRequests(ViewHolder holder) {
        if (holder.stranger.foodRequest != null){
            holder.stranger.foodRequest.cancel();
            holder.stranger.foodRequest = null;
        }
        if (holder.stranger.mapRequest != null){
            holder.stranger.mapRequest.cancel();
            holder.stranger.mapRequest = null;
        }
        if (holder.user.foodRequest != null){
            holder.user.foodRequest.cancel();
            holder.user.foodRequest = null;
        }
        if (holder.user.mapRequest != null){
            holder.user.mapRequest.cancel();
            holder.user.mapRequest = null;
        }
    }

    private void setViewSwitcherToDefault(ViewHolder holder) {
        //disable animation for immediately and undetectable switching to zero child:
        holder.viewSwitcher.setInAnimation(null);
        holder.viewSwitcher.setOutAnimation(null);
        holder.viewSwitcher.setDisplayedChild(0);
    }

    private void setPagesToDefault(ViewHolder holder) {
        holder.user.foodPager.setCurrentItem(0);
        holder.stranger.foodPager.setCurrentItem(0);
    }

    private int getFoodImageSize(int orientation, int displayWidth) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return displayWidth / 2 - (Constants.FOOD_PADDING_LANDSCAPE_COLUMN_LEFT + Constants.FOOD_PADDING_LANDSCAPE_COLUMN_RIGHT);
        } else {
            return displayWidth - Constants.FOOD_MARGIN_PORTRAIT;
        }
    }

    private void setAnimations(final ViewHolder holder) {
        final Animation[] leftToRightAnimation = AnimationFactory.flipAnimation(foodImageSize, AnimationFactory.FlipDirection.LEFT_RIGHT, 600, null);
        final Animation[] rightToLeftAnimation = AnimationFactory.flipAnimation(foodImageSize, AnimationFactory.FlipDirection.RIGHT_LEFT, 600, null);

        holder.viewSwitcher.setOutAnimation(leftToRightAnimation[0]);
        holder.viewSwitcher.setInAnimation(leftToRightAnimation[1]);

        Animation.AnimationListener outAnimationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                holder.animationInProgress = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        leftToRightAnimation[0].setAnimationListener(outAnimationListener);
        leftToRightAnimation[1].setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                holder.viewSwitcher.setOutAnimation(rightToLeftAnimation[0]);
                holder.viewSwitcher.setInAnimation(rightToLeftAnimation[1]);
                holder.animationInProgress = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        rightToLeftAnimation[0].setAnimationListener(outAnimationListener);
        rightToLeftAnimation[1].setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                holder.viewSwitcher.setOutAnimation(leftToRightAnimation[0]);
                holder.viewSwitcher.setInAnimation(leftToRightAnimation[1]);
                holder.animationInProgress = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void loadImages(final ViewHolder holder, final FoodPair foodPair) {
        loadFoodImage(holder.stranger, foodPair.stranger, Priority.HIGH);
        loadFoodImage(holder.user, foodPair.user, Priority.LOW);
        loadMapImage(holder.stranger, foodPair.stranger, Priority.NORMAL);
        loadMapImage(holder.user, foodPair.user, Priority.LOW);
    }

    private void loadFoodImage(final ViewHolder.UserHolder userHolder, final FoodPair.User userFoodPair, Priority priority) {
        if (URLUtil.isValidUrl(userFoodPair.foodURL)) {
            Log.d(FoodPairsAdapter.class, "userFoodPair.foodURL: ", userFoodPair.foodURL);
            userHolder.foodRequest = VolleySingleton.getInstance().getRequestQueue().add(new FoodImageRequest(userFoodPair.foodURL, new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap bitmap) {
                    if (userHolder.foodImage != null) {
                        userHolder.foodImage.setImageBitmap(bitmap);
                    } else {
                        userHolder.foodBitmap = bitmap;
                    }
                }

            }, foodImageSize, foodImageSize, Bitmap.Config.RGB_565, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(FoodPairsAdapter.class, "VolleyError when load food image with url: ", userFoodPair.foodURL, " , because ", error.getMessage());
                    if (userHolder.foodImage != null) {
                        userHolder.foodImage.setImageResource(R.drawable.food_error);
                    } else {
                        userHolder.needSetFoodError = true;
                    }
                }
            }, priority));
        } else {
            Log.e(FoodPairsAdapter.class, "Ignore food image because url: ", userFoodPair.foodURL, " incorrect");
            if (userHolder.foodImage != null) {
                userHolder.foodImage.setImageResource(R.drawable.food_error);
            } else {
                userHolder.needSetFoodError = true;
            }
        }
    }

    private void loadMapImage(final ViewHolder.UserHolder userHolder, final FoodPair.User userFoodPair,  Priority priority) {
        if (URLUtil.isValidUrl(userFoodPair.mapURL)) {
            Log.d(FoodPairsAdapter.class, "userFoodPair.mapURL: ", userFoodPair.mapURL);
            userHolder.mapRequest = VolleySingleton.getInstance().getRequestQueue().add(new FoodImageRequest(userFoodPair.mapURL, new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap bitmap) {
                    if (userHolder.mapImage != null) {
                        userHolder.mapImage.setImageBitmap(bitmap);
                    } else {
                        userHolder.mapBitmap = bitmap;
                    }
                }
            }, foodImageSize, foodImageSize, Bitmap.Config.RGB_565, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(FoodPairsAdapter.class, "VolleyError when load map image with url: ", userFoodPair.mapURL, " , because ", error.getMessage());
                    if (userHolder.mapImage != null) {
                        userHolder.mapImage.setImageResource(R.drawable.map_error);
                    } else {
                        userHolder.needSetMapError = true;
                    }
                }
            }, priority));
        } else {
            Log.e(FoodPairsAdapter.class, "Ignore map image because url: ", userFoodPair.foodURL, " incorrect");
            if (userHolder.mapImage != null) {
                userHolder.mapImage.setImageResource(R.drawable.map_error);
            } else {
                userHolder.needSetMapError = true;
            }
        }
    }

    public static class ViewHolder {
        public boolean animationInProgress = false;

        public FoodPair foodPair;

        public ImageButton bonAppetitButton;
        public ViewSwitcher viewSwitcher;

        public UserHolder user;
        public UserHolder stranger;
        public LinearLayout reportDialog;

        public static class UserHolder {
            public ViewPager foodPager;
            public FoodMapSwitcherAdapter foodMapPagerAdatper;

            public ImageView foodImage;
            public ImageView mapImage;

            public Request foodRequest;
            public Request mapRequest;

            public boolean needSetFoodError = false;
            public boolean needSetMapError = false;

            public Bitmap foodBitmap;
            public Bitmap mapBitmap;
        }
    }
}
