package com.example.bibliotecagps.model;

public class Livro {
    private String id;
    private String titulo;
    private String autor;
    private String anoPublicacao;
    private String editora;
    private double latitude;
    private double longitude;
    private String situacao;
    private String status;
    private String observacao;
    private long timestamp;

    // Construtor vazio obrigatório para o Firestore
    public Livro() {}

    public Livro(String titulo, String autor, String anoPublicacao, String editora,
                 double latitude, double longitude,
                 String situacao, String status, String observacao) {
        this.titulo = titulo;
        this.autor = autor;
        this.anoPublicacao = anoPublicacao;
        this.editora = editora;
        this.latitude = latitude;
        this.longitude = longitude;
        this.situacao = situacao;
        this.status = status;
        this.observacao = observacao;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getAnoPublicacao() { return anoPublicacao; }
    public void setAnoPublicacao(String anoPublicacao) { this.anoPublicacao = anoPublicacao; }

    public String getEditora() { return editora; }
    public void setEditora(String editora) { this.editora = editora; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
