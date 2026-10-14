import { useEffect, useRef, useState } from 'react';

interface DelayedValueOptions {
  delay?: number;
  minDisplay?: number;
}

export function useDelayedValue(
  value: boolean,
  options: DelayedValueOptions = {}
): boolean {
  const { delay = 300, minDisplay = 0 } = options;
  const [delayedValue, setDelayedValue] = useState(false);
  const shownAt = useRef<number | null>(null);

  useEffect(() => {
    if (value) {
      const timer = setTimeout(() => {
        shownAt.current = Date.now();
        setDelayedValue(true);
      }, delay);

      return () => clearTimeout(timer);
    }
    if (delayedValue && minDisplay > 0 && shownAt.current) {
      const elapsed = Date.now() - shownAt.current;
      const remaining = minDisplay - elapsed;

      if (remaining > 0) {
        const timer = setTimeout(() => {
          setDelayedValue(false);
          shownAt.current = null;
        }, remaining);

        return () => clearTimeout(timer);
      }
    }

    setDelayedValue(false);
    shownAt.current = null;
  }, [value, delay, minDisplay, delayedValue]);

  return delayedValue;
}