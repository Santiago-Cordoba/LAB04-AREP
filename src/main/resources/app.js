// Variables globales
let allSongs = [];

// Cuando el DOM esté completamente cargado
document.addEventListener('DOMContentLoaded', () => {
    loadSongs();

    // Permitir búsqueda al presionar Enter
    document.getElementById('searchInput').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            searchSongs();
        }
    });
});

// Cargar canciones desde el servidor
async function loadSongs() {
    try {
        showLoading(true);
        const response = await fetch('/api/songs');

        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }

        const data = await response.json();
        allSongs = data.songs;
        updateSongList(allSongs);
    } catch (error) {
        console.error('Error al cargar canciones:', error);
        showError('Error al cargar las canciones. Intenta recargar la página.');
    } finally {
        showLoading(false);
    }
}

// Agregar una nueva canción
async function addSong() {
    const title = document.getElementById('songTitle').value.trim();
    const artist = document.getElementById('songArtist').value.trim();

    if (!title || !artist) {
        showError('Por favor ingresa título y artista');
        return;
    }

    try {
        showLoading(true);
        const response = await fetch(`/api/songs/add?title=${encodeURIComponent(title)}&artist=${encodeURIComponent(artist)}`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }

        // Limpiar formulario
        document.getElementById('songTitle').value = '';
        document.getElementById('songArtist').value = '';

        // Recargar la lista
        await loadSongs();
    } catch (error) {
        console.error('Error al agregar canción:', error);
        showError('Error al agregar la canción. Intenta nuevamente.');
    } finally {
        showLoading(false);
    }
}

// Buscar canciones
function searchSongs() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();

    if (!searchTerm) {
        updateSongList(allSongs);
        return;
    }

    const filteredSongs = allSongs.filter(song =>
        song.title.toLowerCase().includes(searchTerm) ||
        song.artist.toLowerCase().includes(searchTerm)
    );

    updateSongList(filteredSongs);
}

// Actualizar la tabla de canciones
function updateSongList(songs) {
    const tbody = document.querySelector('#songsTable tbody');
    tbody.innerHTML = '';

    if (songs.length === 0) {
        const tr = document.createElement('tr');
        tr.innerHTML = '<td colspan="3" style="text-align: center;">No se encontraron canciones</td>';
        tbody.appendChild(tr);
        return;
    }

    songs.forEach((song, index) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${song.title}</td>
            <td>${song.artist}</td>
        `;
        tbody.appendChild(tr);
    });
}

// Mostrar/ocultar carga
function showLoading(show) {
    document.getElementById('loading').style.display = show ? 'block' : 'none';
}

// Mostrar error
function showError(message) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;
    errorDiv.style.color = 'var(--error-color)';
    errorDiv.style.margin = '10px 0';
    errorDiv.style.padding = '10px';
    errorDiv.style.backgroundColor = '#ffeeee';
    errorDiv.style.borderRadius = '4px';

    const container = document.querySelector('.container');
    const firstChild = container.firstChild;
    container.insertBefore(errorDiv, firstChild);

    setTimeout(() => {
        errorDiv.remove();
    }, 5000);
}

