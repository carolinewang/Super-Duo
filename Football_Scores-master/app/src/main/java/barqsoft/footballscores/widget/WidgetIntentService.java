/*
 * Copyright (C) 2015 The Android Open Source Project
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
package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utility;
import barqsoft.footballscores.scoresAdapter;

/**
 * IntentService which handles updating all Today widgets with the latest data
 */
public class WidgetIntentService extends IntentService {
    public static final String LOG_TAG = "WidgetIntentService";
    public static final String ACTION_DATA_UPDATED = "barqsoft.footballscores.ACTION_DATA_UPDATED";
    private static final String[] SCORE_COLUMNS = {
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL,
            DatabaseContract.scores_table.MATCH_DAY,
            DatabaseContract.scores_table.MATCH_ID,
            DatabaseContract.scores_table.DATE_COL,
            DatabaseContract.scores_table.TIME_COL,
    };
    // these indices must match the projection
    private static final int INDEX_HOME_COL = 0;
    private static final int INDEX_AWAY_COL = 1;
    private static final int INDEX_HOME_GOALS_COL = 2;
    private static final int INDEX_AWAY_GOALS_COL = 3;
    private static final int INDEX_MATCH_DAY = 4;
    private static final int INDEX_MATCH_ID = 5;
    private static final int INDEX_DATE_COL = 6;
    private static final int INDEX_TIME_COL = 7;

    public Cursor cursor;
    public WidgetIntentService() {
        super("WidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG,"inside onHandleIntent");

        // Retrieve all of the Today widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                WidgetProvider.class));

        // Get today's data from the ContentProvider
        Uri scoreWithDateUri = DatabaseContract.scores_table.buildScoreWithDate();
        SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
        String formateDate = mformat.format(System.currentTimeMillis());
        String[] dates = {formateDate};
//        String[] dates = {"2016-01-03"};

        cursor = getContentResolver().query(scoreWithDateUri, SCORE_COLUMNS, null,
                dates, DatabaseContract.scores_table.DATE_COL + " ASC");
        Log.d (LOG_TAG,"cursor"+ cursor);
        if (cursor == null) {
//            for (int appWidgetId : appWidgetIds) {
//                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
//                views.setViewVisibility(R.id.no_score, View.VISIBLE);
//                views.setViewVisibility(R.id.widget_layout, View.INVISIBLE);
//            }
            return;
        }
        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }
        // Extract the data from the Cursor
        int scoreID = cursor.getInt(INDEX_MATCH_ID);
        String homeName = cursor.getString(INDEX_HOME_COL);
        String awayName = cursor.getString(INDEX_AWAY_COL);
        int homeCrest = Utility.getTeamCrestByTeamName(homeName);
        int awayCrest = Utility.getTeamCrestByTeamName(awayName);
        int homeScore = cursor.getInt(INDEX_HOME_GOALS_COL);
        int awayScore = cursor.getInt(INDEX_AWAY_GOALS_COL);
        String time = cursor.getString(INDEX_TIME_COL);
        Log.d(LOG_TAG, "cursor.homeName" + homeName);
        Log.d(LOG_TAG, "cursor.awayName" + awayName);
        Log.d(LOG_TAG, "cursor.homeScore" + homeScore);
        Log.d(LOG_TAG, "cursor.awayScore" + awayScore);
        Log.d(LOG_TAG, "cursor.time" + time);
        cursor.close();

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
            views.setViewVisibility(R.id.no_score, View.INVISIBLE);
            views.setViewVisibility(R.id.app_icon, View.INVISIBLE);

            // Add the data to the RemoteViews
            //display team icons
            views.setImageViewResource(R.id.home_crest, homeCrest);
            views.setImageViewResource(R.id.away_crest, awayCrest);
            //display team names
            views.setTextViewText(R.id.home_name,homeName);
            views.setTextViewText(R.id.away_name,awayName);
            //display scores and time
            views.setTextViewText(R.id.score_textview,Utility.getScores(homeScore,awayScore));
            views.setTextViewText(R.id.time_textview,time);

            // Content Descriptions for RemoteViews were only added in ICS MR1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                views.setContentDescription(R.id.home_crest, homeName);
                views.setContentDescription(R.id.away_crest, awayName);
            }

//             Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget_info
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
