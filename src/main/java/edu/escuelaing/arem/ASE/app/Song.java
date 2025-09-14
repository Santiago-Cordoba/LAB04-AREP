package edu.escuelaing.arem.ASE.app;

/**
 * Clase que representa una canción con su título y artista.
 */
public class Song {
    private final String title;
    private final String artist;

    /**
     * Constructor de una canción.
     * @param title Título de la canción
     * @param artist Artista o banda
     */
    public Song(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    /**
     * @return Título de la canción
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return Artista de la canción
     */
    public String getArtist() {
        return artist;
    }

    /**
     * Representación JSON de la canción.
     * @return String en formato JSON
     */
    @Override
    public String toString() {
        return String.format("{\"title\":\"%s\", \"artist\":\"%s\"}", title, artist);
    }
}