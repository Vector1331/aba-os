<<<<<<< HEAD
import { useState, useMemo, useCallback } from 'react';
import { Play, Square, CheckCircle, X, RotateCcw, Clock, ChevronDown, ChevronRight, AlertTriangle } from 'lucide-react';
import { useApp } from '@/context/AppContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import type { Goal, Session, Trial, SessionTrial } from '@/data/mockData';
import { promptLevelLabels, buildAggregatedTrials, getDomainLabel } from '@/data/mockData';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible';
=======
import { useState } from 'react';
import { Plus, Calendar, CheckCircle } from 'lucide-react';
import { useApp } from '@/context/AppContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Slider } from '@/components/ui/slider';
import { DataSyncHint } from '@/components/DataSyncHint';
import { useToast } from '@/hooks/use-toast';
import type { Goal, Session, SessionTrial } from '@/data/mockData';
import { promptLevelLabels } from '@/data/mockData';
>>>>>>> cd8e445276da75daae379d53706c3f23c9dd9241

interface SessionsTabProps {
  childId: string;
  sessions: Session[];
  goals: Goal[];
}

<<<<<<< HEAD
// Live trial being recorded
interface LiveTrial {
  programId: string;
  stimulus: string;
  result: 'correct' | 'incorrect' | 'no_response' | null;
  promptLevel: number;
  problemBehavior: boolean;
}

// Program stimuli for quick-pick (reuse from mockData or define inline)
const COMMON_STIMULI: Record<string, string[]> = {
  mand: ['과자 줘', '물 주세요', '놀이 하고 싶어', '도와 줘', '더 줘', '이거 줘', '열어 줘', '안아 주세요', '같이 놀자', '그네 타고 싶어'],
  tact: ['사과', '자동차', '강아지', '고양이', '공', '물', '나무', '꽃', '집', '배'],
  listener: ['앉아', '일어나', '이리 와', '공 줘', '손 올려', '박수 쳐', '문 닫아', '신발 벗어', '손 씻어', '가방 가져와'],
  social: ['인사할 때', '이름 부를 때', '놀이 중', '간식 시간', '대화 중', '칭찬할 때'],
  imitation: ['박수', '만세', '점프', '돌기', '손흔들기', '고개 끄덕', '발 구르기'],
  echoic: ['사과', '바나나', '물', '자동차', '강아지', '고양이', '공', '나무', '맘마', '빠빠'],
  play: ['인형 밥주기', '블록 쌓기', '소꿉 요리', '병원 놀이', '전화 놀이', '가게 놀이'],
  vp_mts: ['모래', '점토', '물감', '스티커', '솜', '나무 블록', '고무공', '천'],
  lrffc: ['타는 것', '먹는 것', '과일', '동물', '탈것', '가구'],
  intraverbal: ['기뻐요', '슬퍼요', '화나요', '무서워요', '좋아요', '싫어요'],
  group: ['블록→그림', '그림→간식', '간식→정리', '정리→놀이'],
};

function getStimuliForGoal(goal: Goal): string[] {
  return COMMON_STIMULI[goal.domain || ''] || ['자극1', '자극2', '자극3', '자극4', '자극5'];
}

