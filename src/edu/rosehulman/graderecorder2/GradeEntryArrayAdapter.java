package edu.rosehulman.graderecorder2;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.appspot.boutell_grade_recorder_2.graderecorder.model.GradeEntry;
import com.appspot.boutell_grade_recorder_2.graderecorder.model.Student;


public class GradeEntryArrayAdapter extends ArrayAdapter<GradeEntry> {

	private Context mContext;
	private Map<String, Student> mStudentMap;

	public GradeEntryArrayAdapter(Context context, int resource, List<GradeEntry> gradeEntries, Map<String, Student> studentMap) {
		super(context, resource, gradeEntries);
		mContext = context;
		mStudentMap = studentMap;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		GradeEntryView view = null;
		if (convertView == null) {
			view = new GradeEntryView(mContext);
		} else {
			view = (GradeEntryView) convertView;
		}
		GradeEntry gradeEntry = getItem(position);
		
		// CONSIDER: map could be null...
		String key = gradeEntry.getStudentKey();
		Student student = mStudentMap.get(key);
		
		view.setStudentName(student.getFirstName() + " " + student.getLastName());
		view.setStudentScore(gradeEntry.getScore());
		return view;
	}

}
