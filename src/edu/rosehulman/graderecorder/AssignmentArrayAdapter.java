package edu.rosehulman.graderecorder;

import java.util.List;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AssignmentArrayAdapter extends ArrayAdapter<Assignment> {

	private Context mContext;

	public AssignmentArrayAdapter(Context context, int resource, List<Assignment> assignments) {
		super(context, resource, assignments);
		mContext = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view = null;
		if (convertView == null) {
			view = new TextView(mContext);
		} else {
			view = (TextView) convertView;
		}
		Assignment assignment = getItem(position);
		view.setText(assignment.getName());
		view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
		view.setPadding(10, 16, 10, 16); // Left,Top,Right,Bottom
		return view;
	}

}
