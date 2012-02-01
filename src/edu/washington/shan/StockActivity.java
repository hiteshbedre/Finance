package edu.washington.shan;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import edu.washington.shan.stock.StockViewBinder;
import edu.washington.shan.stock.DBAdapter;
import edu.washington.shan.stock.DBConstants;

/**
 * Handles the Stock tab
 * 
 */
public class StockActivity extends ListActivity {
    
    private static final String TAG = "StockActivity";
    private RefreshBroadcastReceiver refreshBroadcastReceiver;
    private DBAdapter mDbAdapter;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stock);
        
        if(null != (mDbAdapter = (DBAdapter) getLastNonConfigurationInstance ())){
            mDbAdapter.open();
        }else{
            // Always use the application context instead of activity context.
            mDbAdapter = new DBAdapter(getApplicationContext());
            mDbAdapter.open();
        }
        fillData();
        
        refreshBroadcastReceiver = new RefreshBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).
        registerReceiver(refreshBroadcastReceiver, 
                new IntentFilter(Consts.REFRESH_STOCK_VIEW));
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        mDbAdapter.close();
        super.onDestroy();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onRetainNonConfigurationInstance()
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return mDbAdapter;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        // Using the id get the symbol from the db
        Cursor cursor = mDbAdapter.fetchItemsByRowId(id);
        startManagingCursor(cursor);
        if (cursor != null) {
            int colIndex = cursor.getColumnIndex(DBConstants.symbol_NAME);
            String symbol = cursor.getString(colIndex);
            if (symbol != null && symbol.length() > 0) {
                // Intent to open Stock detail activity
                Intent i = new Intent(this, StockDetailActivity.class);
                i.putExtra(Consts.STOCK_SYMBOL, symbol);
                startActivity(i);
            }else{
                Log.e(TAG, "symbol retreived from the database is invalid");
            }
        }
    }

    /**
     * Populate the list view from the data in db
     */
    private void fillData() {
        try {
            // Get the rows from the database and create the item list
            Cursor cursor = mDbAdapter.fetchAllItems();
            startManagingCursor(cursor);

            // Create an array to specify the fields we want to display in the
            // list (only TITLE)
            String[] from = new String[] { 
                DBConstants.symbol_NAME,
                DBConstants.last_NAME,
                DBConstants.change_NAME,
                DBConstants.perc_change_NAME,
                DBConstants.company_NAME
                };

            // and an array of the fields we want to bind those fields to
            int[] to = new int[] { 
                R.id.stock_row_text1,
                R.id.stock_row_text2,
                R.id.stock_row_text3,
                R.id.stock_row_text4,
                R.id.stock_row_text_company_name
                };

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                    R.layout.stock_row, cursor, from, to);
            StockViewBinder stockViewBinder = new StockViewBinder(this);
            adapter.setViewBinder(stockViewBinder);
            setListAdapter(adapter);
        } catch (java.lang.IllegalStateException e) {
            Log.e(TAG, "Exception in fillData", e);
        } catch (java.lang.RuntimeException e) {
            Log.e(TAG, "Exception in fillData", e);
        }
    }    
    
    /**
     * Upon receiving a broadcast message,
     * refresh the list view.
     */
    public class RefreshBroadcastReceiver extends BroadcastReceiver 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();
            if(action.equals(Consts.REFRESH_STOCK_VIEW))
            {
                Log.v(TAG, "RefreshBroadcastReceiver for stocks");
                fillData();
            }
        }
    }
}
