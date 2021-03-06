package org.itxtech.daedalus.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import org.itxtech.daedalus.Daedalus;
import org.itxtech.daedalus.R;
import org.itxtech.daedalus.activity.MainActivity;
import org.itxtech.daedalus.service.DaedalusVpnService;
import org.itxtech.daedalus.util.DnsServerHelper;

/**
 * Daedalus Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class MainFragment extends Fragment {

    private View view = null;
    private MainFragmentHandler mHandler = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = (new MainFragmentHandler()).setFragment(this);
        MainActivity.getInstance().setMainFragmentHandler(mHandler);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_main, container, false);

        Button but = (Button) view.findViewById(R.id.button_activate);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Daedalus.getInstance().isServiceActivated()) {
                    Daedalus.getInstance().deactivateService();
                } else {
                    activateService();
                }
            }
        });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mHandler.shutdown();
        MainActivity.getInstance().setMainFragmentHandler(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        view = null;
    }

    public void activateService() {
        Intent intent = VpnService.prepare(Daedalus.getInstance());
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, Activity.RESULT_OK, null);
        }

        long activateCounter = Daedalus.configurations.getActivateCounter();
        if (activateCounter == -1) {
            return;
        }
        activateCounter++;
        Daedalus.configurations.setActivateCounter(activateCounter);
        if (activateCounter % 20 == 0) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("觉得还不错？")
                    .setMessage("您的支持是我动力来源！\n请考虑为我买杯咖啡醒醒脑，甚至其他…… ;)")
                    .setPositiveButton("为我买杯咖啡", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Daedalus.donate();
                            new AlertDialog.Builder(getActivity())
                                    .setMessage("感谢您的支持！;)\n我会再接再厉！")
                                    .setPositiveButton("确认", null)
                                    .show();
                        }
                    })
                    .setNeutralButton("不再显示", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Daedalus.configurations.setActivateCounter(-1);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    public void onActivityResult(int request, int result, Intent data) {
        if (result == Activity.RESULT_OK) {
            DaedalusVpnService.primaryServer = DnsServerHelper.getAddressById(DnsServerHelper.getPrimary());
            DaedalusVpnService.secondaryServer = DnsServerHelper.getAddressById(DnsServerHelper.getSecondary());

            Daedalus.getInstance().startService(Daedalus.getInstance().getServiceIntent().setAction(DaedalusVpnService.ACTION_ACTIVATE));

            Button button = (Button) view.findViewById(R.id.button_activate);
            button.setText(R.string.button_text_deactivate);

            Daedalus.updateShortcut(Daedalus.getInstance());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserInterface();
    }

    private void updateUserInterface() {
        Log.d("DMainFragment", "updateInterface");
        Button but = (Button) view.findViewById(R.id.button_activate);
        if (Daedalus.getInstance().isServiceActivated()) {
            but.setText(R.string.button_text_deactivate);
        } else {
            but.setText(R.string.button_text_activate);
        }

    }

    public static class MainFragmentHandler extends Handler {
        public static final int MSG_REFRESH = 0;

        private MainFragment fragment = null;

        MainFragmentHandler setFragment(MainFragment fragment) {
            this.fragment = fragment;
            return this;
        }

        void shutdown() {
            fragment = null;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_REFRESH:
                    ((Button) fragment.view.findViewById(R.id.button_activate)).setText(R.string.button_text_activate);
                    break;
            }
        }
    }
}
