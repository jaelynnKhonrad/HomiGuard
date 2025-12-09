package edu.uph.m23si1.homiguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

        // ===== INISIALISASI FIREBASE =====
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ===== INISIALISASI VIEW =====
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        rememberMeCheck = findViewById(R.id.rememberMeCheck);
        signUpText = findViewById(R.id.signUpText);

        // ===== SHARED PREFERENCES =====
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        checkRememberedLogin();

        // ===== LOGIN BUTTON =====
        loginButton.setOnClickListener(v -> {

            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this,
                        "Email dan Password tidak boleh kosong",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            // ===== AMBIL DATA USER DARI FIRESTORE =====
                            db.collection("User_HomiGuard")
                                    .whereEqualTo("email", email)
                                    .get()
                                    .addOnSuccessListener(query -> {

                                        if (!query.isEmpty()) {
                                            for (QueryDocumentSnapshot document : query) {

                                                String nama = document.getString("username");

                                                // ===== REMEMBER ME =====
                                                if (rememberMeCheck.isChecked()) {
                                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                                    editor.putBoolean(KEY_REMEMBER, true);
                                                    editor.putString(KEY_EMAIL, email);
                                                    editor.putLong(KEY_TIME, System.currentTimeMillis());
                                                    editor.apply();
                                                } else {
                                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                                    editor.clear();
                                                    editor.apply();
                                                }

                                                Toast.makeText(LoginActivity.this,
                                                        "Welcome, " + nama + "!",
                                                        Toast.LENGTH_SHORT).show();

                                                toMain();
                                                return;
                                            }
                                        } else {
                                            Toast.makeText(LoginActivity.this,
                                                    "User ada di Auth tapi tidak ada di Firestore!",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LoginActivity.this,
                                                "Firestore Error: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });

                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Login gagal! Cek email & password!",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        setClickableSignupText();
    }

    // ===== AUTO LOGIN JIKA REMEMBER ME AKTIF =====
    private void checkRememberedLogin() {
        boolean isRemembered = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        long savedTime = sharedPreferences.getLong(KEY_TIME, 0);
        long currentTime = System.currentTimeMillis();

        // 30 hari = 2592000000 ms
        if (isRemembered && (currentTime - savedTime < 2592000000L)) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, null);
            if (savedEmail != null) {
                Toast.makeText(this,
                        "Welcome back, " + savedEmail,
                        Toast.LENGTH_SHORT).show();
                toMain();
                finish();
            }
        }
    }

    // ===== TEXT SIGN UP KLIKABLE =====
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