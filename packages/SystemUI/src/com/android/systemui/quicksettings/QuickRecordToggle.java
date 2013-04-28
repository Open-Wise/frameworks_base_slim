/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.quicksettings;

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Environment;
import android.view.View;

import com.android.systemui.R;

import java.io.File;
import java.io.IOException;

public class QuickRecordToggle extends QuickSettingsTile {

    public static final int STATE_IDLE = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_RECORDING = 2;
    public static final int STATE_JUST_RECORDED = 3;
    public static final int STATE_NO_RECORDING = 4;

    private static String mQuickAudio = null;

    private int mRecordingState;

    private MediaPlayer mPlayer = null;
    private MediaRecorder mRecorder = null;

    @Override
    protected void init(Context c, int style) {
        super.init(c, style);
        mQuickAudio = Environment.getExternalStorageDirectory().getAbsolutePath();
        mQuickAudio += "/quickrecord.3gp";
        queryRecordingInformation();
    }

    @Override
    public void onClick(View v) {
        File file = new File(mQuickAudio);
        if (!file.exists()) {
            mRecordingState = STATE_NO_RECORDING;
        }
        switch (mRecordingState) {
            case STATE_RECORDING:
                stopRecording();
                break;
            case STATE_NO_RECORDING:
                return;
            case STATE_IDLE:
            case STATE_JUST_RECORDED:
                startPlaying();
                break;
            case STATE_PLAYING:
                stopPlaying();
                break;
        }
    }
    @Override
    public boolean onLongClick(View v) {
        switch (mRecordingState) {
            case STATE_NO_RECORDING:
            case STATE_IDLE:
            case STATE_JUST_RECORDED:
                startRecording();
                break;
        }
        return true;
    }

    private void queryRecordingInformation() {
        final Resources r = mContext.getResources();
        String playStateName = r.getString(
                R.string.quick_settings_quickrecord);
        int playStateIcon = R.drawable.ic_qs_quickrecord;
        File file = new File(mQuickAudio);
        if (!file.exists()) {
            mRecordingState = STATE_NO_RECORDING;
        }
        switch (mRecordingState) {
            case STATE_IDLE:
                playStateName = r.getString(
                        R.string.quick_settings_quickrecord);
                playStateIcon = R.drawable.ic_qs_quickrecord;
                break;
            case STATE_PLAYING:
                playStateName = r.getString(
                        R.string.quick_settings_playing);
                playStateIcon = R.drawable.ic_qs_playing;
                break;
            case STATE_RECORDING:
                playStateName = r.getString(
                        R.string.quick_settings_recording);
                playStateIcon = R.drawable.ic_qs_recording;
                break;
            case STATE_JUST_RECORDED:
                playStateName = r.getString(
                        R.string.quick_settings_recordingcap);
                playStateIcon = R.drawable.ic_qs_saved;
                break;
            case STATE_NO_RECORDING:
                playStateName = r.getString(
                        R.string.quick_settings_nofile);
                playStateIcon = R.drawable.ic_qs_quickrecord;
                break;
        }
    setInfo(playStateName, playStateIcon);
    scheduleViewUpdate();
    };

    final Runnable delayTileRevert = new Runnable () {
        public void run() {
            if (mRecordingState == STATE_JUST_RECORDED) {
                mRecordingState = STATE_IDLE;
                queryRecordingInformation();
            }
        }
    };

    final Runnable autoStopRecord = new Runnable() {
        public void run() {
            if (mRecordingState == STATE_RECORDING) {
                stopRecording();
            }
        }
    };

    final OnCompletionListener stoppedPlaying = new OnCompletionListener(){
        public void onCompletion(MediaPlayer mp) {
            mRecordingState = STATE_IDLE;
            queryRecordingInformation();
        }
    };

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mQuickAudio);
            mPlayer.prepare();
            mPlayer.start();
            mRecordingState = STATE_PLAYING;
            queryRecordingInformation();
            mPlayer.setOnCompletionListener(stoppedPlaying);
        } catch (IOException e) {
            log("prepare() failed", e);
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
        mRecordingState = STATE_IDLE;
        queryRecordingInformation();
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mQuickAudio);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            mRecorder.prepare();
            mRecorder.start();
            mRecordingState = STATE_RECORDING;
            queryRecordingInformation();
            mHandler.postDelayed(autoStopRecord, 60000);
        } catch (IOException e) {
            log("prepare() failed", e);
        }
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        mRecordingState = STATE_JUST_RECORDED;
        queryRecordingInformation();
        mHandler.postDelayed(delayTileRevert, 2000);
    }
}
