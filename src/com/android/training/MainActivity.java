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
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

//	For additional reference. Please refer here
//	http://developer.android.com/guide/topics/connectivity/nfc/nfc.html
public class MainActivity extends Activity {

	private final String SPLITTER_STRING = "###";

	private final CharSequence STRING_EMPTY = "";
	
	private TextView _txtHint = null;
	private EditText _etFirstName = null;
	private EditText _etLastName = null;
	private NfcAdapter _nfcAdapter = null;
	
	private  enum Mode{
		READ,
		WRITE,
		NONE
	};
	
	private Mode _mode = Mode.NONE;
	
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
		
		_txtHint = (TextView) findViewById(R.id.txtHint);
		_etFirstName = (EditText) findViewById(R.id.etFirstName);
		_etLastName = (EditText) findViewById(R.id.etLastName);
		
		_txtHint.setText(R.string.read_write);
		clear();
		setInputControlEnabled(false);
		
		Intent intent = getIntent();
		if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()))
		{
			if(!readNFCTag(intent))
			{
				Toast.makeText(this, "Unable to Read Tag", Toast.LENGTH_LONG).show();
			}
			_txtHint.setText(R.string.read_tag);
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
//		setForegroundDisptachWithIntentFilters();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	
		switch (item.getItemId()) {
			case R.id.item_clear:
				_mode = Mode.NONE;
				_txtHint.setText(R.string.read_write);
				Log.i("MainActivity", "item_clear");
				clear();
				break;
		
			case R.id.item_read:
				_mode = Mode.READ;
				_txtHint.setText(R.string.read_tag);
				clear();
				setInputControlEnabled(false);
				Log.i("MainActivity", "item_read");
				
				break;
			case R.id.item_write:
				_mode = Mode.WRITE;
				_txtHint.setText(R.string.write_tag);
				setInputControlEnabled(true);
				Log.i("MainActivity", "item_write");
				break;
		}		
		
		return false;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.i("MainActivity", "onNewIntent");
	
		switch (_mode) {
			case READ:
				boolean isNDEF_TECH_TAG = false;
				String action = intent.getAction();
				
				//Based on the documentation it is arranged by Priority, 
				//NDEF, TECH, TAG
				if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
					Log.i("MainActivity","ACTION_NDEF_DISCOVERED");
					isNDEF_TECH_TAG = true;
				}
				else if(NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))
				{
					Log.i("MainActivity","ACTION_TECH_DISCOVERED");
					isNDEF_TECH_TAG = true;
				}
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
		_nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (_nfcAdapter == null) {
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
		
		_nfcAdapter.enableForegroundDispatch(this,
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
		_nfcAdapter.enableForegroundDispatch(this,
		pendingIntent, intentFilters, null);
	}
	
	private void stopForegroundDispatch()
	{
		_nfcAdapter.disableForegroundDispatch(this);
	}
	
	private void clear()
	{
		_etFirstName.setText(STRING_EMPTY);
		_etLastName.setText(STRING_EMPTY);
	}
	
	private void setInputControlEnabled(boolean enabled)
	{
		_etFirstName.setEnabled(enabled);
		_etLastName.setEnabled(enabled);
	}
	
	private boolean writeNFCTag(Intent intent)
	{
		Log.i("MainActivity","writeNFCTag");
		
		Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		Ndef ndef = Ndef.get(detectedTag);
		
		if (ndef != null) {
			
			String fullName = String.format("%s%s%s", _etFirstName.getText(),
					SPLITTER_STRING
					,_etLastName.getText());
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
			//Decided to use a AsyncTask
//			ArrayList<NdefMessage> ndefMessages = new ArrayList<>();
//			Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
//			if (rawMessages!= null) {
//				for (Parcelable rawMessage : rawMessages) {
//					ndefMessages.add((NdefMessage)rawMessage);
//				}
//			}			
//
//			ArrayList<String> messages = new ArrayList<String>();
//			if (ndefMessages.size() > 0) {
//				for (NdefMessage ndefMessage : ndefMessages) {
//					for (NdefRecord ndefRecord : ndefMessage.getRecords()) {
//						messages.add(new String(ndefRecord.getPayload()));
//					}
//				}
//			}
//			
//			Toast.makeText(this, messages.get(0), Toast.LENGTH_LONG).show();
			
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
						_etFirstName.setText(stringMessages[0]);
						_etLastName.setText(stringMessages[1]);
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
