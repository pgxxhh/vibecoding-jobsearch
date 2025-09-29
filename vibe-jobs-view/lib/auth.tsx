'use client';

import React, { createContext, useContext, useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';

interface User {
  userId: string;
  email: string;
  sessionExpiresAt: string;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  error: string | null;
  login: (sessionToken: string) => void;
  logout: () => void;
  refetch: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

async function fetchUserSession(): Promise<User | null> {
  try {
    const response = await fetch('/api/auth/session', {
      method: 'GET',
      cache: 'no-store',
    });

    if (response.status === 401) {
      return null; // No session
    }

    if (!response.ok) {
      throw new Error('Failed to fetch session');
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error('Session fetch error:', error);
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient();
  
  const {
    data: user,
    isLoading: loading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['auth', 'session'],
    queryFn: fetchUserSession,
    retry: false,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });

  const login = (sessionToken: string) => {
    // The session token is already stored in cookies by the API route
    // Just refetch the user data
    refetch();
  };

  const logout = async () => {
    try {
      // Clear the session cookie by calling logout endpoint if it exists
      await fetch('/api/auth/logout', { method: 'POST' }).catch(() => {});
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      // Clear query cache and set user to null
      queryClient.setQueryData(['auth', 'session'], null);
      queryClient.invalidateQueries({ queryKey: ['auth'] });
    }
  };

  const value: AuthContextType = {
    user: user || null,
    loading,
    error: error?.message || null,
    login,
    logout,
    refetch,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}