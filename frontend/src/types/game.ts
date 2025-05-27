export interface Player {
  id: string;
  username: string;
  picture?: string;
}

export interface Puzzle {
  id: string;
  name: string;
  description: string;
  type: 'BY_PASS' | 'FAULTY' | 'MULTI_STEP';
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  starterCode?: string;
  testCases?: string[];
  solutionSteps?: SolutionStep[];
  categories?: string[];
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
  currentPuzzle: Puzzle;
  playerStatus: Record<string, PlayerStatus>;
  scores?: Record<string, number>;
  state?: 'IN_PROGRESS' | 'ENDED';
}

export type WebSocketMessageType =
  | 'GAME_STATE'
  | 'ERROR'
  | 'SUBMIT_SOLUTION'
  | 'COMPLETE_PUZZLE'
  | 'FORFEIT'
  | 'AI_RESPONSE'
  | 'PLAYER_COMPLETION';

export interface WebSocketMessage {
  type: WebSocketMessageType;
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
  message?: string;
}

export interface GameError {
  message: string;
}

export interface SolutionStep {
  title: string;
  description: string;
  code?: string;
}
