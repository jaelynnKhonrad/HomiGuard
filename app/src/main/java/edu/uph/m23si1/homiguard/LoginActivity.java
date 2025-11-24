package edu.uph.m23si1.homiguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.TextPaint;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.graphics.Color;

public class LoginActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginButton;
    CheckBox rememberMeCheck;
    TextView signUpText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "login_pref";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_TIME = "remember_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inisialisasi
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        rememberMeCheck = findViewById(R.id.rememberMeCheck);
        signUpText = findViewById(R.id.signUpText);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        checkRememberedLogin();

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Ambil data user dari Firestore berdasarkan email
                            db.collection("User_HomiGuard")
                                    .whereEqualTo("email", email)
                                    .get()
                                    .addOnSuccessListener(query -> {
                                        if (!query.isEmpty()) {
                                            for (QueryDocumentSnapshot document : query) {
                                                String nama = document.getString("username");

                                                // 🔹 Cek apakah checkbox diaktifkan
                                                if (rememberMeCheck.isChecked()) {
                                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                                    editor.putBoolean(KEY_REMEMBER, true);
                                                    editor.putString(KEY_EMAIL, email);
                                                    editor.putLong(KEY_TIME, System.currentTimeMillis());
                                                    editor.apply();
                                                } else {
                                                    // 🔹 Hapus data kalau user gak mau diingat
                                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                                    editor.clear();
                                                    editor.apply();
                                                }

                                                Toast.makeText(LoginActivity.this, "Welcome, " + nama + "!", Toast.LENGTH_SHORT).show();
                                                toMain();
                                                return;
                                            }
                                        } else {
                                            Toast.makeText(LoginActivity.this, "User data not found in Firestore.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LoginActivity.this, "Failed to retrieve Firestore data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(LoginActivity.this, "Login failed. Check your email & password.", Toast.LENGTH_LONG).show();
                        }
                    });
        });

        setClickableSignupText();
    }

    private void checkRememberedLogin() {
        boolean isRemembered = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        long savedTime = sharedPreferences.getLong(KEY_TIME, 0);
        long currentTime = System.currentTimeMillis();

        if (isRemembered && (currentTime - savedTime < 2592000000L)) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, null);
            if (savedEmail != null) {
                Toast.makeText(this, "Welcome back, " + savedEmail, Toast.LENGTH_SHORT).show();
                toMain();
                finish();
            }
        }
    }

    private void setClickableSignupText() {
        String text = "Don’t have an account? Sign Up";
        SpannableString ss = new SpannableString(text);

        int start = text.indexOf("Sign Up");
        int end = start + "Sign Up".length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                toRegister();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#5C6BC0"));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        };

        ss.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        signUpText.setText(ss);
        signUpText.setMovementMethod(LinkMovementMethod.getInstance());
        signUpText.setHighlightColor(Color.TRANSPARENT);
    }

    private void toRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void toMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}