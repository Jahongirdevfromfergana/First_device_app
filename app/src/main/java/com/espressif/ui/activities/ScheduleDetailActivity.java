// Copyright 2020 Espressif Systems (Shanghai) PTE LTD
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.espressif.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ContentLoadingProgressBar;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.espressif.AppConstants;
import com.espressif.EspApplication;
import com.espressif.cloudapi.ApiManager;
import com.espressif.cloudapi.ApiResponseListener;
import com.espressif.rainmaker.R;
import com.espressif.ui.Utils;
import com.espressif.ui.models.Action;
import com.espressif.ui.models.Device;
import com.espressif.ui.models.EspNode;
import com.espressif.ui.models.Param;
import com.espressif.ui.models.Schedule;
import com.espressif.ui.models.Service;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class ScheduleDetailActivity extends AppCompatActivity {

    private final String TAG = ScheduleDetailActivity.class.getSimpleName();

    public static final int REQ_CODE_ACTIONS = 10;

    private RelativeLayout rlScheduleName, rlActions, rlRepeat;
    private TextView tvScheduleName, tvActionDevices;
    private TimePicker timePicker;
    private MaterialCardView btnRemoveSchedule;
    private TextView txtRemoveScheduleBtn;
    private ImageView removeScheduleImage;
    private ContentLoadingProgressBar progressBar;
    private RelativeLayout rlSchProgress, rlAddSch;
    private LinearLayout daysLayout;
    private TextView day1, day2, day3, day4, day5, day6, day7;
    private MenuItem menuSave;

    private String scheduleName = "";
    private StringBuilder days;
    private boolean isRepeatOptionsVisible = false;
    private EspApplication espApp;
    private ApiManager apiManager;
    private ArrayList<Device> devices;
    private ArrayList<Device> selectedDevices;
    private ArrayList<String> selectedNodeIds;
    private Schedule schedule;
    private String operation = AppConstants.KEY_OPERATION_ADD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_detail);

        espApp = (EspApplication) getApplicationContext();
        apiManager = ApiManager.getInstance(this);
        devices = new ArrayList<>();
        selectedNodeIds = new ArrayList<>();
        schedule = getIntent().getParcelableExtra(AppConstants.KEY_SCHEDULE);
        scheduleName = getIntent().getStringExtra(AppConstants.KEY_NAME);
        selectedDevices = new ArrayList<>();
        days = new StringBuilder("00000000");

        for (Map.Entry<String, EspNode> entry : espApp.nodeMap.entrySet()) {

            String key = entry.getKey();
            EspNode node = entry.getValue();
            ArrayList<Service> services = node.getServices();

            if (node != null) {

                for (int i = 0; i < services.size(); i++) {

                    Service s = services.get(i);
                    if (!TextUtils.isEmpty(s.getType()) && s.getType().equals(AppConstants.SERVICE_TYPE_SCHEDULE)) {

                        ArrayList<Device> espDevices = node.getDevices();
                        Iterator<Device> iterator = espDevices.iterator();

                        while (iterator.hasNext()) {
                            devices.add(new Device(iterator.next()));
                        }
                    }
                }
            }
        }

        Iterator deviceIterator = devices.iterator();
        while (deviceIterator.hasNext()) {

            Device device = (Device) deviceIterator.next();
            ArrayList<Param> params = device.getParams();
            Iterator itr = params.iterator();

            while (itr.hasNext()) {
                Param p = (Param) itr.next();

                if (!p.isDynamicParam()) {
                    itr.remove();
                } else if (p.getParamType() != null && p.getParamType().equals(AppConstants.PARAM_TYPE_NAME)) {
                    itr.remove();
                } else if (!p.getProperties().contains(AppConstants.KEY_PROPERTY_WRITE)) {
                    itr.remove();
                }
            }

            if (params.size() <= 0) {
                deviceIterator.remove();
            }
        }
        initViews();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            switch (requestCode) {

                case REQ_CODE_ACTIONS:

                    if (data != null) {

                        ArrayList<Device> actionDevices = data.getParcelableArrayListExtra(AppConstants.KEY_ACTIONS);
                        selectedDevices.clear();
                        selectedDevices.addAll(actionDevices);
                        Log.d(TAG, "Selected devices list size : " + selectedDevices.size());
                        setActionDevicesNames();

                        if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                            if (selectedDevices.size() > 0) {
                                menuSave.setVisible(true);
                            } else {
                                menuSave.setVisible(false);
                            }
                        } else {
                            menuSave.setVisible(true);
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menuSave = menu.add(Menu.NONE, 1, Menu.NONE, R.string.btn_save);
        menuSave.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
            if (selectedDevices.size() > 0) {
                menuSave.setVisible(true);
            } else {
                menuSave.setVisible(false);
            }
        } else {
            menuSave.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case 1:
                saveSchedule();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initViews() {

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_schedule_details);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnRemoveSchedule = findViewById(R.id.btn_remove_schedule);
        txtRemoveScheduleBtn = findViewById(R.id.text_btn);
        removeScheduleImage = findViewById(R.id.iv_remove);
        progressBar = findViewById(R.id.progress_indicator);

        rlScheduleName = findViewById(R.id.rl_schedule_name);
        rlActions = findViewById(R.id.rl_actions);
        rlRepeat = findViewById(R.id.rl_repeat);
        daysLayout = findViewById(R.id.days_layout);

        rlAddSch = findViewById(R.id.rl_add_schedule);
        rlSchProgress = findViewById(R.id.rl_progress_add_sch);

        tvScheduleName = findViewById(R.id.tv_schedule_name);
        timePicker = findViewById(R.id.time_picker);
        tvActionDevices = findViewById(R.id.tv_action_device_list);

        day1 = findViewById(R.id.tv_day_1);
        day2 = findViewById(R.id.tv_day_2);
        day3 = findViewById(R.id.tv_day_3);
        day4 = findViewById(R.id.tv_day_4);
        day5 = findViewById(R.id.tv_day_5);
        day6 = findViewById(R.id.tv_day_6);
        day7 = findViewById(R.id.tv_day_7);

        isRepeatOptionsVisible = false;
        daysLayout.setVisibility(View.GONE);
        tvActionDevices.setVisibility(View.GONE);
        btnRemoveSchedule.setVisibility(View.GONE);

        if (schedule != null) {

            operation = AppConstants.KEY_OPERATION_EDIT;
            scheduleName = schedule.getName();
            getSupportActionBar().setTitle(R.string.title_activity_schedule_details);

            HashMap<String, Integer> triggers = schedule.getTriggers();
            int daysValue = triggers.get(AppConstants.KEY_DAYS);

            if (daysValue != 0) {

                String daysStr = Integer.toBinaryString(daysValue);
                char[] daysCharValue = daysStr.toCharArray();
                int j = 7;

                for (int i = (daysCharValue.length - 1); i >= 0; i--) {
                    days.setCharAt(j, daysCharValue[i]);
                    j--;
                }
            }

            int mins = triggers.get(AppConstants.KEY_MINUTES);
            int h = mins / 60;
            int m = mins % 60;
            timePicker.setHour(h);
            timePicker.setMinute(m);

            btnRemoveSchedule.setVisibility(View.VISIBLE);

            ArrayList<Action> actions = schedule.getActions();
            for (int i = 0; i < actions.size(); i++) {
                Device device = actions.get(i).getDevice();
                String nodeId = device.getNodeId();
                selectedDevices.add(device);
                if (!selectedNodeIds.contains(nodeId)) {
                    selectedNodeIds.add(nodeId);
                }
            }
            setActionDevicesNames();
            setRepeatDaysText();

            if (menuSave != null) {
                if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                    if (selectedDevices.size() > 0) {
                        menuSave.setVisible(true);
                    } else {
                        menuSave.setVisible(false);
                    }
                } else {
                    menuSave.setVisible(true);
                }
            }
        }

        if (TextUtils.isEmpty(scheduleName)) {
            tvScheduleName.setText(R.string.not_set);
        } else {
            tvScheduleName.setText(scheduleName);
        }

        rlScheduleName.setOnClickListener(scheduleNameClickListener);
        rlActions.setOnClickListener(actionsClickListener);
        rlRepeat.setOnClickListener(repeatClickListener);
        btnRemoveSchedule.setOnClickListener(removeScheduleBtnClickListener);
        day1.setOnClickListener(daysClickListener);
        day2.setOnClickListener(daysClickListener);
        day3.setOnClickListener(daysClickListener);
        day4.setOnClickListener(daysClickListener);
        day5.setOnClickListener(daysClickListener);
        day6.setOnClickListener(daysClickListener);
        day7.setOnClickListener(daysClickListener);
    }

    private View.OnClickListener scheduleNameClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            askForScheduleName();
        }
    };

    private View.OnClickListener actionsClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            gotoActionsScreen();
        }
    };

    private View.OnClickListener repeatClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            if (isRepeatOptionsVisible) {

                isRepeatOptionsVisible = false;
                daysLayout.setVisibility(View.GONE);
                findViewById(R.id.iv_right_arrow_1).animate().rotation(0).setInterpolator(new LinearInterpolator()).setDuration(200);

            } else {

                isRepeatOptionsVisible = true;
                daysLayout.setVisibility(View.VISIBLE);
                findViewById(R.id.iv_right_arrow_1).animate().rotation(90).setInterpolator(new LinearInterpolator()).setDuration(200);
            }
        }
    };

    private View.OnClickListener daysClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            int id = v.getId();

            switch (id) {

                case R.id.tv_day_1:
                    changeDayValue(7, day1);
                    break;

                case R.id.tv_day_2:
                    changeDayValue(6, day2);
                    break;

                case R.id.tv_day_3:
                    changeDayValue(5, day3);
                    break;

                case R.id.tv_day_4:
                    changeDayValue(4, day4);
                    break;

                case R.id.tv_day_5:
                    changeDayValue(3, day5);
                    break;

                case R.id.tv_day_6:
                    changeDayValue(2, day6);
                    break;

                case R.id.tv_day_7:
                    changeDayValue(1, day7);
                    break;
            }
        }
    };

    private View.OnClickListener removeScheduleBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            confirmForRemoveSchedule();
        }
    };

    private void askForScheduleName() {

        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_attribute, null);
        final EditText etSchName = dialogView.findViewById(R.id.et_attr_value);
        etSchName.setInputType(InputType.TYPE_CLASS_TEXT);
        etSchName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle(R.string.dialog_title_add_name)
                .setPositiveButton(R.string.btn_ok, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
        etSchName.setText(scheduleName);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button buttonPositive = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String value = etSchName.getText().toString();
                        if (!TextUtils.isEmpty(value)) {
                            scheduleName = value;
                            tvScheduleName.setText(scheduleName);
                            dialog.dismiss();
                        } else {
                            etSchName.setError(getString(R.string.error_invalid_schedule_name));
                        }
                    }
                });

                Button buttonNegative = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void changeDayValue(int position, TextView tvDay) {

        char ch = days.charAt(position);
        if (ch == '1') {
            ch = '0';
        } else {
            ch = '1';
        }
        updateDayText(ch, tvDay);
        days.setCharAt(position, ch);
    }

    private void updateDayText(char dayValue, TextView tvDay) {

        if (dayValue == '1') {
            tvDay.setTextColor(getColor(R.color.colorPrimary));
            tvDay.setTypeface(null, Typeface.BOLD);
        } else {
            tvDay.setTextColor(getColor(R.color.colorPrimaryDark));
            tvDay.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void confirmForRemoveSchedule() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title_remove);
        builder.setMessage(R.string.dialog_msg_confirmation);

        // Set up the buttons
        builder.setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                removeSchedule();
            }
        });

        builder.setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });

        AlertDialog userDialog = builder.create();
        userDialog.show();
    }

    private void removeSchedule() {

        if (schedule == null) {
            Log.e(TAG, "Schedule is null");
            return;
        }

        showRemoveScheduleLoading();
        ArrayList<Action> actions = schedule.getActions();
        ArrayList<String> nodeIdList = new ArrayList<>();
        HashMap<String, JsonObject> schJsonBodyMap = new HashMap<>();

        for (int i = 0; i < actions.size(); i++) {
            final String nodeId = actions.get(i).getNodeId();
            if (!nodeIdList.contains(nodeId)) {
                nodeIdList.add(nodeId);
            }
        }

        JsonObject scheduleJson = new JsonObject();
        scheduleJson.addProperty(AppConstants.KEY_ID, schedule.getId());
        scheduleJson.addProperty(AppConstants.KEY_OPERATION, AppConstants.KEY_OPERATION_REMOVE);

        JsonArray scheduleArr = new JsonArray();
        scheduleArr.add(scheduleJson);
        JsonObject schedulesJson = new JsonObject();
        schedulesJson.add(AppConstants.KEY_SCHEDULES, scheduleArr);

        for (int i = 0; i < nodeIdList.size(); i++) {
            String nodeId = nodeIdList.get(i);
            EspNode espNode = espApp.nodeMap.get(nodeId);
            ArrayList<Service> services = espNode.getServices();
            String serviceName = "";
            for (Service service : services) {
                if (AppConstants.SERVICE_TYPE_SCHEDULE.equals(service.getType())) {
                    serviceName = service.getName();
                    break;
                }
            }

            if (TextUtils.isEmpty(serviceName)) {
                serviceName = AppConstants.KEY_SCHEDULE;
            }
            JsonObject serviceJson = new JsonObject();
            serviceJson.add(serviceName, schedulesJson);

            schJsonBodyMap.put(nodeId, serviceJson);
        }

        updateScheduleRequest(schJsonBodyMap, new ApiResponseListener() {

            @Override
            public void onSuccess(Bundle data) {

                if (data != null) {
                    String jsonResponse = data.getString(AppConstants.KEY_RESPONSE);

                    if (jsonResponse.contains(AppConstants.KEY_FAILURE_RESPONSE)) {
                        String deviceNames = Utils.processScheduleResponse(schedule, jsonResponse, schJsonBodyMap.size());

                        if (!TextUtils.isEmpty(deviceNames)) {
                            String msg = getString(R.string.error_schedule_remove_partial) + " " + deviceNames;
                            Toast.makeText(ScheduleDetailActivity.this, msg, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_remove, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(ScheduleDetailActivity.this, R.string.msg_schedule_removed, Toast.LENGTH_LONG).show();
                    }
                }
                hideRemoveScheduleLoading();
                finish();
            }

            @Override
            public void onResponseFailure(Exception exception) {
                Log.e(TAG, "Failed to remove schedule for few devices");
                exception.printStackTrace();
                Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_remove, Toast.LENGTH_LONG).show();
                hideRemoveScheduleLoading();
            }

            @Override
            public void onNetworkFailure(Exception exception) {
                Log.e(TAG, "Failed to remove schedule for few devices");
                exception.printStackTrace();
                Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_remove, Toast.LENGTH_LONG).show();
                hideRemoveScheduleLoading();
            }
        });
    }

    private void gotoActionsScreen() {
        Intent intent = new Intent(this, ScheduleActionsActivity.class);
        intent.putParcelableArrayListExtra(AppConstants.KEY_DEVICES, devices);
        intent.putParcelableArrayListExtra(AppConstants.KEY_SELECTED_DEVICES, selectedDevices);
        startActivityForResult(intent, REQ_CODE_ACTIONS);
    }

    private void setRepeatDaysText() {

        char[] daysChar = days.toString().toCharArray();

        if (daysChar.length >= 7) {

            updateDayText(daysChar[7], day1);
            updateDayText(daysChar[6], day2);
            updateDayText(daysChar[5], day3);
            updateDayText(daysChar[4], day4);
            updateDayText(daysChar[3], day5);
            updateDayText(daysChar[2], day6);
            updateDayText(daysChar[1], day7);
        }
    }

    private void setActionDevicesNames() {

        String deviceNames = null;

        for (int i = 0; i < selectedDevices.size(); i++) {

            String deviceName = selectedDevices.get(i).getUserVisibleName();

            if (deviceNames == null) {
                deviceNames = deviceName;
            } else {
                deviceNames = deviceNames + ", " + deviceName;
            }
        }

        if (TextUtils.isEmpty(deviceNames)) {
            tvActionDevices.setVisibility(View.GONE);
        } else {
            tvActionDevices.setVisibility(View.VISIBLE);
            tvActionDevices.setText(deviceNames);
        }
    }

    private HashMap<String, String> prepareActionMap() {

        HashMap<String, JsonObject> nodeJsonActionsMap = new HashMap<String, JsonObject>();

        for (int i = 0; i < selectedDevices.size(); i++) {

            Device device = selectedDevices.get(i);
            JsonObject actionJsonBody = null;

            actionJsonBody = nodeJsonActionsMap.get(device.getNodeId());

            if (actionJsonBody == null) {
                actionJsonBody = new JsonObject();
            }
            JsonObject jsonParam = new JsonObject();
            ArrayList<Param> params = device.getParams();

            for (int j = 0; j < params.size(); j++) {

                Param param = params.get(j);

                if (param.isSelected()) {

                    String dataType = param.getDataType();

                    if (AppConstants.UI_TYPE_SLIDER.equalsIgnoreCase(param.getUiType())) {

                        if (dataType.equalsIgnoreCase("int")
                                || dataType.equalsIgnoreCase("integer")) {

                            int max = param.getMaxBounds();
                            int min = param.getMinBounds();

                            if ((min < max)) {
                                int value = (int) param.getValue();
                                jsonParam.addProperty(param.getName(), value);
                            } else {

                                int value = Integer.parseInt(param.getLabelValue());
                                jsonParam.addProperty(param.getName(), value);
                            }
                        } else if (dataType.equalsIgnoreCase("float")
                                || dataType.equalsIgnoreCase("double")) {

                            int max = param.getMaxBounds();
                            int min = param.getMinBounds();

                            if ((min < max)) {
                                jsonParam.addProperty(param.getName(), param.getValue());
                            } else {

                                float value = Float.parseFloat(param.getLabelValue());
                                jsonParam.addProperty(param.getName(), value);
                            }
                        }
                    } else if (AppConstants.UI_TYPE_TRIGGER.equalsIgnoreCase(param.getUiType())
                            && dataType.equalsIgnoreCase("bool")
                            || dataType.equalsIgnoreCase("boolean")) {

                        jsonParam.addProperty(param.getName(), true);

                    } else {
                        if (dataType.equalsIgnoreCase("bool")
                                || dataType.equalsIgnoreCase("boolean")) {

                            jsonParam.addProperty(param.getName(), param.getSwitchStatus());

                        } else if (dataType.equalsIgnoreCase("int")
                                || dataType.equalsIgnoreCase("integer")) {

                            int value = (int) param.getValue();
                            jsonParam.addProperty(param.getName(), value);

                        } else if (dataType.equalsIgnoreCase("float")
                                || dataType.equalsIgnoreCase("double")) {

                            jsonParam.addProperty(param.getName(), param.getValue());

                        } else if (dataType.equalsIgnoreCase("string")) {

                            jsonParam.addProperty(param.getName(), param.getLabelValue());
                        }
                    }
                }
            }
            actionJsonBody.add(device.getDeviceName(), jsonParam);
            nodeJsonActionsMap.put(device.getNodeId(), actionJsonBody);
        }

        HashMap<String, String> nodeActionsMap = new HashMap<>();

        for (Map.Entry<String, JsonObject> entry : nodeJsonActionsMap.entrySet()) {

            String nodeId = entry.getKey();
            JsonObject action = entry.getValue();
            nodeActionsMap.put(nodeId, action.toString());
        }
        return nodeActionsMap;
    }

    private void saveSchedule() {

        // Schedule id
        String id = generateScheduleId();
        while (isScheduleIdExist(id)) {
            id = generateScheduleId();
        }

        // Name
        if (TextUtils.isEmpty(scheduleName)) {
            Toast.makeText(this, R.string.error_schedule_name_empty, Toast.LENGTH_LONG).show();
            return;
        }

        if (schedule == null) {
            schedule = new Schedule();
            schedule.setId(id);
        }
        schedule.setName(scheduleName);

        prepareScheduleJsonAndUpdate();
    }

    private void prepareScheduleJsonAndUpdate() {

        JsonObject scheduleJson = new JsonObject();
        scheduleJson.addProperty(AppConstants.KEY_OPERATION, operation);

        // Time
        int hour = timePicker.getHour();
        int min = timePicker.getMinute();
        int minValue = min + hour * 60;

        // Repeat
        String daysStr = days.toString();
        int daysValue = Integer.parseInt(daysStr, 2);

        // Schedule JSON
        scheduleJson.addProperty(AppConstants.KEY_ID, schedule.getId());
        scheduleJson.addProperty(AppConstants.KEY_NAME, scheduleName);

        JsonObject jsonTrigger = new JsonObject();
        jsonTrigger.addProperty(AppConstants.KEY_DAYS, daysValue);
        jsonTrigger.addProperty(AppConstants.KEY_MINUTES, minValue);

        JsonArray triggerArr = new JsonArray();
        triggerArr.add(jsonTrigger);
        scheduleJson.add(AppConstants.KEY_TRIGGERS, triggerArr);

        HashMap<String, String> actionMap = prepareActionMap();

        if (operation.equals(AppConstants.KEY_OPERATION_ADD) && actionMap.size() == 0) {
            Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_action, Toast.LENGTH_LONG).show();
            return;
        }

        String progressMsg = "";
        if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
            progressMsg = getString(R.string.progress_add_sch);
        } else {
            progressMsg = getString(R.string.progress_update_sch);
        }
        showAddScheduleLoading(progressMsg);

        ArrayList<String> removedNodeIds = new ArrayList<>();
        for (int i = 0; i < selectedNodeIds.size(); i++) {
            String preSelectedNodeId = selectedNodeIds.get(i);
            if (!actionMap.containsKey(preSelectedNodeId)) {
                removedNodeIds.add(preSelectedNodeId);
            }
        }

        HashMap<String, JsonObject> schJsonBodyMap = new HashMap<>();

        if (actionMap.size() > 0) {

            for (Map.Entry<String, String> entry : actionMap.entrySet()) {

                String nodeId = entry.getKey();
                String actionStr = entry.getValue();
                Gson gson = new Gson();
                JsonObject actionsJson = gson.fromJson(actionStr, JsonObject.class);

                JsonObject schJson = new Gson().fromJson(scheduleJson.toString(), JsonObject.class);
                schJson.add(AppConstants.KEY_ACTION, actionsJson);

                JsonArray schArr = new JsonArray();
                schArr.add(schJson);

                JsonObject schedulesJson = new JsonObject();
                schedulesJson.add(AppConstants.KEY_SCHEDULES, schArr);

                EspNode espNode = espApp.nodeMap.get(nodeId);
                ArrayList<Service> services = espNode.getServices();
                String serviceName = "";
                for (Service service : services) {
                    if (AppConstants.SERVICE_TYPE_SCHEDULE.equals(service.getType())) {
                        serviceName = service.getName();
                        break;
                    }
                }

                if (TextUtils.isEmpty(serviceName)) {
                    serviceName = AppConstants.KEY_SCHEDULE;
                }
                JsonObject serviceJson = new JsonObject();
                serviceJson.add(serviceName, schedulesJson);

                schJsonBodyMap.put(nodeId, serviceJson);
            }
        }

        if (removedNodeIds.size() > 0) {

            scheduleJson.addProperty(AppConstants.KEY_ID, schedule.getId());
            scheduleJson.addProperty(AppConstants.KEY_OPERATION, AppConstants.KEY_OPERATION_REMOVE);

            JsonArray schArr = new JsonArray();
            schArr.add(scheduleJson);

            JsonObject schedulesJson = new JsonObject();
            schedulesJson.add(AppConstants.KEY_SCHEDULES, schArr);

            for (int i = 0; i < removedNodeIds.size(); i++) {

                final String nodeId = removedNodeIds.get(i);
                EspNode espNode = espApp.nodeMap.get(nodeId);
                ArrayList<Service> services = espNode.getServices();
                String serviceName = "";
                for (Service service : services) {
                    if (AppConstants.SERVICE_TYPE_SCHEDULE.equals(service.getType())) {
                        serviceName = service.getName();
                        break;
                    }
                }

                if (TextUtils.isEmpty(serviceName)) {
                    serviceName = AppConstants.KEY_SCHEDULE;
                }
                JsonObject serviceJson = new JsonObject();
                serviceJson.add(serviceName, schedulesJson);

                schJsonBodyMap.put(nodeId, serviceJson);
            }
        }

        updateScheduleRequest(schJsonBodyMap, new ApiResponseListener() {

            @Override
            public void onSuccess(Bundle data) {

                if (data != null) {
                    String jsonResponse = data.getString(AppConstants.KEY_RESPONSE);

                    if (jsonResponse.contains(AppConstants.KEY_FAILURE_RESPONSE)) {
                        String deviceNames = Utils.processScheduleResponse(schedule, jsonResponse, schJsonBodyMap.size());

                        if (!TextUtils.isEmpty(deviceNames)) {
                            String msg = "";
                            if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                                msg = getString(R.string.error_schedule_add_partial) + " " + deviceNames;
                            } else {
                                msg = getString(R.string.error_schedule_save_partial) + " " + deviceNames;
                            }
                            Toast.makeText(ScheduleDetailActivity.this, msg, Toast.LENGTH_LONG).show();
                        } else {
                            if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                                Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_add, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_save, Toast.LENGTH_LONG).show();
                            }
                        }
                        hideAddScheduleLoading();
                        finish();

                    } else {

                        schJsonBodyMap.clear();
                        JsonObject scheduleJson = new JsonObject();
                        scheduleJson.addProperty(AppConstants.KEY_ID, schedule.getId());
                        scheduleJson.addProperty(AppConstants.KEY_OPERATION, AppConstants.KEY_OPERATION_ENABLE);

                        JsonArray schArr = new JsonArray();
                        schArr.add(scheduleJson);

                        JsonObject finalBody = new JsonObject();
                        finalBody.add(AppConstants.KEY_SCHEDULES, schArr);

                        for (Map.Entry<String, String> entry : actionMap.entrySet()) {

                            String nodeId = entry.getKey();
                            EspNode espNode = espApp.nodeMap.get(nodeId);
                            ArrayList<Service> services = espNode.getServices();
                            String serviceName = "";
                            for (Service s : services) {
                                if (AppConstants.SERVICE_TYPE_SCHEDULE.equals(s.getType())) {
                                    serviceName = s.getName();
                                    break;
                                }
                            }
                            if (TextUtils.isEmpty(serviceName)) {
                                serviceName = AppConstants.KEY_SCHEDULE;
                            }

                            JsonObject body = new JsonObject();
                            body.add(serviceName, finalBody);
                            schJsonBodyMap.put(nodeId, body);
                        }

                        if (schJsonBodyMap.size() > 0) {
                            updateScheduleRequest(schJsonBodyMap, new ApiResponseListener() {

                                @Override
                                public void onSuccess(Bundle data) {
                                    if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                                        Toast.makeText(ScheduleDetailActivity.this, R.string.msg_schedule_added, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(ScheduleDetailActivity.this, R.string.msg_schedule_updated, Toast.LENGTH_LONG).show();
                                    }
                                    hideAddScheduleLoading();
                                    finish();
                                }

                                @Override
                                public void onResponseFailure(Exception exception) {
                                    Log.e(TAG, "Failed to save schedule for few devices");
                                    exception.printStackTrace();
                                    if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                                        Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_add, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_save, Toast.LENGTH_LONG).show();
                                    }
                                    hideAddScheduleLoading();
                                }

                                @Override
                                public void onNetworkFailure(Exception exception) {
                                    Log.e(TAG, "Failed to save schedule for few devices");
                                    exception.printStackTrace();
                                    if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                                        Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_add, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_save, Toast.LENGTH_LONG).show();
                                    }
                                    hideAddScheduleLoading();
                                }
                            });
                        } else {
                            if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                                Toast.makeText(ScheduleDetailActivity.this, R.string.msg_schedule_added, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ScheduleDetailActivity.this, R.string.msg_schedule_updated, Toast.LENGTH_LONG).show();
                            }
                            hideAddScheduleLoading();
                            finish();
                        }
                    }
                }
            }

            @Override
            public void onResponseFailure(Exception exception) {
                Log.e(TAG, "Failed to save schedule for few devices");
                exception.printStackTrace();
                Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_save, Toast.LENGTH_LONG).show();
                hideAddScheduleLoading();
            }

            @Override
            public void onNetworkFailure(Exception exception) {
                Log.e(TAG, "Failed to save schedule for few devices");
                exception.printStackTrace();
                Toast.makeText(ScheduleDetailActivity.this, R.string.error_schedule_save, Toast.LENGTH_LONG).show();
                hideAddScheduleLoading();
            }
        });
    }

    private void updateScheduleRequest(HashMap<String, JsonObject> scheduleJsonBodyMap, ApiResponseListener listener) {
        apiManager.updateParamsForMultiNode(scheduleJsonBodyMap, new ApiResponseListener() {

            @Override
            public void onSuccess(Bundle data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSuccess(data);
                    }
                });
            }

            @Override
            public void onResponseFailure(Exception exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onResponseFailure(exception);
                    }
                });
            }

            @Override
            public void onNetworkFailure(Exception exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onNetworkFailure(exception);
                    }
                });
            }
        });
    }

    private String generateScheduleId() {

        Random random = new Random();
        char[] alphabet = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
                'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
        int size = 4;
        String id = NanoIdUtils.randomNanoId(random, alphabet, size);

        return id;
    }

    private boolean isScheduleIdExist(String schId) {

        boolean isExist = false;

        for (Map.Entry<String, Schedule> entry : espApp.scheduleMap.entrySet()) {

            String key = entry.getKey();
            Schedule schedule = entry.getValue();

            if (schedule != null) {

                if (schedule.getId().equals(schId)) {
                    isExist = true;
                    break;
                }
            }
        }
        return isExist;
    }

    private void showAddScheduleLoading(String msg) {
        rlAddSch.setAlpha(0.3f);
        rlSchProgress.setVisibility(View.VISIBLE);
        TextView progressText = findViewById(R.id.tv_loading_sch);
        progressText.setText(msg);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void hideAddScheduleLoading() {
        rlAddSch.setAlpha(1);
        rlSchProgress.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void showRemoveScheduleLoading() {

        btnRemoveSchedule.setEnabled(false);
        btnRemoveSchedule.setAlpha(0.5f);
        txtRemoveScheduleBtn.setText(R.string.btn_removing);
        progressBar.setVisibility(View.VISIBLE);
        removeScheduleImage.setVisibility(View.GONE);
    }

    public void hideRemoveScheduleLoading() {

        btnRemoveSchedule.setEnabled(true);
        btnRemoveSchedule.setAlpha(1f);
        txtRemoveScheduleBtn.setText(R.string.btn_remove);
        progressBar.setVisibility(View.GONE);
        removeScheduleImage.setVisibility(View.VISIBLE);
    }
}
