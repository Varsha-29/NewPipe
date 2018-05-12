package org.schabi.newpipe.player.helper;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import org.schabi.newpipe.R;
import org.schabi.newpipe.util.SliderStrategy;

import static org.schabi.newpipe.player.BasePlayer.DEBUG;

public class PlaybackParameterDialog extends DialogFragment {
    @NonNull private static final String TAG = "PlaybackParameterDialog";

    // Minimum allowable range in ExoPlayer
    public static final double MINIMUM_PLAYBACK_VALUE = 0.10f;
    public static final double MAXIMUM_PLAYBACK_VALUE = 3.00f;

    public static final char STEP_UP_SIGN = '+';
    public static final char STEP_DOWN_SIGN = '-';
    public static final double DEFAULT_PLAYBACK_STEP_VALUE = 0.05f;

    public static final double DEFAULT_TEMPO = 1.00f;
    public static final double DEFAULT_PITCH = 1.00f;
    public static final boolean DEFAULT_SKIP_SILENCE = false;

    @NonNull private static final String INITIAL_TEMPO_KEY = "initial_tempo_key";
    @NonNull private static final String INITIAL_PITCH_KEY = "initial_pitch_key";

    public interface Callback {
        void onPlaybackParameterChanged(final float playbackTempo, final float playbackPitch,
                                        final boolean playbackSkipSilence);
    }

    @Nullable private Callback callback;

    @NonNull private final SliderStrategy strategy = new SliderStrategy.Quadratic(
            MINIMUM_PLAYBACK_VALUE, MAXIMUM_PLAYBACK_VALUE,
            /*centerAt=*/1.00f, /*sliderGranularity=*/10000);

    private double initialTempo = DEFAULT_TEMPO;
    private double initialPitch = DEFAULT_PITCH;
    private boolean initialSkipSilence = DEFAULT_SKIP_SILENCE;

    @Nullable private SeekBar tempoSlider;
    @Nullable private TextView tempoMinimumText;
    @Nullable private TextView tempoMaximumText;
    @Nullable private TextView tempoCurrentText;
    @Nullable private TextView tempoStepDownText;
    @Nullable private TextView tempoStepUpText;

    @Nullable private SeekBar pitchSlider;
    @Nullable private TextView pitchMinimumText;
    @Nullable private TextView pitchMaximumText;
    @Nullable private TextView pitchCurrentText;
    @Nullable private TextView pitchStepDownText;
    @Nullable private TextView pitchStepUpText;

    @Nullable private TextView stepSizeOnePercentText;
    @Nullable private TextView stepSizeFivePercentText;
    @Nullable private TextView stepSizeTenPercentText;
    @Nullable private TextView stepSizeTwentyFivePercentText;
    @Nullable private TextView stepSizeOneHundredPercentText;

    @Nullable private CheckBox unhookingCheckbox;
    @Nullable private CheckBox skipSilenceCheckbox;

