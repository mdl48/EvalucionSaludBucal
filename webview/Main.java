package app.facoluz_evaluacion_bucal;

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

class Global {
	public static String apiUrl = "http://10.3.24.33:8080";
}

class Handler {
	SQLiteDatabase db;
	WebView web;
	Activity activity;
	HashMap<String, String> variables;
	public HashMap<String, String> cursorMap(Cursor cursor) {
		if(!cursor.moveToNext())
			return null;
		HashMap<String, String> map = new HashMap<>();
		for (int i = 0; i < cursor.getColumnCount(); i++)
			map.put(cursor.getColumnName(i), cursor.getString(i));
		return map;
	}
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
			Log.e("facoluz_evaluacion_bucal", "[UpdateCount] " + e.toString() + "\n" + Log.getStackTraceString(e));
		}
	}
	@JavascriptInterface
	public void export() {
		try {
			URL url = new URL("http://" + getvar("ip") + ":8080/recieverData");
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			http.setRequestMethod("POST");
			http.setRequestProperty("Content-Type", "application/json");
			http.setDoOutput(true);
			Cursor c = db.rawQuery("select * from formularios where exportado = 0", new String[] {});
			HashMap<String, String> row;
			JSONArray list = new JSONArray();
			while((row = cursorMap(c)) != null) {
				JSONObject object = new JSONObject();
				for (Map.Entry<String, String> entry : row.entrySet()) {
					object.put(entry.getKey(), entry.getValue());
					object.put("password", getvar("password"));
				}
				list.put(object);
			}
			byte[] bytes = list.toString().getBytes("utf-8");
			http.setRequestProperty("Content-Length", String.valueOf(bytes.length));
			
			OutputStream out = http.getOutputStream();
			out.write(bytes);
			out.flush();
			out.close();
			int ret = http.getResponseCode();
			Log.i("facoluz_evaluacion_bucal", "[export] HTTP Response: " + ret);
			if (ret != HttpURLConnection.HTTP_OK) {
				activity.runOnUiThread(() -> Toast.makeText(activity, "Error en el servidor al subir los datos: " + ret, Toast.LENGTH_LONG).show());
				return;
			}
		} catch(Exception e) {
			Log.e("facoluz_evaluacion_bucal", "[export] " + e.toString() + "\n" + Log.getStackTraceString(e));
			activity.runOnUiThread(() -> Toast.makeText(activity, "No se pudieron exportar los datos: " + e.toString(), Toast.LENGTH_LONG).show());
			return;
		}
		activity.runOnUiThread(() -> Toast.makeText(activity, "Los datos fueron exportados exitosamente", Toast.LENGTH_LONG).show());
		db.execSQL("delete from formularios");
		updateCount();
	}
	@JavascriptInterface
	public void log(String msg) {
		Log.d("facoluz_evaluacion_bucal", msg);
	}
	@JavascriptInterface
	public String getvar(String key) {
		Log.i("facoluz_evaluacion_bucal", "[GETVAR] " + key + " = " + variables.get(key));
		return variables.get(key);
	}
	@JavascriptInterface
	public String setvar(String key, String val) {
		Log.i("facoluz_evaluacion_bucal", "[SETVAR] " + key + " = " + val);
		return variables.put(key, val);
	}
	public boolean mutate(String table, HashMap<String, String> form, String action) {
		ContentValues vals = new ContentValues();
		for (Map.Entry<String, String> entry : form.entrySet())
			vals.put(entry.getKey(), entry.getValue());
		Log.i("facoluz_evaluacion_bucal", "[DB] " + table + ":" + action + ": " + vals.toString());
		try {
			db.beginTransaction();
			if (action.equals("insert"))
				db.insert(table, null, vals);
			else if(action.equals("update"))
				db.update(table, vals, null, null);
			db.setTransactionSuccessful();
		}
		catch(Exception e) {
			activity.runOnUiThread(() -> Toast.makeText(activity, "Hubo un error al guardar los datos", Toast.LENGTH_LONG).show());
			db.endTransaction();
			return false;
		}
		db.endTransaction();
		return true;
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
		Log.i("facoluz_evaluacion_bucal", "[POST] Got method: " + method);
		Log.i("facoluz_evaluacion_bucal", "[POST] Got form: " + form.toString());
		try {
			if (method.equals("form")) {
				form.put("examinador", variables.get("name"));
				form.put("examinador_cedula", variables.get("cedula"));
				if(mutate("formularios", form, "insert")) {
					updateCount();
					activity.runOnUiThread(() -> Toast.makeText(activity, "El formulario fue guardado exitosamente", Toast.LENGTH_LONG).show());
					activity.runOnUiThread(() -> web.loadUrl("file:///android_asset/welcome.html"));
				}
			}
			else if(method.equals("login")) {
				Log.i("facoluz_evaluacion_bucal", "[POST] Login start");
				String password = sha1(form.get("password"));
				if (password == null) {
					String msg = "No se encontro el algoritmo de hash SHA-1";
					Log.e("facoluz_evaluacion_bucal", msg);
					activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_LONG).show());
					return;
				}
				Log.i("facoluz_evaluacion_bucal", "[POST] Password hash: " + password);
				Cursor cursor = db.rawQuery("select nombre from usuarios where cedula = ? and password = ?", new String[] {form.get("cedula"), password});
				if (cursor.getCount() != 0) {
					cursor.moveToNext();
					variables.put("name", cursor.getString(0));
					variables.put("cedula", form.get("cedula"));
					variables.put("password", password);
					activity.runOnUiThread(() -> web.loadUrl("file:///android_asset/welcome.html"));
				}
				else
					activity.runOnUiThread(() -> Toast.makeText(activity, "La cedula o claves son incorrectas.", Toast.LENGTH_LONG).show());
			}
			else if(method.equals("config")) {
				mutate("configuracion", form, "update");
				setvar("ip", form.get("ip"));
				activity.runOnUiThread(() -> Toast.makeText(activity, "Se ha ingresado exitosamente la configuración.", Toast.LENGTH_LONG).show());
				Main.updateUsers(getvar("ip"), activity, db);
				activity.runOnUiThread(() -> web.loadUrl("file:///android_asset/login.html"));
			}
		} catch (Exception e) {
			Log.e("facoluz_evaluacion_bucal", "[POST] " + e.toString() + "\n" + Log.getStackTraceString(e));
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
	public static void updateUsers(String ip, Activity activity, SQLiteDatabase db) {
		try {
			URL url = new URL("http://" + ip + ":8080/syncUsers");
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			http.setConnectTimeout(1000);
			InputStream in = http.getInputStream();
			BufferedReader read = new BufferedReader(new InputStreamReader(in, "utf-8"));
			String line = read.readLine();
			StringBuilder buf = new StringBuilder();
			while(line != null) {
				buf.append(line).append("\n");
				line = read.readLine();
			}
			read.close();
			
			JSONObject body = new JSONObject(buf.toString());
			JSONArray result = body.getJSONArray("dataUsers");
			db.beginTransaction();
			try {
				db.execSQL("delete from usuarios");
				for (int i = 0; i < result.length(); i++) {
					JSONObject user = result.getJSONObject(i);
					String sql = "insert into usuarios(cedula, nombre, password) values(?, ?, ?)";
					Log.i("facoluz_evaluacion_bucal", "[syncUsers] Exec SQL: '" + sql + "'");
					Log.i("facoluz_evaluacion_bucal", "[syncUsers] Arguments: '" + user.toString() + "'");
					db.execSQL(sql, new Object[] {
						       new Integer(user.getInt("cedula")),
						       user.getString("nombre"),
						       user.getString("password")
					});
				}
				db.setTransactionSuccessful();
			}
			catch(Exception e) {
				activity.runOnUiThread(() -> Toast.makeText(activity, "Error en la base de datos al sincronizar usuarios.", Toast.LENGTH_LONG).show());
			}
			db.endTransaction();
		}
		catch(SocketTimeoutException e) {
			Log.i("facoluz_evaluacion_bucal", "[syncUsers] Timeout");
		}
		catch(ConnectException e) {
			Log.i("facoluz_evaluacion_bucal", "[syncUsers] Can't Connect");
		}
		catch(Exception e) {
			Log.e("facoluz_evaluacion_bucal", "[syncUsers] Error: " + e.toString() + "\n" + Log.getStackTraceString(e));
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Remove Title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		// Copy database file from assets into internal storage to open it if it doesn't exist
		File dbfile = new File(getFilesDir() + "/file.db");
		if (!dbfile.exists()) {
			AssetManager assets = this.getAssets();
			try {
				InputStream dbasset = assets.open("file.db");
				OutputStream dbfile_stream = new FileOutputStream(dbfile);
				Copy(dbasset, dbfile_stream);
			} catch (IOException io) {
				Log.e("facoluz_evaluacion_bucal", "Fatal. Could not copy to " + dbfile.getPath());
			}
		}
		Log.i("facoluz_evaluacion_bucal", "Opening Database on " + dbfile.getPath());
		db = SQLiteDatabase.openDatabase(dbfile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
		WebView web = findViewById(R.id.webview);
		
		Handler handler = new Handler(this, web, db);
		// Setup IP
		Cursor cursor = db.rawQuery("select ip from configuracion", new String[] {});
		cursor.moveToNext();
		handler.setvar("ip", cursor.getString(0));
		Thread sync = new Thread(() -> updateUsers(handler.getvar("ip"), this, db));
		sync.start();


		// Configure WebView
		WebSettings cfg = web.getSettings();
		cfg.setJavaScriptEnabled(true);
		cfg.setDomStorageEnabled(true);
		web.addJavascriptInterface(handler, "Android");
		web.setWebChromeClient(new WebChromeClient());
		web.loadUrl("file:///android_asset/login.html");
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(db != null)
			db.close();
	}
}

