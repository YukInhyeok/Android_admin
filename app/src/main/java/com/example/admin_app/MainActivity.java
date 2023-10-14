package com.example.admin_app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    //파이어베이스
    private FirebaseFirestore db;
    //통제 시간 설정
    private TextView timeText;
    private int selectedHour = 0;
    private int selectedMinute = 0;
    private Button changeBtn;
    private TextView score_text;

    //독후감 설정
    private TextView bWorkText;
    private Button bWorkBtn;
    private int numberOfBWorks = 0;
    //모드 변경
    private TextView modText;
    //앱 사용시간
    private TextView app_use_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        //파이어 베이스
        db = FirebaseFirestore.getInstance();

        //통제 시간
        timeText = findViewById(R.id.time_text);
        changeBtn = findViewById(R.id.change_btn);

        //독후감 설정
        bWorkText = findViewById(R.id.b_work_text);

//        //모드 변경
        modText = findViewById(R.id.mod_text);
//
//        //앱 사용시간
        app_use_time = findViewById(R.id.app_use_time);

        //일일차트
        BarChart barChart = findViewById(R.id.chart);
        // fetchData 메서드 호출하여 데이터 가져오기
        ArrayList<BarEntry> entries = new ArrayList<>();
        fetchData(barChart, entries);

        //주간 차트
        BarChart weekChart = findViewById(R.id.week_chart);
        setWeeklyChart(weekChart);

        // 통제 시간
        timeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNumberPickerDialog(); // 메서드 호출
            }
        });

        changeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timeText != null) { // 선택된 시간이 있을 경우에만 updateTimeText() 호출
                    updateTimeText(); // 메소드 호출
                    Toast.makeText(MainActivity.this, "설정 되었습니다.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "시간을 선택해주세요.", Toast.LENGTH_SHORT).show(); // 선택된 시간이 없으면 안내문 출력
                }

                String timeFormat = String.format("%02d:%02d", selectedHour, selectedMinute);

                // Firestore에 시간 저장
                Map<String, Object> timeData = new HashMap<>();
                timeData.put("time", timeFormat);
                db.collection("Time").document("SetTime")
                        .set(timeData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("MainActivity", "SetTime 문서에 시간이 업데이트 되었습니다.");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("MainActivity", "SetTime 문서에 시간 업데이트 오류", e);
                            }
                        });

                // Firestore에 책 개수 저장

                Map<String, Object> bookData = new HashMap<>();
                bookData.put("worknum", numberOfBWorks);
                db.collection("Book").document("report")
                        .set(bookData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("MainActivity", "report 문서에 책 개수가 업데이트 되었습니다.");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("MainActivity", "report 문서에 책 개수 업데이트 오류", e);
                            }
                        });
            }

        });

        // 차트 터치 이벤트
        score_text = findViewById(R.id.score_text);
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e instanceof BarEntry) {
                    BarEntry barEntry = (BarEntry) e;
                    float value = barEntry.getY();
                    int intValue = Math.round(value);


                    score_text.setText(String.valueOf(intValue) + "점");
                }
            }

            @Override
            public void onNothingSelected() {

            }
        });

        //독후감 설정
        bWorkText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBWorkPopup();
            }
        });
        initLockModeListener();
        initTargetTimeListener();
    }


    //클릭 메소드
    private void showMessage(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    //통제 시간
    private void showNumberPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.picker, null);

        NumberPicker hourPicker = view.findViewById(R.id.hourPicker);
        NumberPicker minutePicker = view.findViewById(R.id.minutePicker);

        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(selectedHour);

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(selectedMinute);

        builder.setView(view)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        selectedHour = hourPicker.getValue();
                        selectedMinute = minutePicker.getValue();
                        updateTimeText();
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        builder.create().show();
    }

    private void updateTimeText() {
        String timeFormat = String.format("%02d시간 %02d분", selectedHour, selectedMinute);
        timeText.setText(timeFormat);
    }

    //독후감
    private void showBWorkPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.b_work_popup, null);
        builder.setView(popupView);

        final NumberPicker numberPicker = popupView.findViewById(R.id.b_work_num_picker);
        numberPicker.setMinValue(1); // 최소값 설정
        numberPicker.setMaxValue(100); // 최대값 설정
        bWorkBtn = popupView.findViewById(R.id.b_work_btn);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        bWorkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberOfBWorks = numberPicker.getValue();
                bWorkText.setText("독후감 " + numberOfBWorks + "개 작성하기");
                alertDialog.dismiss();
            }
        });
    }
