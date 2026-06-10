import { Navigate, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { Loading } from "@/components/common/Loading";
import { useAuth } from "@/hooks/useAuth";

export function AuthGuard({ children }: { children: ReactNode }) {
  const location = useLocation();
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) return <Loading />;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />;

  return <>{children}</>;
}

