'use client';

import { useEffect, useRef, useState } from 'react';
import { Button } from '@/shared/ui';

interface Props {
  challengeId: string | null;
  email: string | null;
  resendAvailableAt?: string | null;
  onBack: () => void;
  onSuccess: () => void;
  onError: (message: string | null) => void;
  onChallengeUpdate?: (challengeId: string, resendAvailableAt?: string | null) => void;
}

const CODE_LENGTH = 6;

function computeCooldown(resendAvailableAt?: string | null) {
  if (!resendAvailableAt) return 0;
  const target = new Date(resendAvailableAt);
  if (Number.isNaN(target.getTime())) return 0;
  const diff = Math.ceil((target.getTime() - Date.now()) / 1000);
  return diff > 0 ? diff : 0;
}

export default function LoginStepVerify({
  challengeId,
  email,
  resendAvailableAt,
  onBack,
  onSuccess,
  onError,
  onChallengeUpdate,
}: Props) {
  const [digits, setDigits] = useState<string[]>(Array(CODE_LENGTH).fill(''));
  const [loading, setLoading] = useState(false);
  const [cooldown, setCooldown] = useState<number>(() => computeCooldown(resendAvailableAt));
  const inputsRef = useRef<Array<HTMLInputElement | null>>([]);

  useEffect(() => {
    inputsRef.current?.[0]?.focus();
  }, []);

  useEffect(() => {
    setDigits(Array(CODE_LENGTH).fill(''));
    setCooldown(computeCooldown(resendAvailableAt));
  }, [challengeId, email, resendAvailableAt]);

  useEffect(() => {
    const timer = setInterval(() => {
      setCooldown((current) => {
        if (current <= 0) return 0;
        return current - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const code = digits.join('');

  const focusNext = (index: number) => {
    const next = inputsRef.current[index + 1];
    if (next) {
      next.focus();
      next.select();
    }
  };

  const focusPrev = (index: number) => {
    const prev = inputsRef.current[index - 1];
    if (prev) {
      prev.focus();
      prev.select();
    }
  };

  const handleChange = (value: string, index: number) => {
    if (!/^[0-9]?$/.test(value)) return;
    const next = [...digits];
    next[index] = value;
    setDigits(next);
    if (value) {
      focusNext(index);
    }
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>, index: number) => {
    if (event.key === 'Backspace' && !digits[index]) {
      focusPrev(index);
    }
  };

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onError(null);

    if (!challengeId) {
      onError('会话已过期，请重新发送验证码');
      onBack();
      return;
    }

    if (code.length !== CODE_LENGTH) {
      onError('请输入完整的 6 位验证码');
      return;
    }

    setLoading(true);
    try {
      const res = await fetch('/api/auth/verify-code', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ challengeId, code }),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data?.message ?? '验证码验证失败');
      }
      onSuccess();
    } catch (error) {
      onError(error instanceof Error ? error.message : '验证码验证失败');
    } finally {
      setLoading(false);
    }
  };

  const resend = async () => {
    if (!email) {
      onBack();
      return;
    }
    onError(null);
    setLoading(true);
    try {
      const res = await fetch('/api/auth/request-code', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ email }),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data?.message ?? '验证码发送失败');
      }
      setCooldown(computeCooldown(data?.resendAvailableAt));
      setDigits(Array(CODE_LENGTH).fill(''));
      inputsRef.current?.[0]?.focus();
      onChallengeUpdate?.(String(data.challengeId), data?.resendAvailableAt);
    } catch (error) {
      onError(error instanceof Error ? error.message : '验证码发送失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="space-y-6" onSubmit={submit}>
      <div className="text-center">
        <h1 className="text-2xl font-bold text-slate-900">Verify your Email</h1>
        <p className="mt-2 text-sm text-slate-500">
          Enter the 6-digit code we sent to <span className="font-medium text-slate-700">{email}</span>
        </p>
      </div>
      <div className="flex justify-between gap-2">
        {digits.map((digit, index) => (
          <input
            key={index}
            ref={(el) => {
              inputsRef.current[index] = el;
            }}
            value={digit}
            onChange={(event) => handleChange(event.target.value.slice(-1), index)}
            onKeyDown={(event) => handleKeyDown(event, index)}
            type="text"
            inputMode="numeric"
            maxLength={1}
            className="h-12 w-12 rounded-xl border border-slate-200 bg-white text-center text-lg font-semibold text-slate-800 transition-all duration-200 focus:border-brand-400 focus:outline-none focus:ring-4 focus:ring-brand-500/10"
          />
        ))}
      </div>
      <Button
        type="submit"
        disabled={loading || code.length !== CODE_LENGTH}
        className="h-12 w-full text-base"
      >
        {loading ? '验证中…' : 'Verify'}
      </Button>
      <div className="text-center text-sm">
        {cooldown > 0 ? (
          <span className="text-slate-400">重新发送验证码需要等待 {cooldown} 秒</span>
        ) : (
          <button
            type="button"
            onClick={resend}
            className="font-medium text-brand-600 underline-offset-2 hover:underline transition-colors"
            disabled={loading}
          >
            Resend code
          </button>
        )}
      </div>
      <button type="button" onClick={onBack} className="w-full text-center text-xs text-slate-400 underline-offset-2 hover:underline hover:text-slate-600 transition-colors">
        返回修改邮箱
      </button>
    </form>
  );
}
