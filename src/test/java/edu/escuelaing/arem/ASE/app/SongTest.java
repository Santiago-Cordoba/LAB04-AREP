package edu.escuelaing.arem.ASE.app;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SongTest {

    @Test
    public void testSongCreation() {
        Song song = new Song("Bohemian Rhapsody", "Queen");
        assertAll(
                () -> assertEquals("Bohemian Rhapsody", song.getTitle()),
                () -> assertEquals("Queen", song.getArtist())
        );
    }

    @Test
    public void testToStringJsonFormat() {
        Song song = new Song("Imagine", "John Lennon");
        String expectedJson = "{\"title\":\"Imagine\", \"artist\":\"John Lennon\"}";
        assertEquals(expectedJson, song.toString());
    }

    @Test
    public void testSongWithSpecialCharacters() {
        Song song = new Song("Música", "Artista con ñ");
        String expectedJson = "{\"title\":\"Música\", \"artist\":\"Artista con ñ\"}";
        assertEquals(expectedJson, song.toString());
    }
}