package com.robertoreym.findroute;

import android.location.Location;
import android.support.design.widget.Snackbar;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.PolyUtil;
import com.robertoreym.findroute.models.Instruction;
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

/**
 * Created by robertoreym on 30/03/16.
 */
public class RouteFinder {


    private static final int DEFAULT_TOLERANCE = 500;
    private static final int DEFAULT_INTERSECTION_TOLERANCE = 100;

    private HashMap<String,Route> mRoutes;

    private LatLng mSource;
    private LatLng mDestination;

    private SearchPoint mSourcePoint;
    private SearchPoint mDestinationPoint;

    public void setSource(LatLng source){

        mSource = source;
    }

    public void setDestination(LatLng destination){

        mDestination = destination;
    }

    public void setRoutes(ArrayList<Route> routesArray){

        //Pre-processing all routes
        mRoutes = preProcessingRoutes(routesArray);
    }

    public List<Result> searchRoutes(){

        //get available routes for source and destination points
        searchSourceAndDestinationPoints(mSource,mDestination);

        //check for common routes
        HashMap<String,Result> results = checkFirstLevel(mSourcePoint,mDestinationPoint);

        if(results.size() == 0){

            //check for intersections between source an destination routes
            results = checkSecondLevel(mSourcePoint,mDestinationPoint);

        }

        if(results.size() == 0){

            //check for intersections for 3 routes result
            results = checkThirdLevel(mSourcePoint, mDestinationPoint);

        }

        if(results.size()>0){


            List<Result> sortedResults = new ArrayList<Result>(results.values());

            Collections.sort(sortedResults, new Comparator<Result>() {
                @Override
                public int compare(Result lhs, Result rhs) {
                    return (int) (lhs.getDistance() - rhs.getDistance());
                }
            });

            return sortedResults;
        }

        return null;
    }

