package edu.rosehulman.graderecorder2;

import android.app.Activity;
import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class GradeEntryView extends RelativeLayout {

	private TextView studentNameTextView;
	private TextView studentScoreTextView;

	public GradeEntryView(Context context) {
		super(context);
		((Activity) context).getLayoutInflater().inflate(R.layout.grade_entry_view, this);
		studentNameTextView = (TextView) findViewById(R.id.grade_entry_student_name);
		studentScoreTextView = (TextView) findViewById(R.id.grade_entry_student_score);
	}

	void setStudentName(String name) {
		studentNameTextView.setText(name);
	}

	void setStudentScore(long score) {
		studentScoreTextView.setText("" + score);
	}

}
