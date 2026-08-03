// Single source of truth for every sound effect the app can play. Mirrors
// the convention in src/assets/theme.ts: components never import a raw
// asset path directly, they ask useSound() for a named effect from here so
// every sound (and its volume) still traces back to one declared source —
// swap a file or retune a volume in one place instead of hunting call sites.
//
// All files live in src/assets/sounds/Audio/ (the Kenney-style UI SFX pack)
// plus two standalone one-offs in src/assets/sounds/ for the "wrong answer"
// stings. Import URLs are resolved by Vite at build time.

import back001 from './sounds/Audio/back_001.ogg'
import bong001 from './sounds/Audio/bong_001.ogg'
import click003 from './sounds/Audio/click_003.ogg'
import confirmation002 from './sounds/Audio/confirmation_002.ogg'
import confirmation004 from './sounds/Audio/confirmation_004.ogg'
import drop002 from './sounds/Audio/drop_002.ogg'
import drop004 from './sounds/Audio/drop_004.ogg'
import error002 from './sounds/Audio/error_002.ogg'
import glassBreak from './sounds/Audio/glass_003.ogg'
import glitch002 from './sounds/Audio/glitch_002.ogg'
import glass005 from './sounds/Audio/glass_005.ogg'
import maximize003 from './sounds/Audio/maximize_003.ogg'
import maximize006 from './sounds/Audio/maximize_006.ogg'
import minimize003 from './sounds/Audio/minimize_003.ogg'
import minimize004 from './sounds/Audio/minimize_004.ogg'
import open002 from './sounds/Audio/open_002.ogg'
import pluck001 from './sounds/Audio/pluck_001.ogg'
import pluck002 from './sounds/Audio/pluck_002.ogg'
import question002 from './sounds/Audio/question_002.ogg'
import scratch003 from './sounds/Audio/scratch_003.ogg'
import select004 from './sounds/Audio/select_004.ogg'
import switch003 from './sounds/Audio/switch_003.ogg'
import tick001 from './sounds/Audio/tick_001.ogg'
import toggle002 from './sounds/Audio/toggle_002.ogg'
import wrongAnswerSting from './sounds/lesiakower-error-mistake-sound-effect-incorrect-answer-437420.mp3'
import victoryFanfare from './sounds/eaglaxle-gaming-victory-464016.mp3'

// Every effect the app can trigger, keyed by the moment it fires at — the
// key is what components ask useSound().play() for, never the file name.
export const SOUND_EFFECTS = {
  // Generic UI chrome
  buttonClick: drop004,
  buttonToggle: toggle002,
  cardOpen: open002,
  cardClose: back001,
  navigateBack: back001,
  menuSelect: select004,

  // Lobby / room lifecycle
  roomEnter: maximize006,
  playerJoin: pluck001,
  playerLeave: minimize004,
  playerKicked: glitch002,
  hostStartGame: maximize003,
  settingsChange: switch003,

  // Word-chain turn flow
  yourTurn: bong001,
  turnPassed: tick001,
  wordTyping: scratch003,
  wordSubmit: click003,
  wordAccepted: confirmation002,
  wordRejected: error002,
  wordTimedOut: drop002,
  targetReached: confirmation004,
  rewindUsed: minimize003,
  timeRunningOutTick: glass005,

  // Voting phase
  votingStart: question002,
  voteCast: pluck002,

  // Round outcomes
  crewWon: confirmation004,
  infiltratorEscaped: glassBreak,
  infiltratorWordGuessed: wrongAnswerSting,
  gameError: error002,
  gameResultsShown: victoryFanfare,
} as const

export type SoundEffectName = keyof typeof SOUND_EFFECTS
