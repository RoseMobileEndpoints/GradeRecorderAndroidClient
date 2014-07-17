package edu.rosehulman.graderecorder2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.appspot.boutell_grade_recorder_2.graderecorder.Graderecorder;
import com.appspot.boutell_grade_recorder_2.graderecorder.model.Assignment;
import com.appspot.boutell_grade_recorder_2.graderecorder.model.GradeEntry;
import com.appspot.boutell_grade_recorder_2.graderecorder.model.GradeEntryCollection;
import com.appspot.boutell_grade_recorder_2.graderecorder.model.Student;
import com.appspot.boutell_grade_recorder_2.graderecorder.model.StudentCollection;

public class GradeEntryListActivity extends ListActivity {

	private String mAssignmentKey;
	private Map<String, Student> mStudentMap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grade_entry);

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		getListView().setMultiChoiceModeListener(new MyMultiClickListener());

		// We'll need the assignment ID to query for assignments.
		Intent intent = getIntent();
		mAssignmentKey = intent.getStringExtra(AssignmentListActivity.KEY_ASSIGNMENT_ENTITY_KEY);
		String assignmentName = intent.getStringExtra(AssignmentListActivity.KEY_ASSIGNMENT_NAME);
		setTitle(assignmentName);

		updateStudents();
		updateGradeEntries();
	}

	private void updateStudents() {
		new QueryForStudentsTask().execute();
	}

	private void updateGradeEntries() {
		new QueryForGradeEntriesTask().execute(mAssignmentKey);
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
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(R.string.dialog_add_grade_entry_title);
				View view = getLayoutInflater().inflate(R.layout.dialog_add_grade_entry, null);
				final EditText scoreEditText = (EditText) view.findViewById(R.id.dialog_add_grade_entry_score);
				final Spinner nameSpinner = (Spinner)view.findViewById(R.id.dialog_add_grade_entry_student_spinner);
				// TODO: set up array adapter
				List<Student> students = new ArrayList<Student>(mStudentMap.values());
				StudentAdapter studentAdapter = new StudentAdapter(GradeEntryListActivity.this, students);
				studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				nameSpinner.setAdapter(studentAdapter);
				
				builder.setView(view);
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String studentKey = ((Student)nameSpinner.getSelectedItem()).getEntityKey();
						long score = Long.parseLong(scoreEditText.getText().toString());
						// add the data and send to server
						GradeEntry gradeEntry = new GradeEntry();
						gradeEntry.setStudentKey(studentKey);
						gradeEntry.setScore(score);
						// Fails without the assignment ID!
						gradeEntry.setAssignmentKey(mAssignmentKey);
						((GradeEntryArrayAdapter) getListAdapter()).add(gradeEntry);
						((GradeEntryArrayAdapter) getListAdapter()).notifyDataSetChanged();
						new InsertGradeEntryTask().execute(gradeEntry);
						dismiss();
					}
				});
				builder.setNegativeButton(android.R.string.cancel, null);
				return builder.create();
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
				new DeleteGradeEntryTask().execute(gradeEntry.getEntityKey());
			}
			((ArrayAdapter<Assignment>) getListAdapter()).notifyDataSetChanged();
		}
	}

	// ---------------------------------------------------------------------------------
	// Backend communication
	// ---------------------------------------------------------------------------------

	class QueryForGradeEntriesTask extends AsyncTask<String, Void, GradeEntryCollection> {

		@Override
		protected GradeEntryCollection doInBackground(String... entityKeys) {
			GradeEntryCollection gradeEntries = null;
			try {
				Graderecorder.Gradeentry.List query = AssignmentListActivity.mService.gradeentry().list(
						entityKeys[0]);
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
					android.R.layout.simple_list_item_1, result.getItems(), mStudentMap);
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

	class DeleteGradeEntryTask extends AsyncTask<String, Void, GradeEntry> {

		@Override
		protected GradeEntry doInBackground(String... entityKeys) {
			GradeEntry returnedGradeEntry = null;

			try {
				returnedGradeEntry = AssignmentListActivity.mService.gradeentry().delete(entityKeys[0]).execute();
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
	
	class QueryForStudentsTask extends AsyncTask<Void, Void, StudentCollection> {

		@Override
		protected StudentCollection doInBackground(Void... unused) {
			StudentCollection students = null;
			try {
				// The logs are here to help debug authentication issues I
				// had...
				// Need qualification here for import below to work unqualified,
				// since there are two identically-named Assignment classes, in
				// the service and the model.
				Graderecorder.Student.List query = AssignmentListActivity.mService.student().list();
				Log.d(AssignmentListActivity.GR, "Query = " + (query == null ? "null " : query.toString()));
				query.setLimit(50L);
				students = query.execute();
				Log.d(AssignmentListActivity.GR, "Students = " + students);

			} catch (IOException e) {
				Log.d(AssignmentListActivity.GR, "Failed loading " + e, e);

			}
			return students;
		}

		@Override
		protected void onPostExecute(StudentCollection result) {
			super.onPostExecute(result);
			if (result == null) {
				Log.d(AssignmentListActivity.GR, "Failed loading, result is null");
				return;
			}

			// Store students for later use.
			mStudentMap = new TreeMap<String, Student>();
			for (Student s : result.getItems()) {
				mStudentMap.put(s.getEntityKey(), s);
			}
		}
	}

	
	
	
}
