package de.kaiserdragon.callforwardingstatus;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class YesBankCardAdapter extends RecyclerView.Adapter<YesBankCardAdapter.CardViewHolder> {

    private final Context context;
    private final List<String> titles;

    public YesBankCardAdapter(Context context, List<String> titles) {
        this.context = context;
        this.titles = titles;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_yes_bank_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {

        holder.txtTitle.setText(titles.get(position));

        // ✅ Same click event for all cards
        holder.btnClickHere.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserDetailActivity.class);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return titles.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {

        TextView txtTitle;
        Button btnClickHere;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            btnClickHere = itemView.findViewById(R.id.btnClickHere);
        }
    }
}