// 통제 모드
private void initLockModeListener() {
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    db.collection("Screen").document("Lock")
            .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                    @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w("MainActivity", "listen:error", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        int stateValue = documentSnapshot.getLong("state").intValue();
                        if (stateValue == 1) {
                            modText.setText("자유 모드");
                            modText.setTextColor(Color.BLUE);
                        } else {
                            modText.setText("통제 모드");
                            modText.setTextColor(Color.RED);
                        }
                    }
                }
            });
        }

    // 앱 사용시간
    private void initTargetTimeListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Time").document("TargetTime")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("MainActivity", "listen:error", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            long targetTime = documentSnapshot.getLong("Ttime");
                            app_use_time.setText(targetTime + " 분");
                        }
                    }
                });
    }

    private void fetchData(final BarChart barChart, final ArrayList<BarEntry> entries) {
        db.collection("Chart").orderBy("label")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            entries.clear(); // 기존 데이터를 비워줍니다.

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                float value = document.getDouble("value").floatValue();
                                int label = document.getLong("label").intValue();
                                // RadarEntry 대신 BarEntry 사용
                                entries.add(new BarEntry(label, value));
                            }
                            // 데이터를 가져오고 나서 setData 메서드를 호출합니다.
                            setData(barChart, entries); // 여기서 entries 변수를 전달해줍니다.
                        } else {
                            Log.e("MainActivity", "Error fetching data", task.getException());
                        }
                    }
                });
    }

    // 일일차트
    private void setData(BarChart barChart, ArrayList<BarEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setX(i + 1);
        }

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        for (BarEntry entry : entries) {
            BarDataSet dataSet = new BarDataSet(Arrays.asList(entry), "");

            int startColor = Color.parseColor("#FF003A");
            int endColor = Color.parseColor("#FF006D");
            dataSet.setGradientColor(startColor, endColor);

            dataSet.setDrawValues(false); // 값 레이블 그리기 비활성화;

            dataSets.add(dataSet);
        }

        BarData data = new BarData(dataSets);
        data.setBarWidth(0.5f);
        barChart.invalidate();
        barChart.setData(data);

        String[] labels = {"", "독해력", "문해력", "어휘력"};
        XAxis xAxis = barChart.getXAxis();

        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setGranularity(20f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setDrawGridLines(false);
        leftAxis.setLabelCount(6, true);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setDrawLabels(false);

        YAxis rightYAxis = barChart.getAxisRight();
        rightYAxis.setGranularity(20f);
        rightYAxis.setAxisMinimum(0f);
        rightYAxis.setAxisMaximum(100f);
        rightYAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(false);
        rightYAxis.setDrawAxisLine(false);
        xAxis.setDrawAxisLine(false);
        rightYAxis.setLabelCount(6, true);
        rightYAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART); // 값 레이블을 차트 바깥쪽에 표시

        barChart.getLegend().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.setScaleEnabled(false);

        barChart.animateY(800);
    }

    // 저희 복호두 먹을까요?

    // 주간 차트를 설정하고 표시하는 메서드
    private void setWeeklyChart(BarChart weeklyChart) {
        fetchDataForWeeklyChart(new FirestoreCallback() {
            @Override
            public void onDataLoaded(ArrayList<BarEntry> entries) {
                // 주간 차트 데이터 설정 및 그래프 스타일 지정
                for (int i = 0; i < entries.size(); i++) {
                    entries.get(i).setX(i + 1);
                }
                ArrayList<IBarDataSet> dataSets = new ArrayList<>();
                for (BarEntry entry : entries) {
                    BarDataSet dataSet = new BarDataSet(Arrays.asList(entry), "요일별 점수");

                    int startColor = Color.parseColor("#FF003A");
                    int endColor = Color.parseColor("#FF006D");
                    dataSet.setGradientColor(startColor, endColor);
                    dataSet.setDrawValues(false);
                    dataSets.add(dataSet);
                }

                BarData data = new BarData(dataSets);
                data.setBarWidth(0.5f);

                // weeklyChart를 사용하도록 수정
                weeklyChart.setData(data);
                weeklyChart.invalidate();

                String[] labels = {"", "Mon", "Tue", "Wed", "Thr", "Fri"};

                XAxis xAxis = weeklyChart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setDrawAxisLine(false);
                xAxis.setAxisMinimum(0f);
                xAxis.setAxisMaximum(6f);
                xAxis.setDrawGridLines(false);
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                YAxis leftAxis = weeklyChart.getAxisLeft();
                leftAxis.setGranularity(20f);
                leftAxis.setAxisMinimum(0f);
                leftAxis.setAxisMaximum(100f);
                leftAxis.setDrawGridLines(false);
                leftAxis.setDrawLabels(false);
                leftAxis.setDrawAxisLine(false);

                leftAxis.setLabelCount(0, true);

                YAxis rightYAxis = weeklyChart.getAxisRight();
                rightYAxis.setDrawLabels(true);
                rightYAxis.setGranularity(20f);
                rightYAxis.setAxisMinimum(0f);
                rightYAxis.setAxisMaximum(100f);
                // 오른쪽 Y축 그리드 선 없애기
                rightYAxis.setDrawGridLines(false);
                leftAxis.setDrawAxisLine(false);
                rightYAxis.setDrawAxisLine(false);

                weeklyChart.setScaleEnabled(false);
                weeklyChart.getDescription().setEnabled(false);
                weeklyChart.getLegend().setEnabled(false);
                weeklyChart.animateY(800);
            }
        });
    }


    // 주간 차트 데이터를 가져오는 메서드
    private void fetchDataForWeeklyChart(FirestoreCallback callback) {
        // Firestore에서 주간 차트 데이터를 가져오는 코드
        db.collection("WeekChart").orderBy("label")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<BarEntry> entries = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                int average = document.getLong("average").intValue();
                                int label = document.getLong("label").intValue();
                                // RadarEntry 대신 BarEntry 사용
                                //
                                entries.add(new BarEntry(label, average));
                            }

                            callback.onDataLoaded(entries);
                        } else {
                            Log.e("MyInfo", "Error fetching data", task.getException());
                        }
                    }
                });
    }
//
    public interface FirestoreCallback {
        void onDataLoaded(ArrayList<BarEntry> entries);
    }

}