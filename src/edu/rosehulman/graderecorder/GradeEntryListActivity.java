package edu.rosehulman.graderecorder;

import java.io.IOException;
import java.util.ArrayList;

import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.Intent;
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

public class GradeEntryListActivity extends ListActivity {

	private long mAssignmentId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grade_entry);

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		getListView().setMultiChoiceModeListener(new MyMultiClickListener());

		// We'll need the assignment ID to query for assignments.
		Intent intent = getIntent();
		mAssignmentId = intent.getLongExtra(AssignmentListActivity.KEY_ASSIGNMENT_ID, -1);
		String assignmentName = intent.getStringExtra(AssignmentListActivity.KEY_ASSIGNMENT_NAME);
		setTitle(assignmentName);

		updateGradeEntries();

	}

	private void updateGradeEntries() {
		new QueryForGradeEntriesTask().execute(mAssignmentId);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		// TODO: Edit the grade entry via a dialog.
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.grade_entry_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_grade_entry_add:
			addGradeEntry();
			return true;
		case R.id.menu_grade_entry_sync:
			updateGradeEntries();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void addGradeEntry() {
		DialogFragment df = new DialogFragment() {
			@Override
			public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
				View view = inflater.inflate(R.layout.dialog_add_grade_entry, container);
				getDialog().setTitle(R.string.dialog_add_grade_entry_title);
				final Button confirmButton = (Button) view.findViewById(R.id.dialog_add_grade_entry_ok);
				final Button cancelButton = (Button) view.findViewById(R.id.dialog_add_grade_entry_cancel);
				final EditText studentNameEditText = (EditText) view
						.findViewById(R.id.dialog_add_grade_entry_student_name);
				// TODO: Replace with a spinner of defaults like "TeamNN"
				final EditText scoreEditText = (EditText) view.findViewById(R.id.dialog_add_grade_entry_score);

				confirmButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						String name = studentNameEditText.getText().toString();
						long score = Long.parseLong(scoreEditText.getText().toString());

						Toast.makeText(GradeEntryListActivity.this, "Got the name " + name + " score " + score,
								Toast.LENGTH_LONG).show();
						// add the data and send to server
						GradeEntry gradeEntry = new GradeEntry();
						gradeEntry.setStudentName(name);
						gradeEntry.setScore(score);
						// Fails without the assignment ID!
						gradeEntry.setAssignmentId(mAssignmentId);
						((GradeEntryArrayAdapter) getListAdapter()).add(gradeEntry);
						((GradeEntryArrayAdapter) getListAdapter()).notifyDataSetChanged();
						new InsertGradeEntryTask().execute(gradeEntry);
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

		private ArrayList<GradeEntry> mGradeEntriesToDelete = new ArrayList<GradeEntry>();

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
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
			GradeEntry item = (GradeEntry) getListAdapter().getItem(position);
			if (checked) {
				mGradeEntriesToDelete.add(item);
			} else {
				mGradeEntriesToDelete.remove(item);
			}
			mode.setTitle("Selected " + mGradeEntriesToDelete.size() + " entries");
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// purposefully empty
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			mGradeEntriesToDelete = new ArrayList<GradeEntry>();
			return true;
		}

		private void deleteSelectedItems() {
			for (GradeEntry gradeEntry : mGradeEntriesToDelete) {
				((ArrayAdapter<GradeEntry>) getListAdapter()).remove(gradeEntry);
				new DeleteGradeEntryTask().execute(gradeEntry.getId());
			}
			((ArrayAdapter<Assignment>) getListAdapter()).notifyDataSetChanged();
		}
	}

	// ---------------------------------------------------------------------------------
	// Backend communication
	// ---------------------------------------------------------------------------------

	class QueryForGradeEntriesTask extends AsyncTask<Long, Void, GradeEntryCollection> {

		@Override
		protected GradeEntryCollection doInBackground(Long... assignmentIds) {
			GradeEntryCollection gradeEntries = null;
			try {
				GradeRecorder.Gradeentry.List query = AssignmentListActivity.mService.gradeentry().list(
						assignmentIds[0]);
				query.setOrder("student_name");
				query.setLimit(50L);
				gradeEntries = query.execute();
				Log.d(AssignmentListActivity.GR, "Grade entries = " + gradeEntries);

			} catch (IOException e) {
				Log.d(AssignmentListActivity.GR, "Failed loading " + e, e);

			}
			return gradeEntries;
		}

		@Override
		protected void onPostExecute(GradeEntryCollection result) {
			super.onPostExecute(result);
			if (result == null) {
				Log.d(AssignmentListActivity.GR, "Failed loading, result is null");
				return;
			}

			if (result.getItems() == null) {
				result.setItems(new ArrayList<GradeEntry>());
			}

			GradeEntryArrayAdapter adapter = new GradeEntryArrayAdapter(GradeEntryListActivity.this,
					android.R.layout.simple_list_item_1, result.getItems());
			setListAdapter(adapter);
		}
	}

	class InsertGradeEntryTask extends AsyncTask<GradeEntry, Void, GradeEntry> {

		@Override
		protected GradeEntry doInBackground(GradeEntry... items) {
			try {
				GradeEntry gradeEntry = AssignmentListActivity.mService.gradeentry().insert(items[0]).execute();
				return gradeEntry;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(GradeEntry result) {
			super.onPostExecute(result);
			if (result == null) {
				Log.d(AssignmentListActivity.GR, "Error inserting grade entry, result is null");
				return;
			}
			updateGradeEntries();
		}
	}

	class DeleteGradeEntryTask extends AsyncTask<Long, Void, GradeEntry> {

		@Override
		protected GradeEntry doInBackground(Long... ids) {
			GradeEntry returnedGradeEntry = null;

			try {
				returnedGradeEntry = AssignmentListActivity.mService.gradeentry().delete(ids[0]).execute();
			} catch (IOException e) {
				Log.d(AssignmentListActivity.GR, "Failed deleting " + e, e);
			}
			return returnedGradeEntry;
		}

		@Override
		protected void onPostExecute(GradeEntry result) {
			super.onPostExecute(result);
			if (result == null) {
				Log.d(AssignmentListActivity.GR, "Failed deleting, result is null");
				return;
			}
			updateGradeEntries();
		}
	}
}
