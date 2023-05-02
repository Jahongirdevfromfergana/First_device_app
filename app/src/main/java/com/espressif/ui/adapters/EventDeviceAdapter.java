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

package com.espressif.ui.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.AppConstants;
import com.espressif.rainmaker.R;
import com.espressif.ui.EventSelectionListener;
import com.espressif.ui.fragments.ParamSelectionFragment;
import com.espressif.ui.models.Automation;
import com.espressif.ui.models.Device;

import java.util.ArrayList;

public class EventDeviceAdapter extends RecyclerView.Adapter<EventDeviceAdapter.EventDeviceViewHolder> {

    private AppCompatActivity context;
    private ArrayList<Device> deviceList;
    private Automation automation;
    private EventSelectionListener eventSelectionListener;

    public EventDeviceAdapter(AppCompatActivity context, Automation automation,
                              ArrayList<Device> list, EventSelectionListener listener) {
        this.context = context;
        this.automation = automation;
        this.deviceList = list;
        this.eventSelectionListener = listener;
    }

    @Override
    public EventDeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_device, parent, false);
        return new EventDeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final EventDeviceViewHolder holder, int position) {

        Device device = deviceList.get(position);
        holder.tvDeviceName.setText(device.getUserVisibleName());

        if (TextUtils.isEmpty(device.getDeviceType())) {
            holder.ivDevice.setImageResource(R.drawable.ic_device);
        } else {
            if (AppConstants.ESP_DEVICE_BULB.equals(device.getDeviceType())) {
                holder.ivDevice.setImageResource(R.drawable.ic_device_bulb);
            } else if (AppConstants.ESP_DEVICE_BULB_CCT.equals(device.getDeviceType())) {
                holder.ivDevice.setImageResource(R.drawable.ic_device_bulb_cct);
            } else if (AppConstants.ESP_DEVICE_BULB_RGB.equals(device.getDeviceType())) {
                holder.ivDevice.setImageResource(R.drawable.ic_device_bulb_rgb);
            } else if (AppConstants.ESP_DEVICE_SWITCH.equals(device.getDeviceType())) {
                holder.ivDevice.setImageResource(R.drawable.ic_device_switch);
            } else if (AppConstants.ESP_DEVICE_LOCK.equals(device.getDeviceType())) {
                holder.ivDevice.setImageResource(R.drawable.ic_device_lock);
            } else if (AppConstants.ESP_DEVICE_THERMOSTAT.equals(device.getDeviceType())) {
                holder.ivDevice.setImageResource(R.drawable.ic_device_thermostat);
            } else if (AppConstants.ESP_DEVICE_FAN.equals(device.getDeviceType())) {
                holder.ivDevice.setImageResource(R.drawable.ic_device_fan);
            } else if (AppConstants.ESP_DEVICE_SENSOR.equals(device.getDeviceType())) {
                holder.ivDevice.setImageResource(R.drawable.ic_device);
            } else if (AppConstants.ESP_DEVICE_TEMP_SENSOR.equals(device.getDeviceType())) {
                holder.ivDevice.setImageResource(R.drawable.ic_device_temp_sensor);
            } else if (AppConstants.ESP_DEVICE_OUTLET.equals(device.getDeviceType())) {
                holder.ivDevice.setImageResource(R.drawable.ic_device_outlet);
            } else {
                holder.ivDevice.setImageResource(R.drawable.ic_device);
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Device d = deviceList.get(holder.getAdapterPosition());
                showBottomSheetDialog(d);
                notifyItemChanged(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList == null ? 0 : deviceList.size();
    }

    private void showBottomSheetDialog(Device selectedDevice) {

        ParamSelectionFragment paramSelectionFragment = new ParamSelectionFragment(context, automation, selectedDevice, eventSelectionListener);
        paramSelectionFragment.show(context.getSupportFragmentManager(), paramSelectionFragment.getTag());
    }

    static class EventDeviceViewHolder extends RecyclerView.ViewHolder {

        private TextView tvDeviceName;
        private ImageView ivDevice;

        public EventDeviceViewHolder(View itemView) {
            super(itemView);

            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            ivDevice = itemView.findViewById(R.id.iv_device);
        }
    }
}
