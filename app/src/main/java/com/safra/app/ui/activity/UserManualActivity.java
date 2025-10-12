package com.safra.app.ui.activity;
import com.safra.app.R;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class UserManualActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manual);

        // Optional: Set up the ActionBar/Toolbar to display the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("User Manual");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // The content is loaded automatically using the @string/safra_manual_content
        // as defined in the XML. No extra Java code is needed here unless you
        // are loading the content dynamically from a file (e.g., an asset).
    }

    // Handle the Up button (back arrow) in the action bar
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}