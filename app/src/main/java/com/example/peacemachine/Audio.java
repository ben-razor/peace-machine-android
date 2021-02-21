package com.example.peacemachine;

import android.util.Log;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.data.FloatSample;
import com.jsyn.devices.android.AndroidAudioForJSyn;
import com.jsyn.ports.UnitInputPort;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.LinearRamp;
import com.jsyn.unitgen.PinkNoise;
import com.jsyn.unitgen.FilterLowPass;
import com.jsyn.unitgen.FilterHighPass;
import com.jsyn.unitgen.UnitGenerator;
import com.jsyn.unitgen.VariableRateDataReader;
import com.jsyn.unitgen.VariableRateMonoReader;
import com.jsyn.unitgen.VariableRateStereoReader;
import com.jsyn.util.SampleLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Audio {

    private final Synthesizer mSynth;
    private final LinearRamp mAmpJack;
    private final LinearRamp mLPJack;
    // private final PinkNoise mPinkNoise;
    private final LineOut mLineOut; // stereo output
    private final FilterLowPass mLowPass;
    private final FilterHighPass mHighPass;
    private HashMap<String, FloatSample> samples = new HashMap<>();
    private HashMap<String, PeaceMachineSource> sources = new HashMap<>();
    AndroidAudioForJSyn androidAudioForJSyn = null;
    private List<VibeInfo> vibeInfos;
    private String TAG = "Audio.java";

    public Audio() {
        // Create a JSyn synthesizer that uses the Android output.
        androidAudioForJSyn =  new AndroidAudioForJSyn();
        mSynth = JSyn.createSynthesizer(androidAudioForJSyn);

        // Create the unit generators and add them to the synthesizer.
        mSynth.add(mAmpJack = new LinearRamp());
        mSynth.add(mLPJack = new LinearRamp());
        // mPinkNoise = new PinkNoise();
        mLowPass = new FilterLowPass();
        mHighPass = new FilterHighPass();

        mSynth.add(mLPJack);
        mSynth.add(mLowPass);
        mSynth.add(mHighPass);
        mSynth.add(mLineOut = new LineOut());

        mLPJack.output.connect(mLowPass.frequency);
        mLowPass.frequency.set(120.0);
        mLowPass.Q.set(1.5);
        mHighPass.frequency.set(140);
        mHighPass.Q.set(1);

        mAmpJack.time.set(1);
        mAmpJack.output.connect(mHighPass.amplitude);

        mLowPass.output.connect(mHighPass.input);
        mHighPass.output.connect(0, mLineOut.input, 0);
        mHighPass.output.connect(0, mLineOut.input, 1);
    }

    public void setLPFreqExact(float val) {
        mLowPass.frequency.set(val);
    }
    public void setLPFreq(float val, float t) {
        float freq = 80 + val * 400;
        Log.i("Low pass freqency", Float.toString(freq));
        mLPJack.time.set(t);
        mLPJack.getInput().set(freq);
        //mLowPass.frequency.set(freq);
    }

    public void setHPFreq(float val) {
        float freq = 5 + val * 200;
        Log.i("Low pass freqency", Float.toString(freq));
        mHighPass.frequency.set(freq);
    }

    public void setVolume(float val, float t) {
        mAmpJack.time.set(t);
        mAmpJack.getInput().set(val);
    }

    public void start() {
        mSynth.start();
        mLineOut.start();
    }

    interface PeaceMachineSource {
        void handleFloat(String id, float value);
        void setLerpTimeForVolume(float time);
        void setVolume(float volume);
        void stop();
    }

    public class SampleSource implements PeaceMachineSource {
        VariableRateDataReader samplePlayer;
        private final LinearRamp ampJack;

        public SampleSource(String sampleID) {
            FloatSample sample = samples.get(sampleID);
            int channels = sample.getChannelsPerFrame();
            VariableRateDataReader samplePlayer = new VariableRateMonoReader();
            if(channels == 2) {
                samplePlayer = new VariableRateStereoReader();
            }
            samplePlayer.dataQueue.queueLoop(sample, 0, sample.getNumFrames());
            mSynth.add(samplePlayer);
            mSynth.add(ampJack = new LinearRamp());
            ampJack.output.connect(samplePlayer.amplitude);

            samplePlayer.output.connect(mLowPass.input);
            samplePlayer.rate.set(sample.getFrameRate());
            samplePlayer.amplitude.set(1);
            setLerpTimeForVolume(0);
            setVolume(0);
            setLerpTimeForVolume(1);
            samplePlayer.start();
        }

        public void setLerpTimeForVolume(float time) {
            ampJack.time.set(time);
        }

        public void setVolume(float volume) {
            ampJack.getInput().set(volume);
        }

        @Override
        public void handleFloat(String id, float value) { }

        public void stop() {
            samplePlayer.stop();
        }
    }

    public class NoiseSource implements PeaceMachineSource {
        PinkNoise pinkNoise = new PinkNoise();
        private final LinearRamp ampJack;

        public NoiseSource() {
            mSynth.add(pinkNoise);
            mSynth.add(ampJack = new LinearRamp());
            ampJack.output.connect(pinkNoise.amplitude);
            setVolume(0);
            setLerpTimeForVolume(1);
            pinkNoise.output.connect(mLowPass.input);
        }

        public void setLerpTimeForVolume(float time) {
            ampJack.time.set(time);
        }

        public void setVolume(float volume) {
            ampJack.getInput().set(volume);
        }

        @Override
        public void handleFloat(String id, float value) { }

        public void stop() {
            mSynth.stop();
        }
    }

    public void changeVibe(String vibeID) {
        for(Map.Entry<String, PeaceMachineSource> entry: sources.entrySet()) {
            PeaceMachineSource source = entry.getValue();
            source.setLerpTimeForVolume(1);

            if(entry.getKey().equals(vibeID)) {
                source.setVolume(1);
            }
            else {
                source.setVolume(0);
            }
        }
    }

    public void addSample(String id, FloatSample sample) {
        if(samples.get(id) == null) {
            samples.put(id, sample);
        }
    }

    public void addVibeInfos(List<VibeInfo> vibeInfos) {
        this.vibeInfos = vibeInfos;
    }

    public void addVibe(VibeInfo vibeInfo) {
        if(sources.get(vibeInfo.id) == null) {
            PeaceMachineSource source = null;

            if(vibeInfo.audio.contains(".")) {
                source = new SampleSource(vibeInfo.id);
            }
            else {
                source = new NoiseSource();
            }

            sources.put(vibeInfo.id, source);
        }
    }

    public void destroy() {
        Log.d(TAG, "destroy()");
        mSynth.stop();
        Log.d(TAG, "destroy() after mSynth.stop()");
    }

    public UnitInputPort getAmplitudePort() {
        return mAmpJack.getInput();
    }
}
