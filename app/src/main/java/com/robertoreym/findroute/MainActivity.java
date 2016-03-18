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
            "u~f}B`ifvRxIv@`IfAbCTbHp@xIpAlHdAfGV|Fb@jGl@`GbD~GpC`JCrFY",
            "ube}BbufvRiNgBiK}AiNuA_MgBiLkAqGs@_B}HoAoJ_DiNw@kLy@_JXkOn@}LqDaKwEgMo@kLgCsOqEsG",
            "a`h}BtvcvRx@sKyCgJwEyJo@mI_@aOqD_JgFaG_D}H_BsKiGiGaLcH_L{KyEiKgC{GyE{H_EyJwBaKqGFgIpCqHnF_F~EgEhDqDpBo@fGgC~IwD|LqCpFgI|AiMsD"
    };

    private GoogleMap mMap;

    private LatLng mSource;
    private LatLng mDestination;
    private SearchPoint mSourcePoint;
    private SearchPoint mDestinationPoint;

    private ArrayList<String> mPolylines;
    private HashMap<String,Route> mRoutes;
    private HashMap<String,Route> mSourceRoutes;
    private HashMap<String,Route> mDestinationRoutes;

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
        mDestination = new LatLng(20.707888,-103.377678);

        mPolylines = new ArrayList<>();
        mSourceRoutes = new HashMap<>();
        mDestinationRoutes = new HashMap<>();

        for(int i = 0; i<POLYLINES.length;i++){

            mPolylines.add(POLYLINES[i]);
        }

        mFabSearch = (FloatingActionButton) findViewById(R.id.fab);
        assert mFabSearch != null;
        mFabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                searchRoutes();
            }
        });



    }

    public void onResume(){
        super.onResume();

        //Preprocessing all routes
        mRoutes = preProcessingRoutes(mPolylines);

    }
    private void searchRoutes(){

        //get available routes for source and destination points
        searchSourceAndDestinationPoints(mSource,mDestination);

        //check for common routes
        ArrayList<Result> results = checkFirstLevel(mSourcePoint,mDestinationPoint);

        if(results.size() == 0){

            //check for intersections between source an destination routes
            results = checkSecondLevel(mSourcePoint,mDestinationPoint);

        }

        if(results.size() == 0){

            //check for intersections for 3 routes result
            results = checkThirdLevel(mSourcePoint, mDestinationPoint);

        }

        if(results.size()>0){

            //get best result possible
            Result winnerResult = getWinnerResult(results);

            //paint winner result
            paintResult(winnerResult);

            Snackbar.make(mFabSearch,String.format("Distancia de la ruta: %f",winnerResult.getDistance()),Snackbar.LENGTH_SHORT).show();
        }
    }

    private HashMap<String,Route> preProcessingRoutes(ArrayList<String> polylines){

        HashMap<String,Route> routes = new HashMap<>();

        //iterate through polylines
        for(int i1 = 0; i1< polylines.size();i1++) {

            Route route = new Route();
            route.setId(String.valueOf(i1));
            route.setPolyline(polylines.get(i1));
            route.setIntersectedRoutes(new HashMap<String, RouteIntersection>());
            routes.put(route.getId(),route);

            //get list of points
            route.setPoints(PolyUtil.decode(polylines.get(i1)));

            //iterate through points inside route
            for(int i2 = 0; i2<route.getPoints().size();i2++){

                //get current point
                LatLng point1 = route.getPoints().get(i2);

                //go through all other polylines get possible intersections
                for(int i3 = 0; i3<polylines.size();i3++){

                    //Create new route intersection
                    RouteIntersection routeIntersection = new RouteIntersection();
                    routeIntersection.setRouteID(String.valueOf(i3));
                    routeIntersection.setPointIntersections(new ArrayList<PointIntersection>());

                    //as long is not the same route
                    if(i3 != i1) {

                        //get list of points of route to compare with
                        List<LatLng> points2 = PolyUtil.decode(polylines.get(i3));

                        //iterate through points on route to compare
                        for(int i4 = 0; i4<points2.size();i4++) {

                            float distance[] = new float[2];

                            //get current point
                            LatLng point2 = points2.get(i4);

                            //get distance to source
                            Location.distanceBetween(
                                    point1.latitude,
                                    point1.longitude,
                                    point2.latitude,
                                    point2.longitude,
                                    distance);

                            //check if is close enough
                            if (distance[0] < DEFAULT_INTERSECTION_TOLERANCE) {

                                PointIntersection pointIntersection = new PointIntersection();

                                //Assign r1 information
                                Stop stop = new Stop();
                                stop.setName(String.format("Stop R%d P%d", i1, i2));
                                stop.setPosition(point1);
                                pointIntersection.setR1Stop(stop);
                                pointIntersection.setR1ID(String.format("%d", i1));

                                //Assign r2 information
                                stop = new Stop();
                                stop.setName(String.format("Stop R%d P%d", i3,i2));
                                stop.setPosition(point2);
                                pointIntersection.setR2Stop(stop);
                                pointIntersection.setR2ID(String.format("%d", i3));

                                //add to route intersection
                                routeIntersection.getPointIntersections().add(pointIntersection);

                            }
                        }

                        //check for a valid route intersection object
                        if(routeIntersection.getPointIntersections().size()>0){

                            //Add route intersection to route
                            route.getIntersectedRoutes().put(routeIntersection.getRouteID(),routeIntersection);
                        }

                    }

                }
            }

        }

        return routes;
    }

    private void searchSourceAndDestinationPoints(LatLng source,LatLng destination){


        mSourcePoint = new SearchPoint();
        mSourcePoint.setPosition(mSource);
        mSourcePoint.setClosestStops(new HashMap<String, Stop>());
        mSourcePoint.setAvailableRoutes(new HashMap<String, Route>());

        mDestinationPoint = new SearchPoint();
        mDestinationPoint.setPosition(mDestination);
        mDestinationPoint.setClosestStops(new HashMap<String, Stop>());
        mDestinationPoint.setAvailableRoutes(new HashMap<String, Route>());

        //iterate through routes
        for(int i = 0; i< mRoutes.size();i++){

            boolean isSourceCloseEnough = false;
            boolean isDestinationCloseEnough = false;
            float closestSourceDistance = 0;
            float closestDestinationDistance = 0;
            LatLng closestSourcePoint = null;
            LatLng closestDestinationPoint = null;

            Route currentRoute = mRoutes.get(String.valueOf(i));

            //iterate through points
            for(int i2 = 0; i2<currentRoute.getPoints().size();i2++){

                float distanceToSource[] = new float[2];
                float distanceToDestination[] = new float[2];

                //get current point
                LatLng point = currentRoute.getPoints().get(i2);

                //get distance to source
                Location.distanceBetween(
                        source.latitude,
                        source.longitude,
                        point.latitude,
                        point.longitude,
                        distanceToSource);

                //check if source is close enough
                if(distanceToSource[0] < DEFAULT_TOLERANCE){
                    isSourceCloseEnough = true;

                    //check if existing closest point
                    if(closestSourcePoint !=null) {

                        //check if is closer than previous
                        if(distanceToSource[0]<closestSourceDistance){

                            //assign new closest point
                            closestSourcePoint = point;
                            closestSourceDistance = distanceToSource[0];
                        }
                    }else{

                        //assign new closest point
                        closestSourcePoint = point;
                        closestSourceDistance = distanceToSource[0];
                    }
                }


                //get distance to destination
                Location.distanceBetween(
                        destination.latitude,
                        destination.longitude,
                        point.latitude,
                        point.longitude,
                        distanceToDestination);

                //check if destination is close enough
                if(distanceToDestination[0] < DEFAULT_TOLERANCE){
                    isDestinationCloseEnough = true;

                    //check if existing closest point
                    if(closestDestinationPoint !=null) {

                        //check if is closer than previous
                        if(distanceToDestination[0]<closestDestinationDistance){

                            //assign new closest point
                            closestDestinationPoint = point;
                            closestDestinationDistance = distanceToDestination[0];
                        }
                    }else{

                        //assign new closest point
                        closestDestinationPoint = point;
                        closestDestinationDistance = distanceToDestination[0];
                    }
                }
            }

            if(isSourceCloseEnough){

                Stop stop = new Stop();
                stop.setName("Closest stop");
                stop.setPosition(closestSourcePoint);
                mSourcePoint.getClosestStops().put(currentRoute.getId(), stop);
                mSourcePoint.getAvailableRoutes().put(currentRoute.getId(),currentRoute);
            }
            if(isDestinationCloseEnough){

                Stop stop = new Stop();
                stop.setName("Closest stop");
                stop.setPosition(closestDestinationPoint);
                mDestinationPoint.getClosestStops().put(currentRoute.getId(), stop);
                mDestinationPoint.getAvailableRoutes().put(currentRoute.getId(), currentRoute);
            }
        }

    }

    private ArrayList<Result> checkFirstLevel(SearchPoint sourcePoint,SearchPoint destinationPoint){

        ArrayList<Result> results = new ArrayList<>();

        //go through source routes
        for (HashMap.Entry<String, Route> entry : sourcePoint.getAvailableRoutes().entrySet()) {

            //check for a common route
            if(destinationPoint.getAvailableRoutes().containsKey(entry.getKey())){

                //get common route
                Route route = entry.getValue();

                Trajectory trajectory = getTrajectory(entry.getValue().getPolyline(),
                        sourcePoint.getClosestStops().get(route.getId()).getPosition(),
                        destinationPoint.getClosestStops().get(route.getId()).getPosition());

                if(trajectory!=null){

                    //create result
                    Result result = new Result();
                    result.setTrajectories(new ArrayList<Trajectory>());
                    result.getTrajectories().add(trajectory);
                    paintResult(result);

                    results.add(result);
                }
            }
        }

        return results;

    }

    private ArrayList<Result> checkSecondLevel(SearchPoint sourcePoint,SearchPoint destinationPoint){

        ArrayList<Result> results = new ArrayList<>();


        //go through source routes
        for (HashMap.Entry<String, Route> sourceEntry : sourcePoint.getAvailableRoutes().entrySet()) {

            for(HashMap.Entry<String, Route> destinationEntry : destinationPoint.getAvailableRoutes().entrySet()){

                //check if source intersects with current destination route
                if(sourceEntry.getValue().getIntersectedRoutes().containsKey(destinationEntry.getKey())){

                    ArrayList<PointIntersection> pointIntersections = sourceEntry.getValue().getIntersectedRoutes().get(destinationEntry.getKey()).getPointIntersections();

                    //iterate intersections
                    for(PointIntersection pointIntersection : pointIntersections){

                        Route sourceRoute = sourcePoint.getAvailableRoutes().get(pointIntersection.getR1ID());
                        Route destinationRoute = destinationPoint.getAvailableRoutes().get(pointIntersection.getR2ID());

                        Trajectory trajectory1 = getTrajectory(sourceRoute.getPolyline(),
                                sourcePoint.getClosestStops().get(sourceRoute.getId()).getPosition(),
                                pointIntersection.getR1Stop().getPosition());

                        Trajectory trajectory2 = getTrajectory(destinationRoute.getPolyline(),
                                pointIntersection.getR2Stop().getPosition(),
                                destinationPoint.getClosestStops().get(destinationRoute.getId()).getPosition());


                        if(trajectory1!=null && trajectory2!=null) {
                            //add trajectories to result
                            Result result = new Result();
                            result.setTrajectories(new ArrayList<Trajectory>());
                            result.getTrajectories().add(trajectory1);
                            result.getTrajectories().add(trajectory2);

                            results.add(result);
                        }
                    }
                }
            }
        }



        return results;
    }

    private ArrayList<Result> checkThirdLevel(SearchPoint sourcePoint,SearchPoint destinationPoint){

        ArrayList<Result> results = new ArrayList<>();

        ArrayList<PointIntersection> pointIntersections1 = new ArrayList<PointIntersection>();
        ArrayList<PointIntersection> pointIntersections2 = new ArrayList<PointIntersection>();

        //go through source routes
        for (HashMap.Entry<String, Route> sourceEntry : sourcePoint.getAvailableRoutes().entrySet()) {

            //go through source intersected routes
            for(HashMap.Entry<String,RouteIntersection>intersectedSourceEntry: sourceEntry.getValue().getIntersectedRoutes().entrySet()){

                Route currentIntersectedRoute = mRoutes.get(intersectedSourceEntry.getKey());

                //go through destination routes
                for(HashMap.Entry<String, Route> destinationEntry : destinationPoint.getAvailableRoutes().entrySet()){

                    // find an intersection with current intersected route
                    if(currentIntersectedRoute.getIntersectedRoutes().containsKey(destinationEntry.getKey())){

                        //go though pointer intersections for first stop
                        for(PointIntersection pointIntersection1:intersectedSourceEntry.getValue().getPointIntersections()){

                            RouteIntersection currentRouteIntersection =
                                    currentIntersectedRoute.getIntersectedRoutes().get(destinationEntry.getKey());

                            for(PointIntersection pointIntersection2 : currentRouteIntersection.getPointIntersections()){


                                Route sourceRoute = sourcePoint.getAvailableRoutes().get(sourceEntry.getKey());
                                Route destinationRoute = destinationPoint.getAvailableRoutes().get(pointIntersection2.getR2ID());


                                Trajectory trajectory1 = getTrajectory(sourceRoute.getPolyline(),
                                        sourcePoint.getClosestStops().get(sourceRoute.getId()).getPosition(),
                                        pointIntersection1.getR1Stop().getPosition());

                                Trajectory trajectory2 = getTrajectory(currentIntersectedRoute.getPolyline(),
                                        pointIntersection1.getR2Stop().getPosition(),
                                        pointIntersection2.getR1Stop().getPosition());

                                Trajectory trajectory3 = getTrajectory(destinationRoute.getPolyline(),
                                        pointIntersection2.getR2Stop().getPosition(),
                                        destinationPoint.getClosestStops().get(destinationRoute.getId()).getPosition());


                                if(trajectory1!=null && trajectory2!=null && trajectory3!=null) {
                                    //add trajectories to result
                                    Result result = new Result();
                                    result.setTrajectories(new ArrayList<Trajectory>());
                                    result.getTrajectories().add(trajectory1);
                                    result.getTrajectories().add(trajectory2);
                                    result.getTrajectories().add(trajectory3);

                                    results.add(result);
                                }

                            }
                        }




                    }

                }
            }

        }

        return results;
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


    private Trajectory getTrajectory(String encodedPolyline,LatLng p1,LatLng p2){

        List<LatLng> points = PolyUtil.decode(encodedPolyline);
        boolean isPointOnPath = false;
        boolean isP1Found = false;
        boolean isP2Found = false;

        float totalDistance = 0.0f;
        LatLng previousPoint = null;

        for (Iterator<LatLng> iterator = points.iterator(); iterator.hasNext();) {

            LatLng point = iterator.next();

            if((point.latitude == p1.latitude && point.longitude == p1.longitude)){

                isP1Found = true;
            }

            if((point.latitude == p2.latitude && point.longitude == p2.longitude)){

                isP2Found = true;
            }

            //check if p1 was found first to check direction
            if(isP1Found){

                if(isP2Found){

                    //check if its destination point
                    if((point.latitude == p2.latitude && point.longitude == p2.longitude)){

                        if(previousPoint == null){

                            previousPoint = point;
                        }else{

                            float distance[] = new float[2];
                            Location.distanceBetween(previousPoint.latitude,previousPoint.longitude,point.latitude,point.longitude,distance);
                            previousPoint = point;
                            totalDistance+=distance[0];
                        }

                    }else{

                        iterator.remove();
                    }
                }else{

                    if(previousPoint == null){

                        previousPoint = point;
                    }else{

                        float distance[] = new float[2];
                        Location.distanceBetween(previousPoint.latitude,previousPoint.longitude,point.latitude,point.longitude,distance);
                        previousPoint = point;
                        totalDistance+=distance[0];
                    }
                }

            }else if(isP2Found){

                return null;

            }else {

                iterator.remove();
            }
        }

        Trajectory trajectory =  new Trajectory();
        trajectory.setPoints(points);
        trajectory.setDistance(totalDistance);

        return trajectory;
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

    private Result getWinnerResult(ArrayList<Result>results){

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

        return winnerResult;
    }


}
