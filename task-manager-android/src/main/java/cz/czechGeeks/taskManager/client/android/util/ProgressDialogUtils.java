package cz.czechGeeks.taskManager.client.android.util;

import android.app.ProgressDialog;
import android.content.Context;
import cz.czechGeeks.taskManager.client.android.R;

/**
 * Utilita pro vytvareni progras dialogu
 * 
 * @author lukasb
 * 
 */
public class ProgressDialogUtils {

	private ProgressDialogUtils() {
	}

	public static ProgressDialog create(Context context) {
		return create(context, R.string.loadingData);
	}

	/**
	 * 
	 * @param context
	 * @param message
	 *            ID zpravy
	 * @return
	 */
	public static ProgressDialog create(Context context, int message) {
		ProgressDialog progressDialog = new ProgressDialog(context);

		progressDialog.setIcon(R.drawable.ic_launcher);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

		progressDialog.setCancelable(false);
		progressDialog.setMessage(context.getText(message));

		return progressDialog;
	}

}
