package com.example.dopplerapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.jtransforms.fft.DoubleFFT_1D;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
Button start_button;
Button stop_button;
TextView frequencyText;

private MediaRecorder recorder;
private String outputFile;
private boolean isRecording = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start_button = findViewById(R.id.start_btn);
        stop_button = findViewById(R.id.stop_btn);
        frequencyText = findViewById(R.id.frequency_text);

        outputFile = getExternalCacheDir().getAbsolutePath() + "/recording.3gp";

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Requires API level 26
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startRecording();
                }
                updateUI();
            }
        });

        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
                calculateFrequency();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startRecording(){
        // Check if the MediaRecorder object is null and create a new one if necessary.
        if (recorder == null) {
            // Set the audio source, output format, output file, and audio encoder for the recorder.
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(outputFile);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            // Try to prepare the recorder for recording audio.
            try {
                recorder.setOutputFile(outputFile);
                recorder.prepare();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Start recording audio.
        recorder.start();
        isRecording = true;

    }

    public void stopRecording() {
        // Stop recording audio
        recorder.stop();
        recorder.release();
        isRecording = false;
    }

    private void updateUI() {

        if(!isRecording){
            start_button.setVisibility(View.VISIBLE);
            stop_button.setVisibility(View.VISIBLE);
            frequencyText.setText("It is not recording");
        }
        else if (isRecording) {
            start_button.setVisibility(View.GONE);
            stop_button.setVisibility(View.VISIBLE);
            frequencyText.setText("Calculating...");
        } else {
            start_button.setVisibility(View.VISIBLE);
            stop_button.setVisibility(View.GONE);
        }
    }
    private void calculateFrequency(){
        // Create a file object for the recorded audio file.
        File file = new File (outputFile);

        // Try to create a FileInputStream object for the recorded audio file.
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Set the sample rate and buffer size for the AudioRecord object.
        int sampleRate = 44100;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        // Check if the app has been granted the RECORD_AUDIO permission.
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions


            // If the app has not been granted the permission, return from the method.
            Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show();
            return;

        }

        // Create a new AudioRecord object.
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        // Read the audio data into a buffer
        short[] buffer = new short[bufferSize];
        int readSize;
        ArrayList<Short> audioData = new ArrayList<>();
        audioRecord.startRecording();
        while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            readSize = audioRecord.read(buffer, 0, bufferSize);
            for (int i = 0; i < readSize; i++) {
                audioData.add(buffer[i]);
            }
        }
        audioRecord.stop();

        // Convert the audio data to a double array
        double[] audioDataDouble = new double[audioData.size()];
        for (int i = 0; i < audioData.size(); i++) {
            audioDataDouble[i] = audioData.get(i);
        }

        // Apply the FFT algorithm
        DoubleFFT_1D fft = new DoubleFFT_1D(audioDataDouble.length);
        double[] fftData = new double[audioDataDouble.length * 2];
        for (int i = 0; i < audioDataDouble.length; i++) {
            fftData[2 * i] = audioDataDouble[i];
        }
        fft.realForward(fftData);
        // Find the frequency of the signal with the highest amplitude
        double maxAmplitude = 0;
        int maxIndex = 0;
        for (int i = 0; i < fftData.length / 2; i++) {
            double amplitude = Math.sqrt(fftData[2 * i] * fftData[2 * i] + fftData[2 * i + 1] * fftData[2 * i + 1]);
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude;
                maxIndex = i;
            }
        }
        double frequency = maxIndex * sampleRate / fftData.length;
        String frequencyText = String.format(Locale.getDefault(), "Frequency: %.2f Hz", frequency);

        TextView frequencyTextView = findViewById(R.id.frequency_text);
        frequencyTextView.setText(frequencyText);


    }
}
