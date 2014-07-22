package edu.rosehulman.graderecorder2;

import java.io.IOException;
import java.util.ArrayList;

import android.accounts.AccountManager;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.appspot.boutell_grade_recorder_2.graderecorder.Graderecorder;
import com.appspot.boutell_grade_recorder_2.graderecorder.model.Assignment;
import com.appspot.boutell_grade_recorder_2.graderecorder.model.AssignmentCollection;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;

public class AssignmentListActivity extends ListActivity {

	/**
	 * Credentials object that maintains tokens to send to the back end.
	 */
	GoogleAccountCredential mCredential;

	/**
	 * Preference object where we store the name of the current user.
	 */
	SharedPreferences mSettings = null;

	// FIXME: Replace this evil global variable with a more robust singleton
	// object.
	static Graderecorder mService;

	public static final String SHARED_PREFERENCES_NAME = "GradeRecorder";
	public static final String PREF_ACCOUNT_NAME = "PREF_ACCOUNT_NAME";
	static final int REQUEST_ACCOUNT_PICKER = 1;
	public static final String GR = "GR";

	static final String KEY_ASSIGNMENT_ENTITY_KEY = "KEY_ASSIGNMENT_ID";
	static final String KEY_ASSIGNMENT_NAME = "KEY_ASSIGNMENT_NAME";
	static final String KEY_SERVICE = "KEY_SERVICE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_assignment_list);

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		getListView().setMultiChoiceModeListener(new MyMultiClickListener());
		// FIXME: Check to ensure they have Google Play services.

		// TODO: Create a credential object with the client id of the *deployed*
		// web client, NOT the Android client. (Go figure.)
//		mCredential = GoogleAccountCredential.usingAudience(this,
//				"server:client_id:<<your web client ID here>>>.apps.googleusercontent.com");
		mCredential = GoogleAccountCredential.usingAudience(this,
				"server:client_id:3607967651-5nqg6qis8ivo294oenp8nff9k35dp70h.apps.googleusercontent.com");
		
		// Easy if only ever 1 user.
		// FIXME: Known issue: since on the backend, we used the user property
		// and not the user ID, this only works if the username is all
		// lowercase. Sorry.
		// mCredential.setSelectedAccountName("username@gmail.com");

		// More robust approach: let the user pick the account, but save it as a
		// shared preference so they don't have to keep logging in every time
		// onCreate is called.
		mSettings = getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
		setAccountName(mSettings.getString(PREF_ACCOUNT_NAME, null));

		// Note the last parameter isn't null.
		Graderecorder.Builder builder = new Graderecorder.Builder(
				AndroidHttp.newCompatibleTransport(), new GsonFactory(), mCredential);
		mService = builder.build();

		if (mCredential.getSelectedAccountName() == null) {
			// Not signed in, show login window or request an existing account.
			chooseAccount();
		}