    private HashMap<String,Route> preProcessingRoutes(ArrayList<Route> routesArray){

        HashMap<String,Route> routes = new HashMap<>();

        //iterate through polylines
        for(int i1 = 0; i1< routesArray.size();i1++) {

            Route route = new Route();
            route.setId(String.valueOf(i1));
            route.setPolyline(routesArray.get(i1).getPolyline());
            route.setIntersectedRoutes(new HashMap<String, RouteIntersection>());
            routes.put(route.getId(), route);

            //get list of points
            route.setPoints(PolyUtil.decode(route.getPolyline()));

            //get stops from route
            ArrayList<Stop> stops1 = new ArrayList<>();
            int c1 = 0;
            for(LatLng point:route.getPoints()){
                Stop stop = new Stop();
                stop.setPosition(point);
                stop.setName(String.format("Stop %d", c1));
                stops1.add(stop);
                c1++;
            }
            route.setStops(stops1);

            //iterate through points inside route
            for(int i2 = 0; i2<route.getStops().size();i2++){

                //get current point
                Stop stop1 = route.getStops().get(i2);
                LatLng point1 = stop1.getPosition();

                //go through all other polylines get possible intersections
                for(int i3 = 0; i3<routesArray.size();i3++){

                    //Create new route intersection
                    RouteIntersection routeIntersection = new RouteIntersection();
                    routeIntersection.setRouteID(String.valueOf(i3));
                    routeIntersection.setPointIntersections(new ArrayList<PointIntersection>());

                    //as long is not the same route
                    if(i3 != i1) {

                        //get list of points of route to compare with
                        List<LatLng> points2 = PolyUtil.decode(routesArray.get(i3).getPolyline());

                        //get stops from route
                        ArrayList<Stop> stops2 = new ArrayList<>();
                        int c2 = 0;
                        for(LatLng point:points2){
                            Stop stop = new Stop();
                            stop.setPosition(point);
                            stop.setName(String.format("Stop %d", c2));
                            stops2.add(stop);
                            c2++;
                        }
                        //iterate through points on route to compare
                        for(int i4 = 0; i4<stops2.size();i4++) {

                            float distance[] = new float[2];

                            //get current point
                            Stop stop2 = stops2.get(i4);
                            LatLng point2 = stop2.getPosition();

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
                                pointIntersection.setR1Stop(stop1);
                                pointIntersection.setR1ID(String.format("%d", i1));

                                //Assign r2 information
                                pointIntersection.setR2Stop(stop2);
                                pointIntersection.setR2ID(String.format("%d", i3));

                                //add to route intersection
                                routeIntersection.getPointIntersections().add(pointIntersection);

                            }

                        }

                        //check for a valid route intersection object
                        if(routeIntersection.getPointIntersections().size()>0){

                            if(route.getIntersectedRoutes().containsKey(routeIntersection.getRouteID())) {

                                //add point intersections to current route intersection
                                route.getIntersectedRoutes().get(routeIntersection.getRouteID())
                                        .getPointIntersections().addAll(routeIntersection.getPointIntersections());

                            }else{

                                //Add route intersection to route
                                route.getIntersectedRoutes().put(routeIntersection.getRouteID(), routeIntersection);
                            }
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
            Stop closestSourceStop = null;
            Stop closestDestinationStop = null;

            Route currentRoute = mRoutes.get(String.valueOf(i));

            //iterate through points
            for(int i2 = 0; i2<currentRoute.getStops().size();i2++){

                float distanceToSource[] = new float[2];
                float distanceToDestination[] = new float[2];

                //get current point
                Stop stop = currentRoute.getStops().get(i2);
                LatLng point = stop.getPosition();

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
                    if(closestSourceStop !=null) {

                        //check if is closer than previous
                        if(distanceToSource[0]<closestSourceDistance){

                            //assign new closest point
                            closestSourceStop = stop;
                            closestSourceDistance = distanceToSource[0];
                        }
                    }else{

                        //assign new closest point
                        closestSourceStop = stop;
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
                    if(closestDestinationStop !=null) {

                        //check if is closer than previous
                        if(distanceToDestination[0]<closestDestinationDistance){

                            //assign new closest point
                            closestDestinationStop = stop;
                            closestDestinationDistance = distanceToDestination[0];
                        }
                    }else{

                        //assign new closest point
                        closestDestinationStop = stop;
                        closestDestinationDistance = distanceToDestination[0];
                    }
                }
            }

            if(isSourceCloseEnough){

                mSourcePoint.getClosestStops().put(currentRoute.getId(), closestSourceStop);
                mSourcePoint.getAvailableRoutes().put(currentRoute.getId(),currentRoute);
            }
            if(isDestinationCloseEnough){

                mDestinationPoint.getClosestStops().put(currentRoute.getId(), closestDestinationStop);
                mDestinationPoint.getAvailableRoutes().put(currentRoute.getId(), currentRoute);
            }
        }

    }

    private Trajectory getTrajectory(String encodedPolyline,LatLng p1,LatLng p2){

        List<LatLng> points = PolyUtil.decode(encodedPolyline);
        ArrayList<LatLng> trajectoryPoints = new ArrayList<>();
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

                        float distance[] = new float[2];
                        //get distance to source
                        Location.distanceBetween( p1.latitude, p1.longitude,  point.latitude, point.longitude, distance);

                        //check if is close enough
                        if (distance[0] < DEFAULT_INTERSECTION_TOLERANCE) {

                            totalDistance = 0.0f;
                            previousPoint = null;
                            trajectoryPoints.clear();
                        }
                        trajectoryPoints.add(point);

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

                    float distance[] = new float[2];
                    //get distance to source
                    Location.distanceBetween( p1.latitude, p1.longitude,  point.latitude, point.longitude, distance);

                    //check if is close enough
                    if (distance[0] < DEFAULT_INTERSECTION_TOLERANCE) {
                        totalDistance = 0.0f;
                        previousPoint = null;
                        trajectoryPoints.clear();
                    }
                    trajectoryPoints.add(point);
                }

            }else if(isP2Found){

                return null;

            }
        }

        Trajectory trajectory =  new Trajectory();
        trajectory.setPoints(trajectoryPoints);
        trajectory.setDistance(totalDistance);

        return trajectory;
    }

