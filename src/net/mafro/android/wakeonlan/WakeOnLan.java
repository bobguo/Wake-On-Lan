package net.mafro.android.wakeonlan;

import android.app.TabActivity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.content.Context;
import android.content.UriMatcher;
import android.content.ContentValues;
import android.content.Intent;
import android.content.DialogInterface;

import android.database.Cursor;

import android.util.Log;

import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.EditText;
import android.widget.Button;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;

import android.net.Uri;

import android.provider.BaseColumns;


public class WakeOnLan extends TabActivity implements OnClickListener
{

	private static final String TAG = "WakeOnLan";
	
    public static final int MENU_ITEM_WAKE = Menu.FIRST;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;
	
	private Cursor cursor;	//main history cursor

    private static final String[] PROJECTION = new String[]
	{
		History.Items._ID,
		History.Items.TITLE,
		History.Items.MAC,
		History.Items.IP,
		History.Items.PORT
    };
	
	private static Toast notification;
	

	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		//configure tabs
		TabHost th = getTabHost();

		th.addTab(th.newTabSpec("tab_history").setIndicator(getString(R.string.tab_history_en), getResources().getDrawable(R.drawable.ical)).setContent(R.id.historyview));
		th.addTab(th.newTabSpec("tab_wake").setIndicator(getString(R.string.tab_wake_en), getResources().getDrawable(R.drawable.wake)).setContent(R.id.wakeview));
		
		th.setCurrentTab(0);
		
		//add listener for wake button
		Button sendWake = (Button)findViewById(R.id.send_wake);
		sendWake.setOnClickListener(this);
		
		//set defaults on Wake tab
		EditText vip = (EditText)findViewById(R.id.ip);
		EditText vport = (EditText)findViewById(R.id.port);
		vip.setText(MagicPacket.BROADCAST);
		vport.setText(Integer.toString(MagicPacket.PORT));
		
		//load History list
		cursor = getContentResolver().query(History.Items.CONTENT_URI, PROJECTION, null, null, null);

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.history_row, cursor, new String[] { History.Items.TITLE, History.Items.MAC, History.Items.IP, History.Items.PORT }, new int[] { R.id.history_row_title, R.id.history_row_mac, R.id.history_row_ip, R.id.history_row_port });

		ListView lvHistory = (ListView)findViewById(R.id.history);
		lvHistory.setAdapter(adapter);
		
		//set self as context menu listener
		registerForContextMenu(lvHistory);

		//check for updates
		Updater.checkForUpdates(this, handler);
    }

	public void onClick(View v)
	{
		if(v.getId() == R.id.send_wake) {
			EditText vtitle = (EditText)findViewById(R.id.title);
			EditText vmac = (EditText)findViewById(R.id.mac);
			EditText vip = (EditText)findViewById(R.id.ip);
			EditText vport = (EditText)findViewById(R.id.port);
			
			String title = vtitle.getText().toString();
			String mac = vmac.getText().toString();
			String ip = vip.getText().toString();
			int port = Integer.valueOf(vport.getText().toString());
			
			String formattedMac = sendPacket(mac, ip, port);
			if(formattedMac != null) {
				//on succesful send, add to history list
				addToHistory(title, formattedMac, ip, port);
			}
		}
	}
	
	private String sendPacket(String mac, String ip, int port)
	{
		Log.i(TAG, mac+" "+ip+":"+Integer.toString(port));
		String formattedMac = null;
		
		try {
			formattedMac = MagicPacket.send(mac, ip, port);
			
		}catch(IllegalArgumentException iae) {
			Log.e(TAG, "Sending Failed", iae);
			notifyUser("Sending Failed:\n"+iae.getMessage(), WakeOnLan.this);
			return null;
			
		}catch(Exception e) {
			Log.e(TAG, "Sending Failed", e);
			notifyUser("Sending Failed", WakeOnLan.this);
			return null;
		}
		
		notifyUser("Magic Packet sent", WakeOnLan.this);
		return formattedMac;
	}
	
	private void addToHistory(String title, String mac, String ip, int port)
	{
		boolean exists = false;
		
		//check mac doesnt already exist
		if(cursor.moveToFirst()) {
			int macColumn = cursor.getColumnIndex(History.Items.MAC);

			do {
				if(mac.equals(cursor.getString(macColumn))) {
					exists = true;
					break;
				}
			} while (cursor.moveToNext());
		}
		
		//create if not exists
		if(exists == false) {
			ContentValues values = new ContentValues(4);
			values.put(History.Items.TITLE, title);
			values.put(History.Items.MAC, mac);
			values.put(History.Items.IP, ip);
			values.put(History.Items.PORT, port);
			Uri uri = getContentResolver().insert(History.Items.CONTENT_URI, values);
		}
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.history_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		//extract data about clicked item
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		//move bound cursor to item that was clicked
		cursor.moveToPosition(info.position);
		
		switch (item.getItemId()) {
		case R.id.menu_wake:
			//HACK hardcoded column indexes
			sendPacket(cursor.getString(2), cursor.getString(3), cursor.getInt(4));
			return true;
			
		case R.id.menu_edit:
			//fire this record into edit mode in the next tab
			EditText vtitle = (EditText)findViewById(R.id.title);
			EditText vmac = (EditText)findViewById(R.id.mac);
			EditText vip = (EditText)findViewById(R.id.ip);
			EditText vport = (EditText)findViewById(R.id.port);
			
			vtitle.setText(cursor.getString(1));
			vmac.setText(cursor.getString(2));
			vip.setText(cursor.getString(3));
			vport.setText(cursor.getString(4));
			
			TabHost th = getTabHost();
			th.setCurrentTab(1);
			return true;
			
		case R.id.menu_delete:
			//use HistoryProvider to remove this row
			Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(cursor.getInt(0)));
			getContentResolver().delete(itemUri, null, null);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public static void notifyUser(String message, Context context) {
		if (notification != null) {
			notification.setText(message);
			notification.show();
		} else {
			notification = Toast.makeText(context, message, Toast.LENGTH_SHORT);
			notification.show();
		}
	}


	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//prompt user for action
			new AlertDialog.Builder(WakeOnLan.this)
				.setTitle("Update Available")
				.setMessage("Do you want to install the latest version?")
				.setIcon(R.drawable.icon)
				.setPositiveButton(R.string.yes_en, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						//if version numbers don't match then open Market application
						Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:"+getPackageName()));
						startActivity(market);
					}

			}).setNegativeButton(R.string.no_en, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {}
			}).show();
		}
	};

}