package com.android.training;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private final String SPLITTER_STRING = "###";
	private final CharSequence STRING_EMPTY = "";
	
	private enum Mode{
		READ,
		WRITE,
		NONE
	};
	
	private TextView mlblHint = null;
	private EditText metFirstName = null;
	private EditText metLastName = null;
	
	private NfcAdapter mNFCAdapter = null;
		
	private Mode mMode = Mode.NONE;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		
		if (nfcInitialization() == false) {
			Toast.makeText(this, 
					"This device does not support NFC!",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		mlblHint = (TextView) findViewById(R.id.txtHint);
		metFirstName = (EditText) findViewById(R.id.etFirstName);
		metLastName = (EditText) findViewById(R.id.etLastName);
		
		mlblHint.setText(R.string.read_write);
		clear();
		setInputControlEnabled(false);
		
		Intent intent = getIntent();
		if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()))
		{
			mlblHint.setText(R.string.read_tag);
			if(!readNFCTag(intent))
			{
				Toast.makeText(this, "Unable to Read Tag", Toast.LENGTH_LONG).show();
			}
		}
		
		Log.i("MainActivity", "onCreate");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options, menu);
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Log.i("MainActivity", "onResume");
		setForegroundDisptach( );	
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	
		switch (item.getItemId()) {
			case R.id.item_clear:
				mMode = Mode.NONE;
				mlblHint.setText(R.string.read_write);
				clear();
				Log.i("MainActivity", "item_clear");
				break;
		
			case R.id.item_read:
				mMode = Mode.READ;
				mlblHint.setText(R.string.read_tag);
				clear();
				setInputControlEnabled(false);
				Log.i("MainActivity", "item_read");
				
				break;
			case R.id.item_write:
				mMode = Mode.WRITE;
				mlblHint.setText(R.string.write_tag);
				setInputControlEnabled(true);
				Log.i("MainActivity", "item_write");
				break;
		}		
		
		return false;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.i("MainActivity", "onNewIntent");
	
		switch (mMode) {
			case READ:
				boolean isNDEF_TECH_TAG = false;
				String action = intent.getAction();
				
				//Based on the Google documentation, arrangement is based on the intents priority. 
				//NDEF, TECH, TAG
				if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
					Log.i("MainActivity","ACTION_NDEF_DISCOVERED");
					isNDEF_TECH_TAG = true;
				}
				//Unused as of the moment since always write in NDEF format
				else if(NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))
				{
					Log.i("MainActivity","ACTION_TECH_DISCOVERED");
					isNDEF_TECH_TAG = true;
				}
				//Unused as of the moment since always write in NDEF format
				else if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action))
				{
					Log.i("MainActivity","ACTION_TAG_DISCOVERED");
					isNDEF_TECH_TAG = true;
				}
				
				if (isNDEF_TECH_TAG) {
					if(!readNFCTag(intent))
					{
						Toast.makeText(this, "Unable to Read Tag", Toast.LENGTH_LONG).show();
					}
				}
				
				break;

			case WRITE:
				if (writeNFCTag(intent)) {
					Toast.makeText(this, "Successfully Saved Message!", Toast.LENGTH_LONG).show();
				} 
				else
				{						
					Toast.makeText(this, "Unable to Write Tag", Toast.LENGTH_LONG).show();
				}
				break;
				
			case NONE:
				//Do Nothing
				break;
			//No default for this switch, using enum types and the initial value is NONE. 
		}
	}
	
	@Override
	protected void onPause() {
		Log.i("MainActivity", "onPause");
		stopForegroundDispatch();
		super.onPause();
	}
	
	//Step 1 in using NFC
	private boolean nfcInitialization()
	{
		mNFCAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNFCAdapter == null) {
			return false;
		}
		return true;
	}
	
	//Step 2 in using NFC
	private void setForegroundDisptach()
	{
		Intent intent = new Intent(this, this.getClass());
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 
				0,
				intent, 
				0);
		
		mNFCAdapter.enableForegroundDispatch(this,
				pendingIntent, null, null);
	}
	
	private void setForegroundDisptachWithIntentFilters()
	{
		Intent intent = new Intent(this, this.getClass());
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 
				0,
				intent, 
				0);
		
		//Notice that the filter is same as in the manifest.
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
		intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
		try {
			intentFilter.addDataType("text/plain");
		} catch (MalformedMimeTypeException e) {
			e.printStackTrace();
		}
		
		IntentFilter[] intentFilters = new IntentFilter[]{intentFilter};
		mNFCAdapter.enableForegroundDispatch(this,
		pendingIntent, intentFilters, null);
	}
	
	private void stopForegroundDispatch()
	{
		mNFCAdapter.disableForegroundDispatch(this);
	}
	
	private void clear()
	{
		metFirstName.setText(STRING_EMPTY);
		metLastName.setText(STRING_EMPTY);
	}
	
	private void setInputControlEnabled(boolean enabled)
	{
		metFirstName.setEnabled(enabled);
		metLastName.setEnabled(enabled);
	}
	
	private boolean writeNFCTag(Intent intent)
	{
		Log.i("MainActivity","writeNFCTag");
		
		Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		Ndef ndef = Ndef.get(detectedTag);
		
		if (ndef != null) {
			
			String fullName = String.format("%s%s%s", metFirstName.getText(),
					SPLITTER_STRING
					,metLastName.getText());
			NdefRecord textRecord = createMimeMedia(fullName);
			NdefMessage textMessage = new NdefMessage(new NdefRecord[]{textRecord});
			
			try {
				ndef.connect();
				
				//Check if NFC is Writable
				if (!ndef.isWritable()) {
					Log.i("MainActivity","Not Writable");
					return false;
				}
				else
				{
					Log.i("MainActivity","Writable");
				}
				
				//Check if the NFC size will accomodate the message
				if ( textMessage.toByteArray().length > ndef.getMaxSize() ) {
					Log.i("MainActivity","Input data is too long.");
					return false;
				}
				
				if (!ndef.isConnected()) {
					Log.i("MainActivity","Not Connected");
					return false;
				}
				else {
					Log.i("MainActivity","Connected");
				}
				
				try {
					ndef.writeNdefMessage(textMessage);
					Log.i("MainActivity", new String(textMessage.toByteArray()));
				} catch (FormatException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		else
		{
			return false;
		}

		return true;
	}
	
	private boolean readNFCTag(Intent intent)
	{
		Log.i("MainActivity","readNFCTag");
		
		Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		Ndef ndef = Ndef.get(detectedTag);
		
		if (ndef!= null) {			
			
			if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
				
				ArrayList<String> messages = null;
				try {
					messages = new ReadNFCTask().execute(ndef).get();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return false;
				} catch (ExecutionException e) {
					e.printStackTrace();
					return false;
				}
				
				if (messages != null) {
					String message = messages.get(0);
					Log.i("MainActivity", message);
					
					try {						
						String[] stringMessages = message.split(SPLITTER_STRING);
						metFirstName.setText(stringMessages[0]);
						metLastName.setText(stringMessages[1]);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return true;
	}

	private NdefRecord createMimeMedia(String message) {
		return new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
				"application/com.android.training.beam".getBytes(Charset.forName("US-ASCII")),
				new byte[0],
				message.getBytes(Charset.forName("US-ASCII")));
	}
	
	private class ReadNFCTask extends AsyncTask<Ndef, Void, ArrayList<String>>
	{
		@Override
		protected void onPreExecute() {
			Log.i("ReadNFCTask", "onPreExecute");
			super.onPreExecute();
		}
		
		@Override
		protected ArrayList<String> doInBackground(Ndef... ndefs) {
			ArrayList<String> messages = new ArrayList<String>();
			
			for (Ndef ndef : ndefs) {
				for (NdefRecord ndefRecord : ndef.getCachedNdefMessage().getRecords()) {
					messages.add(new String(ndefRecord.getPayload()));
				}
			}
			
			return messages;
		}
	}
}

/* 
 * Jerome Dulay Bautista
 * 
 * Notes:
 *  1. No source code documentation added yet.
 *  2. Used Samsung Tectiles
 * 
 * References:
 *  - http://developer.android.com/guide/topics/connectivity/nfc/nfc.html
 *  - http://www.samsung.com/us/microsite/tectile/
 *  
*/
