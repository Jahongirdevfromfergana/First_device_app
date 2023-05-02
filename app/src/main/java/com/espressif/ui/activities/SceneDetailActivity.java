// Copyright 2022 Espressif Systems (Shanghai) PTE LTD
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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
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
import com.espressif.ui.models.Scene;
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

public class SceneDetailActivity extends AppCompatActivity {

    private final String TAG = SceneDetailActivity.class.getSimpleName();

    public static final int REQ_CODE_ACTIONS = 10;

    private RelativeLayout rlSceneName, rlActions, rlDescription;
    private TextView tvSceneName, tvActionDevices;
    private AppCompatEditText etDescription;
    private MaterialCardView btnRemoveScene;
    private TextView txtRemoveSceneBtn;
    private ImageView removeSceneImage;
    private ContentLoadingProgressBar progressBar;
    private RelativeLayout rlSceneProgress, rlAddScene;
    private MenuItem menuSave;

    private String sceneName = "", sceneDescription = "";
    private EspApplication espApp;
    private ApiManager apiManager;
    private ArrayList<Device> devices;
    private ArrayList<Device> selectedDevices;
    private ArrayList<String> selectedNodeIds;
    private Scene scene;
    private String operation = AppConstants.KEY_OPERATION_ADD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_detail);

        espApp = (EspApplication) getApplicationContext();
        apiManager = ApiManager.getInstance(this);
        devices = new ArrayList<>();
        selectedNodeIds = new ArrayList<>();
        scene = getIntent().getParcelableExtra(AppConstants.KEY_SCENE);
        sceneName = getIntent().getStringExtra(AppConstants.KEY_NAME);
        selectedDevices = new ArrayList<>();

        for (Map.Entry<String, EspNode> entry : espApp.nodeMap.entrySet()) {

            String key = entry.getKey();
            EspNode node = entry.getValue();
            ArrayList<Service> services = node.getServices();

            if (node != null) {

                for (int i = 0; i < services.size(); i++) {

                    Service s = services.get(i);
                    if (!TextUtils.isEmpty(s.getType()) && s.getType().equals(AppConstants.SERVICE_TYPE_SCENES)) {

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
                saveScene();
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
        getSupportActionBar().setTitle(R.string.title_activity_scene_details);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnRemoveScene = findViewById(R.id.btn_remove_scene);
        txtRemoveSceneBtn = findViewById(R.id.text_btn);
        removeSceneImage = findViewById(R.id.iv_remove);
        progressBar = findViewById(R.id.progress_indicator);

        rlSceneName = findViewById(R.id.rl_scene_name);
        rlActions = findViewById(R.id.rl_actions);
        rlDescription = findViewById(R.id.rl_info);

        rlAddScene = findViewById(R.id.rl_add_scene);
        rlSceneProgress = findViewById(R.id.rl_progress_add_scene);

        tvSceneName = findViewById(R.id.tv_scene_name);
        tvActionDevices = findViewById(R.id.tv_action_device_list);
        etDescription = findViewById(R.id.et_description);
        etDescription.setHorizontallyScrolling(false);
        etDescription.setMaxLines(10);

        tvActionDevices.setVisibility(View.GONE);
        btnRemoveScene.setVisibility(View.GONE);

        if (scene != null) {

            operation = AppConstants.KEY_OPERATION_EDIT;
            sceneName = scene.getName();
            sceneDescription = scene.getInfo();
            getSupportActionBar().setTitle(R.string.title_activity_scene_details);
            btnRemoveScene.setVisibility(View.VISIBLE);

            if (!TextUtils.isEmpty(sceneDescription)) {
                etDescription.setText(sceneDescription);
            }

            ArrayList<Action> actions = scene.getActions();
            for (int i = 0; i < actions.size(); i++) {
                Device device = actions.get(i).getDevice();
                String nodeId = device.getNodeId();
                selectedDevices.add(device);
                if (!selectedNodeIds.contains(nodeId)) {
                    selectedNodeIds.add(nodeId);
                }
            }
            setActionDevicesNames();

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

        if (TextUtils.isEmpty(sceneName)) {
            tvSceneName.setText(R.string.not_set);
        } else {
            tvSceneName.setText(sceneName);
        }

        rlSceneName.setOnClickListener(sceneNameClickListener);
        rlDescription.setOnClickListener(sceneDesClickListener);
        rlActions.setOnClickListener(actionsClickListener);
        btnRemoveScene.setOnClickListener(removeSceneBtnClickListener);
    }

    private View.OnClickListener sceneNameClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            askForSceneName();
        }
    };

    private View.OnClickListener sceneDesClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            etDescription.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etDescription, InputMethodManager.SHOW_IMPLICIT);
        }
    };

    private View.OnClickListener actionsClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            gotoActionsScreen();
        }
    };

    private View.OnClickListener removeSceneBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            confirmForRemoveScene();
        }
    };

    private void askForSceneName() {

        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_attribute, null);
        final EditText etSceneName = dialogView.findViewById(R.id.et_attr_value);
        etSceneName.setInputType(InputType.TYPE_CLASS_TEXT);
        etSceneName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle(R.string.dialog_title_add_name)
                .setPositiveButton(R.string.btn_ok, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
        etSceneName.setText(sceneName);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button buttonPositive = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String value = etSceneName.getText().toString();
                        if (!TextUtils.isEmpty(value)) {
                            sceneName = value;
                            tvSceneName.setText(sceneName);
                            dialog.dismiss();
                        } else {
                            etSceneName.setError(getString(R.string.error_invalid_scene_name));
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

    private void confirmForRemoveScene() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title_remove);
        builder.setMessage(R.string.dialog_msg_confirmation);

        // Set up the buttons
        builder.setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                removeScene();
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

    private void setActionDevicesNames() {

        StringBuilder deviceNames = null;

        for (int i = 0; i < selectedDevices.size(); i++) {

            String deviceName = selectedDevices.get(i).getUserVisibleName();

            if (deviceNames == null) {
                deviceNames = new StringBuilder();
            } else {
                deviceNames.append(", ");
            }
            deviceNames.append(deviceName);
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

    private void removeScene() {

        if (scene == null) {
            Log.e(TAG, "Scene is null");
            return;
        }
        showRemoveSceneLoading();
        ArrayList<Action> actions = scene.getActions();
        ArrayList<String> nodeIdList = new ArrayList<>();
        HashMap<String, JsonObject> sceneJsonBodyMap = new HashMap<>();

        for (int i = 0; i < actions.size(); i++) {
            final String nodeId = actions.get(i).getNodeId();
            if (!nodeIdList.contains(nodeId)) {
                nodeIdList.add(nodeId);
            }
        }

        JsonObject sceneJson = new JsonObject();
        sceneJson.addProperty(AppConstants.KEY_ID, scene.getId());
        sceneJson.addProperty(AppConstants.KEY_OPERATION, AppConstants.KEY_OPERATION_REMOVE);

        JsonArray sceneArr = new JsonArray();
        sceneArr.add(sceneJson);
        JsonObject scenesJson = new JsonObject();
        scenesJson.add(AppConstants.KEY_SCENES, sceneArr);

        for (int i = 0; i < nodeIdList.size(); i++) {

            final String nodeId = nodeIdList.get(i);

            EspNode espNode = espApp.nodeMap.get(nodeId);
            ArrayList<Service> services = espNode.getServices();
            String serviceName = "";
            for (Service service : services) {
                if (AppConstants.SERVICE_TYPE_SCENES.equals(service.getType())) {
                    serviceName = service.getName();
                    break;
                }
            }

            if (TextUtils.isEmpty(serviceName)) {
                serviceName = AppConstants.KEY_SCENES;
            }
            JsonObject serviceJson = new JsonObject();
            serviceJson.add(serviceName, scenesJson);

            sceneJsonBodyMap.put(nodeId, serviceJson);
        }

        updateSceneRequest(sceneJsonBodyMap, new ApiResponseListener() {

            @Override
            public void onSuccess(Bundle data) {

                if (data != null) {
                    String jsonResponse = data.getString(AppConstants.KEY_RESPONSE);

                    if (jsonResponse.contains(AppConstants.KEY_FAILURE_RESPONSE)) {
                        String deviceNames = Utils.processSceneResponse(scene, jsonResponse, sceneJsonBodyMap.size());

                        if (!TextUtils.isEmpty(deviceNames)) {
                            String msg = getString(R.string.error_scene_remove_partial) + " " + deviceNames;
                            Toast.makeText(SceneDetailActivity.this, msg, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SceneDetailActivity.this, R.string.error_scene_remove, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(SceneDetailActivity.this, R.string.msg_scene_removed, Toast.LENGTH_LONG).show();
                    }
                }
                hideRemoveSceneLoading();
                finish();
            }

            @Override
            public void onResponseFailure(Exception exception) {
                Log.e(TAG, "Failed to remove scene for few devices");
                exception.printStackTrace();
                hideRemoveSceneLoading();
                Toast.makeText(SceneDetailActivity.this, R.string.error_scene_remove, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNetworkFailure(Exception exception) {
                Log.e(TAG, "Failed to remove scene for few devices");
                exception.printStackTrace();
                hideRemoveSceneLoading();
                Toast.makeText(SceneDetailActivity.this, R.string.error_scene_remove, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveScene() {

        // Scene id
        String id = generateSceneId();
        while (isSceneIdExist(id)) {
            id = generateSceneId();
        }

        // Name
        if (TextUtils.isEmpty(sceneName)) {
            Toast.makeText(this, R.string.error_scene_name_empty, Toast.LENGTH_LONG).show();
            return;
        }

        if (scene == null) {
            scene = new Scene();
            scene.setId(id);
        }

        scene.setName(sceneName);
        sceneDescription = etDescription.getText().toString();
        scene.setInfo(sceneDescription);

        prepareSceneJsonAndUpdate();
    }

    private void prepareSceneJsonAndUpdate() {

        JsonObject sceneJson = new JsonObject();
        sceneJson.addProperty(AppConstants.KEY_OPERATION, operation);

        // Scene JSON
        sceneJson.addProperty(AppConstants.KEY_ID, scene.getId());
        sceneJson.addProperty(AppConstants.KEY_NAME, sceneName);
        sceneJson.addProperty(AppConstants.KEY_INFO, sceneDescription);

        HashMap<String, String> actionMap = prepareActionMap();

        if (operation.equals(AppConstants.KEY_OPERATION_ADD) && actionMap.size() == 0) {
            Toast.makeText(SceneDetailActivity.this, R.string.error_scene_action, Toast.LENGTH_LONG).show();
            return;
        }

        String progressMsg = "";
        if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
            progressMsg = getString(R.string.progress_add_scene);
        } else {
            progressMsg = getString(R.string.progress_update_scene);
        }
        showAddSceneLoading(progressMsg);

        ArrayList<String> removedNodeIds = new ArrayList<>();
        for (int i = 0; i < selectedNodeIds.size(); i++) {
            String preSelectedNodeId = selectedNodeIds.get(i);
            if (!actionMap.containsKey(preSelectedNodeId)) {
                removedNodeIds.add(preSelectedNodeId);
            }
        }

        HashMap<String, JsonObject> sceneJsonBodyMap = new HashMap<>();

        if (actionMap.size() > 0) {

            for (Map.Entry<String, String> entry : actionMap.entrySet()) {

                String nodeId = entry.getKey();
                String actionStr = entry.getValue();
                Gson gson = new Gson();
                JsonObject actionsJson = gson.fromJson(actionStr, JsonObject.class);

                JsonObject scJson = new Gson().fromJson(sceneJson.toString(), JsonObject.class);
                scJson.add(AppConstants.KEY_ACTION, actionsJson);

                JsonArray scArr = new JsonArray();
                scArr.add(scJson);

                JsonObject scenesJson = new JsonObject();
                scenesJson.add(AppConstants.KEY_SCENES, scArr);

                EspNode espNode = espApp.nodeMap.get(nodeId);
                ArrayList<Service> services = espNode.getServices();
                String serviceName = "";
                for (Service service : services) {
                    if (AppConstants.SERVICE_TYPE_SCENES.equals(service.getType())) {
                        serviceName = service.getName();
                        break;
                    }
                }

                if (TextUtils.isEmpty(serviceName)) {
                    serviceName = AppConstants.KEY_SCENES;
                }
                JsonObject serviceJson = new JsonObject();
                serviceJson.add(serviceName, scenesJson);

                sceneJsonBodyMap.put(nodeId, serviceJson);
            }
        }

        if (removedNodeIds.size() > 0) {

            sceneJson.addProperty(AppConstants.KEY_ID, scene.getId());
            sceneJson.addProperty(AppConstants.KEY_OPERATION, AppConstants.KEY_OPERATION_REMOVE);

            JsonArray sceneArr = new JsonArray();
            sceneArr.add(sceneJson);

            JsonObject scenesJson = new JsonObject();
            scenesJson.add(AppConstants.KEY_SCENES, sceneArr);

            for (int i = 0; i < removedNodeIds.size(); i++) {

                final String nodeId = removedNodeIds.get(i);

                EspNode espNode = espApp.nodeMap.get(nodeId);
                ArrayList<Service> services = espNode.getServices();
                String serviceName = "";
                for (Service service : services) {
                    if (AppConstants.SERVICE_TYPE_SCENES.equals(service.getType())) {
                        serviceName = service.getName();
                        break;
                    }
                }

                if (TextUtils.isEmpty(serviceName)) {
                    serviceName = AppConstants.KEY_SCENES;
                }
                JsonObject serviceJson = new JsonObject();
                serviceJson.add(serviceName, scenesJson);

                sceneJsonBodyMap.put(nodeId, serviceJson);
            }
        }

        updateSceneRequest(sceneJsonBodyMap, new ApiResponseListener() {

            @Override
            public void onSuccess(Bundle data) {

                if (data != null) {
                    String jsonResponse = data.getString(AppConstants.KEY_RESPONSE);

                    if (jsonResponse.contains(AppConstants.KEY_FAILURE_RESPONSE)) {
                        String deviceNames = Utils.processSceneResponse(scene, jsonResponse, sceneJsonBodyMap.size());

                        if (!TextUtils.isEmpty(deviceNames)) {
                            String msg = "";
                            if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                                msg = getString(R.string.error_scene_add_partial) + " " + deviceNames;
                            } else {
                                msg = getString(R.string.error_scene_save_partial) + " " + deviceNames;
                            }
                            Toast.makeText(SceneDetailActivity.this, msg, Toast.LENGTH_LONG).show();
                        } else {
                            if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                                Toast.makeText(SceneDetailActivity.this, R.string.error_scene_add, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(SceneDetailActivity.this, R.string.error_scene_save, Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                            Toast.makeText(SceneDetailActivity.this, R.string.msg_scene_added, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SceneDetailActivity.this, R.string.msg_scene_updated, Toast.LENGTH_LONG).show();
                        }
                    }
                }
                hideAddSceneLoading();
                finish();
            }

            @Override
            public void onResponseFailure(Exception exception) {
                Log.e(TAG, "Failed to save scene.");
                exception.printStackTrace();
                if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                    Toast.makeText(SceneDetailActivity.this, R.string.error_scene_add, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SceneDetailActivity.this, R.string.error_scene_save, Toast.LENGTH_LONG).show();
                }
                hideAddSceneLoading();
            }

            @Override
            public void onNetworkFailure(Exception exception) {
                Log.e(TAG, "Failed to save scene due to network failure");
                exception.printStackTrace();
                if (operation.equals(AppConstants.KEY_OPERATION_ADD)) {
                    Toast.makeText(SceneDetailActivity.this, R.string.error_scene_add, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SceneDetailActivity.this, R.string.error_scene_save, Toast.LENGTH_LONG).show();
                }
                hideAddSceneLoading();
            }
        });
    }

    private void updateSceneRequest(HashMap<String, JsonObject> sceneJsonBodyMap, ApiResponseListener listener) {
        apiManager.updateParamsForMultiNode(sceneJsonBodyMap, new ApiResponseListener() {

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

    private String generateSceneId() {

        Random random = new Random();
        char[] alphabet = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
                'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
        int size = 4;
        String id = NanoIdUtils.randomNanoId(random, alphabet, size);

        return id;
    }

    private boolean isSceneIdExist(String schId) {

        boolean isExist = false;

        for (Map.Entry<String, Scene> entry : espApp.sceneMap.entrySet()) {

            String key = entry.getKey();
            Scene scene = entry.getValue();

            if (scene != null) {

                if (scene.getId().equals(schId)) {
                    isExist = true;
                    break;
                }
            }
        }
        return isExist;
    }

    private void gotoActionsScreen() {
        Intent intent = new Intent(this, SceneActionsActivity.class);
        intent.putParcelableArrayListExtra(AppConstants.KEY_DEVICES, devices);
        intent.putParcelableArrayListExtra(AppConstants.KEY_SELECTED_DEVICES, selectedDevices);
        startActivityForResult(intent, REQ_CODE_ACTIONS);
    }

    private void showAddSceneLoading(String msg) {
        rlAddScene.setAlpha(0.3f);
        rlSceneProgress.setVisibility(View.VISIBLE);
        TextView progressText = findViewById(R.id.tv_loading_scene);
        progressText.setText(msg);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void hideAddSceneLoading() {
        rlAddScene.setAlpha(1);
        rlSceneProgress.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void showRemoveSceneLoading() {

        btnRemoveScene.setEnabled(false);
        btnRemoveScene.setAlpha(0.5f);
        txtRemoveSceneBtn.setText(R.string.btn_removing);
        progressBar.setVisibility(View.VISIBLE);
        removeSceneImage.setVisibility(View.GONE);
    }

    public void hideRemoveSceneLoading() {

        btnRemoveScene.setEnabled(true);
        btnRemoveScene.setAlpha(1f);
        txtRemoveSceneBtn.setText(R.string.btn_remove);
        progressBar.setVisibility(View.GONE);
        removeSceneImage.setVisibility(View.VISIBLE);
    }
}
