package com.example.cab;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private LocationRequest locationRequest;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String customerID;
    private DatabaseReference CustomerDatabaseRef;
    private LatLng CustomerPickUpLocation;
    private DatabaseReference DriverAvailableRef;
    private DatabaseReference DriverLocationRef;
    private int radius = 1;
    private boolean driverFounded = false, requestType = false;
    private String driverFoundedID;
    private DatabaseReference DriverRef;
    private Marker DriverMarker, PickUpMarker;

    private Button CustomerLogoutButton;
    private Button CustomerSettingButton;
    private Button CallCabCaeButton;
    private ValueEventListener DriverLocationRefListener;
    private GeoQuery geoQuery;

    private TextView txtName, txtPhone, txtCarName;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        CustomerLogoutButton = findViewById(R.id.customer_logout_but);
        CustomerSettingButton = findViewById(R.id.customer_setting_but);
        CallCabCaeButton = findViewById(R.id.call_car_but);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customers Requests");
        DriverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        DriverLocationRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");

        txtName = findViewById(R.id.name_driver);
        txtPhone = findViewById(R.id.phone_driver);
        txtCarName = findViewById(R.id.car_name_driver);
        profilePic = findViewById(R.id.profile_image_driver);
        relativeLayout = findViewById(R.id.rel1);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        CustomerLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                LogoutCustomer();
            }
        });

        CustomerSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, SettingActivity.class);
                intent.putExtra("type", "Customers");
                startActivity(intent);
            }
        });

        CallCabCaeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestType) {
                    requestType = false;
                    geoQuery.removeAllListeners();
                    DriverAvailableRef.removeEventListener(DriverLocationRefListener);

                    if (!driverFounded) {
                        DriverRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users")
                                .child("Drivers")
                                .child(driverFoundedID)
                                .child("CustomerRideID");

                        DriverRef.removeValue();
                        driverFoundedID = null;
                    }
                    driverFounded = false;
                    radius = 1;

                    GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                    geoFire.removeLocation(customerID);

                    if (PickUpMarker != null) {
                        PickUpMarker.remove();
                    }

                    if (DriverMarker != null) {
                        DriverMarker.remove();
                    }
                    relativeLayout.setVisibility(View.GONE);
                    CallCabCaeButton.setText("Call A Cab..");
                } else {
                    requestType = true;
                    GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                    geoFire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                    CustomerPickUpLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(CustomerPickUpLocation)
                            .title("My Location")
                            .icon(BitmapDescriptorFactory
                                    .fromResource(R.drawable.user)));

                    CallCabCaeButton.setText("Getting your Driver..");
                    GetClosestCabDriver();
                }
            }
        });
    }

    private void GetClosestCabDriver() {
        GeoFire geoFire = new GeoFire(DriverAvailableRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPickUpLocation.latitude, CustomerPickUpLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFounded && requestType) {
                    driverFounded = true;
                    driverFoundedID = key;

                    DriverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundedID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customerID);
                    DriverRef.updateChildren(driverMap);

                    GettingDriverLocation();
                    CallCabCaeButton.setText("Looking For Driver Location..");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFounded) {
                    radius = radius + 2;
                    GetClosestCabDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void GettingDriverLocation() {
        DriverLocationRefListener = DriverLocationRef.child(driverFoundedID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && requestType) {
                            List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                            double locationLat = 0;
                            double locationLng = 0;
                            CallCabCaeButton.setText("Driver Found");

                            relativeLayout.setVisibility(View.VISIBLE);
                            getAssignedDriverInformation();

                            if (driverLocationMap.get(0) != null) {
                                locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }
                            if (driverLocationMap.get(1) != null) {
                                locationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                            }

                            LatLng DriverLatLng = new LatLng(locationLat, locationLng);
                            if (DriverMarker != null) {
                                DriverMarker.remove();
                            }

                            Location location1 = new Location("");
                            location1.setLatitude(CustomerPickUpLocation.latitude);
                            location1.setLatitude(CustomerPickUpLocation.longitude);

                            Location location2 = new Location("");
                            location2.setLatitude(DriverLatLng.latitude);
                            location2.setLatitude(DriverLatLng.longitude);

                            float distance = location1.distanceTo(location2);
                            if (distance < 90) {
                                CallCabCaeButton.setText("Driver's Reached");
                            } else {
                                CallCabCaeButton.setText("Driver Found: " + distance);
                            }
                            DriverMarker = mMap.addMarker(new MarkerOptions()
                                    .position(DriverLatLng)
                                    .title("Your Driver Is Here")
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.drawable.car)));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void LogoutCustomer() {
        Intent welcomeIntent = new Intent(CustomerMapActivity.this, CustomerLoginRegisterActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }


    private void getAssignedDriverInformation() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverFoundedID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    if (dataSnapshot != null) {
                        String name = dataSnapshot.child("name").getValue().toString();
                        String phone = dataSnapshot.child("phone").getValue().toString();
                        String car = dataSnapshot.child("car").getValue().toString();

                        txtName.setText(name);
                        txtPhone.setText(phone);
                        txtCarName.setText(car);
                    }


                    if (dataSnapshot.hasChild("image")) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profilePic);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
