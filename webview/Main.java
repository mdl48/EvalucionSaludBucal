package app.webview;

import android.content.res.AssetManager;
import android.database.Cursor;
import android.text.*;
import android.view.*;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context; 
import android.util.Log;

import android.widget.Toast;

import android.webkit.*;
import android.database.sqlite.*;
import android.content.ContentValues;

import java.util.*;
import java.io.*;
import java.net.*;
import org.json.*;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Handler {
	SQLiteDatabase db;
	WebView web;
	Activity activity;
	HashMap<String, String> variables;
	public Handler(Activity activity, WebView web, SQLiteDatabase db) {
		this.db = db;
		this.web = web;
		this.activity = activity;
		variables = new HashMap<>();
		updateCount();
	}
	public static String sha1(String input) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		byte[] hashBytes = messageDigest.digest(input.getBytes());
		StringBuilder stringBuilder = new StringBuilder();
		for (byte b : hashBytes)
			stringBuilder.append(String.format("%02x", b));
		return stringBuilder.toString();
	}
	public void updateCount() {
		try {
			Cursor cursor = db.rawQuery("select count(*) from formularios where exportado = 0", new String[] {});
			cursor.moveToNext();
			variables.put("count", cursor.getString(0));
		} catch(Exception e) {
			Log.e("webview", "[UpdateCount] " + e.toString() + "\n" + Log.getStackTraceString(e));
		}
	}
	@JavascriptInterface
	public void uploadForms() {
		Url url = new Url("http://localhost:10000/");
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		http.setRequestMethod("POST");
		http.setRequestProperty("Content-Type", "application/json");
        OutputStream out = http.getOutputStream();
	}
	@JavascriptInterface
	public void log(String msg) {
		Log.d("webview", msg);
	}
	@JavascriptInterface
	public String getvar(String key) {
		Log.i("webview", "[GETVAR] " + key + " = " + variables.get(key));
		return variables.get(key);
	}
	@JavascriptInterface
	public void export() {
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
		Log.i("webview", "[POST] Got method: " + method);
		Log.i("webview", "[POST] Got form: " + form.toString());
		try {
			if (method.equals("form")) {
				form.put("examinador", variables.get("name"));
				form.put("examinador_cedula", variables.get("cedula"));
				ContentValues vals = new ContentValues();
				for (Map.Entry<String, String> entry : form.entrySet())
					vals.put(entry.getKey(), entry.getValue());
				db.insert("formularios", null, vals);
				updateCount();
				activity.runOnUiThread(() -> Toast.makeText(activity, "El formulario fue enviado exitosamente", Toast.LENGTH_LONG).show());
				activity.runOnUiThread(() -> web.loadUrl("file:///android_asset/welcome.html"));
			}
			else if(method.equals("login")) {
				Log.i("webview", "[POST] Login start");
				String password = sha1(form.get("password"));
				if (password == null) {
					String msg = "No se encontro el algoritmo de hash SHA-1";
					Log.e("webview", msg);
					activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_LONG).show());
					return;
				}
				Log.i("webview", "[POST] Password hash: " + password);
				Cursor cursor = db.rawQuery("select nombre from usuarios where cedula = ? and password = ?", new String[] {form.get("cedula"), password});
				if (cursor.getCount() != 0) {
					cursor.moveToNext();
					variables.put("name", cursor.getString(0));
					variables.put("cedula", form.get("cedula"));
					activity.runOnUiThread(() -> web.loadUrl("file:///android_asset/welcome.html"));
				}
				else
					activity.runOnUiThread(() -> Toast.makeText(activity, "La cedula o claves son incorrectas.", Toast.LENGTH_LONG).show());
			}
		} catch (Exception e) {
			Log.e("webview", "[POST] " + e.toString() + "\n" + Log.getStackTraceString(e));
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
				Log.e("webview", "Fatal. Could not copy to " + dbfile.getPath());
			}
		}
		Log.i("webview", "Opening Database on " + dbfile.getPath());
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
		web.addJavascriptInterface(new Handler(this, web, db), "Android");
		web.setWebChromeClient(new WebChromeClient());
		web.loadUrl("file:///android_asset/login.html");
	}
	@Override
	protected void onStop() {
		super.onStop();
		// if(db != null)
		// 	db.close();
	}
}

