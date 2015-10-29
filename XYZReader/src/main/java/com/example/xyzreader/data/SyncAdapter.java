package com.example.xyzreader.data;

/**
 * Created by daniellujanvillarreal on 10/27/15.
 *  * copied from android training docs
 */

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.Toast;

import com.example.xyzreader.R;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    // Global variables
    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;
    private Context mContext;
    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
        mContext = context;
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority
            , ContentProviderClient provider, SyncResult syncResult) {
        Log.i("SyncAdapter", "starting service to sync thingies");
        getContext().startService(new Intent(getContext(), UpdaterService.class));
    }

    public static boolean requestSyncNow(Context context, Bundle bundle){
        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authorty);
        if(account != null){
            ContentResolver.requestSync(
                    account, authority, settingsBundle);
            Log.i("SyncAdapter", "requestedSync");
            return true;
        }else{
            return false;
        }
    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    public static Account getSyncAccount(Context context) {
        // Create the account type and default account
        String ACCOUNT = context.getString(R.string.account);
        String ACCOUNT_TYPE = context.getString(R.string.account_type);

        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
//        AccountManager accountManager =
//                AccountManager.get(context);

//        /*
//         * Add the account and account type, no password or user data
//         * If successful, return the Account object, otherwise report an error.
//         */
        if (accountManager.addAccountExplicitly(newAccount, "", null)) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            String authority = context.getString(R.string.content_authorty);
            ContentResolver.setIsSyncable(newAccount, authority, 1);

            return newAccount;
        }
        return newAccount;
// else {
////            /*
////             * The account exists or some other error occurred. Log this, report it,
////             * or handle it internally.
////             */
////            return null;
////        }
    }
}
