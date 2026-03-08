import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, Clock, ChevronLeft, ChevronRight, User } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { useApp } from '@/context/AppContext';
import {
  addDays, subDays, startOfWeek, endOfWeek, startOfMonth, endOfMonth,
  format, isSameDay, isSameMonth, eachDayOfInterval, eachMonthOfInterval,
  getDay, isToday, startOfYear, endOfYear, addMonths, subMonths,
} from 'date-fns';
import { ko } from 'date-fns/locale';
import { cn } from '@/lib/utils';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

type ViewMode = 'year' | 'month' | 'week' | 'day';

export function SessionScheduler() {
  const { sessions, children, therapists, goals, role } = useApp();
  const navigate = useNavigate();
  const [viewMode, setViewMode] = useState<ViewMode>('month');
  const [currentDate, setCurrentDate] = useState(new Date(2025, 0, 3));
  const [selectedSession, setSelectedSession] = useState<string | null>(null);

  const getSessionsForDate = (date: Date) =>
    sessions.filter(s => isSameDay(new Date(s.date), date));

  const getSessionsForMonth = (month: Date) =>
    sessions.filter(s => {
      const d = new Date(s.date);
      return d.getMonth() === month.getMonth() && d.getFullYear() === month.getFullYear();
    });

  // Month view days
  const monthDays = useMemo(() => {
    const start = startOfMonth(currentDate);
    const end = endOfMonth(currentDate);
    const days = eachDayOfInterval({ start, end });
    const startDow = getDay(start);
    const padStart = startDow === 0 ? 6 : startDow - 1;
    const prefixDays = Array.from({ length: padStart }, (_, i) => subDays(start, padStart - i));
    const endDow = getDay(end);
    const padEnd = endDow === 0 ? 0 : 7 - endDow;
    const suffixDays = Array.from({ length: padEnd }, (_, i) => addDays(end, i + 1));
    return [...prefixDays, ...days, ...suffixDays];
  }, [currentDate]);

  // Week view days
  const weekDays = useMemo(() => {
    const start = startOfWeek(currentDate, { weekStartsOn: 1 });
    const end = endOfWeek(currentDate, { weekStartsOn: 1 });
    return eachDayOfInterval({ start, end });
  }, [currentDate]);

  // Year view months
  const yearMonths = useMemo(() => {
    const start = startOfYear(currentDate);
    const end = endOfYear(currentDate);
    return eachMonthOfInterval({ start, end });
  }, [currentDate]);

  const navigate_period = (dir: 1 | -1) => {
    if (viewMode === 'year') {
      setCurrentDate(prev => new Date(prev.getFullYear() + dir, 0, 1));
    } else if (viewMode === 'month') {
      setCurrentDate(prev => new Date(prev.getFullYear(), prev.getMonth() + dir, 1));
    } else if (viewMode === 'week') {
      setCurrentDate(prev => addDays(prev, dir * 7));
    } else {
      setCurrentDate(prev => addDays(prev, dir));
    }
  };

  const getSessionDetail = (sessionId: string) => sessions.find(s => s.id === sessionId);

  const WEEKDAY_LABELS = ['월', '화', '수', '목', '금', '토', '일'];
  const HOURS = Array.from({ length: 12 }, (_, i) => i + 8); // 8AM to 7PM

  // Session detail dialog
  const detailSession = selectedSession ? getSessionDetail(selectedSession) : null;
  const detailChild = detailSession ? children.find(c => c.id === detailSession.childId) : null;
  const detailTherapist = detailSession ? therapists.find(t => t.id === detailSession.therapistId) : null;

  const renderSessionBadge = (session: typeof sessions[0], compact = false) => {
    const child = children.find(c => c.id === session.childId);
    const totalTrials = session.trials.reduce((a, t) => a + t.trials, 0);
    const totalSuccesses = session.trials.reduce((a, t) => a + t.successes, 0);
    const avgRate = totalTrials > 0 ? Math.round((totalSuccesses / totalTrials) * 100) : 0;

    return (
      <button
        key={session.id}
        onClick={(e) => { e.stopPropagation(); setSelectedSession(session.id); }}
        className={cn(
          'w-full text-left rounded-md px-1.5 py-0.5 text-[10px] font-medium truncate transition-colors',
          'bg-primary/10 text-primary hover:bg-primary/20 border border-primary/20'
        )}
        title={`${child?.name} · ${avgRate}%`}
      >
        {compact ? child?.name?.charAt(0) : child?.name}
        {!compact && <span className="ml-1 opacity-70">{avgRate}%</span>}
      </button>
    );
  };

  return (
    <>
      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <Calendar className="h-5 w-5 text-primary" />
              세션 스케줄
            </CardTitle>
            <div className="flex rounded-lg bg-muted p-0.5">
              {(['year', 'month', 'week', 'day'] as ViewMode[]).map(mode => (
                <button
                  key={mode}
                  onClick={() => setViewMode(mode)}
                  className={cn(
                    'rounded-md px-3 py-1 text-xs font-medium transition-colors',
                    viewMode === mode ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'
                  )}
                >
                  {{ year: '연', month: '월', week: '주', day: '일' }[mode]}
                </button>
              ))}
            </div>
          </div>
          <div className="flex items-center justify-between pt-2">
            <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => navigate_period(-1)}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <p className="text-sm font-semibold">
              {viewMode === 'year'
                ? format(currentDate, 'yyyy년', { locale: ko })
                : viewMode === 'month'
                ? format(currentDate, 'yyyy년 M월', { locale: ko })
                : viewMode === 'week'
                ? `${format(weekDays[0], 'M/d', { locale: ko })} ~ ${format(weekDays[6], 'M/d', { locale: ko })}`
                : format(currentDate, 'yyyy년 M월 d일 (EEEE)', { locale: ko })
              }
            </p>
            <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => navigate_period(1)}>
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </CardHeader>

        <CardContent className="p-0">
          {/* ═══ Year View ═══ */}
          {viewMode === 'year' && (
            <div className="grid grid-cols-3 sm:grid-cols-4 gap-3 p-4">
              {yearMonths.map((month, i) => {
                const monthSessions = getSessionsForMonth(month);
                const isCurrent = isSameMonth(month, new Date());
                return (
                  <button
                    key={i}
                    onClick={() => { setCurrentDate(month); setViewMode('month'); }}
                    className={cn(
                      'rounded-lg border p-3 text-center transition-colors hover:bg-muted/50',
                      isCurrent && 'border-primary/50'
                    )}
                  >
                    <p className={cn('text-sm font-semibold', isCurrent && 'text-primary')}>{format(month, 'M월')}</p>
                    <p className="text-lg font-bold mt-1">{monthSessions.length}</p>
                    <p className="text-[10px] text-muted-foreground">세션</p>
                    {/* Session dots */}
                    {monthSessions.length > 0 && (
                      <div className="flex justify-center gap-0.5 mt-1.5">
                        {monthSessions.slice(0, 5).map((_, j) => (
                          <div key={j} className="h-1.5 w-1.5 rounded-full bg-primary/60" />
                        ))}
                        {monthSessions.length > 5 && <span className="text-[8px] text-muted-foreground">+{monthSessions.length - 5}</span>}
                      </div>
                    )}
                  </button>
                );
              })}
            </div>
          )}

          {/* ═══ Month View ═══ */}
          {viewMode === 'month' && (
            <div className="px-4 pb-4">
              <div className="grid grid-cols-7 mb-1">
                {WEEKDAY_LABELS.map(d => (
                  <div key={d} className="text-center text-xs font-medium text-muted-foreground py-1">{d}</div>
                ))}
              </div>
              <div className="grid grid-cols-7 gap-px bg-border/30 rounded-lg overflow-hidden">
                {monthDays.map((day, i) => {
                  const daySessions = getSessionsForDate(day);
                  const isCurrentMonth = isSameMonth(day, currentDate);
                  const today = isToday(day);

                  return (
                    <div
                      key={i}
                      onClick={() => { setCurrentDate(day); setViewMode('day'); }}
                      className={cn(
                        'relative flex flex-col p-1.5 min-h-[72px] bg-card transition-colors cursor-pointer hover:bg-muted/50',
                        !isCurrentMonth && 'opacity-30',
                      )}
                    >
                      <span className={cn(
                        'text-xs font-medium flex items-center justify-center w-6 h-6 rounded-full self-end',
                        today && 'bg-primary text-primary-foreground',
                      )}>
                        {format(day, 'd')}
                      </span>
                      {daySessions.length > 0 && (
                        <div className="flex flex-col gap-0.5 mt-0.5 flex-1">
                          {daySessions.slice(0, 2).map(s => renderSessionBadge(s, true))}
                          {daySessions.length > 2 && (
                            <span className="text-[9px] text-muted-foreground text-center">+{daySessions.length - 2}</span>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* ═══ Week View ═══ */}
          {viewMode === 'week' && (
            <div className="px-4 pb-4">
              <div className="grid grid-cols-7 gap-px bg-border/30 rounded-lg overflow-hidden">
                {weekDays.map((day, i) => {
                  const daySessions = getSessionsForDate(day);
                  const today = isToday(day);

                  return (
                    <div
                      key={i}
                      className={cn(
                        'flex flex-col bg-card min-h-[200px]',
                      )}
                    >
                      {/* Day header */}
                      <div className={cn(
                        'text-center py-2 border-b border-border/30',
                        today && 'bg-primary/5'
                      )}>
                        <p className="text-[10px] text-muted-foreground">{WEEKDAY_LABELS[i]}</p>
                        <p className={cn(
                          'text-sm font-semibold',
                          today && 'text-primary'
                        )}>{format(day, 'd')}</p>
                      </div>
                      {/* Sessions in cell */}
                      <div className="flex flex-col gap-1 p-1 flex-1">
                        {daySessions.map(s => renderSessionBadge(s))}
                        {daySessions.length === 0 && (
                          <p className="text-[9px] text-muted-foreground text-center mt-4 opacity-50">—</p>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* ═══ Day View ═══ */}
          {viewMode === 'day' && (
            <div className="px-4 pb-4">
              {(() => {
                const daySessions = getSessionsForDate(currentDate);
                return (
                  <div className="space-y-1">
                    {HOURS.map(hour => {
                      // Simple placement: distribute sessions across morning hours
                      const sessionsAtHour = hour === 10 ? daySessions.slice(0, Math.ceil(daySessions.length / 2))
                        : hour === 14 ? daySessions.slice(Math.ceil(daySessions.length / 2))
                        : [];

                      return (
                        <div key={hour} className="flex gap-3 min-h-[48px]">
                          <div className="w-12 text-xs text-muted-foreground text-right pt-1 shrink-0">
                            {hour}:00
                          </div>
                          <div className={cn(
                            'flex-1 border-t border-border/30 py-1',
                            sessionsAtHour.length > 0 && 'border-t-primary/30'
                          )}>
                            <div className="space-y-1">
                              {sessionsAtHour.map(session => {
                                const child = children.find(c => c.id === session.childId);
                                const therapist = therapists.find(t => t.id === session.therapistId);
                                const totalTrials = session.trials.reduce((a, t) => a + t.trials, 0);
                                const totalSuccesses = session.trials.reduce((a, t) => a + t.successes, 0);
                                const avgRate = totalTrials > 0 ? Math.round((totalSuccesses / totalTrials) * 100) : 0;

                                return (
                                  <button
                                    key={session.id}
                                    onClick={() => setSelectedSession(session.id)}
                                    className="w-full text-left rounded-lg bg-primary/10 border border-primary/20 p-2 hover:bg-primary/15 transition-colors"
                                  >
                                    <div className="flex items-center justify-between">
                                      <div>
                                        <p className="text-sm font-medium text-foreground">{child?.name}</p>
                                        <p className="text-[11px] text-muted-foreground">
                                          {therapist?.name} · {session.duration}분
                                        </p>
                                      </div>
                                      <Badge className={cn('text-xs',
                                        avgRate >= 70 ? 'bg-success/10 text-success border-0' :
                                        avgRate >= 50 ? 'bg-warning/10 text-warning border-0' :
                                        'bg-destructive/10 text-destructive border-0'
                                      )}>
                                        {avgRate}%
                                      </Badge>
                                    </div>
                                  </button>
                                );
                              })}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                    {daySessions.length === 0 && (
                      <p className="text-sm text-muted-foreground text-center py-8">이 날짜에 기록된 세션이 없습니다</p>
                    )}
                  </div>
                );
              })()}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Session Detail Dialog */}
      <Dialog open={!!selectedSession} onOpenChange={() => setSelectedSession(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              세션 상세
            </DialogTitle>
          </DialogHeader>
          {detailSession && (
            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-sm font-bold text-primary">
                  {detailChild?.name?.charAt(0)}
                </div>
                <div>
                  <p className="font-semibold">{detailChild?.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {format(new Date(detailSession.date), 'yyyy년 M월 d일 (EEE)', { locale: ko })} · {detailTherapist?.name} · {detailSession.duration}분
                  </p>
                </div>
              </div>

              <div className="grid gap-2">
                {detailSession.trials.map((trial, ti) => {
                  const goal = goals.find(g => g.id === trial.goalId);
                  const rate = trial.trials > 0 ? Math.round((trial.successes / trial.trials) * 100) : 0;
                  return (
                    <div key={ti} className="flex items-center justify-between rounded-lg bg-muted/40 px-3 py-2">
                      <span className="text-sm font-medium truncate mr-2">{goal?.title || trial.goalId}</span>
                      <span className={cn(
                        'text-sm font-semibold whitespace-nowrap',
                        rate >= 70 ? 'text-success' : rate >= 50 ? 'text-warning' : 'text-destructive'
                      )}>
                        {trial.successes}/{trial.trials} ({rate}%)
                      </span>
                    </div>
                  );
                })}
              </div>

              {detailSession.notes && (
                <p className="text-sm text-muted-foreground bg-muted/20 rounded-lg px-3 py-2">
                  📝 {detailSession.notes}
                </p>
              )}

              <Button
                variant="outline" size="sm" className="w-full"
                onClick={() => { setSelectedSession(null); navigate(`/cases/${detailSession.childId}`); }}
              >
                케이스 보기
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