		updateAssignments();
	}

	private void updateAssignments() {
		new QueryForAssignmentsTask().execute();
	}

	/**
	 * Save the account name in preferences and the credentials
	 * 
	 * @param accountName
	 */
	private void setAccountName(String accountName) {
		SharedPreferences.Editor editor = mSettings.edit();
		editor.putString(PREF_ACCOUNT_NAME, accountName);
		editor.commit();
		mCredential.setSelectedAccountName(accountName);
	}

	void chooseAccount() {
		// This picker is built in to the Android framework.
		startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.assignment_list, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_ACCOUNT_PICKER:
			if (data != null && data.getExtras() != null) {
				String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					setAccountName(accountName); // User is authorized.
					updateAssignments();
				}
			}
			break;
		}
	}

	/**
	 * If they click an assignment, we launch an activity to display all that
	 * assignment's grade entries.
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent gradeIntent = new Intent(this, GradeEntryListActivity.class);
		Assignment assignment = (Assignment) getListAdapter().getItem(position);
		gradeIntent.putExtra(KEY_ASSIGNMENT_NAME, assignment.getName());
		gradeIntent.putExtra(KEY_ASSIGNMENT_ENTITY_KEY, assignment.getEntityKey());
		startActivity(gradeIntent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_assignment_add:
			addAssignment();
			return true;
		case R.id.menu_assignment_sync:
			updateAssignments();
			return true;
		case R.id.menu_assignment_login:
			chooseAccount();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// Dialog to add an assignment.
	private void addAssignment() {
		DialogFragment df = new DialogFragment() {
			@Override
			public View onCreateView(LayoutInflater inflater, ViewGroup container,
					Bundle savedInstanceState) {
				View view = inflater.inflate(R.layout.dialog_add_assignment, container);
				getDialog().setTitle(R.string.dialog_add_assignment_title);
				final Button confirmButton = (Button) view
						.findViewById(R.id.dialog_add_assignment_ok);
				final Button cancelButton = (Button) view
						.findViewById(R.id.dialog_add_assignment_cancel);
				final EditText assignmentNameEditText = (EditText) view
						.findViewById(R.id.dialog_add_assignment_name);

				confirmButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						String name = assignmentNameEditText.getText().toString(); 
						Toast.makeText(AssignmentListActivity.this,
								"Got the assignment named " + name, Toast.LENGTH_LONG).show();
						// add the data and send to server
						Assignment assignment = new Assignment();
						assignment.setName(name);
						// The owner will be set in the backend.
						((AssignmentArrayAdapter) getListAdapter()).add(assignment);
						((AssignmentArrayAdapter) getListAdapter()).notifyDataSetChanged();
						new InsertAssignmentTask().execute(assignment);
						dismiss();
					}
				});

				cancelButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
					}
				});
				return view;
			}
		};
		df.show(getFragmentManager(), "");

	}

	// Our standard listener to delete multiple items.
	private class MyMultiClickListener implements MultiChoiceModeListener {

		private ArrayList<Assignment> mAssignmentsToDelete = new ArrayList<Assignment>();

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.context, menu);
			mode.setTitle(R.string.context_delete_title);
			return true; // gives tactile feedback
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.context_delete:
				deleteSelectedItems();
				mode.finish();
				return true;
			}
			return false;
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
				boolean checked) {
			Assignment item = (Assignment) getListAdapter().getItem(position);
			if (checked) {
				mAssignmentsToDelete.add(item);
			} else {
				mAssignmentsToDelete.remove(item);
			}
			mode.setTitle("Selected " + mAssignmentsToDelete.size() + " Assignments");
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// purposefully empty
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			mAssignmentsToDelete = new ArrayList<Assignment>();
			return true;
		}

		private void deleteSelectedItems() {
			for (Assignment assignment : mAssignmentsToDelete) {
				((ArrayAdapter<Assignment>) getListAdapter()).remove(assignment);
				new DeleteAssignmentTask().execute(assignment.getEntityKey());
			}
			((ArrayAdapter<Assignment>) getListAdapter()).notifyDataSetChanged();
		}
	}

	// TODO: Make a way to edit assignments: short and long press actions
	// already used. Could add item to multiclick listener, but we probably
	// don't want to edit multiple items at once.

	// ---------------------------------------------------------------------------------
	// Backend communication
	// ---------------------------------------------------------------------------------

	class QueryForAssignmentsTask extends AsyncTask<Void, Void, AssignmentCollection> {

		@Override
		protected AssignmentCollection doInBackground(Void... unused) {
			AssignmentCollection assignments = null;
			try {
				// The logs are here to help debug authentication issues I
				// had...
				// Need qualification here for import below to work unqualified,
				// since there are two identically-named Assignment classes, in
				// the service and the model.
				Log.d(GR, "Using account name = " + mCredential.getSelectedAccountName());
				Graderecorder.Assignment.List query = mService.assignment().list();
				Log.d(GR, "Query = " + (query == null ? "null " : query.toString()));
				query.setLimit(50L);
				assignments = query.execute();
				Log.d(GR, "Assignments = " + assignments);

			} catch (IOException e) {
				Log.d(GR, "Failed loading " + e, e);

			}
			return assignments;
		}

		@Override
		protected void onPostExecute(AssignmentCollection result) {
			super.onPostExecute(result);
			if (result == null) {
				Log.d(GR, "Failed loading, result is null");
				return;
			}

			if (result.getItems() == null) {
				result.setItems(new ArrayList<Assignment>());
			}

			AssignmentArrayAdapter adapter = new AssignmentArrayAdapter(
					AssignmentListActivity.this, android.R.layout.simple_list_item_1,
					result.getItems());
			setListAdapter(adapter);
		}
	}

	class InsertAssignmentTask extends AsyncTask<Assignment, Void, Assignment> {

		@Override
		protected Assignment doInBackground(Assignment... items) {
			try {
				Assignment assignment = mService.assignment().insert(items[0]).execute();
				return assignment;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Assignment result) {
			super.onPostExecute(result);
			if (result == null) {
				Log.d(GR, "Error inserting assignment, result is null");
				return;
			}
			updateAssignments();
		}

	}

	class DeleteAssignmentTask extends AsyncTask<String, Void, Assignment> {

		@Override
		protected Assignment doInBackground(String... entityKeys) {
			Assignment returnedAssignment = null;

			try {
				returnedAssignment = mService.assignment().delete(entityKeys[0]).execute();
			} catch (IOException e) {
				Log.d(GR, "Failed deleting " + e);
			}
			return returnedAssignment;
		}

		@Override
		protected void onPostExecute(Assignment result) {
			super.onPostExecute(result);
			if (result == null) {
				Log.d(GR, "Failed deleting, result is null");
				return;
			}
			updateAssignments();
		}
	}
}
