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

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private LocationRequest locationRequest;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Button DriverLogoutButton;
    private Button DriverSettingButton;
    private boolean currentDriverLogoutStatus = false;
    private DatabaseReference AssignedCustomerRef, AssignedCustomerPickUpRef;
    private String driverID, customerID = "";
    private Marker PickUpMarke;
    private ValueEventListener AssignedCustomerPickUpListener;

    private TextView txtName, txtPhone;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        DriverLogoutButton = findViewById(R.id.driver_logout_but);
        DriverSettingButton = findViewById(R.id.driver_setting_but);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        driverID = mAuth.getCurrentUser().getUid();

        txtName = findViewById(R.id.name_customer);
        txtPhone = findViewById(R.id.phone_customer);
        profilePic = findViewById(R.id.profile_image_customer);
        relativeLayout = findViewById(R.id.rel2);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        DriverSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapActivity.this, SettingActivity.class);
                intent.putExtra("type", "Drivers");
                startActivity(intent);
            }
        });

        DriverLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDriverLogoutStatus = true;
                DisConnectWithDriver();
                mAuth.signOut();
                LogoutDriver();
            }
        });

        GetAssignedCustomerRequest();
    }

    private void GetAssignedCustomerRequest() {
        AssignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Drivers")
                .child(driverID)
                .child("CustomerRideID");

        AssignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    customerID = dataSnapshot.getValue().toString();
                    GetAssignedCustomerPickUpLocation();

                    relativeLayout.setVisibility(View.VISIBLE);
                    getAssignedCustomerInformation();

                } else {
                    customerID = "";
                    if (PickUpMarke != null) {
                        PickUpMarke.remove();
                    }
                    if (AssignedCustomerPickUpListener != null) {
                        AssignedCustomerPickUpRef.removeEventListener(AssignedCustomerPickUpListener);
                    }
                    relativeLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void GetAssignedCustomerPickUpLocation() {
        AssignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference()
                .child("Customers Requests")
                .child(customerID)
                .child("l");
        AssignedCustomerPickUpListener = AssignedCustomerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<Object> customerLocationMap = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (customerLocationMap.get(0) != null) {
                        locationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                    }
                    if (customerLocationMap.get(1) != null) {
                        locationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                    }
                    LatLng DriverLatLng = new LatLng(locationLat, locationLng);
                    mMap.addMarker(new MarkerOptions().position(DriverLatLng)
                            .title("Customer Pick Location")
                            .icon(BitmapDescriptorFactory
                                    .fromResource(R.drawable.user)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void LogoutDriver() {
        Intent welcomeIntent = new Intent(DriverMapActivity.this, DriverLoginRegisterActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);
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
        if (getApplicationContext() != null) {
            lastLocation = location;
            // Add a marker in Sydney and move the camera
            LatLng sydney = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
            GeoFire geoFireAvailability = new GeoFire(DriverAvailabilityRef);


            DatabaseReference DriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
            GeoFire geoFireWorking = new GeoFire(DriverWorkingRef);

            switch (customerID) {
                case "":
                    geoFireWorking.removeLocation(userID);
                    geoFireAvailability.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;

                default:
                    geoFireAvailability.removeLocation(userID);
                    geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!currentDriverLogoutStatus) {
            DisConnectWithDriver();
        }
    }

    private void DisConnectWithDriver() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        GeoFire geoFire = new GeoFire(DriverAvailabilityRef);
        geoFire.removeLocation(userID);
    }

    private void getAssignedCustomerInformation() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Customers").child(customerID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);

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