    private HashMap<String,Result> checkFirstLevel(SearchPoint sourcePoint,SearchPoint destinationPoint){

        HashMap<String,Result> results = new HashMap<>();

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
                    result.setRoutes(route.getId());
                    result.setTrajectories(new ArrayList<Trajectory>());
                    result.getTrajectories().add(trajectory);


                    //Check for the same result
                    if(results.containsKey(result.getRoutes())){

                        //compare distances
                        if(results.get(result.getRoutes()).getDistance()>result.getDistance()){

                            //replace result
                            results.put(result.getRoutes(),result);
                        }

                    }else{

                        //add result
                        results.put(result.getRoutes(), result);
                    }
                }
            }
        }

        return results;

    }

    private HashMap<String,Result> checkSecondLevel(SearchPoint sourcePoint,SearchPoint destinationPoint){

        HashMap<String,Result> results = new HashMap<>();


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
                            result.setRoutes(String.format("%s%s",sourceRoute.getId(),destinationRoute.getId()));
                            result.setTrajectories(new ArrayList<Trajectory>());
                            result.getTrajectories().add(trajectory1);
                            result.getTrajectories().add(trajectory2);

                            //Check for the same result
                            if(results.containsKey(result.getRoutes())){

                                //compare distances
                                if(results.get(result.getRoutes()).getDistance()>result.getDistance()){

                                    //replace result
                                    results.put(result.getRoutes(),result);
                                }

                            }else{

                                //add result
                                results.put(result.getRoutes(), result);
                            }
                        }
                    }
                }
            }
        }



        return results;
    }

    private HashMap<String,Result> checkThirdLevel(SearchPoint sourcePoint,SearchPoint destinationPoint){

        HashMap<String,Result> results = new HashMap<>();

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

                                //get source and destination routes to be used
                                Route sourceRoute = sourcePoint.getAvailableRoutes().get(sourceEntry.getKey());
                                Route destinationRoute = destinationPoint.getAvailableRoutes().get(pointIntersection2.getR2ID());

                                //get trajectory from source to first intersection
                                Trajectory trajectory1 = getTrajectory(sourceRoute.getPolyline(),
                                        sourcePoint.getClosestStops().get(sourceRoute.getId()).getPosition(),
                                        pointIntersection1.getR1Stop().getPosition());

                                //get trajectory from intersection one to intersection 2
                                Trajectory trajectory2 = getTrajectory(currentIntersectedRoute.getPolyline(),
                                        pointIntersection1.getR2Stop().getPosition(),
                                        pointIntersection2.getR1Stop().getPosition());

                                //get trajectory from intersection 2 to destination
                                Trajectory trajectory3 = getTrajectory(destinationRoute.getPolyline(),
                                        pointIntersection2.getR2Stop().getPosition(),
                                        destinationPoint.getClosestStops().get(destinationRoute.getId()).getPosition());


                                if(trajectory1!=null && trajectory2!=null && trajectory3!=null) {
                                    //add trajectories to result
                                    Result result = new Result();
                                    result.setRoutes(String.format("%s,%s,%s",
                                            sourceRoute.getId(),currentIntersectedRoute.getId(),destinationRoute.getId()));

                                    result.setTrajectories(new ArrayList<Trajectory>());
                                    result.getTrajectories().add(trajectory1);
                                    result.getTrajectories().add(trajectory2);
                                    result.getTrajectories().add(trajectory3);

                                    //Check for the same result
                                    if(results.containsKey(result.getRoutes())){

                                        //compare distances
                                        if(results.get(result.getRoutes()).getDistance()>result.getDistance()){

                                            //replace result with shorter distance
                                            results.put(result.getRoutes(),result);
                                        }

                                    }else{

                                        //add result
                                        results.put(result.getRoutes(), result);
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }

        //remove results alike

        return results;
    }


}
