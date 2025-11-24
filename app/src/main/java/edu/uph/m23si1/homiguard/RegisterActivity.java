package edu.uph.m23si1.homiguard;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText edtEmail, edtPassword, edtUsername;
    Button btnRegister;
    TextView tvLogin;
    FirebaseAuth auth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 🔹 Inisialisasi komponen
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtUsername = findViewById(R.id.edtUsername);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // 🔹 Inisialisasi Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // 🔹 Event klik tombol register
        btnRegister.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString();
            String username = edtUsername.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(username)) {
                Toast.makeText(RegisterActivity.this, "All fields must be filled", Toast.LENGTH_SHORT).show();
            } else {
                registerUser(email, password, username);
            }
        });

        // 🔹 Event klik “Login”
        setupLoginText();
    }

    private void setupLoginText() {
        String text = "Already have an account? Login";
        SpannableString ss = new SpannableString(text);
        int start = text.indexOf("Login");
        int end = start + "Login".length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                toLogin();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#ACC0FF")); // 💙 warna biru lembut
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        };

        ss.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvLogin.setText(ss);
        tvLogin.setMovementMethod(LinkMovementMethod.getInstance());
        tvLogin.setHighlightColor(Color.TRANSPARENT);
    }

    private void registerUser(String email, String password, String username) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            saveUserData(email, username);
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserData(String email, String username) {
        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);
        userMap.put("username", username);

        firestore.collection("User_HomiGuard")
                .add(userMap)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    toLogin();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void toLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}