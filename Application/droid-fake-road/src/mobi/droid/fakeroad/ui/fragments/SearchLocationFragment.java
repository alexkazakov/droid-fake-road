package mobi.droid.fakeroad.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import mobi.droid.fakeroad.R;

/**
 * Created by max on 13.01.14.
 */
public class SearchLocationFragment extends Fragment implements View.OnClickListener{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View inflate = inflater.inflate(R.layout.search_location_fragment, container, false);

        inflate.findViewById(R.id.btnSearchFrom).setOnClickListener(this);

        return inflate;
    }

    @Override
    public void onClick(final View v){
        switch(v.getId()){
            case R.id.btnSearchFrom:

                break;
            case R.id.btnSearchTo:

                break;
        }
    }
}