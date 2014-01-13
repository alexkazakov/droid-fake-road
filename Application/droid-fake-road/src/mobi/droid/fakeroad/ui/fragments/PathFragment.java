package mobi.droid.fakeroad.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import mobi.droid.fakeroad.R;
import mobi.droid.fakeroad.ui.activity.MainActivity;

public class PathFragment extends Fragment{

    private MainActivity mMainActivity;

    @Override
    public void onAttach(final Activity activity){
        super.onAttach(activity);
        mMainActivity = (MainActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View inflate = inflater.inflate(R.layout.fragment_path, container, false);

        return inflate;
    }
}