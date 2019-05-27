package com.leombrosoft.demonhunter;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.jorgecastilloprz.FABProgressCircle;
import com.github.jorgecastilloprz.listeners.FABProgressListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.maps.StreetViewPanoramaView;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by neku on 02/11/15.
 */
public class DemonScopeFragment
    extends Fragment
    implements OnMapReadyCallback, LocationListener, GoogleMap.OnInfoWindowClickListener{

    public static final String TAG = MainActivity.TAG + "/DemonScope";
    private static final int QUERY_DELAY = 1000;

    private static final int FIND_DEMON = 1;
    public static final int PERMISSION_REQUEST_LOCATION = 1;

    private GoogleMap mMap;
    private GameValues gv;
    private DatabaseManager dbm;
    private HashMap<String, CustomMarker> markerinfos;
    private Handler mHandler;
    private CoordinatorLayout layout;
    private FragmentActivity activity;
    private LocationManager locman;
    private boolean isSearching = false;
    private boolean demonsOnMap = false;
    private boolean continueMusic = false;
    private LatLng playerPos;
    private String currMarkID;
    private MarkerOptions player;
    private int demonNo = -1;
    private int MAX_ITER;
    private int inserted = 0;
    private int iterations = 0;

    private String playername;
    private SupportMapFragment mapFragment;
    private StreetViewPanoramaView svpView;
    private Snackbar searching;
    private FloatingActionButton fab_ok;
    private FloatingActionButton fab_no;
    private FABProgressCircle progressCircle;
    private BitmapDescriptor player_marker;
    private BitmapDescriptor demon_marker;

    private View.OnClickListener fabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case R.id.FABMap:
                    if (!isSearching) {
                        if (demonsOnMap) {
                            Snackbar
                                    .make(layout.findViewById(R.id.MapLayout), R.string.demons_present, Snackbar.LENGTH_INDEFINITE)
                                    .setAction(android.R.string.ok, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            searchDemons();
                                        }
                                    })
                                    .show();
                        } else {
                            searchDemons();
                        }
                    } else {
                        if (!searching.isShown()) searching.show();
                    }
                    break;

                case R.id.FABMapCross:
                    if (locman.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        CharSequence label = getText(R.string.not_ready);
                        Snackbar
                                .make(layout.findViewById(R.id.MapLayout), label, Snackbar.LENGTH_LONG)
                                .show();
                        requestUpdates();
                    } else {
                        makeSnackbarNotActive();
                    }
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        gv = GameValues.get(getContext());
        dbm = DatabaseManager.get(getContext());
        markerinfos = new HashMap<>();
        activity = getActivity();
        locman = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
        playername = getString(R.string.player);

        svpView = new StreetViewPanoramaView(getActivity(), new StreetViewPanoramaOptions());
        svpView.onCreate(null);
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layout = (CoordinatorLayout) inflater.inflate(R.layout.activity_demon_scope, container, false);

        Toolbar t = (Toolbar)activity.findViewById(R.id.toolbar);
        t.setBackgroundColor(Color.argb(0, 0, 0, 0));
        t.setTitle("");

        fab_ok = (FloatingActionButton) layout.findViewById(R.id.FABMap);
        fab_no = (FloatingActionButton) layout.findViewById(R.id.FABMapCross);
        progressCircle = (FABProgressCircle) layout.findViewById(R.id.progress2);
        progressCircle.attachListener(new FABProgressListener() {
            @Override
            public void onFABProgressAnimationEnd() {
                progressCircle.hide();
                fab_no.hide();
                fab_ok.setVisibility(View.VISIBLE);
                progressCircle.setVisibility(View.GONE);
            }
        });
        progressCircle.measure(15, 15); // hack to avoid nullpointerexception

        fab_ok.setOnClickListener(fabClickListener);
        fab_no.setOnClickListener(fabClickListener);
        makeFABs();

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            mapFragment.setRetainInstance(true);
            getChildFragmentManager().beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        searching = genSearching();

        return layout;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!continueMusic)
            BackgroundMusicManager.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        continueMusic = false;
        BackgroundMusicManager.start(getContext(), BackgroundMusicManager.MUSIC_WHEN_MOONS_REACHING_OUT_STARS);
        makeFABs();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setInfoWindowAdapter(new InfoAdapter(activity.getLayoutInflater()));
        mMap.setOnInfoWindowClickListener(this);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        player_marker = BitmapDescriptorFactory.fromResource(R.drawable.protagonist);
        demon_marker = BitmapDescriptorFactory.fromResource(R.drawable.marker);

        playerPos = gv.getLastLocation();
        if (playerPos.latitude != 0 || playerPos.longitude != 0) {
            updatePlayerPosOnMap(false, 0);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions();
        }

        requestUpdates();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        makeFABs();
    }

    @Override
    public void onProviderEnabled(String provider) {
        makeFABs();
    }

    @Override
    public void onProviderDisabled(String provider) {
        makeFABs();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (progressCircle.isShown()) {
            progressCircle.beginFinalAnimation();
            progressCircle.beginFinalAnimation();
            progressCircle.beginFinalAnimation();  // three is the magic number! the library is bugged
        }
        int time = 200;
        if (playerPos.latitude == 0 && playerPos.longitude == 0) {
           time = 1000;
        }
        playerPos = new LatLng(location.getLatitude(), location.getLongitude());
        gv.setLastLocation(playerPos);
        if (mMap != null) {
            updatePlayerPosOnMap(true, time);
        }
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
        Log.d(TAG, "In onInfoWindowClick");
        CustomMarker m1  = markerinfos.get(marker.getId());
        if (m1 == null) m1 = markerinfos.get("PLAYER");
        final CustomMarker m = m1;
        final int id = m.getDemonID();

        if (id != -1) {
            final Demon d = dbm.getDemon(id);
            AlertDialog.Builder build = new AlertDialog.Builder(activity);
            build.setMessage(R.string.demon_catch_confirm);
            build.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(activity, FindDemonActivity.class);
                    intent.putExtra("DEMON", id);
                    intent.putExtra("LAT", m.getLatitude());
                    intent.putExtra("LNG", m.getLongitude());
                    intent.putExtra("DISTANCE", m.getDistance());
                    currMarkID = marker.getId();
                    startActivityForResult(intent, FIND_DEMON);
                }
            });
            build.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            build.show();
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(m.getLatLng(), 16), 200, null);
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent extra) {
        super.onActivityResult(reqCode, resCode, extra);

        if (reqCode == FIND_DEMON) {
            switch (resCode) {
                case Activity.RESULT_FIRST_USER: {
                    CustomMarker m = markerinfos.get(currMarkID);
                    dbm.deleteMarker(m.getDbKey());
                    markerinfos.clear();
                    mMap.clear();
                    putDemonMarkers();
                    if (player!=null) {
                        mMap.addMarker(player);
                        Log.d(TAG, "Markerinfos put marker " + "PLAYER");
                        markerinfos.put("PLAYER", new CustomMarker(player.getPosition(), 0.0, -1));
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    @TargetApi(23)
    private void checkPermissions() {
        boolean condition = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (condition) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Snackbar
                        .make(layout.findViewById(R.id.MapLayout),
                                getText(R.string.why_permissions),
                                Snackbar.LENGTH_LONG)
                        .show();
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
            }
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        Log.d(TAG, "In onRequestPermissionResult");
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION: {
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                    requestUpdates();
                } else {
                    AlertDialog.Builder b = new AlertDialog.Builder(activity);
                    b.setMessage(getText(R.string.why_permissions));
                    b.setNeutralButton(getText(android.R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finish();
                        }
                    });
                    b.show();
                }
            }
            break;
            default: break;
        }
    }

    private void requestUpdates() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 10, this);
        }
    }

    private void updatePlayerPosOnMap(boolean animate, int time) {
        mMap.clear();
        markerinfos.remove("PLAYER");
        player = new MarkerOptions()
                .position(playerPos)
                .title(playername)
                .icon(player_marker);
        mMap.addMarker(player);
        putDemonMarkers();
        Log.d(TAG, "Markerinfos put marker " + "PLAYER");
        markerinfos.put("PLAYER", new CustomMarker(playerPos, 0.0, -1));
        if (animate) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(playerPos, 16), time, null);
        else mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(playerPos, 16));
    }

    private void putDemonMarkers() {
        if (markerinfos.isEmpty()) {
            ArrayList<CustomMarker> arr = dbm.getMarkers();
            if (arr.isEmpty()) {
                demonsOnMap = false;
                return;
            }
            for (CustomMarker mt : arr) {
                String demonName = dbm.getDemon(mt.getDemonID()).getName();
                Marker mark = mMap.addMarker(new MarkerOptions()
                                .position(mt.getLatLng())
                                .title(demonName)
                                .icon(demon_marker)
                );
                Log.d(TAG, "Markerinfos put marker " + mark.getId());
                markerinfos.put(mark.getId(), mt);
            }
            demonsOnMap = true;
        } else {
            MarkerOptions m = new MarkerOptions().icon(demon_marker);
            Collection<CustomMarker> coll = markerinfos.values();
            HashMap<String, CustomMarker> markerinfos2 = new HashMap<>();
            for (CustomMarker cm : coll) {
                String demonName = dbm.getDemon(cm.getDemonID()).getName();
                m.position(cm.getLatLng())
                        .title(demonName);
                Marker m1 = mMap.addMarker(m);
                markerinfos2.put(m1.getId(), cm);
            }
            markerinfos.clear();
            Log.d(TAG, "Markerinfos put markers " + markerinfos2);
            markerinfos.putAll(markerinfos2);
        }
    }

    private void makeFABs() {
        if (locman.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            long lastloc = gv.getLastLocationTime();
            if (System.currentTimeMillis() > lastloc + 300000) {  // 5 min in millis
                progressCircle.show();
                fab_no.setVisibility(View.VISIBLE);
                fab_ok.setVisibility(View.INVISIBLE);
            } else {
                progressCircle.hide();
                fab_no.setVisibility(View.INVISIBLE);
                fab_ok.setVisibility(View.VISIBLE);
            }
        } else {
            fab_no.setVisibility(View.VISIBLE);
            fab_ok.setVisibility(View.INVISIBLE);
            progressCircle.hide();
        }
    }

    private void makeSnackbarNotActive() {
        CharSequence text = getText(R.string.request_gps);
        Snackbar
                .make(layout.findViewById(R.id.MapLayout),
                        text, Snackbar.LENGTH_LONG)
                .setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .show();
    }

    private Snackbar genSearching() {
        String text = getString(R.string.search_demons);
        return Snackbar.make(layout.findViewById(R.id.MapLayout), text, Snackbar.LENGTH_INDEFINITE).setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                searching = genSearching();
            }
        });

    }

    private void searchDemons() {
        if (gv.canSearch()) {
            int howmany = 5 + dbm.getItem(Item.UPGRADE).getQuantity() * 5;
            startGenerateMarkers(playerPos, 400, howmany);
            layout.findViewById(R.id.generate_progressbar).setVisibility(View.VISIBLE);
            searching.show();
            isSearching = true;
        } else {
            long[] res = gv.explainRejection();
            int[] hms = takeTime(res[1]);
            String text = getString(R.string.demonscope_low_battery, res[0], hms[0], hms[1], hms[2]);
            Snackbar
                    .make(layout.findViewById(R.id.MapLayout), text, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private int[] takeTime (long startT) {
        int secondsMillis = 1000,
                minutesMillis = secondsMillis * 60,
                hoursMillis = minutesMillis * 60;
        long diff = (startT + 86400 * secondsMillis) - System.currentTimeMillis();

        int hours = (int)(diff / hoursMillis);
        diff = diff % hoursMillis;

        int minutes = (int)(diff / minutesMillis);
        diff = diff % minutesMillis;

        int seconds = (int)(diff / secondsMillis);

        return new int[] { hours, minutes, seconds };
    }

    private void startGenerateMarkers(LatLng point, double radius, int howMany) {

        MAX_ITER = 10*howMany;
        if (demonNo == -1) demonNo = dbm.getTotalDemons();
        dbm.flushMarkers();
        markerinfos.clear();
        mMap.clear();
        mMap.addMarker(player);
        Log.d(TAG, "Markerinfos put marker " + "PLAYER");
        markerinfos.put("PLAYER", new CustomMarker(player.getPosition(), 0.0, -1));
        Log.d(TAG,  "demonNo is " + demonNo);
        generatePoint(point.latitude, point.longitude, radius, howMany);
    }

    private void generatePoint(final double latitude, final double longitude, final double radius, final int quantity) {
        if (iterations > MAX_ITER) {
            onGeneratePointReturn(false);
        } else {
            Log.d(TAG,  "In iteration " + iterations + ", inserted " + inserted);
            iterations++;
            if (inserted < quantity) {
                Random r = new Random();
                double x0 = latitude;
                double y0 = longitude;
                double radiusInDeg = radius / 111300f;
                double u = r.nextDouble();
                double v = r.nextDouble();
                double w = radiusInDeg * Math.sqrt(u);
                double t = 2 * Math.PI * v;
                double x = w * Math.cos(t);
                double y = w * Math.sin(t);
                final int demon = 1 + r.nextInt(demonNo - 1);

                final LatLng loc = new LatLng(x0 + x, y0 + y);
                float[] distResult = new float[1];
                Location.distanceBetween(x0, y0, loc.latitude, loc.longitude, distResult);
                final float distance = distResult[0];

                svpView.getStreetViewPanoramaAsync(new OnStreetViewPanoramaReadyCallback() {
                    @Override
                    public void onStreetViewPanoramaReady(final StreetViewPanorama streetViewPanorama) {
                        streetViewPanorama.setPosition(loc, 20);
                        Log.d(TAG,  "Checking " + loc);
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (streetViewPanorama.getLocation() != null) {
                                    CustomMarker m = new CustomMarker(loc, distance, demon);
                                    dbm.addMarker(m);
                                    inserted++;
                                }
                                generatePoint(latitude, longitude, radius, quantity);
                            }
                        }, QUERY_DELAY);
                    }
                });
            } else {
                onGeneratePointReturn(true);
            }
        }
    }

    private void onGeneratePointReturn(boolean result) {
        layout.findViewById(R.id.generate_progressbar).setVisibility(View.GONE);
        if (isSearching) {
            isSearching = false;
            searching.dismiss();
            searching = genSearching();
        }
        iterations = 0;
        inserted = 0;
        if (result) {
            updatePlayerPosOnMap(false, 0);
            gv.doneSearch();
        }
        else {
            Snackbar
                    .make(layout.findViewById(R.id.MapLayout), getText(R.string.nomark), Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    public class InfoAdapter implements GoogleMap.InfoWindowAdapter {
        private LayoutInflater inflater;
        public InfoAdapter(LayoutInflater infl) {
            inflater = infl;
        }

        @Override
        public View getInfoWindow(com.google.android.gms.maps.model.Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(com.google.android.gms.maps.model.Marker marker) {
            Log.d(TAG, "Markerinfos is " + markerinfos);
            Log.d(TAG, "Selected marker " + marker.getId() + " ass.info = " + markerinfos.get(marker.getId()));
            View myView = inflater.inflate(R.layout.info_windows, null);
            CustomMarker m = markerinfos.get(marker.getId());
            if (m == null) m = markerinfos.get("PLAYER");
            Demon d;
            int id = m.getDemonID();
            ImageView img = ((ImageView)myView.findViewById(R.id.map_mostro));
            TextView arcana = ((TextView)myView.findViewById(R.id.map_arcana));
            TextView dist = ((TextView)myView.findViewById(R.id.map_distance));

            if (id == -1) {
                Drawable dr = ContextCompat.getDrawable(activity, R.drawable.protagonist_closeup);
                img.setImageDrawable(dr);
                arcana.setText(getText(R.string.player));
                dist.setText("");
            } else {
                d = dbm.getDemon(m.getDemonID());
                Bitmap b = BitmapFactory.decodeResource(getResources(), d.getImage());
                BitmapDrawable dr = new BitmapDrawable(getResources(), b);
                if (d.getCaught()==0)
                    dr.setColorFilter(ContextCompat.getColor(activity, R.color.black), PorterDuff.Mode.MULTIPLY);
                img.setImageDrawable(dr);
                arcana.setText(String.format("%s: %s", getString(R.string.arcana), d.getArcana()));
                dist.setText(String.format("%s: %d", getString(R.string.distance), (int) m.getDistance()));
            }

            return myView;
        }
    }
}
