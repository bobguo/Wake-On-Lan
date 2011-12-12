/*
Copyright (C) 2008-2011 Matt Black.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
* Neither the name of the author nor the names of its contributors may be used
  to endorse or promote products derived from this software without specific
  prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package net.mafro.android.wakeonlan;

import android.app.Activity;

import android.os.Bundle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Intent.ShortcutIconResource;

import android.util.Log;

import android.widget.ListView;

import android.net.Uri;


/**
 *	@desc	Activity to display a list of history for creating homescreen shortcuts
 */
public class ShortcutActivity extends Activity
{

	public static final String TAG = "Shortcut";

	private HistoryListHandler histHandler;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// display only the history list
		setContentView(R.layout.history_list);

		//load our sort mode
		SharedPreferences settings = getSharedPreferences(TAG, 0);
		int sort_mode = settings.getInt("sort_mode", 0);	//default to CREATED

		// grab the history ListView
		ListView lv = (ListView)findViewById(R.id.history);

		//load history handler (deals with cursor and history ListView)
		histHandler = new HistoryListHandler(this, lv);
		histHandler.bind(sort_mode);
	}


	public void createShortcut(HistoryItem histItem)
	{
		// create a shortcut on the home screen
		ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(ShortcutActivity.this, R.drawable.icon);

		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(histItem.id));

		Log.d(TAG,histItem.title);

		/*Intent shortcutIntent = new Intent(this, WakeOnLan.class);
		shortcutIntent.setClassName("net.mafro.android.wakeonlan", "WakeOnLan");
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);*/

		/*Intent contents = new Intent("net.mafro.android.wakeonlan.WAKE", itemUri);
		contents.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);*/

		/*Intent contents = new Intent(this, WakeOnLan.class);
		contents.putExtra("net.mafro.android.wakeonlan.id", histItem.id);*/

		Intent contents = new Intent(Intent.ACTION_VIEW, itemUri);
		contents.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, contents);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, histItem.title);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

		setResult(RESULT_OK, intent);
		finish();
	}

}