    public static PlaybackParameterDialog newInstance(final double playbackTempo,
                                                      final double playbackPitch,
                                                      final boolean playbackSkipSilence) {
        PlaybackParameterDialog dialog = new PlaybackParameterDialog();
        dialog.initialTempo = playbackTempo;
        dialog.initialPitch = playbackPitch;
        dialog.initialSkipSilence = playbackSkipSilence;
        return dialog;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Lifecycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context != null && context instanceof Callback) {
            callback = (Callback) context;
        } else {
            dismiss();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            initialTempo = savedInstanceState.getDouble(INITIAL_TEMPO_KEY, DEFAULT_TEMPO);
            initialPitch = savedInstanceState.getDouble(INITIAL_PITCH_KEY, DEFAULT_PITCH);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(INITIAL_TEMPO_KEY, initialTempo);
        outState.putDouble(INITIAL_PITCH_KEY, initialPitch);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Dialog
    //////////////////////////////////////////////////////////////////////////*/

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final View view = View.inflate(getContext(), R.layout.dialog_playback_parameter, null);
        setupControlViews(view);

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.playback_speed_control)
                .setView(view)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) ->
                        setPlaybackParameters(initialTempo, initialPitch, initialSkipSilence))
                .setNeutralButton(R.string.playback_reset, (dialogInterface, i) ->
                        setPlaybackParameters(DEFAULT_TEMPO, DEFAULT_PITCH, DEFAULT_SKIP_SILENCE))
                .setPositiveButton(R.string.finish, (dialogInterface, i) ->
                        setCurrentPlaybackParameters());

        return dialogBuilder.create();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Control Views
    //////////////////////////////////////////////////////////////////////////*/

    private void setupControlViews(@NonNull View rootView) {
        setupHookingControl(rootView);
        setupSkipSilenceControl(rootView);

        setupTempoControl(rootView);
        setupPitchControl(rootView);
        setupStepSize(DEFAULT_PLAYBACK_STEP_VALUE);

        setupStepSizeSelector(rootView);
    }

    private void setupTempoControl(@NonNull View rootView) {
        tempoSlider = rootView.findViewById(R.id.tempoSeekbar);
        tempoMinimumText = rootView.findViewById(R.id.tempoMinimumText);
        tempoMaximumText = rootView.findViewById(R.id.tempoMaximumText);
        tempoCurrentText = rootView.findViewById(R.id.tempoCurrentText);
        tempoStepUpText = rootView.findViewById(R.id.tempoStepUp);
        tempoStepDownText = rootView.findViewById(R.id.tempoStepDown);

        if (tempoCurrentText != null)
            tempoCurrentText.setText(PlayerHelper.formatSpeed(initialTempo));
        if (tempoMaximumText != null)
            tempoMaximumText.setText(PlayerHelper.formatSpeed(MAXIMUM_PLAYBACK_VALUE));
        if (tempoMinimumText != null)
            tempoMinimumText.setText(PlayerHelper.formatSpeed(MINIMUM_PLAYBACK_VALUE));

        if (tempoStepUpText != null) {
            tempoStepUpText.setText(getStepUpPercentString(DEFAULT_PLAYBACK_STEP_VALUE));
            tempoStepUpText.setOnClickListener(view -> {
                onTempoSliderUpdated(getCurrentTempo() + DEFAULT_PLAYBACK_STEP_VALUE);
                setCurrentPlaybackParameters();
            });
        }

        if (tempoStepDownText != null) {
            tempoStepDownText.setText(getStepDownPercentString(DEFAULT_PLAYBACK_STEP_VALUE));
            tempoStepDownText.setOnClickListener(view -> {
                onTempoSliderUpdated(getCurrentTempo() - DEFAULT_PLAYBACK_STEP_VALUE);
                setCurrentPlaybackParameters();
            });
        }

        if (tempoSlider != null) {
            tempoSlider.setMax(strategy.progressOf(MAXIMUM_PLAYBACK_VALUE));
            tempoSlider.setProgress(strategy.progressOf(initialTempo));
            tempoSlider.setOnSeekBarChangeListener(getOnTempoChangedListener());
        }
    }

    private void setupPitchControl(@NonNull View rootView) {
        pitchSlider = rootView.findViewById(R.id.pitchSeekbar);
        pitchMinimumText = rootView.findViewById(R.id.pitchMinimumText);
        pitchMaximumText = rootView.findViewById(R.id.pitchMaximumText);
        pitchCurrentText = rootView.findViewById(R.id.pitchCurrentText);
        pitchStepDownText = rootView.findViewById(R.id.pitchStepDown);
        pitchStepUpText = rootView.findViewById(R.id.pitchStepUp);

        if (pitchCurrentText != null)
            pitchCurrentText.setText(PlayerHelper.formatPitch(initialPitch));
        if (pitchMaximumText != null)
            pitchMaximumText.setText(PlayerHelper.formatPitch(MAXIMUM_PLAYBACK_VALUE));
        if (pitchMinimumText != null)
            pitchMinimumText.setText(PlayerHelper.formatPitch(MINIMUM_PLAYBACK_VALUE));

        if (pitchSlider != null) {
            pitchSlider.setMax(strategy.progressOf(MAXIMUM_PLAYBACK_VALUE));
            pitchSlider.setProgress(strategy.progressOf(initialPitch));
            pitchSlider.setOnSeekBarChangeListener(getOnPitchChangedListener());
        }
    }

    private void setupHookingControl(@NonNull View rootView) {
        unhookingCheckbox = rootView.findViewById(R.id.unhookCheckbox);
        if (unhookingCheckbox != null) {
            unhookingCheckbox.setChecked(initialPitch != initialTempo);
            unhookingCheckbox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if (isChecked) return;
                // When unchecked, slide back to the minimum of current tempo or pitch
                final double minimum = Math.min(getCurrentPitch(), getCurrentTempo());
                setSliders(minimum);
                setCurrentPlaybackParameters();
            });
        }
    }

    private void setupSkipSilenceControl(@NonNull View rootView) {
        skipSilenceCheckbox = rootView.findViewById(R.id.skipSilenceCheckbox);
        if (skipSilenceCheckbox != null) {
            skipSilenceCheckbox.setChecked(initialSkipSilence);
            skipSilenceCheckbox.setOnCheckedChangeListener((compoundButton, isChecked) ->
                    setCurrentPlaybackParameters());
        }
    }

    private void setupStepSizeSelector(@NonNull final View rootView) {
        stepSizeOnePercentText = rootView.findViewById(R.id.stepSizeOnePercent);
        stepSizeFivePercentText = rootView.findViewById(R.id.stepSizeFivePercent);
        stepSizeTenPercentText = rootView.findViewById(R.id.stepSizeTenPercent);
        stepSizeTwentyFivePercentText = rootView.findViewById(R.id.stepSizeTwentyFivePercent);
        stepSizeOneHundredPercentText = rootView.findViewById(R.id.stepSizeOneHundredPercent);

        if (stepSizeOnePercentText != null) {
            final double onePercent = 0.01f;
            stepSizeOnePercentText.setText(getPercentString(onePercent));
            stepSizeOnePercentText.setOnClickListener(view -> setupStepSize(onePercent));
        }

        if (stepSizeFivePercentText != null) {
            final double fivePercent = 0.05f;
            stepSizeFivePercentText.setText(getPercentString(fivePercent));
            stepSizeFivePercentText.setOnClickListener(view -> setupStepSize(fivePercent));
        }

        if (stepSizeTenPercentText != null) {
            final double tenPercent = 0.10f;
            stepSizeTenPercentText.setText(getPercentString(tenPercent));
            stepSizeTenPercentText.setOnClickListener(view -> setupStepSize(tenPercent));
        }

        if (stepSizeTwentyFivePercentText != null) {
            final double twentyFivePercent = 0.25f;
            stepSizeTwentyFivePercentText.setText(getPercentString(twentyFivePercent));
            stepSizeTwentyFivePercentText.setOnClickListener(view ->
                    setupStepSize(twentyFivePercent));
        }

        if (stepSizeOneHundredPercentText != null) {
            final double oneHundredPercent = 1.00f;
            stepSizeOneHundredPercentText.setText(getPercentString(oneHundredPercent));
            stepSizeOneHundredPercentText.setOnClickListener(view ->
                    setupStepSize(oneHundredPercent));
        }
    }

    private void setupStepSize(final double stepSize) {
        if (tempoStepUpText != null) {
            tempoStepUpText.setText(getStepUpPercentString(stepSize));
            tempoStepUpText.setOnClickListener(view -> {
                onTempoSliderUpdated(getCurrentTempo() + stepSize);
                setCurrentPlaybackParameters();
            });
        }

        if (tempoStepDownText != null) {
            tempoStepDownText.setText(getStepDownPercentString(stepSize));
            tempoStepDownText.setOnClickListener(view -> {
                onTempoSliderUpdated(getCurrentTempo() - stepSize);
                setCurrentPlaybackParameters();
            });
        }

        if (pitchStepUpText != null) {
            pitchStepUpText.setText(getStepUpPercentString(stepSize));
            pitchStepUpText.setOnClickListener(view -> {
                onPitchSliderUpdated(getCurrentPitch() + stepSize);
                setCurrentPlaybackParameters();
            });
        }

        if (pitchStepDownText != null) {
            pitchStepDownText.setText(getStepDownPercentString(stepSize));
            pitchStepDownText.setOnClickListener(view -> {
                onPitchSliderUpdated(getCurrentPitch() - stepSize);
                setCurrentPlaybackParameters();
            });
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Sliders
    //////////////////////////////////////////////////////////////////////////*/

    private SeekBar.OnSeekBarChangeListener getOnTempoChangedListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                final double currentTempo = strategy.valueOf(progress);
                if (fromUser) {
                    onTempoSliderUpdated(currentTempo);
                    setCurrentPlaybackParameters();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do Nothing.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do Nothing.
            }
        };
    }

    private SeekBar.OnSeekBarChangeListener getOnPitchChangedListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                final double currentPitch = strategy.valueOf(progress);
                if (fromUser) { // this change is first in chain
                    onPitchSliderUpdated(currentPitch);
                    setCurrentPlaybackParameters();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do Nothing.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do Nothing.
            }
        };
    }

    private void onTempoSliderUpdated(final double newTempo) {
        if (unhookingCheckbox == null) return;
        if (!unhookingCheckbox.isChecked()) {
            setSliders(newTempo);
        } else {
            setTempoSlider(newTempo);
        }
    }

    private void onPitchSliderUpdated(final double newPitch) {
        if (unhookingCheckbox == null) return;
        if (!unhookingCheckbox.isChecked()) {
            setSliders(newPitch);
        } else {
            setPitchSlider(newPitch);
        }
    }

    private void setSliders(final double newValue) {
        setTempoSlider(newValue);
        setPitchSlider(newValue);
    }

    private void setTempoSlider(final double newTempo) {
        if (tempoSlider == null) return;
        tempoSlider.setProgress(strategy.progressOf(newTempo));
    }

    private void setPitchSlider(final double newPitch) {
        if (pitchSlider == null) return;
        pitchSlider.setProgress(strategy.progressOf(newPitch));
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Helper
    //////////////////////////////////////////////////////////////////////////*/

    private void setCurrentPlaybackParameters() {
        setPlaybackParameters(getCurrentTempo(), getCurrentPitch(), getCurrentSkipSilence());
    }

    private void setPlaybackParameters(final double tempo, final double pitch,
                                       final boolean skipSilence) {
        if (callback != null && tempoCurrentText != null && pitchCurrentText != null) {
            if (DEBUG) Log.d(TAG, "Setting playback parameters to " +
                    "tempo=[" + tempo + "], " +
                    "pitch=[" + pitch + "]");

            tempoCurrentText.setText(PlayerHelper.formatSpeed(tempo));
            pitchCurrentText.setText(PlayerHelper.formatPitch(pitch));
            callback.onPlaybackParameterChanged((float) tempo, (float) pitch, skipSilence);
        }
    }

    private double getCurrentTempo() {
        return tempoSlider == null ? initialTempo : strategy.valueOf(
                tempoSlider.getProgress());
    }

    private double getCurrentPitch() {
        return pitchSlider == null ? initialPitch : strategy.valueOf(
                pitchSlider.getProgress());
    }

    private boolean getCurrentSkipSilence() {
        return skipSilenceCheckbox != null && skipSilenceCheckbox.isChecked();
    }

    @NonNull
    private static String getStepUpPercentString(final double percent) {
        return STEP_UP_SIGN + getPercentString(percent);
    }

    @NonNull
    private static String getStepDownPercentString(final double percent) {
        return STEP_DOWN_SIGN + getPercentString(percent);
    }

    @NonNull
    private static String getPercentString(final double percent) {
        return PlayerHelper.formatPitch(percent);
    }
}
