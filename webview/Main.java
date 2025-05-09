package app.webview;

import android.content.res.AssetManager;
import android.database.Cursor;
import android.text.*;
import android.view.*;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context; 
import android.util.Log;

import android.webkit.*;
import android.database.sqlite.*;
import android.content.ContentValues;

import java.util.*;
import java.io.*;
import org.json.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Handler {
	SQLiteDatabase db;
	public Handler(SQLiteDatabase db) {
		this.db = db;
	}
	public static String sha1(String input) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
			byte[] hashBytes = messageDigest.digest(input.getBytes());
			StringBuilder stringBuilder = new StringBuilder();
			for (byte b : hashBytes)
				stringBuilder.append(String.format("%02x", b));
			return stringBuilder.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@JavascriptInterface
	public void post(String method, String json) {
		HashMap<String, String> form = null;
		try {
			JSONObject form_json = new JSONObject(json);
			Iterator<String> keys = form_json.keys();
			form = new HashMap<>();
			while(keys.hasNext()) {
				String key = keys.next();
				form.put(key, (String)form_json.get(key));
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
		
		if (method == "form") {
			ContentValues vals = new ContentValues();
			for (Map.Entry<String, String> entry : form.entrySet())
				vals.put(entry.getKey(), entry.getValue());
			db.insert("formularios", null, vals);
		}
		else if(method == "login") {
			Cursor cursor = this.db.rawQuery("select name from usuarios where name = ? and password = ?", new String[] {form.get("name"), sha1(form.get("password"))});
			if (cursor.getCount() != 0) {
				// Load form page
			}
			else {
				// Show error
			}
		}
	}
}
public class Main extends Activity {
	SQLiteDatabase db = null;
	public static void Copy(InputStream in, OutputStream out) throws IOException
	{
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Copy database file from assets into internal storage to open it if it doesn't exist
		File dbfile = new File(getFilesDir() + "/file.db");
		if (!dbfile.exists()) {
			AssetManager assets = this.getAssets();
			try {
				InputStream dbasset = assets.open("file.db");
				OutputStream dbfile_stream = new FileOutputStream(dbfile);
				Copy(dbasset, dbfile_stream);
			} catch (IOException io) {
				// Ignore
			}
		}
		db = SQLiteDatabase.openDatabase(dbfile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
		// if (internet 1s) {
		// 	db.delete("usuarios", ); // TODO:
		// 	http
		// 	db.insert();
		// }
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		
		WebView web = findViewById(R.id.webview);
		WebSettings cfg = web.getSettings();
		cfg.setJavaScriptEnabled(true);
		cfg.setDomStorageEnabled(true);
		web.addJavascriptInterface(new Handler(db), "Android");
		web.setWebChromeClient(new WebChromeClient());
		web.loadUrl("file:///android_asset/login.html");
	}
	@Override
	protected void onStop() {
		super.onStop();
		if(db != null)
			db.close();
	}
}

