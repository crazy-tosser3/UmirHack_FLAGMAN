import { useNavigate, useSearchParams } from 'react-router-dom';
import { isSafeRedirect } from './is-safe-redirect';

export function useRedirectTo(fallback = '/') {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const redirectTo = () => {
    const path = searchParams.get('redirect') ?? '/';
    const safePath = isSafeRedirect(path) ? path : fallback;
    navigate(safePath, { replace: true });
  };

  return redirectTo;
}