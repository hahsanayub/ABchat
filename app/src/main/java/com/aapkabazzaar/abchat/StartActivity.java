package com.aapkabazzaar.abchat;

        import android.content.Intent;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.support.v7.widget.Toolbar;
        import android.view.View;
        import android.widget.Button;

public class StartActivity extends AppCompatActivity {

    private Button mRegBtn;
    private Button mHaveBtn;
    private Toolbar mToolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        mRegBtn = findViewById(R.id.start_reg_btn);
        mHaveBtn = findViewById(R.id.start_have_btn);
        mToolbar = findViewById(R.id.main_page_toolbar);
        mRegBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent reg_Intent = new Intent(StartActivity.this,RegisterActivity.class);
                startActivity(reg_Intent);
            }
        });

        mHaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent login_Intent = new Intent(StartActivity.this,LoginActivity.class);
                startActivity(login_Intent);
            }
        });

    }
}

