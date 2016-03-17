package com.robertoreym.findroute;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import com.robertoreym.findroute.models.Intersection;
import com.robertoreym.findroute.models.Result;
import com.robertoreym.findroute.models.Route;
import com.robertoreym.findroute.models.Stop;
import com.robertoreym.findroute.models.Trajectory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity  implements OnMapReadyCallback {

    //Request code to get last known location
    private static final int REQUEST_CODE_MY_LOCATION = 100;
    private static final int DEFAULT_TOLERANCE = 500;
    private static final int DEFAULT_INTERSECTION_TOLERANCE = 100;
    private static final LatLngBounds BOUNDS_GREATER_GUADALAJARA = new LatLngBounds(
            new LatLng(20.661764, -103.452766), new LatLng(20.699594,-103.390514));

    private String[] POLYLINES = {
            "qkf}BdgivRrA{IzDo\\bAuJnBmPlBkNjBoOjDiLbD{H|E`BnGnB`Cp@tN|D|TfHpMhEhL`DhN~EvJxCpGvB`KvBfI`ChJ`DvFtA",
            "mca}BbyfvRcWtAiS~@eQ~@aKv@uK`@sF_AiGuCgH{B}MmAyScC_UmCyPaBsJiAiDYoDw@gDcC_BoD}AwM{AsB{DGqK?}IFgJvB}GXcKmAyHsBuJ_EwHmCaG}CoCwDwBwE_DiG",
            "u~f}B`ifvRxIv@`IfAbCTbHp@xIpAlHdAfGV|Fb@jGl@`GbD~GpC`JCrFY"
    };

    private GoogleMap mMap;
    private LatLng mSource;
    private LatLng mDestination;
    private ArrayList<String> mPolylines;
    private HashMap<String,Route> mSourceRoutes;
    private HashMap<String,Route> mDestinationRoutes;

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
        mDestination = new LatLng(20.672739,-103.422797);

        mPolylines = new ArrayList<>();
        mSourceRoutes = new HashMap<>();
        mDestinationRoutes = new HashMap<>();

        for(int i = 0; i<POLYLINES.length;i++){

            mPolylines.add(POLYLINES[i]);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                findAvailableRoutes(mSource, mDestination, mPolylines, mSourceRoutes, mDestinationRoutes);

                boolean isRouteFound = false;

                //go through source routes
                for (HashMap.Entry<String, Route> entry : mSourceRoutes.entrySet()) {

                    //check for a common route
                    if(mDestinationRoutes.containsKey(entry.getKey())){

                        Route sourceRoute = entry.getValue();
                        Route destinationRoute = mDestinationRoutes.get(entry.getKey());
                        Trajectory trajectory = getTrajectory(entry.getValue().getPolyline(),
                                sourceRoute.getClosestStop().getPosition(),destinationRoute.getClosestStop().getPosition());
                        paintPolyline(trajectory.getPoints());

                        isRouteFound = true;
                    }
                }

                if(!isRouteFound){

                    ArrayList<Intersection> intersections = new ArrayList<Intersection>();

                    //go through source routes
                    for (HashMap.Entry<String, Route> sourceEntry : mSourceRoutes.entrySet()) {

                        for(HashMap.Entry<String, Route> destinationEntry : mDestinationRoutes.entrySet()){

                            ArrayList<Intersection> results = checkRoutesIntersection(sourceEntry.getValue(),destinationEntry.getValue());
                            intersections.addAll(results);
                        }


                    }

                    ArrayList<Result> results = new ArrayList<Result>();

                    //iterate intersections
                    for(Intersection intersection : intersections){

                        Route sourceRoute = mSourceRoutes.get(intersection.getR1ID());
                        Route destinationRoute = mDestinationRoutes.get(intersection.getR2ID());

                        Trajectory sourceTrajectory = getTrajectory(sourceRoute.getPolyline(),
                                sourceRoute.getClosestStop().getPosition(), intersection.getR1Stop().getPosition());

                        Trajectory destinationTrajectory = getTrajectory(destinationRoute.getPolyline(),
                                destinationRoute.getClosestStop().getPosition(),intersection.getR2Stop().getPosition());

                        Result result = new Result();
                        result.setDistance(sourceTrajectory.getDistance()+destinationTrajectory.getDistance());
                        result.setTrajectories(new ArrayList<Trajectory>());
                        result.getTrajectories().add(sourceTrajectory);
                        result.getTrajectories().add(destinationTrajectory);

                        results.add(result);
                    }

                    Result winnerResult = null;
                    float smallerDistance = 0.0f;
                    //get better result
                    for(Result result: results){

                        if(smallerDistance == 0){
                            smallerDistance = result.getDistance();
                            winnerResult = result;
                        }else if(result.getDistance()<smallerDistance){
                            smallerDistance = result.getDistance();
                            winnerResult = result;
                        }
                    }

                    for(Trajectory trajectory :winnerResult.getTrajectories()){
                        paintPolyline(trajectory.getPoints());
                    }

                }
            }
        });
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

    private void setPolyline(String encodedPolyline) {

        mMap.addPolyline(new PolylineOptions()
                .color(Color.argb(128, new Random().nextInt(255), new Random().nextInt(255), new Random().nextInt(255)))
                .visible(true)
                .geodesic(true)
                .addAll(PolyUtil.decode(encodedPolyline)));

    }

    private Trajectory getTrajectory(String encodedPolyline,LatLng p1,LatLng p2){

        List<LatLng> points = PolyUtil.decode(encodedPolyline);
        boolean isPointOnPath = false;
        float totalDistance = 0.0f;
        LatLng previousPoint = null;

        for (Iterator<LatLng> iterator = points.iterator(); iterator.hasNext();) {

            LatLng point = iterator.next();
            //check for p1 or p2
            if((point.latitude == p1.latitude && point.longitude == p1.longitude) ||
                    (point.latitude == p2.latitude && point.longitude == p2.longitude)){

                isPointOnPath = !isPointOnPath;

            }else if(!isPointOnPath){
                iterator.remove();
            }

            if(isPointOnPath){

                if(previousPoint == null){

                    previousPoint = point;
                }else{

                    float distance[] = new float[2];
                    Location.distanceBetween(previousPoint.latitude,previousPoint.longitude,point.latitude,point.longitude,distance);
                    previousPoint = point;
                    totalDistance+=distance[0];
                }
            }
        }

        Trajectory trajectory =  new Trajectory();
        trajectory.setPoints(points);
        trajectory.setDistance(totalDistance);

        return trajectory;
    }
    private void paintPolyline(List<LatLng> points) {


        mMap.addPolyline(new PolylineOptions()
                .color(Color.argb(128, new Random().nextInt(255), new Random().nextInt(255), new Random().nextInt(255)))
                .visible(true)
                .geodesic(true)
                .addAll(points));

    }
    private void findAvailableRoutes(LatLng source,LatLng destination,ArrayList<String> polylines,
                                     HashMap<String,Route> sourceRoutes,HashMap<String,Route> destinationRoutes){
        sourceRoutes.clear();
        destinationRoutes.clear();

        //iterate through polylines
        for(int i = 0; i< polylines.size();i++){

            //get list of points
            List<LatLng> points = PolyUtil.decode(polylines.get(i));

            boolean isSourceCloseEnough = false;
            boolean isDestinationCloseEnough = false;
            float closestSourceDistance = 0;
            float closestDestinationDistance = 0;
            LatLng closestSourcePoint = null;
            LatLng closestDestinationPoint = null;

            //iterate through points
            for(int i2 = 0; i2<points.size();i2++){

                float resultsToSource[] = new float[2];
                float resultsToDestination[] = new float[2];

                //get current point
                LatLng point = points.get(i2);

                //get distance to source
                Location.distanceBetween(
                        source.latitude,
                        source.longitude,
                        point.latitude,
                        point.longitude,
                        resultsToSource);

                //check if source is close enough
                if(resultsToSource[0] < DEFAULT_TOLERANCE){
                    isSourceCloseEnough = true;

                    //check if existing closest point
                    if(closestSourcePoint !=null) {

                        //check if is closer than previous
                        if(resultsToSource[0]<closestSourceDistance){

                            //assing new closest point
                            closestSourcePoint = point;
                            closestSourceDistance = resultsToSource[0];
                        }
                    }else{

                        //assing new closest point
                        closestSourcePoint = point;
                        closestSourceDistance = resultsToSource[0];
                    }
                }


                //get distance to destination
                Location.distanceBetween(
                        destination.latitude,
                        destination.longitude,
                        point.latitude,
                        point.longitude,
                        resultsToDestination);

                //check if source is close enough
                if(resultsToDestination[0] < DEFAULT_TOLERANCE){
                    isDestinationCloseEnough = true;

                    //check if existing closest point
                    if(closestDestinationPoint !=null) {

                        //check if is closer than previous
                        if(resultsToDestination[0]<closestDestinationDistance){

                            //assign new closest point
                            closestDestinationPoint = point;
                            closestDestinationDistance = resultsToDestination[0];
                        }
                    }else{

                        //assign new closest point
                        closestDestinationPoint = point;
                        closestDestinationDistance = resultsToDestination[0];
                    }
                }
            }

            if(isSourceCloseEnough){

                Route route = new Route();
                route.setId(String.valueOf(i));
                route.setPolyline(polylines.get(i));
                route.setPoints(points);
                Stop stop = new Stop();
                stop.setName("Closest stop");
                stop.setPosition(closestSourcePoint);
                route.setClosestStop(stop);
                mSourceRoutes.put(route.getId(), route);
            }
            if(isDestinationCloseEnough){

                Route route = new Route();
                route.setId(String.valueOf(i));
                route.setPolyline(polylines.get(i));
                route.setPoints(points);
                Stop stop = new Stop();
                stop.setName("Closest stop");
                stop.setPosition(closestDestinationPoint);
                route.setClosestStop(stop);
                mDestinationRoutes.put(route.getId(), route);
            }
        }
    }

    private ArrayList<Intersection> checkRoutesIntersection(Route r1, Route r2){

        ArrayList<Intersection> intersections = new ArrayList<>();


        for(LatLng pointR1 :r1.getPoints()){

            boolean isAValidIntersection = true;
            for(LatLng pointR2 :r2.getPoints()){


                if(isAValidIntersection){

                    float distance[] = new float[2];
                    //check for closest stop to know route direction
                    if(pointR2.latitude == r2.getClosestStop().getPosition().latitude &&
                            pointR2.longitude == r2.getClosestStop().getPosition().longitude){

                        //if closest stop to destination has been reached then there is no sense
                        //to keep looking
                        isAValidIntersection = false;

                    }
                    //get distance to source
                    Location.distanceBetween(
                            pointR1.latitude,
                            pointR1.longitude,
                            pointR2.latitude,
                            pointR2.longitude,
                            distance);

                    if(distance[0] < DEFAULT_INTERSECTION_TOLERANCE) {


                        Intersection intersection = new Intersection();
                        intersection.setR1ID(r1.getId());
                        intersection.setR2ID(r2.getId());
                        Stop stopR1 = new Stop();
                        stopR1.setName("Stop R1");
                        stopR1.setPosition(pointR1);
                        intersection.setR1Stop(stopR1);
                        Stop stopR2 = new Stop();
                        stopR2.setName("Stop R2");
                        stopR2.setPosition(pointR2);
                        intersection.setR2Stop(stopR2);
                        intersections.add(intersection);

                    }
                }
            }

        }

        return intersections;

    }
}
