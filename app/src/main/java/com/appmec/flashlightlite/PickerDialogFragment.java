package com.appmec.flashlightlite;

import mobi.upod.timedurationpicker.TimeDurationPicker;
import mobi.upod.timedurationpicker.TimeDurationPickerDialogFragment;

public class PickerDialogFragment extends TimeDurationPickerDialogFragment {

    @Override
    protected long getInitialDuration() {
        if(MainActivity.timepickerDuration!=(30 * 1000)){
            return MainActivity.timepickerDuration;
        }
        return 30 * 1000;
    }


    @Override
    protected int setTimeUnits() {
        return TimeDurationPicker.MM_SS;
    }

    @Override
    public void onDurationSet(TimeDurationPicker view, long duration) {
        MainActivity.timepickerDuration = duration;
    }

}