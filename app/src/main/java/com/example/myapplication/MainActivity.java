package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private TextView locationStatusTextView;
    private TextView photoStatusTextView;
    private TextView sendStatusTextView;
    private TextView serverStatusTextView;
    private ImageView imagePreview;
    private Button clearPhotoButton;
    private Location currentLocation;
    private String photoBase64 = null;
    private final String apiUrl = "http://186.98.29.101:5000/reports/";

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationStatusTextView = findViewById(R.id.locationStatusTextView);
        photoStatusTextView = findViewById(R.id.photoStatusTextView);
        sendStatusTextView = findViewById(R.id.sendStatusTextView);
        serverStatusTextView = findViewById(R.id.serverStatusTextView);
        imagePreview = findViewById(R.id.imagePreview);
        clearPhotoButton = findViewById(R.id.clearPhotoButton);
        locationStatusTextView.setVisibility(View.GONE);

        Button takePhotoButton = findViewById(R.id.takePhotoButton);
        Button pickFromGalleryButton = findViewById(R.id.pickFromGalleryButton);
        Button clearPhotoButton = findViewById(R.id.clearPhotoButton);
        Button updateLocationButton = findViewById(R.id.updateLocationButton);
        Button sendButton = findViewById(R.id.sendButton);

        takePhotoButton.setOnClickListener(v -> takePhoto());
        pickFromGalleryButton.setOnClickListener(v -> pickFromGallery());
        clearPhotoButton.setOnClickListener(v -> clearPhoto());
        updateLocationButton.setOnClickListener(v -> getLocation());
        sendButton.setOnClickListener(v -> sendReport());

        serverStatusTextView.setText("Verificando conexión...");

        checkServerConnection();

        // Inicializa el launcher para la cámara
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            photoBase64 = bitmapToBase64(imageBitmap);
                            imagePreview.setImageBitmap(imageBitmap);
                            imagePreview.setVisibility(View.VISIBLE);
                            clearPhotoButton.setVisibility(View.VISIBLE);
                            photoStatusTextView.setText("Imagen cargada");
                        }
                    }
                }
        );

        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        photoBase64 = uriToBase64(uri);
                        if (photoBase64 != null) {
                            Bitmap bitmap = uriToBitmap(uri);
                            imagePreview.setImageBitmap(bitmap);
                            imagePreview.setVisibility(View.VISIBLE);
                            clearPhotoButton.setVisibility(View.VISIBLE);
                            photoStatusTextView.setText("Imagen cargada");
                        } else {
                            photoStatusTextView.setText("Error al seleccionar la foto");
                        }
                    } else {
                        photoStatusTextView.setText("Sin imagen");
                        imagePreview.setVisibility(View.GONE);
                        clearPhotoButton.setVisibility(View.GONE);
                    }
                }
        );

        // Location permission launcher
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        checkGpsAndGetLocation();
                    } else {
                        locationStatusTextView.setText("Permiso de ubicación denegado");
                        Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        photoStatusTextView.setText("Permiso de cámara denegado");
                        Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Gallery permission launcher
        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        galleryLauncher.launch("image/*");
                    } else {
                        photoStatusTextView.setText("Permiso de galería denegado");
                        Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void checkServerConnection() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://191.110.32.119:5000/") // Changed from apiUrl to root endpoint
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> serverStatusTextView.setText("No conectado al servidor"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        serverStatusTextView.setText("Conectado a: http://191.110.32.119:5000");
                    } else {
                        serverStatusTextView.setText("No conectado al servidor: Código " + response.code());
                    }
                });
            }
        });
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            checkGpsAndGetLocation();
        }
    }

    private void checkGpsAndGetLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationStatusTextView.setText("GPS no activado. Actívalo en ajustes.");
            locationStatusTextView.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Activa el GPS", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = location;
                        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                        locationStatusTextView.setText("Lat: " + location.getLatitude() + "\nLong: " + location.getLongitude() +
                                "\nPrecisión: " + (location.hasAccuracy() ? location.getAccuracy() + "m" : "N/A") +
                                "\nHora: " + time);
                        locationStatusTextView.setVisibility(View.VISIBLE);
                    } else {
                        locationStatusTextView.setText("No se pudo obtener ubicación");
                        locationStatusTextView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    locationStatusTextView.setText("Error: " + e.getMessage());
                    locationStatusTextView.setVisibility(View.VISIBLE);
                });
    }

    private void takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            photoStatusTextView.setText("No se puede abrir la cámara");
        }
    }

    private void pickFromGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            galleryLauncher.launch("image/*");
        }
    }

    private void clearPhoto() {
        photoBase64 = null;
        imagePreview.setVisibility(View.GONE); // Ocultar el ImageView
        clearPhotoButton.setVisibility(View.GONE);
        photoStatusTextView.setText("Sin imagen");
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private String uriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();
            return bitmapToBase64(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap uriToBitmap(Uri uri) {
        try {
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendReport() {
        if (currentLocation == null) {
            sendStatusTextView.setText("Primero obtén la ubicación");
            return;
        }
        if (photoBase64 == null) {
            sendStatusTextView.setText("Primero toma una foto");
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        JSONObject json = new JSONObject();
        try {
            json.put("latitude", currentLocation.getLatitude());
            json.put("longitude", currentLocation.getLongitude());
            json.put("timestamp", timestamp);
            json.put("photo_base64", photoBase64);
        } catch (Exception e) {
            sendStatusTextView.setText("Error al crear JSON: " + e.getMessage());
            return;
        }

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, json.toString());
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> sendStatusTextView.setText("No se pudo conectar al servidor, verifica tu internet y vuelve a intentarlo"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        sendStatusTextView.setText("Reporte enviado exitosamente");
                        // Mostrar la barra de estado
                        // Opcional: Ocultar la barra después de 3 segundos
                        new android.os.Handler().postDelayed(() -> {
                        }, 3000); // 3000 ms = 3 segundos
                    } else {
                        sendStatusTextView.setText("Error al enviar el reporte: Código " + response.code() + " - " + response.message());
                    }
                });
            }
        });
    }
}