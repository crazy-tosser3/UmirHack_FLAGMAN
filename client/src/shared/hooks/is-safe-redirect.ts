export function isSafeRedirect(path: string) {
  return (
    path.startsWith('/')
    && !path.startsWith('//')
  );
}