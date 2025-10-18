'use client';

import { useState } from 'react';
import Input from '@/shared/ui/Input';
import Button from '@/shared/ui/Button';

interface Props {
  onSuccess: (challengeId: string, email: string, resendAvailableAt?: string | null, debugCode?: string | null) => void;
  onError: (message: string | null) => void;
}

export default function LoginStepEmail({ onSuccess, onError }: Props) {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onError(null);
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      onError('请输入有效的邮箱地址');
      return;
    }

    setLoading(true);
    try {
      const res = await fetch('/api/auth/request-code', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ email: trimmedEmail }),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data?.message ?? '发送验证码失败');
      }
      onSuccess(String(data.challengeId), trimmedEmail, data?.resendAvailableAt, data?.debugCode ?? null);
    } catch (error) {
      onError(error instanceof Error ? error.message : '发送验证码失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="space-y-6" onSubmit={submit}>
      <div className="space-y-2 text-center">
        <h1 className="text-2xl font-bold text-slate-900">Sign in or Register</h1>
        <p className="text-sm text-slate-500">Use your email to continue</p>
      </div>
      <div className="space-y-2 text-left">
        <label className="text-sm font-medium text-slate-600">Email address</label>
        <Input
          type="email"
          placeholder="you@example.com"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          disabled={loading}
          required
        />
      </div>
      <Button
        type="submit"
        variant="ghost"
        disabled={loading}
        className="h-12 w-full bg-gradient-to-r from-pink-500 to-purple-500 text-base font-semibold text-white shadow-lg shadow-pink-200 transition hover:from-pink-400 hover:to-purple-400"
      >
        {loading ? '发送中…' : 'Send Verification Code'}
      </Button>
      <p className="text-center text-xs text-slate-400">We'll email you a 6-digit verification code.</p>
    </form>
  );
}
