package com.github.fakegps.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.github.fakegps.BroadcastEvent;
import com.github.fakegps.DbUtils;
import com.github.fakegps.FakeGpsApp;
import com.github.fakegps.FakeGpsUtils;
import com.github.fakegps.JoyStickManager;
import com.github.fakegps.model.LocBookmark;
import com.github.fakegps.model.LocPoint;
import com.tencent.fakegps.R;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int DELETE_ID = 1001;

    private EditText mHostEditText;
    private EditText mMoveStepEditText;
    private ListView mListView;
    private Button mBtnStart;
    private Button mBtnSetNew;
    private BookmarkAdapter mAdapter;


    SharedPreferences sharedPref;
    public static final String PREFS_KEY="address";
    public static final String PREFS_DEFAULT ="192.168.43.1:2947";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //hostname input
        sharedPref= PreferenceManager.getDefaultSharedPreferences(this);
        mHostEditText = (EditText) findViewById(R.id.inputHostname);


        mHostEditText.setText(sharedPref.getString(PREFS_KEY, PREFS_DEFAULT));


        //each move step delta
        mMoveStepEditText = (EditText) findViewById(R.id.inputStep);
        double currentMoveStep = JoyStickManager.get().getMoveStep();
        mMoveStepEditText.setText(String.valueOf(currentMoveStep));

        mListView = (ListView) findViewById(R.id.list_bookmark);

        mBtnStart = (Button) findViewById(R.id.btn_start);
        mBtnStart.setOnClickListener(this);
        updateBtnStart();

        mBtnSetNew = (Button) findViewById(R.id.btn_set_loc);
        mBtnSetNew.setOnClickListener(this);
        updateBtnSetNew();

        initListView();

        registerBroadcastReceiver();
    }

    @Override
    public void onClick(View view) {
        double step = FakeGpsUtils.getMoveStepFromInput(this, mMoveStepEditText);

        switch (view.getId()) {
            case R.id.btn_start:


                String value = mHostEditText.getText().toString();
                SharedPreferences.Editor editor=sharedPref.edit();
                editor.putString(PREFS_KEY,value);
                editor.apply();


                if (!JoyStickManager.get().isStarted()) {
                    JoyStickManager.get().setMoveStep(step);
                    JoyStickManager.get().start();
                    finish();
                } else {
                    JoyStickManager.get().stop();
                    finish();
                }
                updateBtnStart();
                updateBtnSetNew();
                break;
        }
    }

    private void updateBtnStart() {
        if (JoyStickManager.get().isStarted()) {
            mBtnStart.setText(R.string.btn_stop);
        } else {
            mBtnStart.setText(R.string.btn_start);
        }
    }

    private void updateBtnSetNew() {
        if (JoyStickManager.get().isStarted()) {
            mBtnSetNew.setEnabled(true);
        } else {
            mBtnSetNew.setEnabled(false);
        }
    }

    private void initListView() {
        mAdapter = new BookmarkAdapter(this);
        ArrayList<LocBookmark> allBookmark = DbUtils.getAllBookmark();
        mAdapter.setLocBookmarkList(allBookmark);
        mListView.setAdapter(mAdapter);

        View emptyView = findViewById(R.id.empty_view);
        mListView.setEmptyView(emptyView);

        registerForContextMenu(mListView);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(Menu.NONE, DELETE_ID, Menu.NONE, R.string.menu_delete);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DELETE_ID:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                delete(info.position);
                return true;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void delete(final int position) {
        if (position < 0) return;
        final LocBookmark bookmark = mAdapter.getItem(position);
        new AlertDialog.Builder(this)
                .setTitle("Delete " + bookmark.toString())
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        DbUtils.deleteBookmark(bookmark);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter(BroadcastEvent.BookMark.ACTION_BOOK_MARK_UPDATE);
        LocalBroadcastManager.getInstance(FakeGpsApp.get()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void unregisterBroadcastReceiver() {
        LocalBroadcastManager.getInstance(FakeGpsApp.get()).unregisterReceiver(mBroadcastReceiver);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BroadcastEvent.BookMark.ACTION_BOOK_MARK_UPDATE.equals(action)) {
                ArrayList<LocBookmark> allBookmark = DbUtils.getAllBookmark();
                mAdapter.setLocBookmarkList(allBookmark);
            }
        }
    };


    @Override
    protected void onDestroy() {
        unregisterBroadcastReceiver();
        super.onDestroy();
    }

    public static void startPage(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }


}
