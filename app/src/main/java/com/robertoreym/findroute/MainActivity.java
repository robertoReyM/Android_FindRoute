package com.robertoreym.findroute;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.robertoreym.findroute.models.PointIntersection;
import com.robertoreym.findroute.models.Result;
import com.robertoreym.findroute.models.Route;
import com.robertoreym.findroute.models.RouteIntersection;
import com.robertoreym.findroute.models.SearchPoint;
import com.robertoreym.findroute.models.Stop;
import com.robertoreym.findroute.models.Trajectory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity  implements OnMapReadyCallback {

    //Request code to get last known location
    private static final int REQUEST_CODE_MY_LOCATION = 100;
    private static final LatLngBounds BOUNDS_GREATER_GUADALAJARA = new LatLngBounds(
            new LatLng(20.661764, -103.452766), new LatLng(20.699594,-103.390514));

    private String[] POLYLINES = {
            "qkf}BdgivRrA{IzDo\\bAuJnBmPlBkNjBoOjDiLbD{H|E`BnGnB`Cp@tN|D|TfHpMhEhL`DhN~EvJxCpGvB`KvBfI`ChJ`DvFtA",
            "mca}BbyfvRcWtAiS~@eQ~@aKv@uK`@sF_AiGuCgH{B}MmAyScC_UmCyPaBsJiAiDYoDw@gDcC_BoD}AwM{AsB{DGqK?}IFgJvB}GXcKmAyHsBuJ_EwHmCaG}CoCwDwBwE_DiG",
            "u~f}B`ifvRxIv@`IfAbCTbHp@xIpAlHdAfGV|Fb@jGl@`GbD~GpC`JCrFY",
            "ube}BbufvRiNgBiK}AiNuA_MgBiLkAqGs@_B}HoAoJ_DiNw@kLy@_JXkOn@}LqDaKwEgMo@kLgCsOqEsG_GuIaCcOoHqGiKaGaLcLwDeIwEiJaDaH_A{C_DcH_B_JyEr@wIjD",
            "oog}BlzfvReBkWkHmb@eCs^^_YaI{SqCm^gJgJ_D}H_BsKiGiGaLcH_L{KyEiKgC{GyE{H_EyJwBaKqGFgIpCqHnF_F~EgEhDqDpBo@fGgC~IwD|LqCpFgI|AfKmIhDkIjByEbAkDj@eEvA{DpDaCrA_AbEkDrDoDhCwBvGgCtG_BhCMdI{A`GcBzFoAdFeArFmBhDoB",
            "cfm}Bl``vR~H_BhByGvAiGfCaG~@gFv@oFhDaDnFsCvCaDxHgF~FiChFcA~IcA`I{C~FcAxEmAfIyC`F_C~DmAxJa@~JPhLFxMP",
            "kud}BnwfvRqSeBqKwBaPuAqQwBiP}AaM{CyEcPgCiGqLQaPQ_L|DyNmAyOmEyNcHyIuEoHyNqDwMoFyCoDuDOcEnBqCf@sGn@mMoCqJ_BeM^aG~GeInDuHVeF_@{K_DmWgA_JoAwI_@aHo@gFW_Fo@_J"
    };

    private GoogleMap mMap;

    private LatLng mSource;
    private LatLng mDestination;
    private RouteFinder mRouteFinder;
    private List<Result> mResults;
    private ArrayList<Route> mRoutes;
    private int mCounter = 0;

    private FloatingActionButton mFabSearch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //Set map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mSource = new LatLng(20.681568,-103.433966);
        //mDestination = new LatLng(20.680323,-103.427153);
        //mDestination = new LatLng(20.690124,-103.416188);
        //mDestination = new LatLng(20.672739,-103.422797);
        //mDestination = new LatLng(20.690365,-103.403958);
        //mDestination = new LatLng(20.700919,-103.375442);

        mRoutes = new ArrayList<>();

        for(int i = 0; i<POLYLINES.length;i++){

            Route route = new Route();
            route.setPolyline(POLYLINES[i]);
            mRoutes.add(route);
        }

        mRouteFinder = new RouteFinder();
        mRouteFinder.setSource(mSource);
        mRouteFinder.setDestination(mDestination);
        mRouteFinder.setRoutes(mRoutes);

        mFabSearch = (FloatingActionButton) findViewById(R.id.fab);
        assert mFabSearch != null;
        mFabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //searchRoutes();
                mResults = mRouteFinder.searchRoutes();

                if(mResults!=null && mCounter<mResults.size()){
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(mSource));
                    mMap.addMarker(new MarkerOptions().position(mDestination));
                    //paint winner result
                    paintResult(mResults.get(mCounter));

                    Snackbar.make(mFabSearch,String.format("Distancia de la ruta: %f",mResults.get(mCounter).getDistance()),Snackbar.LENGTH_SHORT).show();
                    mCounter++;
                }
            }
        });



    }

    public void onResume(){
        super.onResume();

    }

    /***********************************************************************************************
     Google maps
     **********************************************************************************************/

    @Override
    public void onMapReady(GoogleMap googleMap) {

        //keep reference of map
        mMap = googleMap;

        //check for location permissions
        if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            //enable my location on map
            mMap.setMyLocationEnabled(true);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    //set bounds
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(BOUNDS_GREATER_GUADALAJARA, 0));

                    //set source and destination
                    mMap.addMarker(new MarkerOptions().position(mSource));
                    mMap.addMarker(new MarkerOptions().position(mDestination));

                }
            },500);

        } else {

            // Show rationale and request permission.
            ActivityCompat.requestPermissions((Activity) getBaseContext(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_MY_LOCATION);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        //check for request code
        if (requestCode == REQUEST_CODE_MY_LOCATION) {

            //check for requested permission granted
            if (permissions.length == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                //confirm permission granted
                if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    //set my location on map
                    mMap.setMyLocationEnabled(true);

                    //set bounds
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(BOUNDS_GREATER_GUADALAJARA, 250));
                }
            }
        } else {
            // Permission was denied. Display an error message.
        }
    }

    private void paintResult(Result result){

        if(result!=null && result.getTrajectories()!=null) {
            for (Trajectory trajectory : result.getTrajectories()) {
                paintPolyline(trajectory.getPoints());
            }
        }
    }

    private void paintPolyline(List<LatLng> points) {


        mMap.addPolyline(new PolylineOptions()
                .color(Color.argb(128, new Random().nextInt(255), new Random().nextInt(255), new Random().nextInt(255)))
                .visible(true)
                .geodesic(true)
                .addAll(points));

    }


}
