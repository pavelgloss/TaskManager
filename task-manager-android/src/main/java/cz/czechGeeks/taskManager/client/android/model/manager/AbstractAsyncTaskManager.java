package cz.czechGeeks.taskManager.client.android.model.manager;

import java.net.HttpURLConnection;
import java.net.URL;

import org.codehaus.jackson.map.ObjectMapper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;
import cz.czechGeeks.taskManager.client.android.R;
import cz.czechGeeks.taskManager.client.android.model.ErrorMessage;
import cz.czechGeeks.taskManager.client.android.util.PreferencesUtils;
import cz.czechGeeks.taskManager.client.android.util.PreferencesUtils.ConnectionItems;
import cz.czechGeeks.taskManager.client.android.util.ProgressDialogUtils;

public abstract class AbstractAsyncTaskManager {

	public enum RequestMethod {
		GET, POST, PUT, DELETE
	}

	private static final int STATUS_CODE_OK = 200;
	private static final int STATUS_CODE_SYSTEM_ERROR = 500;
	private static final int STATUS_CODE_NOT_AUTHORIZED = 401;

	private static final String LOG_TAG = "AsyncTaskManager";

	private final Context context;
	private final ProgressDialog dialog;

	public AbstractAsyncTaskManager(Context context) {
		this.context = context;
		this.dialog = ProgressDialogUtils.create(context);
	}

	protected Context getContext() {
		return context;
	}

	protected <T> void run(String baseUrlPostFix, RequestMethod requestMethod, final Class<T> returnValueClass, final AsyncTaskCallBack<T> callBack) {
		ConnectionItems connectionItems = PreferencesUtils.getConnectionItems(context);

		final String URL = connectionItems.BASE_URL + baseUrlPostFix;
		final String USER_NAME = connectionItems.USER_NAME;
		final String PASSWORD = connectionItems.PASSWORD;
		final RequestMethod REQUEST_METHOD = requestMethod;

		run(URL, USER_NAME, PASSWORD, REQUEST_METHOD, returnValueClass, callBack);
	}

	protected <T> void run(final String URL, final String USER_NAME, final String PASSWORD, final RequestMethod REQUEST_METHOD, final Class<T> returnValueClass, final AsyncTaskCallBack<T> callBack) {
		run(URL, USER_NAME, PASSWORD, REQUEST_METHOD, returnValueClass, callBack, null);
	}

	protected <T> void run(final String URL, final String USER_NAME, final String PASSWORD, final RequestMethod REQUEST_METHOD, final Class<T> returnValueClass, final AsyncTaskWithResultCodeCallBack callBackWithResultCode) {
		run(URL, USER_NAME, PASSWORD, REQUEST_METHOD, returnValueClass, null, callBackWithResultCode);
	}

	private <T> void run(final String URL, final String USER_NAME, final String PASSWORD, final RequestMethod REQUEST_METHOD, final Class<T> returnValueClass, final AsyncTaskCallBack<T> callBack, final AsyncTaskWithResultCodeCallBack callBackWithResultCode) {
		Log.d(LOG_TAG, "Pripojeni k URL: " + URL);
		Log.d(LOG_TAG, "Metoda: " + REQUEST_METHOD);
		Log.d(LOG_TAG, "Uzivatelske jmeno: " + USER_NAME);
		Log.d(LOG_TAG, "Heslo: " + PASSWORD);

		ConnectivityManager connectionManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();

		if (networkInfo != null && networkInfo.isConnected()) {
			dialog.show();

			new Thread() {
				public void run() {
					T returnValue = null;// Navratova hodnota
					Integer responseCode = null;// Respose code
					ErrorMessage errorMessage = null;// Chyba

					HttpURLConnection connection = null;

					try {
						URL url = new URL(URL);

						connection = (HttpURLConnection) url.openConnection();
						connection.setRequestMethod(REQUEST_METHOD.name());
						connection.setRequestProperty("Accept", "application/json");
						connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((USER_NAME + ":" + PASSWORD).getBytes(), Base64.NO_WRAP));

						connection.connect();

						responseCode = connection.getResponseCode();
						Log.i(LOG_TAG, "Kod odpovedi:" + responseCode);

						if (responseCode == STATUS_CODE_OK) {
							if (callBack != null) {
								returnValue = new ObjectMapper().readValue(connection.getInputStream(), returnValueClass);
								Log.i(LOG_TAG, "Odpoved ze serveru OK.");
							}
							
						} else if (responseCode == STATUS_CODE_NOT_AUTHORIZED) {
							errorMessage = new ErrorMessage(context.getString(R.string.signIn_error_noValid));
							Log.e(LOG_TAG, "Neplatne uzivatelske jmeno nebo heslo!");

						} else if (responseCode == STATUS_CODE_SYSTEM_ERROR) {
							errorMessage = new ObjectMapper().readValue(connection.getInputStream(), ErrorMessage.class);
							Log.e(LOG_TAG, "Systemova chyba: " + errorMessage.getMessage());
						}

					} catch (final Exception e) {
						errorMessage = new ErrorMessage(e.getLocalizedMessage());
						Log.e(LOG_TAG, "Systemova chyba: " + errorMessage.getMessage());
					} finally {
						if (connection != null) {
							connection.disconnect();
						}
					}

					final T retValue = returnValue;
					final Integer returnResponseCode = responseCode;
					final ErrorMessage returnErrorMessage = errorMessage;

					((Activity) context).runOnUiThread(new Runnable() {

						@Override
						public void run() {
							dialog.dismiss();
							if (returnErrorMessage == null) {
								if (callBack != null) {
									callBack.onSuccess(retValue);
								}
								if (callBackWithResultCode != null) {
									callBackWithResultCode.onSuccess(returnResponseCode);
								}
							} else {
								// nastala chyba
								if (callBack != null) {
									callBack.onError(returnErrorMessage);
								}
								if (callBackWithResultCode != null) {
									callBackWithResultCode.onError(returnErrorMessage);
								}
							}
						}
					});
				}
			}.start();
		} else {
			callBack.onError(new ErrorMessage(context.getString(R.string.connection_error_noEnabled)));
		}
	}

}
