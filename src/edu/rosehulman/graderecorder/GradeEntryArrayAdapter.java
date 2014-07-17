package edu.rosehulman.graderecorder;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;


public class GradeEntryArrayAdapter extends ArrayAdapter<GradeEntry> {

	private Context mContext;

	public GradeEntryArrayAdapter(Context context, int resource, List<GradeEntry> gradeEntries) {
		super(context, resource, gradeEntries);
		mContext = context;
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
		view.setStudentName(gradeEntry.getStudentName());
		view.setStudentScore(gradeEntry.getScore());
		return view;
	}

}
