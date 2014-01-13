package mobi.droid.fakeroad.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.location.Address;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.google.android.gms.maps.model.LatLng;
import mobi.droid.fakeroad.R;
import mobi.droid.fakeroad.ui.activity.MainActivity;
import mobi.droid.fakeroad.ui.view.AutoCompleteAddressTextView;

/**
 * Created by max on 13.01.14.
 */
public class SearchLocationFragment extends Fragment implements View.OnClickListener{

    private MainActivity mMainActivity;
    private Address mFrom;
    private Address mTo;

    @Override
    public void onAttach(final Activity activity){
        super.onAttach(activity);
        mMainActivity = (MainActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View inflate = inflater.inflate(R.layout.fragment_search_location, container, false);

        //noinspection ConstantConditions
        inflate.findViewById(R.id.btnSearchFrom).setOnClickListener(this);
        AutoCompleteAddressTextView edtFrom = (AutoCompleteAddressTextView) inflate.findViewById(R.id.edtFrom);
        edtFrom.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id){

                mFrom = (Address) parent.getItemAtPosition(position);
                if(mFrom != null){
                    mMainActivity.addMarkerStart(new LatLng(mFrom.getLatitude(), mFrom.getLongitude()));
                    calculateRoute();
                }
            }
        });

        AutoCompleteAddressTextView viewById = (AutoCompleteAddressTextView) inflate.findViewById(R.id.edtTo);
        viewById.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id){
                mTo = (Address) parent.getItemAtPosition(position);
                if(mTo != null){
                    mMainActivity.addMarkerEnd(new LatLng(mTo.getLatitude(), mTo.getLongitude()));
                    calculateRoute();
                }
            }
        });
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

    private void calculateRoute(){
        if(mTo != null && mFrom != null){
            mMainActivity.calculateRoute(new LatLng(mFrom.getLatitude(), mFrom.getLongitude()),
                                         new LatLng(mTo.getLatitude(), mTo.getLongitude()));
        }
    }

}