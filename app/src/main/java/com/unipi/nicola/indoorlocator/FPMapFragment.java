package com.unipi.nicola.indoorlocator;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FPMapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FPMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FPMapFragment extends Fragment {

    public FPMapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fp_map, container, false);
    }
}
