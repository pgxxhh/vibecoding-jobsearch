'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import LoginStepEmail from '@/components/auth/LoginStepEmail';
import LoginStepVerify from '@/components/auth/LoginStepVerify';
import { useAuth } from '@/lib/auth';

export type LoginStep = 'email' | 'verify';

export default function LoginPage() {
  const [step, setStep] = useState<LoginStep>('email');
  const [challengeId, setChallengeId] = useState<string | null>(null);
  const [email, setEmail] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [resendAvailableAt, setResendAvailableAt] = useState<Date | null>(null);
  const router = useRouter();
  const { login } = useAuth();

  const handleEmailSubmitted = (
    nextChallengeId: string,
    submittedEmail: string,
    nextResendAvailableAt?: string | null,
    _devCode?: string | null,
  ) => {
    setChallengeId(nextChallengeId);
    setEmail(submittedEmail);
    setStep('verify');
    setError(null);
    if (nextResendAvailableAt) {
      const date = new Date(nextResendAvailableAt);
      setResendAvailableAt(Number.isNaN(date.getTime()) ? null : date);
    } else {
      setResendAvailableAt(null);
    }
  };

  const handleVerificationSuccess = () => {
    setError(null);
    // Refresh the auth state to get the user info
    login(''); // The session token is already stored in cookies
    router.push('/');
  };

  const handleChallengeUpdate = (nextChallengeId: string, nextResend?: string | null) => {
    setChallengeId(nextChallengeId);
    if (nextResend) {
      const date = new Date(nextResend);
      setResendAvailableAt(Number.isNaN(date.getTime()) ? null : date);
    } else {
      setResendAvailableAt(null);
    }
  };

  return (
    <div className="w-full max-w-md rounded-3xl bg-white/95 p-8 shadow-2xl shadow-pink-100">
      {step === 'email' ? (
        <LoginStepEmail onSuccess={handleEmailSubmitted} onError={setError} />
      ) : (
        <LoginStepVerify
          challengeId={challengeId}
          email={email}
          resendAvailableAt={resendAvailableAt?.toISOString() ?? null}
          onBack={() => setStep('email')}
          onSuccess={handleVerificationSuccess}
          onError={setError}
          onChallengeUpdate={handleChallengeUpdate}
        />
      )}
      {error && <p className="mt-2 text-center text-sm text-red-500">{error}</p>}
    </div>
  );
}
