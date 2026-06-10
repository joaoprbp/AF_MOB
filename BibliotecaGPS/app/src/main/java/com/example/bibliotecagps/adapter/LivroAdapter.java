package com.example.bibliotecagps.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bibliotecagps.R;
import com.example.bibliotecagps.model.Livro;

import java.util.List;

public class LivroAdapter extends RecyclerView.Adapter<LivroAdapter.LivroViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Livro livro);
        void onItemLongClick(Livro livro);
    }

    private List<Livro> livros;
    private OnItemClickListener listener;

    public LivroAdapter(List<Livro> livros, OnItemClickListener listener) {
        this.livros = livros;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LivroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_livro, parent, false);
        return new LivroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LivroViewHolder holder, int position) {
        Livro livro = livros.get(position);
        holder.bind(livro, listener);
    }

    @Override
    public int getItemCount() {
        return livros.size();
    }

    public void atualizarLista(List<Livro> novaLista) {
        this.livros = novaLista;
        notifyDataSetChanged();
    }

    static class LivroViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvAutor, tvStatus, tvSituacao, tvCoordenadas;

        LivroViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tv_titulo);
            tvAutor = itemView.findViewById(R.id.tv_autor);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvSituacao = itemView.findViewById(R.id.tv_situacao);
            tvCoordenadas = itemView.findViewById(R.id.tv_coordenadas);
        }

        void bind(Livro livro, OnItemClickListener listener) {
            tvTitulo.setText(livro.getTitulo());
            tvAutor.setText(livro.getAutor());
            tvStatus.setText(livro.getStatus());
            tvSituacao.setText(livro.getSituacao());
            tvCoordenadas.setText(String.format("%.5f, %.5f", livro.getLatitude(), livro.getLongitude()));

            itemView.setOnClickListener(v -> listener.onItemClick(livro));
            itemView.setOnLongClickListener(v -> {
                listener.onItemLongClick(livro);
                return true;
            });
        }
    }
}
