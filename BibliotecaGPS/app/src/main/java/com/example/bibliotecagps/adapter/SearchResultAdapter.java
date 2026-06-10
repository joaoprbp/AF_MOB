package com.example.bibliotecagps.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bibliotecagps.R;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ResultViewHolder> {

    public interface OnResultClickListener {
        void onResultClick(String titulo, String autor, String ano, String editora);
    }

    public static class BookResult {
        public String titulo;
        public String autor;
        public String ano;
        public String editora;

        public BookResult(String titulo, String autor, String ano, String editora) {
            this.titulo = titulo;
            this.autor = autor;
            this.ano = ano;
            this.editora = editora;
        }
    }

    private List<BookResult> resultados;
    private OnResultClickListener listener;

    public SearchResultAdapter(List<BookResult> resultados, OnResultClickListener listener) {
        this.resultados = resultados;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        BookResult result = resultados.get(position);
        holder.bind(result, listener);
    }

    @Override
    public int getItemCount() {
        return resultados.size();
    }

    public void atualizarResultados(List<BookResult> novaLista) {
        this.resultados = novaLista;
        notifyDataSetChanged();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvAutor, tvAno, tvEditora;

        ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tv_titulo);
            tvAutor = itemView.findViewById(R.id.tv_autor);
            tvAno = itemView.findViewById(R.id.tv_ano);
            tvEditora = itemView.findViewById(R.id.tv_editora);
        }

        void bind(BookResult result, OnResultClickListener listener) {
            tvTitulo.setText(result.titulo);
            tvAutor.setText(result.autor.isEmpty() ? "Autor desconhecido" : result.autor);
            tvAno.setText(result.ano.isEmpty() ? "" : result.ano);
            tvEditora.setText(result.editora.isEmpty() ? "" : result.editora);

            itemView.setOnClickListener(v ->
                    listener.onResultClick(result.titulo, result.autor, result.ano, result.editora));
        }
    }
}
