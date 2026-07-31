<template>
  <div class="animated-bg" aria-hidden="true" :style="{ backgroundImage: tileLayer }" />
</template>

<script setup lang="ts">
// Tiled background made of random alphabet letters (mixed case), rendered as bold
// text inside a single repeating SVG background-image tile. Letters are placed on a
// brick-style grid (every other row offset by half a cell) so they interlock instead
// of lining up, and the whole tile is animated with one CSS transform along a single
// diagonal — far cheaper than mounting hundreds of DOM text nodes for decoration.
import { THEME_COLORS } from '@/assets/theme'

const FILL = `%23${THEME_COLORS.bgSky300.slice(1)}` // url-encoded hex for use inside the SVG data URI
const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'

function randomLetter(): string {
  return LETTERS[Math.floor(Math.random() * LETTERS.length)]
}

function letterGlyph(x: number, y: number): string {
  const letter = randomLetter()
  const rotation = Math.round(Math.random() * 30 - 15)
  // Baseline offset instead of dominant-baseline='middle': some SVG-as-CSS-background
  // rasterizers (notably Chromium) ignore dominant-baseline there, which clips glyphs
  // to their top half. A manual y-shift centers reliably in every renderer.
  const baselineY = y + 12
  return (
    `%3Ctext x='${x}' y='${baselineY}' transform='rotate(${rotation} ${x} ${baselineY})' ` +
    `font-family='Arial, sans-serif' font-size='34' font-weight='700' ` +
    `fill='${FILL}' text-anchor='middle'%3E${letter}%3C/text%3E`
  )
}

// Brick-style layout: every other row is shifted half a cell so letters
// interlock in a woven pattern instead of lining up in straight rows/columns.
// The tile keeps one extra "phantom" column of margin on each side so a
// shifted row's letters stay fully inside the tile bounds (no wrap-cropping),
// while the repeat is still seamless because that margin lines up with the
// opposite edge's margin on the next tile.
const CELL = 56
const COLS = 3
const ROWS = 6
const TILE_W = CELL * COLS
const TILE_H = CELL * ROWS

let glyphs = ''
for (let row = 0; row < ROWS; row++) {
  const offset = row % 2 === 1 ? CELL / 2 : 0
  for (let col = -1; col <= COLS; col++) {
    const x = col * CELL + offset + CELL / 2
    if (x < -CELL / 2 || x > TILE_W + CELL / 2) continue
    glyphs += letterGlyph(x, row * CELL + CELL / 2)
  }
}

const TILE_SVG =
  `data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='${TILE_W}' height='${TILE_H}' viewBox='0 0 ${TILE_W} ${TILE_H}'%3E` +
  glyphs +
  `%3C/svg%3E`

const tileLayer = `url("${TILE_SVG}")`
</script>

<style scoped>
.animated-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  background-color: var(--color-bg-navy-800);
  background-repeat: repeat;
  background-size: 168px 336px;
  animation: drift 45s linear infinite;
  will-change: background-position;
}

.animated-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    160deg,
    var(--color-bg-navy-900) 0%,
    var(--color-bg-navy-700) 55%,
    var(--color-bg-navy-950) 100%
  );
  opacity: 0.85;
  pointer-events: none;
}

/* Single diagonal direction, exactly one tile per loop so it repeats seamlessly. */
@keyframes drift {
  from {
    background-position: 0 0;
  }
  to {
    background-position: -168px -336px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .animated-bg {
    animation: none;
  }
}
</style>
