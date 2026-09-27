/**
 * Tujuan program: Memfilter katalog film yang sudah dirender dari database.
 * Contributor: 'Aarif Rahmaan J. Faqiih
 * NIM: 103112430182
 * Role: User
 * Kelas: IF-12-07
 * Terakhir diubah: 27 September 2026, 00:00 WIB
 */
(() => {
  const searchInput = document.querySelector('#film-search');
  const genreSelect = document.querySelector('#genre-filter');
  const movieCards = [...document.querySelectorAll('[data-movie]')];
  const catalogueStatus = document.querySelector('#catalogue-status');
  const emptyState = document.querySelector('#catalogue-empty');

  if (!searchInput || !genreSelect || !catalogueStatus || !emptyState) {
    return;
  }

  /**
   * Memecah daftar genre pada data atribut. Nilai berasal dari elemen yang
   * telah dirender server, lalu dinormalisasi agar pencarian tidak peka huruf.
   */
  const getMovieGenres = (movieCard) => {
    const genreText = movieCard.dataset.genres || '';

    return genreText
      .split(',')
      .map((genreName) => genreName.trim().toLocaleLowerCase('id-ID'))
      .filter((genreName) => genreName.length > 0);
  };

  const filterMovies = () => {
    const searchQuery = searchInput.value.trim().toLocaleLowerCase('id-ID');
    const selectedGenre = genreSelect.value.trim().toLocaleLowerCase('id-ID');
    let visibleMovieCount = 0;

    movieCards.forEach((movieCard) => {
      const movieTitle = (movieCard.dataset.title || '').toLocaleLowerCase('id-ID');
      const movieGenres = getMovieGenres(movieCard);
      const matchesTitle = movieTitle.includes(searchQuery);
      const matchesGenre = selectedGenre.length === 0 || movieGenres.includes(selectedGenre);
      const isVisible = matchesTitle && matchesGenre;

      movieCard.hidden = !isVisible;

      if (isVisible) {
        visibleMovieCount++;
      }
    });

    catalogueStatus.textContent = `Menampilkan ${visibleMovieCount} film`;
    emptyState.hidden = visibleMovieCount !== 0;
  };

  searchInput.addEventListener('input', filterMovies);
  genreSelect.addEventListener('change', filterMovies);
})();
