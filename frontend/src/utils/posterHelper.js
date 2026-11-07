/**
 * Utility to provide safe, reliable movie poster URLs with elegant SVG fallbacks.
 * Ensures zero broken image icons appear anywhere in the UI.
 */

export function getFallbackPosterSvg(title = 'Movie', genre = 'Cinema') {
  const cleanTitle = title.replace(/[<>&"]/g, '');
  const cleanGenre = genre.replace(/[<>&"]/g, '');

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="500" height="750" viewBox="0 0 500 750">
    <defs>
      <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#1e1b4b" />
        <stop offset="50%" stop-color="#0f172a" />
        <stop offset="100%" stop-color="#111827" />
      </linearGradient>
      <linearGradient id="accent" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stop-color="#e50914" />
        <stop offset="100%" stop-color="#38bdf8" />
      </linearGradient>
    </defs>
    <rect width="500" height="750" fill="url(#grad)" />
    <rect x="0" y="742" width="500" height="8" fill="url(#accent)" />
    
    <!-- Cinematic Film Reel Icon -->
    <g transform="translate(180, 180)" fill="none" stroke="#38bdf8" stroke-width="4" opacity="0.65">
      <circle cx="70" cy="70" r="60" />
      <circle cx="70" cy="70" r="20" fill="#38bdf8" opacity="0.3" />
      <circle cx="70" cy="35" r="10" fill="#38bdf8" />
      <circle cx="70" cy="105" r="10" fill="#38bdf8" />
      <circle cx="35" cy="70" r="10" fill="#38bdf8" />
      <circle cx="105" cy="70" r="10" fill="#38bdf8" />
    </g>

    <!-- CinemaPulse Watermark -->
    <text x="250" y="100" font-family="'Plus Jakarta Sans', sans-serif" font-size="16" font-weight="700" fill="#e50914" text-anchor="middle" letter-spacing="3">CINEMAPULSE EXCLUSIVE</text>

    <!-- Movie Title Wrap -->
    <text x="250" y="440" font-family="'Plus Jakarta Sans', sans-serif" font-size="28" font-weight="800" fill="#ffffff" text-anchor="middle">
      ${cleanTitle.length > 25 ? cleanTitle.substring(0, 22) + '...' : cleanTitle}
    </text>

    <!-- Genre Pill -->
    <rect x="150" y="480" width="200" height="32" rx="16" fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.15)" />
    <text x="250" y="501" font-family="'Plus Jakarta Sans', sans-serif" font-size="13" font-weight="600" fill="#38bdf8" text-anchor="middle">${cleanGenre}</text>

    <!-- Bottom Badge -->
    <text x="250" y="660" font-family="'Plus Jakarta Sans', sans-serif" font-size="14" font-weight="600" fill="#94a3b8" text-anchor="middle">IN CINEMAS &amp; IMAX</text>
  </svg>`;

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

export function handlePosterError(e, title, genre) {
  e.currentTarget.onerror = null;
  e.currentTarget.src = getFallbackPosterSvg(title, genre);
}
