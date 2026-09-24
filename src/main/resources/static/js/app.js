(() => {
  const search = document.querySelector('#film-search');
  const genre = document.querySelector('#genre-filter');
  const movies = [...document.querySelectorAll('[data-movie]')];
  const status = document.querySelector('#catalogue-status');
  const empty = document.querySelector('#catalogue-empty');

  if (!search || !genre || !movies.length || !status || !empty) return;

  const filterMovies = () => {
    const query = search.value.trim().toLocaleLowerCase('id-ID');
    const selectedGenre = genre.value.toLocaleLowerCase();
    let visible = 0;

    movies.forEach((movie) => {
      const matchesTitle = movie.dataset.title.toLocaleLowerCase('id-ID').includes(query);
      const matchesGenre = selectedGenre === 'semua genre' || movie.dataset.genre === selectedGenre;
      const matches = matchesTitle && matchesGenre;
      movie.hidden = !matches;
      if (matches) visible += 1;
    });

    status.textContent = `Menampilkan ${visible} film`;
    empty.hidden = visible !== 0;
  };

  search.addEventListener('input', filterMovies);
  genre.addEventListener('change', filterMovies);
})();
