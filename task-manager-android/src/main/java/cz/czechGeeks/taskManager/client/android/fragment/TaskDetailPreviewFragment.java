package cz.czechGeeks.taskManager.client.android.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import cz.czechGeeks.taskManager.client.android.R;
import cz.czechGeeks.taskManager.client.android.activity.TaskDetailActivity;
import cz.czechGeeks.taskManager.client.android.activity.TaskModelActionsCallBack;
import cz.czechGeeks.taskManager.client.android.factory.TaskManagerFactory;
import cz.czechGeeks.taskManager.client.android.model.ErrorMessage;
import cz.czechGeeks.taskManager.client.android.model.TaskModel;
import cz.czechGeeks.taskManager.client.android.model.manager.AsyncTaskCallBack;
import cz.czechGeeks.taskManager.client.android.model.manager.TaskManager;
import cz.czechGeeks.taskManager.client.android.util.LoginUtils;

public class TaskDetailPreviewFragment extends Fragment {

	public interface TaskDetailPreviewFragmentCallBack extends TaskModelActionsCallBack {
		void performShowEditFragment();
	}

	private TaskDetailPreviewFragmentCallBack callBack;

	private TextView categ;
	private TextView name;
	private TextView desc;
	private TextView finishToDate;
	private TextView finishedDate;

	private TextView executor;
	private TextView inserter;

	private TaskModel taskModel;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.main_task_detail_preview_activity, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		TaskManager taskManager = TaskManagerFactory.get(getActivity());
		switch (item.getItemId()) {
		case R.id.action_edit_task:
			if (taskModel.isUpdatable()) {
				callBack.performShowEditFragment();
			} else {
				Toast.makeText(getActivity(), R.string.task_isNotUpdatable, Toast.LENGTH_SHORT).show();
			}
			return true;

		case R.id.action_delete_task:
			if (taskModel.isDeletable()) {
				taskManager.delete(taskModel, new AsyncTaskCallBack<TaskModel>() {

					@Override
					public void onSuccess(TaskModel resumeObject) {
						callBack.onTaskDeleted(taskModel);
					}

					@Override
					public void onError(ErrorMessage message) {
						Toast.makeText(getActivity(), getActivity().getString(R.string.valueNotDeleted) + message.getMessage(), Toast.LENGTH_SHORT).show();
					}
				});
			} else {
				Toast.makeText(getActivity(), R.string.task_isNotDeletable, Toast.LENGTH_SHORT).show();
			}
			return true;

		case R.id.action_close_task:
			if (taskModel.isCloseable()) {
				taskManager.close(taskModel, new AsyncTaskCallBack<TaskModel>() {

					@Override
					public void onSuccess(TaskModel resumeObject) {
						setModelData(resumeObject);
						callBack.onTaskUpdated(resumeObject);
					}

					@Override
					public void onError(ErrorMessage message) {
						Toast.makeText(getActivity(), message.getMessage(), Toast.LENGTH_SHORT).show();
					}
				});
			} else {
				Toast.makeText(getActivity(), R.string.task_isNotCloseable, Toast.LENGTH_SHORT).show();
			}
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_task_detail_preview, container, false);

		categ = (TextView) rootView.findViewById(R.id.taskCateg);
		name = (TextView) rootView.findViewById(R.id.taskName);
		desc = (TextView) rootView.findViewById(R.id.taskDesc);
		finishToDate = (TextView) rootView.findViewById(R.id.taskFinishToDate);
		finishedDate = (TextView) rootView.findViewById(R.id.taskFinishedDate);
		executor = (TextView) rootView.findViewById(R.id.taskExecutor);
		inserter = (TextView) rootView.findViewById(R.id.taskInserter);

		TaskModel taskModel = (TaskModel) getArguments().get(TaskDetailActivity.TASK_MODEL);

		if (taskModel == null) {
			throw new IllegalArgumentException("Argument " + TaskDetailActivity.TASK_MODEL + " musi byt zadan");
		}

		setModelData(taskModel);
		markModelAsReaded(taskModel);
		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			callBack = (TaskDetailPreviewFragmentCallBack) activity;
		} catch (Exception e) {
			throw new ClassCastException("Activity musi implementovat " + TaskDetailPreviewFragmentCallBack.class);
		}
	}

	private void setModelData(TaskModel taskModel) {
		this.taskModel = taskModel;

		categ.setText(taskModel.getCategName());
		name.setText(taskModel.getName());
		desc.setText(taskModel.getDesc());

		finishToDate.setText((taskModel.getFinishToDate() != null) ? java.text.DateFormat.getDateTimeInstance().format(taskModel.getFinishToDate()) : "");
		finishedDate.setText((taskModel.getFinishedDate() != null) ? java.text.DateFormat.getDateTimeInstance().format(taskModel.getFinishedDate()) : "");

		executor.setText(taskModel.getExecutorName());
		inserter.setText(taskModel.getInserterName());
	}

	public void markModelAsReaded(TaskModel taskModel) {
		if (LoginUtils.get().getLoggedUserId().equals(taskModel.getExecutorId()) && taskModel.isUnread()) {
			// Pokud jsem ten kdo ma ukol splnit a ukol je neprecteny tak ho oznacim jako precteny
			TaskManager taskManager = TaskManagerFactory.get(getActivity());
			taskManager.markAsReaded(taskModel, new AsyncTaskCallBack<TaskModel>() {

				@Override
				public void onSuccess(TaskModel resumeObject) {
					Toast.makeText(getActivity(), R.string.taskMarkedAsReaded, Toast.LENGTH_SHORT).show();
					setModelData(resumeObject);
					callBack.onTaskUpdated(resumeObject);
				}

				@Override
				public void onError(ErrorMessage message) {
					String errorMessage = getActivity().getText(R.string.taskMarkedAsReaded_error) + message.getMessage();
					Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

}