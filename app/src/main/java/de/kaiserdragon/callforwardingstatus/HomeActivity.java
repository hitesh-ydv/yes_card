package de.kaiserdragon.callforwardingstatus;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.graphics.Color;
import androidx.core.view.WindowCompat;


public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yes_bank_cards);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        getWindow().setStatusBarColor(Color.WHITE);
        controller.setAppearanceLightStatusBars(true);



        RecyclerView recyclerView = findViewById(R.id.recyclerCards);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<String> cardTitles = Arrays.asList(
                "Rewards Point Redeem",
                "Card Protection Cancellation",
                "Credit Limit Enhancement",
                "Generate PIN",
                "Update Communication Address"
        );

        YesBankCardAdapter adapter = new YesBankCardAdapter(this, cardTitles);
        recyclerView.setAdapter(adapter);
    }

    // 🔁 HARD RESTART APP WHEN BACK IS PRESSED
    @Override
    public void onBackPressed() {
        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName());

        if (intent != null) {
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_NEW_TASK
            );
            startActivity(intent);
        }
        finish();
        Runtime.getRuntime().exit(0); // hard restart
    }
}
