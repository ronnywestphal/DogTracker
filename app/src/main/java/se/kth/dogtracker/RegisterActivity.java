package se.kth.dogtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import se.kth.dogtracker.model.User;

/**
 * The activity for registering in the Dog Tracker app.
 * Handles input validation and Firebase authentication & user creation.
 */
public class RegisterActivity extends AppCompatActivity {

    private AppCompatButton bRegister;
    private AppCompatTextView tvLogin;
    private AppCompatEditText etRegName, etRegEmail, etRegPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        bRegister = findViewById(R.id.bRegister);
        tvLogin = findViewById(R.id.tvLogin);
        etRegName = findViewById(R.id.etRegName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegPassword.setTransformationMethod(new PasswordTransformationMethod());

        mAuth = FirebaseAuth.getInstance();

        bRegister.setOnClickListener(this::onRegister);
        tvLogin.setOnClickListener(this::onGoToLogin);

    }

    private void onGoToLogin(View view) {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
    }

    private void onRegister(View view) {
        createUser();
    }

    private void createUser() {
        String name = etRegName.getText().toString();
        String email = etRegEmail.getText().toString();
        String password = etRegPassword.getText().toString();

        if (TextUtils.isEmpty(name)) {
            etRegName.setError("Name is required");
            etRegName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etRegEmail.setError("Email is required");
            etRegEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegEmail.setError("Please provide a valid email");
            etRegEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etRegPassword.setError("Password is required");
            etRegPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etRegPassword.setError("Minimum password length is 6 characters");
            etRegPassword.requestFocus();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            User user = User.newInstance(name, email);

                            FirebaseDatabase.getInstance().getReference("Users")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this, "User registered succesfully", Toast.LENGTH_SHORT).show();

                                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                            } else {
                                                Toast.makeText(RegisterActivity.this, "Registration error " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration error " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("RegisterActivity", "onDestroy");
        mAuth.signOut();
    }
}