export function SessionsTab({ childId, sessions, goals }: SessionsTabProps) {
  const { addSession, therapists, role } = useApp();
  const { toast } = useToast();

  // Session recording state
  const [isRecording, setIsRecording] = useState(false);
  const [sessionStartTime, setSessionStartTime] = useState<Date | null>(null);
  const [sessionNotes, setSessionNotes] = useState('');
  const [expandedProgram, setExpandedProgram] = useState<string | null>(null);

  // Recorded trials during this session
  const [recordedTrials, setRecordedTrials] = useState<LiveTrial[]>([]);

  // Current prompt level (global default, adjustable per trial)
  const [currentPromptLevel, setCurrentPromptLevel] = useState(0);

  // Active STOs only
  const activeSTOs = useMemo(() =>
    goals.filter(g => g.objectiveType === 'STO' && g.status === 'active'),
    [goals]
  );

  // Group STOs by parent LTO
  const groupedByLTO = useMemo(() => {
    const ltos = goals.filter(g => g.objectiveType === 'LTO' && g.status === 'active');
    const groups: { lto: Goal; stos: Goal[] }[] = [];
    ltos.forEach(lto => {
      const stos = activeSTOs.filter(s => s.parentProgramId === lto.id);
      if (stos.length > 0) groups.push({ lto, stos });
    });
    // Orphan STOs
    const orphans = activeSTOs.filter(s => !s.parentProgramId || !ltos.find(l => l.id === s.parentProgramId));
    if (orphans.length > 0) {
      groups.push({ lto: { id: '_orphan', title: '미분류', description: '', category: '', objectiveType: 'LTO' } as Goal, stos: orphans });
    }
    return groups;
  }, [goals, activeSTOs]);

  // Get trials for a specific program in current session
  const getTrialsForProgram = useCallback((programId: string) =>
    recordedTrials.filter(t => t.programId === programId),
    [recordedTrials]
  );

  const getSuccessRate = useCallback((programId: string) => {
    const trials = getTrialsForProgram(programId).filter(t => t.result !== null);
    if (trials.length === 0) return null;
    const correct = trials.filter(t => t.result === 'correct').length;
    return Math.round((correct / trials.length) * 100);
  }, [getTrialsForProgram]);

  // ── Start session ──
  const handleStartSession = () => {
    setIsRecording(true);
    setSessionStartTime(new Date());
    setRecordedTrials([]);
    setSessionNotes('');
    setCurrentPromptLevel(0);
    // Auto-expand first program
    if (groupedByLTO.length > 0 && groupedByLTO[0].stos.length > 0) {
      setExpandedProgram(groupedByLTO[0].stos[0].id);
    }
  };

  // ── Record a trial ──
  const recordTrial = (programId: string, stimulus: string, result: 'correct' | 'incorrect' | 'no_response') => {
    setRecordedTrials(prev => [...prev, {
      programId,
      stimulus,
      result,
      promptLevel: currentPromptLevel,
      problemBehavior: false,
    }]);
  };

  // ── Toggle problem behavior on last trial ──
  const toggleLastProblemBehavior = (programId: string) => {
    setRecordedTrials(prev => {
      const idx = [...prev].reverse().findIndex(t => t.programId === programId);
      if (idx === -1) return prev;
      const realIdx = prev.length - 1 - idx;
      const updated = [...prev];
      updated[realIdx] = { ...updated[realIdx], problemBehavior: !updated[realIdx].problemBehavior };
      return updated;
    });
  };

  // ── Undo last trial for a program ──
  const undoLastTrial = (programId: string) => {
    setRecordedTrials(prev => {
      const idx = [...prev].reverse().findIndex(t => t.programId === programId);
      if (idx === -1) return prev;
      const realIdx = prev.length - 1 - idx;
      return [...prev.slice(0, realIdx), ...prev.slice(realIdx + 1)];
    });
  };

  // ── End & save session ──
  const handleEndSession = () => {
    if (recordedTrials.length === 0) {
      toast({ title: '기록된 시행이 없습니다', description: '최소 1개 이상의 시행을 기록해주세요', variant: 'destructive' });
      return;
    }

    const now = new Date();
    const duration = sessionStartTime ? Math.round((now.getTime() - sessionStartTime.getTime()) / 60000) : 0;

    // Convert LiveTrials to Trial format
    const trialRecords: Trial[] = recordedTrials
      .filter(t => t.result !== null)
      .map((t, i) => ({
        id: `t-${Date.now()}-${i}`,
        sessionId: `s-${Date.now()}`,
        programId: t.programId,
        trialOrder: i + 1,
        stimulus: t.stimulus,
        response: t.result === 'correct' ? t.stimulus : '',
        result: t.result!,
        promptLevel: t.promptLevel,
        problemBehavior: t.problemBehavior,
      }));
=======
export function SessionsTab({ childId, sessions, goals }: SessionsTabProps) {
  const { addSession, therapists, role } = useApp();
  const { toast } = useToast();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newSession, setNewSession] = useState({
    date: new Date().toISOString().split('T')[0],
    duration: 50,
    notes: '',
  });
  const [trialData, setTrialData] = useState<Record<string, SessionTrial>>({});

  // Initialize trial data for all active goals
  const initializeTrialData = () => {
    const initial: Record<string, SessionTrial> = {};
    goals
      .filter((g) => g.status === 'active')
      .forEach((goal) => {
        initial[goal.id] = {
          goalId: goal.id,
          trials: 10,
          successes: 5,
          promptLevel: 1,
          problemBehaviorCount: 0,
        };
      });
    setTrialData(initial);
  };

  const handleOpenDialog = () => {
    initializeTrialData();
    setIsDialogOpen(true);
  };

  const handleCreateSession = () => {
    if (!newSession.date) return;
>>>>>>> cd8e445276da75daae379d53706c3f23c9dd9241

    const session: Session = {
      id: `s${Date.now()}`,
      childId,
      therapistId: therapists[0]?.id || 'th1',
<<<<<<< HEAD
      date: now.toISOString().split('T')[0],
      startTime: `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`,
      duration: Math.max(duration, 1),
      notes: sessionNotes,
      trialRecords,
      trials: buildAggregatedTrials(trialRecords),
      createdAt: now.toISOString(),
    };

    addSession(session);
    setIsRecording(false);
    setRecordedTrials([]);
    setSessionStartTime(null);

    toast({
      title: '세션 저장 완료',
      description: `${trialRecords.length}개 시행이 기록되었습니다. 분석에 반영됩니다.`,
      action: <div className="flex items-center gap-2 text-success"><CheckCircle className="h-4 w-4" /></div>,
    });
  };

  const canCreate = role === 'admin' || role === 'therapist';
  const totalTrials = recordedTrials.filter(t => t.result !== null).length;
  const totalCorrect = recordedTrials.filter(t => t.result === 'correct').length;
  const overallRate = totalTrials > 0 ? Math.round((totalCorrect / totalTrials) * 100) : 0;
  const elapsed = sessionStartTime ? Math.round((Date.now() - sessionStartTime.getTime()) / 60000) : 0;

  // ── Not recording: show start button + recent history ──
  if (!isRecording) {

    return (
      <div className="space-y-6">
        {/* Start Session CTA */}
        <div className="flex flex-col items-center justify-center rounded-xl border-2 border-dashed border-primary/30 bg-primary/5 p-8 space-y-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
            <Play className="h-8 w-8 text-primary" />
          </div>
          <div className="text-center">
            <h2 className="text-lg font-bold text-foreground">세션 시작하기</h2>
            <p className="text-sm text-muted-foreground mt-1">
              활성 STO {activeSTOs.length}개 · 시행 버튼으로 실시간 기록
            </p>
          </div>
          {canCreate && activeSTOs.length > 0 && (
            <Button size="lg" className="gap-2" onClick={handleStartSession}>
              <Play className="h-5 w-5" />
              세션 시작
            </Button>
          )}
          {activeSTOs.length === 0 && (
            <p className="text-sm text-muted-foreground">활성화된 단기목표(STO)가 없습니다. 개요 탭에서 목표를 추가하세요.</p>
          )}
        </div>

      </div>
    );
  }

  // ══════════════════════════════════════════
  // RECORDING MODE
  // ══════════════════════════════════════════
  return (
    <div className="space-y-4">
      {/* Session header bar */}
      <div className="sticky top-0 z-10 rounded-xl border border-primary/30 bg-card p-3 shadow-sm">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-destructive/10 animate-pulse">
              <div className="h-3 w-3 rounded-full bg-destructive" />
            </div>
            <div>
              <p className="text-sm font-bold">세션 기록 중</p>
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Clock className="h-3 w-3" />{elapsed}분 경과
                <span>·</span>
                <span>{totalTrials}회 시행</span>
                <span>·</span>
                <span className={cn('font-semibold', overallRate >= 70 ? 'text-success' : overallRate >= 50 ? 'text-warning' : 'text-destructive')}>
                  {overallRate}%
                </span>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {/* Global prompt level */}
            <div className="flex items-center gap-1">
              {promptLevelLabels.map((label, i) => (
                <button
                  key={i}
                  onClick={() => setCurrentPromptLevel(i)}
                  className={cn(
                    'rounded-md px-2 py-1 text-[10px] font-medium transition-colors',
                    currentPromptLevel === i
                      ? 'bg-primary text-primary-foreground'
                      : 'bg-muted text-muted-foreground hover:bg-muted/80'
                  )}
                  title={`촉진 수준: ${label}`}
                >
                  {label}
                </button>
              ))}
            </div>
            <Button variant="destructive" size="sm" className="gap-1" onClick={handleEndSession}>
              <Square className="h-3 w-3" />
              종료 & 저장
            </Button>
          </div>
        </div>
      </div>

      {/* Programs to record */}
      <div className="space-y-3">
        {groupedByLTO.map(({ lto, stos }) => (
          <div key={lto.id} className="space-y-2">
            {/* LTO header */}
            <div className="flex items-center gap-2 px-1">
              <Badge variant="outline" className="text-[10px]">
                {lto.domain ? getDomainLabel(lto.domain) : lto.title}
              </Badge>
              <span className="text-xs text-muted-foreground font-medium">{lto.title}</span>
            </div>

            {/* STO recording cards */}
            {stos.map(sto => {
              const programTrials = getTrialsForProgram(sto.id);
              const completed = programTrials.filter(t => t.result !== null);
              const correct = completed.filter(t => t.result === 'correct').length;
              const rate = completed.length > 0 ? Math.round((correct / completed.length) * 100) : null;
              const isExpanded = expandedProgram === sto.id;
              const stimuli = getStimuliForGoal(sto);
              const lastTrial = programTrials[programTrials.length - 1];

              return (
                <Card key={sto.id} className={cn(
                  'transition-all',
                  isExpanded && 'ring-1 ring-primary/30'
                )}>
                  <CardContent className="p-0">
                    {/* Program header - always visible */}
                    <Collapsible open={isExpanded} onOpenChange={() => setExpandedProgram(isExpanded ? null : sto.id)}>
                      <CollapsibleTrigger className="w-full text-left p-3">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2 min-w-0">
                            {isExpanded ? <ChevronDown className="h-4 w-4 text-muted-foreground shrink-0" /> : <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0" />}
                            <div className="min-w-0">
                              <p className="text-sm font-semibold truncate">{sto.title}</p>
                              <p className="text-[11px] text-muted-foreground truncate">{sto.description}</p>
                            </div>
                          </div>
                          <div className="flex items-center gap-2 shrink-0 ml-2">
                            {/* Mini trial dots */}
                            <div className="flex items-center gap-0.5">
                              {completed.slice(-10).map((t, i) => (
                                <div key={i} className={cn(
                                  'h-2.5 w-2.5 rounded-full',
                                  t.result === 'correct' ? 'bg-success' : t.result === 'incorrect' ? 'bg-destructive' : 'bg-muted-foreground'
                                )} />
                              ))}
                            </div>
                            <div className="text-right">
                              <span className={cn(
                                'text-sm font-bold',
                                rate === null ? 'text-muted-foreground' :
                                rate >= 70 ? 'text-success' : rate >= 50 ? 'text-warning' : 'text-destructive'
                              )}>
                                {rate !== null ? `${rate}%` : '—'}
                              </span>
                              <p className="text-[10px] text-muted-foreground">{completed.length}회</p>
                            </div>
                          </div>
                        </div>
                      </CollapsibleTrigger>

                      <CollapsibleContent>
                        <div className="border-t border-border/50 p-3 space-y-3">
                          {/* Quick-action buttons for stimuli */}
                          <div>
                            <p className="text-[10px] text-muted-foreground font-medium mb-2">자극을 선택하고 결과를 기록하세요</p>
                            <div className="grid grid-cols-2 sm:grid-cols-3 gap-1.5">
                              {stimuli.map((stim) => (
                                <div key={stim} className="flex items-center rounded-lg border border-border/50 overflow-hidden">
                                  <span className="flex-1 text-xs px-2 py-1.5 truncate">{stim}</span>
                                  <button
                                    onClick={() => recordTrial(sto.id, stim, 'correct')}
                                    className="h-full px-2.5 py-1.5 bg-success/10 text-success hover:bg-success/20 transition-colors border-l border-border/50 text-sm font-bold"
                                  >
                                    ✓
                                  </button>
                                  <button
                                    onClick={() => recordTrial(sto.id, stim, 'incorrect')}
                                    className="h-full px-2.5 py-1.5 bg-destructive/10 text-destructive hover:bg-destructive/20 transition-colors border-l border-border/50 text-sm font-bold"
                                  >
                                    ✗
                                  </button>
                                </div>
                              ))}
                            </div>
                          </div>

                          {/* Controls row */}
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              {lastTrial && (
                                <>
                                  <Button
                                    variant="ghost" size="sm"
                                    className="h-7 gap-1 text-xs"
                                    onClick={() => undoLastTrial(sto.id)}
                                  >
                                    <RotateCcw className="h-3 w-3" />되돌리기
                                  </Button>
                                  <Button
                                    variant={lastTrial.problemBehavior ? 'destructive' : 'ghost'}
                                    size="sm"
                                    className="h-7 gap-1 text-xs"
                                    onClick={() => toggleLastProblemBehavior(sto.id)}
                                  >
                                    <AlertTriangle className="h-3 w-3" />
                                    문제행동
                                  </Button>
                                </>
                              )}
                            </div>
                            {sto.targetCriteria && (
                              <span className="text-[10px] text-muted-foreground">🎯 {sto.targetCriteria}</span>
                            )}
                          </div>

                          {/* Recent trials log */}
                          {completed.length > 0 && (
                            <div className="flex flex-wrap gap-1">
                              {completed.map((t, i) => (
                                <div key={i} className={cn(
                                  'rounded px-1.5 py-0.5 text-[10px] flex items-center gap-1',
                                  t.result === 'correct' ? 'bg-success/10 text-success' : 'bg-destructive/10 text-destructive'
                                )}>
                                  {t.stimulus}
                                  {t.result === 'correct' ? '✓' : '✗'}
                                  {t.promptLevel > 0 && <span className="text-muted-foreground">P{t.promptLevel}</span>}
                                  {t.problemBehavior && <AlertTriangle className="h-2.5 w-2.5" />}
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      </CollapsibleContent>
                    </Collapsible>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        ))}
      </div>

      {/* Session notes */}
      <div className="space-y-2">
        <Label className="text-sm">세션 메모</Label>
        <Textarea
          value={sessionNotes}
          onChange={(e) => setSessionNotes(e.target.value)}
          placeholder="관찰 내용, 특이사항 등을 기록하세요..."
          rows={2}
          className="text-sm"
        />
      </div>
=======
      date: newSession.date,
      duration: newSession.duration,
      notes: newSession.notes,
      trials: Object.values(trialData),
      createdAt: new Date().toISOString(),
    };

    addSession(session);
    setIsDialogOpen(false);
    setNewSession({
      date: new Date().toISOString().split('T')[0],
      duration: 50,
      notes: '',
    });
    setTrialData({});

    // Show success toast with data sync message
    toast({
      title: "세션 기록 완료",
      description: "추이 분석 및 리포트에 즉시 반영됩니다",
      action: (
        <div className="flex items-center gap-2 text-success">
          <CheckCircle className="h-4 w-4" />
        </div>
      ),
    });
  };

  const updateTrialData = (goalId: string, field: keyof SessionTrial, value: number) => {
    setTrialData((prev) => ({
      ...prev,
      [goalId]: {
        ...prev[goalId],
        [field]: value,
        // Ensure successes don't exceed trials
        ...(field === 'trials' && prev[goalId].successes > value
          ? { successes: value }
          : {}),
      },
    }));
  };

  const canCreate = role === 'admin' || role === 'therapist';
  const sortedSessions = [...sessions].sort(
    (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">세션 기록 ({sessions.length}회)</h2>
        {canCreate && goals.length > 0 && (
          <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
            <DialogTrigger asChild>
              <Button size="sm" className="gap-2" onClick={handleOpenDialog}>
                <Plus className="h-4 w-4" />
                새 세션
              </Button>
            </DialogTrigger>
            <DialogContent className="max-h-[90vh] max-w-2xl overflow-y-auto">
              <DialogHeader>
                <DialogTitle>새 세션 기록</DialogTitle>
              </DialogHeader>
              <div className="grid gap-6 py-4">
                {/* Data sync hint */}
                <DataSyncHint />

                {/* Basic Info */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="date">날짜</Label>
                    <Input
                      id="date"
                      type="date"
                      value={newSession.date}
                      onChange={(e) => setNewSession({ ...newSession, date: e.target.value })}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="duration">시간 (분)</Label>
                    <Input
                      id="duration"
                      type="number"
                      value={newSession.duration}
                      onChange={(e) =>
                        setNewSession({ ...newSession, duration: parseInt(e.target.value) })
                      }
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="notes">세션 메모</Label>
                  <Textarea
                    id="notes"
                    value={newSession.notes}
                    onChange={(e) => setNewSession({ ...newSession, notes: e.target.value })}
                    placeholder="오늘 세션에서의 관찰 내용을 기록하세요"
                    rows={3}
                  />
                </div>

                {/* Goal Trials */}
                <div className="space-y-4">
                  <Label className="text-base font-semibold">목표별 기록</Label>
                  {goals
                    .filter((g) => g.status === 'active')
                    .map((goal) => {
                      const successRate = trialData[goal.id] 
                        ? Math.round((trialData[goal.id].successes / trialData[goal.id].trials) * 100)
                        : 0;
                      
                      return (
                        <Card key={goal.id} className="p-4">
                          <div className="mb-4 flex items-center justify-between">
                            <div>
                              <h4 className="font-medium">{goal.title}</h4>
                              <p className="text-sm text-muted-foreground">{goal.category}</p>
                            </div>
                            <div className="text-right">
                              <p className={`text-lg font-bold ${
                                successRate >= 70 ? 'text-success' : 
                                successRate >= 50 ? 'text-warning' : 'text-destructive'
                              }`}>
                                {successRate}%
                              </p>
                              <p className="text-xs text-muted-foreground">성공률</p>
                            </div>
                          </div>
                          <div className="grid gap-4 sm:grid-cols-2">
                            <div className="space-y-2">
                              <Label className="text-sm">시행 횟수: {trialData[goal.id]?.trials || 0}</Label>
                              <Slider
                                value={[trialData[goal.id]?.trials || 10]}
                                min={1}
                                max={20}
                                step={1}
                                onValueChange={([v]) => updateTrialData(goal.id, 'trials', v)}
                              />
                            </div>
                            <div className="space-y-2">
                              <Label className="text-sm">성공 횟수: {trialData[goal.id]?.successes || 0}</Label>
                              <Slider
                                value={[trialData[goal.id]?.successes || 5]}
                                min={0}
                                max={trialData[goal.id]?.trials || 10}
                                step={1}
                                onValueChange={([v]) => updateTrialData(goal.id, 'successes', v)}
                              />
                            </div>
                            <div className="space-y-2">
                              <Label className="text-sm">
                                촉진 수준: {promptLevelLabels[trialData[goal.id]?.promptLevel || 0]}
                              </Label>
                              <Slider
                                value={[trialData[goal.id]?.promptLevel || 0]}
                                min={0}
                                max={3}
                                step={1}
                                onValueChange={([v]) => updateTrialData(goal.id, 'promptLevel', v)}
                              />
                              <div className="flex justify-between text-xs text-muted-foreground">
                                {promptLevelLabels.map((label, i) => (
                                  <span key={i}>{label}</span>
                                ))}
                              </div>
                            </div>
                            <div className="space-y-2">
                              <Label className="text-sm">
                                문제행동: {trialData[goal.id]?.problemBehaviorCount || 0}회
                              </Label>
                              <Slider
                                value={[trialData[goal.id]?.problemBehaviorCount || 0]}
                                min={0}
                                max={10}
                                step={1}
                                onValueChange={([v]) =>
                                  updateTrialData(goal.id, 'problemBehaviorCount', v)
                                }
                              />
                            </div>
                          </div>
                        </Card>
                      );
                    })}
                </div>

                <Button onClick={handleCreateSession} className="mt-2 gap-2">
                  <CheckCircle className="h-4 w-4" />
                  세션 저장
                </Button>
              </div>
            </DialogContent>
          </Dialog>
        )}
      </div>

      {/* Data sync hint for session list */}
      {canCreate && sessions.length > 0 && (
        <DataSyncHint 
          variant="success" 
          message="세션을 기록하면 분석 탭과 리포트에 실시간 반영됩니다"
        />
      )}

      {sessions.length === 0 ? (
        <Card>
          <CardContent className="flex h-32 flex-col items-center justify-center gap-2">
            <p className="text-muted-foreground">기록된 세션이 없습니다</p>
            {canCreate && (
              <p className="text-xs text-muted-foreground">
                세션을 기록하면 분석 차트와 리포트가 자동으로 생성됩니다
              </p>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {sortedSessions.map((session) => {
            const avgSuccess =
              session.trials.reduce((acc, t) => acc + t.successes, 0) /
              session.trials.reduce((acc, t) => acc + t.trials, 0);
            const avgPrompt =
              session.trials.reduce((acc, t) => acc + t.promptLevel, 0) / session.trials.length;

            return (
              <Card key={session.id} className="transition-all hover:shadow-md">
                <CardContent className="p-4">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                        <Calendar className="h-5 w-5 text-primary" />
                      </div>
                      <div>
                        <p className="font-medium">
                          {new Date(session.date).toLocaleDateString('ko-KR', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric',
                          })}
                        </p>
                        <p className="text-sm text-muted-foreground">{session.duration}분</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-sm">
                        성공률{' '}
                        <span
                          className={`font-semibold ${
                            avgSuccess >= 0.7 ? 'text-success' : 'text-warning'
                          }`}
                        >
                          {Math.round(avgSuccess * 100)}%
                        </span>
                      </p>
                      <p className="text-sm text-muted-foreground">
                        촉진 {promptLevelLabels[Math.round(avgPrompt)]}
                      </p>
                    </div>
                  </div>
                  {session.notes && (
                    <p className="mt-3 rounded-lg bg-muted/50 p-3 text-sm">{session.notes}</p>
                  )}
                  <div className="mt-3 flex flex-wrap gap-2">
                    {session.trials.map((trial) => {
                      const goal = goals.find((g) => g.id === trial.goalId);
                      const rate = Math.round((trial.successes / trial.trials) * 100);
                      return (
                        <div
                          key={trial.goalId}
                          className="rounded-md bg-secondary px-2 py-1 text-xs"
                        >
                          {goal?.title}: {rate}%
                        </div>
                      );
                    })}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
>>>>>>> cd8e445276da75daae379d53706c3f23c9dd9241
    </div>
  );
}
