// Mirrors the color tokens defined in src/assets/main.css (@theme block).
// Components that need a real hex value in JS (e.g. for computed shadow
// colors) should import from here instead of hardcoding a literal, so every
// color still traces back to a single declared source.
export const THEME_COLORS = {
  white: '#ffffff',
  black: '#101828',
  accent500: '#8b5cf6',
  accent700: '#5b21b6',
  secondary500: '#3b82f6',
  secondary700: '#1d4ed8',
  success500: '#12b76a',
  error500: '#f04438',
  gray400: '#98a2b3',
  gray500: '#667085',
  gray800: '#1d2939',
  gray900: '#101828',
  brand200: '#c2d6ff',
  brand500: '#465fff',
  brand700: '#2a31d8',
  bgSky300: '#a9c1ff',
} as const
