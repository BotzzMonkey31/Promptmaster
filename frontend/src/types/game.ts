export interface Player {
  id: string;
  username: string;
  picture?: string;
}

export interface Puzzle {
  id: string;
  name: string;
  content: string;
  description: string;
  completed: boolean;
}

export interface PlayerStatus {
  score: number;
  hasCompleted: boolean;
  code?: string;
}

export interface GameState {
  id: string;
  players: Player[];
  currentRound: number;
  totalRounds: number;
  currentTurn: string;
  puzzle: Puzzle;
  playerStatus: Record<string, PlayerStatus>;
}

export type WebSocketMessageType =
  | 'GAME_STATE'
  | 'ERROR'
  | 'SUBMIT_SOLUTION'
  | 'COMPLETE_PUZZLE'
  | 'FORFEIT'
  | 'AI_RESPONSE';

export interface WebSocketMessage {
  type?: WebSocketMessageType;
  payload?: any;
  text?: string;
  code?: string;
  completeCode?: string;
  success?: boolean;
  score?: number;
  correctnessScore?: number;
  qualityScore?: number;
  timeBonus?: number;
  playerId?: string;
}

export interface GameError {
  message: string;
}
