package com.example.myapplication;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private MaterialButton sendButton;
    private MaterialButton backButton;
    private View loadingOverlay;

    // Server URL - CHANGE TO YOUR IP/DOMAIN
    private static final String API_BASE_URL = "http://18.233.249.90:5000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Configure back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Configure send button
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();

                if (validateEmail(email)) {
                    new SendPasswordResetEmailTask().execute(email);
                } else {
                    showSnackbar("Please enter a valid email", false);
                }
            }
        });
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            showSnackbar("Email cannot be empty", false);
            return false;
        }

        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (!email.matches(emailPattern)) {
            showSnackbar("Invalid email format", false);
            return false;
        }

        return true;
    }

    private class SendPasswordResetEmailTask extends AsyncTask<String, Void, Integer> {
        private String errorMessage = null;
        private String email;

        @Override
        protected void onPreExecute() {
            showLoading(true);
            sendButton.setEnabled(false);
        }

        @Override
        protected Integer doInBackground(String... params) {
            email = params[0];
            HttpURLConnection connection = null;
            try {
                URL url = new URL(API_BASE_URL + "/forgot-password/");
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                // Create JSON with email
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("email", email);

                // Send data
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(jsonBody.toString());
                writer.flush();
                writer.close();

                return connection.getResponseCode();

            } catch (Exception e) {
                e.printStackTrace();
                errorMessage = e.getMessage();
                return -1;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            showLoading(false);
            sendButton.setEnabled(true);

            if (responseCode == -1) {
                showSnackbar("Connection error: " + errorMessage, false);
                return;
            }

            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                    // Success
                    showSnackbar(
                            "✅ If the email is registered, you will receive a recovery link",
                            true
                    );

                    // Clear field
                    emailEditText.setText("");

                    // Return to login after 3 seconds
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 3000);
                    break;

                case HttpURLConnection.HTTP_BAD_REQUEST:
                    showSnackbar("Invalid email", false);
                    break;

                default:
                    showSnackbar("Error sending request. Please try again.", false);
                    break;
            }
        }
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showSnackbar(String message, boolean isSuccess) {
        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                message,
                isSuccess ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT
        );

        // Customize colors based on message type
        View snackbarView = snackbar.getView();
        if (isSuccess) {
            snackbarView.setBackgroundColor(getResources().getColor(R.color.success));
        } else {
            snackbarView.setBackgroundColor(getResources().getColor(R.color.error));
        }

        snackbar.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
