package com.example.bibliotecagps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bibliotecagps.adapter.LivroAdapter;
import com.example.bibliotecagps.adapter.SearchResultAdapter;
import com.example.bibliotecagps.model.Livro;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION = 100;

    // Views - pesquisa
    EditText etPesquisa;
    Button btnBuscar;
    RecyclerView recyclerResultados;

    // Views - formulário
    TextView tvTituloSelecionado, tvAutorSelecionado, tvAnoSelecionado, tvEditoraSelecionada;
    Spinner spinnerSituacao, spinnerStatus;
    EditText etObservacao;
    TextView tvCoordenadas;
    Button btnGps, btnSalvar;

    // Views - lista salva
    RecyclerView recyclerLivros;

    // Adapters e listas
    SearchResultAdapter resultadoAdapter;
    LivroAdapter livroAdapter;
    List<SearchResultAdapter.BookResult> resultados = new ArrayList<>();
    List<Livro> listaLivros = new ArrayList<>();

    // Estado do formulário
    String tituloSelecionado, autorSelecionado, anoSelecionado, editoraSelecionada;
    double latitude = 0, longitude = 0;
    boolean localizacaoCapturada = false;
    boolean modoEditar = false;
    String livroIdEditando;

    // GPS e Firebase
    FusedLocationProviderClient fusedLocationClient;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        iniciarComponentes();
        configurarSpinners();
        configurarRecyclerViews();
        configurarEventos();

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarLivros();
    }

    private void iniciarComponentes() {
        etPesquisa = findViewById(R.id.et_pesquisa);
        btnBuscar = findViewById(R.id.btn_buscar);
        recyclerResultados = findViewById(R.id.recycler_resultados);

        tvTituloSelecionado = findViewById(R.id.tv_titulo_selecionado);
        tvAutorSelecionado = findViewById(R.id.tv_autor_selecionado);
        tvAnoSelecionado = findViewById(R.id.tv_ano_selecionado);
        tvEditoraSelecionada = findViewById(R.id.tv_editora_selecionada);
        spinnerSituacao = findViewById(R.id.spinner_situacao);
        spinnerStatus = findViewById(R.id.spinner_status);
        etObservacao = findViewById(R.id.et_observacao);
        tvCoordenadas = findViewById(R.id.tv_coordenadas);
        btnGps = findViewById(R.id.btn_gps);
        btnSalvar = findViewById(R.id.btn_salvar);

        recyclerLivros = findViewById(R.id.recycler_livros);
    }

    private void configurarSpinners() {
        String[] situacoes = getResources().getStringArray(R.array.situacoes);
        spinnerSituacao.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, situacoes));

        String[] status = getResources().getStringArray(R.array.status_leitura);
        spinnerStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, status));
    }

    private void configurarRecyclerViews() {
        recyclerResultados.setLayoutManager(new LinearLayoutManager(this));
        resultadoAdapter = new SearchResultAdapter(resultados, (titulo, autor, ano, editora) -> {
            // Preenche o formulário com o livro clicado na busca
            tituloSelecionado = titulo;
            autorSelecionado = autor;
            anoSelecionado = ano;
            editoraSelecionada = editora;
            tvTituloSelecionado.setText(titulo);
            tvAutorSelecionado.setText(autor.isEmpty() ? "Autor desconhecido" : autor);
            tvAnoSelecionado.setText(ano);
            tvEditoraSelecionada.setText(editora);
            modoEditar = false;
            livroIdEditando = null;
        });
        recyclerResultados.setAdapter(resultadoAdapter);

        recyclerLivros.setLayoutManager(new LinearLayoutManager(this));
        livroAdapter = new LivroAdapter(listaLivros, new LivroAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Livro livro) {
                // Clique curto: preenche formulário para editar
                preencherFormularioEdicao(livro);
            }

            @Override
            public void onItemLongClick(Livro livro) {
                // Clique longo: confirma exclusão
                confirmarExclusao(livro);
            }
        });
        recyclerLivros.setAdapter(livroAdapter);
    }

    private void configurarEventos() {
        btnBuscar.setOnClickListener(v -> buscarLivros());
        btnGps.setOnClickListener(v -> capturarLocalizacao());
        btnSalvar.setOnClickListener(v -> salvarLivro());
    }

    // ===================== BUSCA DE LIVROS =====================

    private void buscarLivros() {
        String query = etPesquisa.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Digite algo para pesquisar", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "https://openlibrary.org/search.json?q="
                            + query.replace(" ", "+")
                            + "&limit=20&fields=title,author_name,first_publish_year,publisher";

                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder resultado = new StringBuilder();
                    String linha;
                    while ((linha = reader.readLine()) != null) {
                        resultado.append(linha);
                    }
                    reader.close();
                    connection.disconnect();

                    String jsonString = resultado.toString();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            processarResposta(jsonString);
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Erro ao buscar livros", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void processarResposta(String jsonString) {
        resultados.clear();
        try {
            JSONObject json = new JSONObject(jsonString);
            JSONArray docs = json.optJSONArray("docs");
            if (docs == null) return;

            for (int i = 0; i < docs.length(); i++) {
                JSONObject doc = docs.getJSONObject(i);

                String titulo = doc.optString("title", "");
                if (titulo.isEmpty()) continue;

                String autor = "";
                JSONArray autores = doc.optJSONArray("author_name");
                if (autores != null && autores.length() > 0) {
                    autor = autores.getString(0);
                }

                String ano = "";
                int anoInt = doc.optInt("first_publish_year", 0);
                if (anoInt > 0) ano = String.valueOf(anoInt);

                String editora = "";
                JSONArray editoras = doc.optJSONArray("publisher");
                if (editoras != null && editoras.length() > 0) {
                    editora = editoras.getString(0);
                }

                resultados.add(new SearchResultAdapter.BookResult(titulo, autor, ano, editora));
            }

            resultadoAdapter.atualizarResultados(new ArrayList<>(resultados));

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao processar resposta", Toast.LENGTH_SHORT).show();
        }
    }

    // ===================== GPS =====================

    private void capturarLocalizacao() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        localizacaoCapturada = true;
                        tvCoordenadas.setText("Lat: " + latitude + "  Lon: " + longitude);
                    } else {
                        Toast.makeText(this,
                                "Não foi possível obter localização. Ative o GPS.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                capturarLocalizacao();
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ===================== FIREBASE CRUD =====================

    private void salvarLivro() {
        if (tituloSelecionado == null || tituloSelecionado.isEmpty()) {
            Toast.makeText(this, "Selecione um livro primeiro", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!localizacaoCapturada) {
            Toast.makeText(this, "Capture a localização antes de salvar", Toast.LENGTH_SHORT).show();
            return;
        }

        String situacao = spinnerSituacao.getSelectedItem().toString();
        String status = spinnerStatus.getSelectedItem().toString();
        String observacao = etObservacao.getText().toString().trim();

        Livro livro = new Livro(tituloSelecionado, autorSelecionado, anoSelecionado,
                editoraSelecionada, latitude, longitude, situacao, status, observacao);

        if (modoEditar && livroIdEditando != null) {
            String idEditando = livroIdEditando;
            db.collection("livros").document(idEditando)
                    .set(livro)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Livro atualizado!", Toast.LENGTH_SHORT).show();
                        // Atualiza o item na lista local sem precisar de nova consulta
                        livro.setId(idEditando);
                        for (int i = 0; i < listaLivros.size(); i++) {
                            if (idEditando.equals(listaLivros.get(i).getId())) {
                                listaLivros.set(i, livro);
                                break;
                            }
                        }
                        livroAdapter.atualizarLista(new ArrayList<>(listaLivros));
                        limparFormulario();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro ao atualizar", Toast.LENGTH_SHORT).show());
        } else {
            db.collection("livros")
                    .add(livro)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Livro salvo!", Toast.LENGTH_SHORT).show();
                        // Adiciona direto na lista local sem precisar de nova consulta
                        livro.setId(documentReference.getId());
                        listaLivros.add(0, livro);
                        livroAdapter.atualizarLista(new ArrayList<>(listaLivros));
                        limparFormulario();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro ao salvar", Toast.LENGTH_SHORT).show());
        }
    }

    private void carregarLivros() {
        db.collection("livros")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaLivros.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Livro livro = doc.toObject(Livro.class);
                        livro.setId(doc.getId());
                        listaLivros.add(livro);
                    }
                    livroAdapter.atualizarLista(new ArrayList<>(listaLivros));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao carregar livros", Toast.LENGTH_SHORT).show());
    }

    private void confirmarExclusao(Livro livro) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir livro?")
                .setMessage(livro.getTitulo())
                .setPositiveButton("Sim", (dialog, which) -> excluirLivro(livro))
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirLivro(Livro livro) {
        db.collection("livros").document(livro.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Livro excluído", Toast.LENGTH_SHORT).show();
                    listaLivros.remove(livro);
                    livroAdapter.atualizarLista(new ArrayList<>(listaLivros));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao excluir", Toast.LENGTH_SHORT).show());
    }

    // ===================== FORMULÁRIO =====================

    private void preencherFormularioEdicao(Livro livro) {
        modoEditar = true;
        livroIdEditando = livro.getId();
        tituloSelecionado = livro.getTitulo();
        autorSelecionado = livro.getAutor();
        anoSelecionado = livro.getAnoPublicacao();
        editoraSelecionada = livro.getEditora();

        tvTituloSelecionado.setText(livro.getTitulo());
        tvAutorSelecionado.setText(livro.getAutor());
        tvAnoSelecionado.setText(livro.getAnoPublicacao());
        tvEditoraSelecionada.setText(livro.getEditora());

        latitude = livro.getLatitude();
        longitude = livro.getLongitude();
        localizacaoCapturada = true;
        tvCoordenadas.setText("Lat: " + latitude + "  Lon: " + longitude);

        etObservacao.setText(livro.getObservacao());
        selecionarSpinner(spinnerSituacao, livro.getSituacao());
        selecionarSpinner(spinnerStatus, livro.getStatus());
    }

    private void limparFormulario() {
        tituloSelecionado = null;
        autorSelecionado = null;
        anoSelecionado = null;
        editoraSelecionada = null;
        tvTituloSelecionado.setText("Nenhum livro selecionado");
        tvAutorSelecionado.setText("");
        tvAnoSelecionado.setText("");
        tvEditoraSelecionada.setText("");
        latitude = 0;
        longitude = 0;
        localizacaoCapturada = false;
        tvCoordenadas.setText("Localização não capturada");
        etObservacao.setText("");
        spinnerSituacao.setSelection(0);
        spinnerStatus.setSelection(0);
        modoEditar = false;
        livroIdEditando = null;
    }

    private void selecionarSpinner(Spinner spinner, String valor) {
        if (valor == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (valor.equals(spinner.getItemAtPosition(i).toString())) {
                spinner.setSelection(i);
                return;
            }
        }
    }
}
