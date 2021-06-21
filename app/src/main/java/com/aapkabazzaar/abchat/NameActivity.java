package com.aapkabazzaar.abchat;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NameActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    private DatabaseReference mStatusDatabase;
    private FirebaseUser mCurrentUser;

    private ProgressDialog mProgress;
    private Snackbar snackbar;
    private RelativeLayout relativeLayout;
    private Button mSavebtn;
    private TextInputLayout mName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_name);
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        String current_uid = mCurrentUser.getUid();

        mStatusDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);
        mStatusDatabase.keepSynced(true);
        mToolbar = (Toolbar) findViewById(R.id.status_NameBar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Name");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final String name_value = getIntent().getStringExtra("name_value");

        mSavebtn = (Button) findViewById(R.id.name_save_btn);
        mName = (TextInputLayout) findViewById(R.id.name_input);
        mName.getEditText().setText(name_value);
        relativeLayout = findViewById(R.id.relativeStatusLayout);

        if (!isConnectedToInternet(this)) {
            showSnackBar("Please check your internet connection", relativeLayout);
        }

        mSavebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mName.getEditText().getText().toString();

                if (TextUtils.isEmpty(name)) {

                    Toast.makeText(getApplicationContext(), "Name Cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                mProgress = new ProgressDialog(NameActivity.this);
                mProgress.setTitle("Saving Changes");
                mProgress.setMessage("Please wait while we save the changes");
                mProgress.show();

                mStatusDatabase.child("name").setValue(name.toUpperCase()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mProgress.dismiss();
                            Toast.makeText(getApplicationContext(), "Name Changed Successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "There was some error while saving the changes", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private boolean isConnectedToInternet(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void showSnackBar(String message, RelativeLayout relativeLayout) {
        snackbar = Snackbar
                .make(relativeLayout, message, Snackbar.LENGTH_INDEFINITE).
                        setAction("Ok", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                snackbar.dismiss();
                            }
                        });
        snackbar.show();
    }
}

