package com.aapkabazzaar.abchat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout mDisplayName;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private Button mCreateBtn, backButton;
    private TextView mLoginBtn;
    private DatabaseReference mDatabase;
    private CircleImageView logoView;
    private Toolbar mToolbar;
    private TextView textView;
    private ProgressDialog mRegProgress;
    private FirebaseAuth mAuth;
    private String uid;
    private Animation bottomToTop, topToBottom;
    private LinearLayout linearLayout;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


//        backButton=findViewById(R.id.main_back);
//        backButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent backIntent = new Intent(RegisterActivity.this, StartActivity.class);
//            startActivity(backIntent);
//            finish();
//            }
//        });

        linearLayout = findViewById(R.id.linearLayout);
        linearLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                InputMethodManager hideKeyBoard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                hideKeyBoard.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                return false;
            }


        });

        if (!isConnectedToInternet(this)) {
            showSnackBar("Please check your internet connection", linearLayout);
        }

        mDisplayName = findViewById(R.id.reg_display_name);
        mEmail = findViewById(R.id.reg_email);
        mPassword = findViewById(R.id.reg_password);
        mCreateBtn = findViewById(R.id.reg_create_btn);
        textView = findViewById(R.id.textView2);
        logoView = findViewById(R.id.imageView3);
        mLoginBtn = findViewById(R.id.reg_login_btn);
        bottomToTop = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top);
        topToBottom = AnimationUtils.loadAnimation(this, R.anim.top_to_bottom);

        mCreateBtn.setAnimation(bottomToTop);
        mLoginBtn.setAnimation(bottomToTop);
        textView.setAnimation(topToBottom);
        logoView.setAnimation(topToBottom);

        mToolbar = findViewById(R.id.register_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("SignUp Page");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent backIntent = new Intent(RegisterActivity.this, StartActivity.class);
                startActivity(backIntent);
                finish();
            }
        });

        mRegProgress = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
            }
        });

        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager hideKeyBoard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                hideKeyBoard.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                String display_name = mDisplayName.getEditText().getText().toString().toUpperCase();
                String emailOrPhone = mEmail.getEditText().getText().toString();
                String password = mPassword.getEditText().getText().toString();
                if (TextUtils.isEmpty(display_name)) {
                    mRegProgress.hide();
                    Toast.makeText(getApplicationContext(), "Enter Display Name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!TextUtils.isEmpty(display_name) || !TextUtils.isEmpty(emailOrPhone) || !TextUtils.isEmpty(password)) {
                    mRegProgress.setTitle("Registering User");
                    mRegProgress.setMessage("Please wait while we create your account !");
                    mRegProgress.show();

                    register_user(display_name, emailOrPhone, password);

                }
            }
        });
    }

    private boolean isConnectedToInternet(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void showSnackBar(String message, LinearLayout linearLayout) {
        snackbar = Snackbar
                .make(linearLayout, message, Snackbar.LENGTH_INDEFINITE).
                        setAction("Ok", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                snackbar.dismiss();
                            }
                        });
        snackbar.show();
    }

    private void register_user(final String display_name, final String email, final String password) {


        if (TextUtils.isEmpty(email)) {
            mRegProgress.hide();
            Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
            return;
        } else if (TextUtils.isEmpty(password)) {
            mRegProgress.hide();
            Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
            return;
        } else if (password.length() < 6) {
            mRegProgress.hide();
            Toast.makeText(getApplicationContext(), "Password too short, enter minimum 6 characters!", Toast.LENGTH_SHORT).show();
            return;
        }


        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
                    try {
                        uid = current_user.getUid();
                        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    String deviceToken = FirebaseInstanceId.getInstance().getToken();
                    HashMap<String, String> userMap = new HashMap<>();
                    userMap.put("device_token", deviceToken);
                    userMap.put("name", display_name);
                    userMap.put("email", email);
                    userMap.put("password", password);
                    userMap.put("status", "Hi there, I am using Realtime Chat App");
                    userMap.put("image", "default");
                    userMap.put("thumb_image", "default");

                    mDatabase.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if (task.isSuccessful()) {
                                mRegProgress.dismiss();
                                Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(mainIntent);
                                finish();
                            }

                        }
                    });

                } else {
                    mRegProgress.hide();
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(RegisterActivity.this, "This email already exists",
                                Toast.LENGTH_LONG).show();
                    } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(RegisterActivity.this, "Enter Valid Email Address",
                                Toast.LENGTH_LONG).show();
                    }


                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.help_btn) {
            Intent helpIntent = new Intent(RegisterActivity.this, HelpActivity.class);
            helpIntent.putExtra("EXTRA_ACTIVITY_CLASS", RegisterActivity.class);
            startActivity(helpIntent);
        }
//        if(item.getItemId()== R.id.home){
//            Intent backIntent = new Intent(RegisterActivity.this, StartActivity.class);
//            backIntent.putExtra("EXTRA_ACTIVITY_CLASS", RegisterActivity.class);
//            startActivity(backIntent);
//            finish();
//        }
        return true;
    }
}

