package edu.rosehulman.graderecorder2;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.appspot.boutell_grade_recorder_2.graderecorder.model.Student;

public class StudentAdapter extends ArrayAdapter<Student> {

	public StudentAdapter(Context context, List<Student> students) {
		super(context, android.R.layout.simple_spinner_item, students);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		TextView tv = (TextView) view.findViewById(android.R.id.text1);
		Student student = getItem(position);
		tv.setText(student.getFirstName() + " " + student.getLastName());
		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		View view = super.getDropDownView(position, convertView, parent);
		TextView tv = (TextView) view.findViewById(android.R.id.text1);
		Student student = getItem(position);
		tv.setText(student.getFirstName() + " " + student.getLastName());
		return view;
	}
	
}
