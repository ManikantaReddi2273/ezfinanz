import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { authApi, clearToken, getStoredToken, storeToken, type User } from "../api/client";

type AuthContextValue = {
  user: User | null;
  loading: boolean;
  setSession: (token: string, user: User) => void;
  updateUser: (user: User) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = getStoredToken();
    if (!token) {
      setLoading(false);
      return;
    }
    authApi
      .me()
      .then(setUser)
      .catch(() => {
        clearToken();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      setSession: (token, nextUser) => {
        storeToken(token);
        setUser(nextUser);
      },
      updateUser: (nextUser) => setUser(nextUser),
      logout: () => {
        clearToken();
        setUser(null);
      },
    }),
    [user, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
