package com.safra.app.ui.activity;
import com.safra.app.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // Make sure this import is present

public class YourHomeActivity extends AppCompatActivity { // Rename as needed

    private CardView cardUserManual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Your main layout file

        // 1. Initialize the new CardView
        cardUserManual = findViewById(R.id.card_user_manual);

        // 2. Set the click listener
        cardUserManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch the new activity
                Intent intent = new Intent(YourHomeActivity.this, UserManualActivity.class);
                startActivity(intent);
            }
        });

        // ... Initialization code for SOS, Safe Check-in, Contacts, etc. ...
    }
}