package de.kaiserdragon.callforwardingstatus;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class YesBankCardsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // 1️⃣ Enable edge-to-edge (Android 13+)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_yes_bank_cards);

        // 3️⃣ NOW find the root layout
        View rootView = findViewById(R.id.rootLayout);

        // 4️⃣ Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });

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
}
