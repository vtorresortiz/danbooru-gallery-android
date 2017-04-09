////////////////////////////////////////////////////////////////////////////////
// Danbooru Gallery Android - an danbooru-style imageboard browser
//     Copyright (C) 2014  Victor Tseng
//
//     This program is free software: you can redistribute it and/or modify
//     it under the terms of the GNU General Public License as published by
//     the Free Software Foundation, either version 3 of the License, or
//     (at your option) any later version.
//
//     This program is distributed in the hope that it will be useful,
//     but WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//     GNU General Public License for more details.
//
//     You should have received a copy of the GNU General Public License
//     along with this program. If not, see <http://www.gnu.org/licenses/>
////////////////////////////////////////////////////////////////////////////////

package tw.idv.palatis.danboorugallery;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.File;

import tw.idv.palatis.danboorugallery.database.DanbooruGalleryDatabase;
import tw.idv.palatis.danboorugallery.database.PostTagsLinkTable;
import tw.idv.palatis.danboorugallery.database.PostsTable;
import tw.idv.palatis.danboorugallery.database.TagsTable;
import tw.idv.palatis.danboorugallery.picasso.Picasso;
import tw.idv.palatis.danboorugallery.siteapi.SiteAPI;
import tw.idv.palatis.danboorugallery.util.SiteSession;

import static tw.idv.palatis.danboorugallery.BuildConfig.DEBUG;

public class DanbooruGalleryApplication
    extends Application
    implements Thread.UncaughtExceptionHandler
{
    private static final String TAG = "DanbooruGalleryApplication";

    // FIXME: shouldn't there be a way to determine this during runtime?
    public static final int MAXIMUM_TEXTURE_SIZE = 2048;

    @Override
    public void onCreate()
    {
        super.onCreate();
        DanbooruGallerySettings.init(this);
        DanbooruGalleryDatabase.init(this);
        Picasso.init(this);
        NetworkChangeReceiver.init(this);
        SiteAPI.init(this);
        SiteSession.init(this);

        if (!DEBUG)
            Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        Picasso.onLowMemory();
        SQLiteDatabase.releaseMemory();
    }

    @Override
    public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        Picasso.onTrimMemory(level);
        SQLiteDatabase.releaseMemory();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable)
    {

            System.exit(-1);

    }

    public static interface OnCacheClearedCallback
    {
        public void onCacheCleared();
    }

    public static void clearCacheWithThread(final Activity activity, final OnCacheClearedCallback callback)
    {
        new Thread()
        {
            private boolean recursiveDelete(File file)
            {
                if (file.isFile())
                {
                    if (file.delete())
                        return true;
                    return false;
                }

                boolean result = true;
                File[] files = file.listFiles();
                if (files != null)
                    for (File subfile : files)
                        if (subfile != null)
                            result &= recursiveDelete(subfile);
                return result;
            }

            @Override
            public void run()
            {
                recursiveDelete(Picasso.getCacheDir());
                Picasso.onLowMemory();
                PostsTable.deleteAllPosts();
                TagsTable.deleteAllTags();
                PostTagsLinkTable.deleteAllPostTagsLink();

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        callback.onCacheCleared();
                    }
                });
            }
        }.start();
    }
}
