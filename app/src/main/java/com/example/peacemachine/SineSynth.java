package com.example.peacemachine;

import android.util.Log;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.devices.android.AndroidAudioForJSyn;
import com.jsyn.ports.UnitInputPort;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.LinearRamp;
import com.jsyn.unitgen.PinkNoise;
import com.jsyn.unitgen.FilterLowPass;
import com.jsyn.unitgen.FilterHighPass;

/**
 * Play independent sine waves on the left and right channel.
 */
public class SineSynth {

    private final Synthesizer mSynth;
    private final LinearRamp mAmpJack;
    private final LinearRamp mLPJack;
    private final PinkNoise mPinkNoise;
    private final LineOut mLineOut; // stereo output
    private final FilterLowPass mLowPass;
    private final FilterHighPass mHighPass;
    AndroidAudioForJSyn androidAudioForJSyn = null;

    public SineSynth() {
        // Create a JSyn synthesizer that uses the Android output.
        androidAudioForJSyn =  new AndroidAudioForJSyn();
        mSynth = JSyn.createSynthesizer(androidAudioForJSyn);

        // Create the unit generators and add them to the synthesizer.
        mSynth.add(mAmpJack = new LinearRamp());
        mSynth.add(mLPJack = new LinearRamp());
        mPinkNoise = new PinkNoise();
        mLowPass = new FilterLowPass();
        mHighPass = new FilterHighPass();

        mSynth.add(mLPJack);
        mSynth.add(mLowPass);
        mSynth.add(mHighPass);
        mSynth.add(mPinkNoise);
        mSynth.add(mLineOut = new LineOut());

        mAmpJack.output.connect(mPinkNoise.amplitude);
        mAmpJack.time.set(0.5); // duration of ramp

        mLPJack.output.connect(mLowPass.frequency);
        mLowPass.frequency.set(120.0);
        mLowPass.Q.set(1);
        mHighPass.frequency.set(140);
        mHighPass.Q.set(1);
        mPinkNoise.output.connect(mLowPass.input);
        mLowPass.output.connect(mHighPass.input);
        // Connect an oscillator to each channel of the LineOut.
        mHighPass.output.connect(0, mLineOut.input, 0);
        mHighPass.output.connect(0, mLineOut.input, 1);
    }

    public void setLPFreqExact(float val) {
        mLowPass.frequency.set(val);
    }
    public void setLPFreq(float val, float t) {
        float freq = 40 + val * 160;
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

    public void destroy() {
        androidAudioForJSyn.stopStreams();
    }

    public void stop() {
        Log.i("Synth", "stop");
        mSynth.stop();
    }

    public UnitInputPort getAmplitudePort() {
        return mAmpJack.getInput();
    }

}
